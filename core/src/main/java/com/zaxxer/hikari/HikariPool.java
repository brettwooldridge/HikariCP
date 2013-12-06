/*
 * Copyright (C) 2013 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.javassist.AgentRegistrationElf;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.JavassistProxyFactoryFactory;
import com.zaxxer.hikari.util.PropertyBeanSetter;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public final class HikariPool implements HikariPoolMBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariPool.class);

    private final HikariConfig configuration;
    private final LinkedTransferQueue<IHikariConnectionProxy> idleConnections;

    private final AtomicInteger totalConnections;
    private final AtomicInteger idleConnectionCount;
    private final AtomicBoolean backgroundFillQueued;
    private final DataSource dataSource;
    private final long leakDetectionThreshold;
    private final boolean jdbc4ConnectionTest;
    private final boolean delegationProxies;

    private final Timer houseKeepingTimer;

    /**
     * Construct a HikariPool with the specified configuration.
     *
     * @param configuration a HikariConfig instance
     */
    HikariPool(HikariConfig configuration)
    {
        configuration.validate();

        this.configuration = configuration;
        this.totalConnections = new AtomicInteger();
        this.idleConnectionCount = new AtomicInteger();
        this.backgroundFillQueued = new AtomicBoolean();
        this.idleConnections = new LinkedTransferQueue<IHikariConnectionProxy>();

        this.jdbc4ConnectionTest = configuration.isJdbc4ConnectionTest();
        this.leakDetectionThreshold = configuration.getLeakDetectionThreshold();

        String dsClassName = configuration.getDataSourceClassName();
        try
        {
            String shadedCodexMapping = configuration.getShadedCodexMapping();
            delegationProxies = !configuration.isUseInstrumentation() || !AgentRegistrationElf.loadTransformerAgent(dsClassName, shadedCodexMapping);
            if (delegationProxies)
            {
                LOGGER.info("Using Javassist delegate-based proxies.");
            }
            
            Class<?> clazz = this.getClass().getClassLoader().loadClass(dsClassName);
            this.dataSource = (DataSource) clazz.newInstance();
            PropertyBeanSetter.setTargetFromProperties(dataSource, configuration.getDataSourceProperties());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not create datasource class: " + dsClassName, e);
        }

        HikariMBeanElf.registerMBeans(configuration, this);

        houseKeepingTimer = new Timer("Hikari Housekeeping Timer", true);

        long idleTimeout = configuration.getIdleTimeout();
        if (idleTimeout > 0 || configuration.getMaxLifetime() > 0)
        {
            houseKeepingTimer.scheduleAtFixedRate(new HouseKeeper(), TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS.toMillis(30));
        }

        fillPool();            
    }

    /**
     * Get a connection from the pool, or timeout trying.
     *
     * @return a java.sql.Connection instance
     * @throws SQLException thrown if a timeout occurs trying to obtain a connection
     */
    Connection getConnection() throws SQLException
    {
        try
        {
            long timeout = configuration.getConnectionTimeout();
            final long start = System.currentTimeMillis();
            do
            {
                addConnections(AddConnectionStrategy.ONLY_IF_EMPTY);
    
                IHikariConnectionProxy connectionProxy = idleConnections.poll(timeout, TimeUnit.MILLISECONDS);
                if (connectionProxy == null)
                {
                	break;
                }

                idleConnectionCount.decrementAndGet();

                final long maxLifetime = configuration.getMaxLifetime();
                if (maxLifetime > 0 && start - connectionProxy._getCreationTime() > maxLifetime)
                {
                    // Throw away the connection that has passed its lifetime, try again
                    closeConnection(connectionProxy);
                    timeout -= (System.currentTimeMillis() - start);
                    continue;
                }

                connectionProxy._unclose();

                Connection connection = (Connection) connectionProxy; 
                if (!isConnectionAlive(connection, timeout))
                {
                    // Throw away the dead connection, try again
                    closeConnection(connectionProxy);
                    timeout -= (System.currentTimeMillis() - start);
                    continue;
                }
    
                if (leakDetectionThreshold > 0)
                {
                    connectionProxy._captureStack(leakDetectionThreshold, houseKeepingTimer);
                }

                connection.setAutoCommit(configuration.isAutoCommit());
                connection.clearWarnings();

                return connection;

            } while (timeout > 0);

        	String msg = String.format("Timeout of %dms encountered waiting for connection.", configuration.getConnectionTimeout());
            LOGGER.error(msg);
            throw new SQLException(msg);
        }
        catch (InterruptedException e)
        {
            return null;
        }
        finally
        {
            addConnections(AddConnectionStrategy.BACKGROUND_FILL);
        }
    }

    /**
     * Release a connection back to the pool, or permanently close it if it
     * is broken.
     *
     * @param connectionProxy the connection to release back to the pool
     */
    public void releaseConnection(IHikariConnectionProxy connectionProxy)
    {
        if (!connectionProxy._isBrokenConnection())
        {
            connectionProxy._markLastAccess();

            idleConnectionCount.incrementAndGet();
            idleConnections.put(connectionProxy);
        }
        else
        {
            closeConnection(connectionProxy);
        }
    }

    // ***********************************************************************
    //                        HikariPoolMBean methods
    // ***********************************************************************
    
    /** {@inheritDoc} */
    public int getActiveConnections()
    {
        return Math.min(configuration.getMaximumPoolSize(), totalConnections.get() - idleConnectionCount.get());
    }
    
    /** {@inheritDoc} */
    public int getIdleConnections()
    {
        return idleConnectionCount.get();
    }

    /** {@inheritDoc} */
    public int getTotalConnections()
    {
        return totalConnections.get();
    }

    /** {@inheritDoc} */
    public int getThreadsAwaitingConnection()
    {
        return idleConnections.getWaitingConsumerCount();
    }

    /** {@inheritDoc} */
    public void closeIdleConnections()
    {
        final int idleCount = idleConnectionCount.get();
        for (int i = 0; i < idleCount; i++)
        {
            IHikariConnectionProxy connectionProxy = idleConnections.poll();
            if (connectionProxy == null)
            {
                break;
            }

            idleConnectionCount.decrementAndGet();
            
            closeConnection(connectionProxy);
        }
    }

    // ***********************************************************************
    //                           Private methods
    // ***********************************************************************
    
    /**
     * Fill the pool up to the minimum size.
     */
    private void fillPool()
    {
    	// maxIters avoids an infinite loop filling the pool if no connections can be acquired
        int maxIters = configuration.getMinimumPoolSize() * configuration.getAcquireRetries();
        while (totalConnections.get() < configuration.getMinimumPoolSize() && maxIters-- > 0)
        {
            addConnection();
        }
    }

    /**
     * Add connections to the pool, not exceeding the maximum allowed.
     */
    private synchronized void addConnections(AddConnectionStrategy strategy)
    {
    	switch (strategy)
    	{
    	case ONLY_IF_EMPTY:
        	if (idleConnectionCount.get() == 0)
        	{
            	final int max = configuration.getMaximumPoolSize();
            	final int increment = configuration.getAcquireIncrement();
        		for (int i = 0; idleConnectionCount.get() < increment && i < increment && totalConnections.get() < max; i++)
        		{
        			addConnection();
        		}        	
        	}
    		break;
    	case MAINTAIN_MINIMUM:
    		final int min = configuration.getMinimumPoolSize();
        	final int max = configuration.getMaximumPoolSize();
        	final int increment = configuration.getAcquireIncrement();
        	for (int i = 0; totalConnections.get() < min && i < increment && totalConnections.get() < max; i++)
        	{
        		addConnection();
        	}        	
    		break;
    	case BACKGROUND_FILL:
    	    if (idleConnectionCount.get() == 0 && backgroundFillQueued.compareAndSet(false, true))
    	    {
    	        houseKeepingTimer.schedule(new TimerTask() {
                    public void run()
                    {
                        final int max = configuration.getMaximumPoolSize();
                        while (idleConnections.hasWaitingConsumer() && totalConnections.get() < max && idleConnections.getWaitingConsumerCount() > idleConnectionCount.get())
                        {
                            addConnection();
                        }
                        backgroundFillQueued.set(false);
                    }
    	        }, configuration.getConnectionTimeout() - 50/*ms*/);
    	    }
    	    break;
    	}
    }

    /**
     * Create and add a single connection to the pool.
     */
    private void addConnection()
    {
        int retries = 0;
        while (true)
        {
            try
            {
                Connection connection = dataSource.getConnection();
                IHikariConnectionProxy proxyConnection;
                if (delegationProxies)
                {
                    proxyConnection = (IHikariConnectionProxy) JavassistProxyFactoryFactory.getProxyFactory().getProxyConnection(connection);
                }
                else
                {
                    proxyConnection = (IHikariConnectionProxy) connection;
                }

                proxyConnection._setParentPool(this);

                boolean alive = isConnectionAlive((Connection) proxyConnection, configuration.getConnectionTimeout());
                if (!alive)
                {
                    // This will be caught below
                    throw new RuntimeException("Connection not alive, retry.");
                }

                String initSql = configuration.getConnectionInitSql();
                if (initSql != null && initSql.length() > 0)
                {
                    Statement statement = connection.createStatement();
                    try
                    {
                        statement.executeQuery(initSql);
                    }
                    finally
                    {
                        statement.close();
                    }
                }

                idleConnectionCount.incrementAndGet();
                totalConnections.incrementAndGet();
                idleConnections.add(proxyConnection);
                break;
            }
            catch (Exception e)
            {
                if (retries++ > configuration.getAcquireRetries())
                {
                    LOGGER.error("Maximum connection creation retries exceeded", e);
                    break;
                }

                try
                {
                    Thread.sleep(configuration.getAcquireRetryDelay());
                }
                catch (InterruptedException e1)
                {
                    break;
                }
            }
        }
    }

    /**
     * Check whether the connection is alive or not.
     *
     * @param connection the connection to test
     * @param timeoutMs the timeout before we consider the test a failure
     * @return true if the connection is alive, false if it is not alive or we timed out
     */
    private boolean isConnectionAlive(final Connection connection, long timeoutMs)
    {
        if (timeoutMs < 1000)
        {
            timeoutMs = 1000;
        }

        try
        {
            if (jdbc4ConnectionTest)
            {
                return connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
            }

            Statement statement = connection.createStatement();
            try
            {
                statement.executeQuery(configuration.getConnectionTestQuery());
            }
            finally
            {
                statement.close();
            }

            return true;
        }
        catch (SQLException e)
        {
            LOGGER.error("Exception during keep alive check.  Connection must be dead.");
            return false;
        }
    }

    /**
     * Permanently close a connection.
     *
     * @param connectionProxy the connection to actually close
     */
    private void closeConnection(IHikariConnectionProxy connectionProxy)
    {
        try
        {
            totalConnections.decrementAndGet();
            connectionProxy.__close();
        }
        catch (SQLException e)
        {
            return;
        }
    }

    /**
     * The house keeping task to retire idle and maxAge connections.
     */
    private class HouseKeeper extends TimerTask
    {
        public void run()
        {
            houseKeepingTimer.purge();

            final long now = System.currentTimeMillis();
            final long idleTimeout = configuration.getIdleTimeout();
            final long maxLifetime = configuration.getMaxLifetime();
            final int idleCount = idleConnectionCount.get();

            for (int i = 0; i < idleCount; i++)
            {
                IHikariConnectionProxy connectionProxy = idleConnections.poll();
                if (connectionProxy == null)
                {
                    break;
                }

                idleConnectionCount.decrementAndGet();

                if ((idleTimeout > 0 && now > connectionProxy._getLastAccess() + idleTimeout)
                    ||
                    (maxLifetime > 0 && now > connectionProxy._getCreationTime() + maxLifetime))
                {
                    closeConnection(connectionProxy);
                }
                else
                {
                    idleConnectionCount.incrementAndGet();
                    idleConnections.add(connectionProxy);
                }
            }

            addConnections(AddConnectionStrategy.MAINTAIN_MINIMUM);
        }
    }

    private static enum AddConnectionStrategy
    {
    	ONLY_IF_EMPTY,
    	BACKGROUND_FILL,
    	MAINTAIN_MINIMUM
    }
}
