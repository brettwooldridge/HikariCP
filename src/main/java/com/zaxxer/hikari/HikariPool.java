/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.PropertyBeanSetter;
import com.zaxxer.hikari.util.ConcurrentBag;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public final class HikariPool implements HikariPoolMBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariPool.class);

    final DataSource dataSource;

    private final IConnectionCustomizer connectionCustomizer;
    private final HikariConfig configuration;
    private final ConcurrentBag<IHikariConnectionProxy> idleConnectionBag;

    private final Timer houseKeepingTimer;    

    private final long leakDetectionThreshold;
    private final AtomicBoolean backgroundFillQueued;
    private final AtomicInteger idleConnectionCount;
    private final AtomicInteger totalConnections;
    private final boolean isAutoCommit;
    private final boolean jdbc4ConnectionTest;
    private final boolean isRegisteredMbeans;
    private final String catalog; 
    private int transactionIsolation;
    private volatile boolean shutdown;
    private boolean debug;

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
        this.idleConnectionBag = new ConcurrentBag<IHikariConnectionProxy>();

        this.jdbc4ConnectionTest = configuration.isJdbc4ConnectionTest();
        this.leakDetectionThreshold = configuration.getLeakDetectionThreshold();
        this.isAutoCommit = configuration.isAutoCommit();
        this.isRegisteredMbeans = configuration.isRegisterMbeans();
        this.transactionIsolation = configuration.getTransactionIsolation();
        this.catalog = configuration.getCatalog();
        this.debug = LOGGER.isDebugEnabled();

        if (configuration.getDataSource() == null)
        {
            String dsClassName = configuration.getDataSourceClassName();
            try
            {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(dsClassName);
                this.dataSource = (DataSource) clazz.newInstance();
                PropertyBeanSetter.setTargetFromProperties(dataSource, configuration.getDataSourceProperties());
            }
            catch (Exception e)
            {
                throw new RuntimeException("Could not create datasource instance: " + dsClassName, e);
            }
        }
        else
        {
            this.dataSource = configuration.getDataSource();
        }

        if (configuration.getConnectionCustomizerClassName() != null)
        {
            try
            {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(configuration.getConnectionCustomizerClassName());
                this.connectionCustomizer = (IConnectionCustomizer) clazz.newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Could not load connection customization class", e);
            }
        }
        else
        {
            this.connectionCustomizer = null;
        }

        if (isRegisteredMbeans)
        {
            HikariMBeanElf.registerMBeans(configuration, this);
        }

        houseKeepingTimer = new Timer("Hikari Housekeeping Timer", true);
        
        fillPool();            

        long idleTimeout = configuration.getIdleTimeout();
        if (idleTimeout > 0 || configuration.getMaxLifetime() > 0)
        {
            long delayPeriod = Long.getLong("com.zaxxer.hikari.housekeeping.period", TimeUnit.SECONDS.toMillis(30));
            houseKeepingTimer.scheduleAtFixedRate(new HouseKeeper(), delayPeriod, delayPeriod);
        }
    }

    /**
     * Get a connection from the pool, or timeout trying.
     *
     * @return a java.sql.Connection instance
     * @throws SQLException thrown if a timeout occurs trying to obtain a connection
     */
    Connection getConnection() throws SQLException
    {
        if (shutdown)
        {
            throw new SQLException("Pool has been shutdown");
        }

        // Speculatively decrement idle count
        final int idleCount = idleConnectionCount.getAndDecrement();
        if (idleCount <= 0)
        {
            addConnections(AddConnectionStrategy.ONLY_IF_EMPTY);
        }
        
        try
        {
            long timeout = configuration.getConnectionTimeout();
            final long start = System.currentTimeMillis();
            do
            {
                IHikariConnectionProxy connectionProxy = idleConnectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
                if (connectionProxy == null)
                {
                    // We timed out... break and throw exception
                	break;
                }

                connectionProxy.unclose();

                if (System.currentTimeMillis() - connectionProxy.getLastAccess() > 1000 && !isConnectionAlive(connectionProxy, timeout))
                {
                    // Throw away the dead connection, try again
                    closeConnection(connectionProxy);
                    timeout -= (System.currentTimeMillis() - start);
                    continue;
                }
    
                if (leakDetectionThreshold > 0)
                {
                    connectionProxy.captureStack(leakDetectionThreshold, houseKeepingTimer);
                }

                return connectionProxy;

            } while (timeout > 0);

            // Undo speculative decrement of idle count
            idleConnectionCount.incrementAndGet();

            logPoolState();

        	String msg = String.format("Timeout of %dms encountered waiting for connection.", configuration.getConnectionTimeout());
            LOGGER.error(msg);
            logPoolState("Timeout failure ");

            throw new SQLException(msg);
        }
        catch (InterruptedException e)
        {
            return null;
        }
        finally
        {
    	    if (idleCount <= 1 && backgroundFillQueued.compareAndSet(false, true))
    	    {
    	    	addConnections(AddConnectionStrategy.BACKGROUND_FILL);
    	    }
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
        if (!connectionProxy.isBrokenConnection() && !shutdown)
        {
            idleConnectionCount.incrementAndGet();
            idleConnectionBag.requite(connectionProxy);
        }
        else
        {
            LOGGER.debug("Connection returned to pool is broken, or the pool is shutting down.  Closing connection.");
            closeConnection(connectionProxy);
        }
    }

    @Override
    public String toString()
    {
        return configuration.getPoolName();
    }

    void shutdown()
    {
        LOGGER.info("HikariCP pool " + configuration.getPoolName() + " is being shutdown.");

        shutdown = true;
        houseKeepingTimer.cancel();

        closeIdleConnections();

        if (isRegisteredMbeans)
        {
            HikariMBeanElf.unregisterMBeans(configuration, this);
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
        int idleCount = idleConnectionCount.get();
        return (idleCount < 0 ? -idleCount : 0);
    }

    /** {@inheritDoc} */
    public void closeIdleConnections()
    {
        List<IHikariConnectionProxy> list = idleConnectionBag.values(ConcurrentBag.STATE_NOT_IN_USE);
        for (IHikariConnectionProxy connectionProxy : list)
        {
            if (!idleConnectionBag.reserve(connectionProxy))
            {
                continue;
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
            int beforeCount = totalConnections.get();
            addConnection();
            if (configuration.isInitializationFailFast() && beforeCount == totalConnections.get())
            {
                throw new RuntimeException("Fail-fast during pool initialization");
            }
        }

        logPoolState("Initial fill ");
    }

    /**
     * Add connections to the pool, not exceeding the maximum allowed.
     */
    private void addConnections(AddConnectionStrategy strategy)
    {
    	switch (strategy)
    	{
    	case ONLY_IF_EMPTY:
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
	        houseKeepingTimer.schedule(new TimerTask() {
                public void run()
                {
                    final int max = configuration.getMaximumPoolSize();
                    int increment = configuration.getAcquireIncrement();
                    while (increment-- > 0 && getThreadsAwaitingConnection() > 0 && totalConnections.get() < max) 
                    {
                        addConnection();
                    }
                    backgroundFillQueued.set(false);
                }
	        }, 100/*ms*/);
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
                
                if (transactionIsolation < 0)
                {
                    transactionIsolation = connection.getTransactionIsolation();
                }

                if (connectionCustomizer != null)
                {
                    connectionCustomizer.customize(connection);
                }

                IHikariConnectionProxy proxyConnection = ProxyFactory.getProxyConnection(this, connection, transactionIsolation, isAutoCommit, catalog);

                String initSql = configuration.getConnectionInitSql();
                if (initSql != null && initSql.length() > 0)
                {
                    connection.setAutoCommit(true);
                    Statement statement = connection.createStatement();
                    try
                    {
                        statement.execute(initSql);
                    }
                    finally
                    {
                    	statement.close();
                    }
                }

                if (!shutdown)
                {
                	proxyConnection.resetConnectionState();
                    idleConnectionCount.incrementAndGet();
                    totalConnections.incrementAndGet();
                    idleConnectionBag.add(proxyConnection);
                }
                break;
            }
            catch (Exception e)
            {
                if (retries++ > configuration.getAcquireRetries())
                {
                    if (debug)
                    {
                        LOGGER.error("Maximum connection creation retries exceeded: {}", e.getMessage(), e);
                    }
                    else
                    {
                        LOGGER.error("Maximum connection creation retries exceeded: {}", e.getMessage());
                    }
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
    private boolean isConnectionAlive(final IHikariConnectionProxy connection, long timeoutMs)
    {
        try
        {
            try
            {
                if (timeoutMs < 1000)
                {
                    timeoutMs = 1000;
                }

                if (jdbc4ConnectionTest)
                {
                    return connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
                }
    
                Statement statement = connection.createStatement();
                try
                {
                    statement.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
                    statement.executeQuery(configuration.getConnectionTestQuery());
                }
                finally
                {
                	statement.close();
                }
            }
            finally
            {
                if (!isAutoCommit)
                {
                    connection.commit();
                }
            }

            return true;
        }
        catch (SQLException e)
        {
            LOGGER.warn("Exception during keep alive check, that means the connection must be dead.", e);
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
            connectionProxy.realClose();
        }
        catch (SQLException e)
        {
            return;
        }
        finally
        {
            idleConnectionBag.remove(connectionProxy);
        }
    }

    private void logPoolState(String... prefix)
    {
        int total = totalConnections.get();
        int idle = idleConnectionCount.get();
        LOGGER.debug("{}Pool stats (total={}, inUse={}, avail={}, waiting={})",
                     (prefix.length > 0 ? prefix[0] : ""), total, total - idle, idle, (isRegisteredMbeans ? getThreadsAwaitingConnection() : "n/a"));
    }

    /**
     * The house keeping task to retire idle and maxAge connections.
     */
    private class HouseKeeper extends TimerTask
    {
        public void run()
        {
            debug = LOGGER.isDebugEnabled();
            houseKeepingTimer.purge();

            logPoolState("Before pool cleanup ");

            final long now = System.currentTimeMillis();
            final long idleTimeout = configuration.getIdleTimeout();
            final long maxLifetime = configuration.getMaxLifetime();

            for (IHikariConnectionProxy connectionProxy : idleConnectionBag.values(ConcurrentBag.STATE_NOT_IN_USE))
            {
                if (!idleConnectionBag.reserve(connectionProxy))
                {
                    continue;
                }

                idleConnectionCount.decrementAndGet();

                if ((idleTimeout > 0 && now > connectionProxy.getLastAccess() + idleTimeout)
                    ||
                    (maxLifetime > 0 && now > connectionProxy.getCreationTime() + maxLifetime))
                {
                    closeConnection(connectionProxy);
                }
                else
                {
                    idleConnectionCount.incrementAndGet();
                    idleConnectionBag.unreserve(connectionProxy);
                }
            }

            addConnections(AddConnectionStrategy.MAINTAIN_MINIMUM);

            logPoolState("After pool cleanup ");
        }
    }

    private static enum AddConnectionStrategy
    {
    	ONLY_IF_EMPTY,
    	BACKGROUND_FILL,
    	MAINTAIN_MINIMUM
    }
}
