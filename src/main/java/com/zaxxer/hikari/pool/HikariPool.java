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

package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.IConnectionCustomizer;
import com.zaxxer.hikari.metrics.CodaHaleMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker.Context;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PoolUtilities;
import com.zaxxer.hikari.util.PropertyBeanSetter;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public final class HikariPool implements HikariPoolMBean, IBagStateListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariPool.class);

    private final DataSource dataSource;

    private final IConnectionCustomizer connectionCustomizer;
    private final HikariConfig configuration;
    private final ConcurrentBag<IHikariConnectionProxy> connectionBag;
    private final ThreadPoolExecutor addConnectionExecutor;
    private final MetricsTracker metricsTracker;

    private final boolean isAutoCommit;
    private final boolean isIsolateInternalQueries;
    private final boolean isReadOnly;
    private final boolean isRegisteredMbeans;
    private final boolean isJdbc4ConnectionTest;
    private final long leakDetectionThreshold;
    private final AtomicReference<Throwable> lastConnectionFailure;
    private final AtomicInteger totalConnections;
    private final Timer houseKeepingTimer;
    private final String catalog;
    private final String username;
    private final String password;

    private volatile boolean isShutdown;
    private int transactionIsolation;
    private boolean isDebug;

    /**
     * Construct a HikariPool with the specified configuration.
     *
     * @param configuration a HikariConfig instance
     */
    public HikariPool(HikariConfig configuration)
    {
        this(configuration, configuration.getUsername(), configuration.getPassword());
    }

    /**
     * Construct a HikariPool with the specified configuration.
     *
     * @param configuration a HikariConfig instance
     * @param username authentication username
     * @param password authentication password
     */
    public HikariPool(HikariConfig configuration, String username, String password)
    {
        this.configuration = configuration;
        this.username = username;
        this.password = password;

        this.totalConnections = new AtomicInteger();
        this.connectionBag = new ConcurrentBag<IHikariConnectionProxy>();
        this.connectionBag.addBagStateListener(this);
        this.isDebug = LOGGER.isDebugEnabled();
        this.lastConnectionFailure = new AtomicReference<Throwable>();

        this.catalog = configuration.getCatalog();
        this.connectionCustomizer = initializeCustomizer();
        this.isAutoCommit = configuration.isAutoCommit();
        this.isIsolateInternalQueries = configuration.isIsolateInternalQueries();
        this.isReadOnly = configuration.isReadOnly();
        this.isRegisteredMbeans = configuration.isRegisterMbeans();
        this.isJdbc4ConnectionTest = configuration.isJdbc4ConnectionTest();
        this.leakDetectionThreshold = configuration.getLeakDetectionThreshold();
        this.transactionIsolation = configuration.getTransactionIsolation();
        this.metricsTracker = (configuration.isRecordMetrics() ? new CodaHaleMetricsTracker(configuration.getPoolName()) : new MetricsTracker());

        this.dataSource = initializeDataSource();

        if (isRegisteredMbeans)
        {
            HikariMBeanElf.registerMBeans(configuration, this);
        }

        houseKeepingTimer = new Timer("Hikari Housekeeping Timer (pool " + configuration.getPoolName() + ")", true);

        addConnectionExecutor = PoolUtilities.createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection filler");

        fillPool();
        
        if (configuration.getIdleTimeout() > 0 || configuration.getMaxLifetime() > 0)
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
    public Connection getConnection() throws SQLException
    {
        final long start = System.currentTimeMillis();
        final long maxLife = configuration.getMaxLifetime();
        final Context context = metricsTracker.recordConnectionRequest(start);
        long timeout = configuration.getConnectionTimeout();
        try
        {
            do
            {
                IHikariConnectionProxy connection = connectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
                if (connection == null)  // We timed out... break and throw exception
                {
                    break;
                }

                connection.unclose();

                final long now = System.currentTimeMillis();
                if ((maxLife > 0 && now - connection.getCreationTime() > maxLife) || (now - connection.getLastAccess() > 1000 && !isConnectionAlive(connection, timeout)))
                {
                    closeConnection(connection);  // Throw away the dead connection, try again
                    timeout -= (System.currentTimeMillis() - start);
                    continue;
                }
                else if (leakDetectionThreshold > 0)
                {
                    connection.captureStack(leakDetectionThreshold, houseKeepingTimer);
                }

                return connection;
            }
            while (timeout > 0);

            logPoolState("Timeout failure ");
            throw new SQLException(String.format("Timeout of %dms encountered waiting for connection.", configuration.getConnectionTimeout()), lastConnectionFailure.getAndSet(null));
        }
        catch (InterruptedException e)
        {
            return null;
        }
        finally
        {
            context.stop();
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
        metricsTracker.recordConnectionUsage(System.currentTimeMillis() - connectionProxy.getLastOpenTime());

        if (!connectionProxy.isBrokenConnection() && !isShutdown)
        {
            connectionBag.requite(connectionProxy);
        }
        else
        {
            LOGGER.debug("Connection returned to pool {} is broken, or the pool is shutting down.  Closing connection.",
                configuration.getPoolName());
            closeConnection(connectionProxy);
        }
    }

    @Override
    public String toString()
    {
        return configuration.getPoolName();
    }

    public void shutdown()
    {
        if (!isShutdown)
        {
            isShutdown = true;
            houseKeepingTimer.cancel();
            addConnectionExecutor.shutdown();
    
            LOGGER.info("HikariCP pool {} is being shutdown.", configuration.getPoolName());
            logPoolState("State at shutdown ");
    
            closeIdleConnections();
    
            if (isRegisteredMbeans)
            {
                HikariMBeanElf.unregisterMBeans(configuration, this);
            }
        }
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }


    /**
     * Permanently close a connection.
     *
     * @param connectionProxy the connection to actually close
     */
    public void closeConnection(IHikariConnectionProxy connectionProxy)
    {
        try
        {
            int tc = totalConnections.decrementAndGet();
            if (tc < 0)
            {
                LOGGER.warn("Internal accounting inconsistency, totalConnections=" + tc, new Exception());
            }
            connectionProxy.realClose();
        }
        catch (SQLException e)
        {
            return;
        }
        finally
        {
            connectionBag.remove(connectionProxy);
        }
    }

    // ***********************************************************************
    //                        IBagStateListener methods
    // ***********************************************************************

    /** {@inheritDoc} */
    @Override
    public void addBagItem()
    {
        class AddConnection implements Runnable {
            public void run()
            {
                int sleepBackoff = 200;
                while (totalConnections.get() < configuration.getMaximumPoolSize())
                {
                    final int minIdle = configuration.getMinimumIdle();
                    if (minIdle != 0 && getIdleConnections() >= minIdle)
                    {
                        break;
                    }
                    else if (!addConnection())
                    {
                        PoolUtilities.quietlySleep(sleepBackoff);
                        sleepBackoff = (int) Math.min(1000f, ((float) sleepBackoff) * 1.5);
                        if (getThreadsAwaitingConnection() == 0)
                        {
                            lastConnectionFailure.set(null);
                            break;
                        }
                        continue;
                    }
                    
                    if (minIdle == 0) // This break is here so we only add one connection when demanded
                    {
                        break;
                    }
                }
            }
        }

        addConnectionExecutor.submit(new AddConnection());
    }

    // ***********************************************************************
    //                        HikariPoolMBean methods
    // ***********************************************************************

    /** {@inheritDoc} */
    @Override
    public int getActiveConnections()
    {
        return Math.min(configuration.getMaximumPoolSize(), totalConnections.get() - getIdleConnections());
    }

    /** {@inheritDoc} */
    @Override
    public int getIdleConnections()
    {
        return connectionBag.getCount(ConcurrentBag.STATE_NOT_IN_USE);
    }

    /** {@inheritDoc} */
    @Override
    public int getTotalConnections()
    {
        return totalConnections.get();
    }

    /** {@inheritDoc} */
    @Override
    public int getThreadsAwaitingConnection()
    {
        return connectionBag.getPendingQueue();
    }

    /** {@inheritDoc} */
    @Override
    public void closeIdleConnections()
    {
        List<IHikariConnectionProxy> list = connectionBag.values(ConcurrentBag.STATE_NOT_IN_USE);
        for (IHikariConnectionProxy connectionProxy : list)
        {
            if (connectionBag.reserve(connectionProxy))
            {
                closeConnection(connectionProxy);
            }
        }
    }

    // ***********************************************************************
    //                           Private methods
    // ***********************************************************************

    /**
     * Create and add a single connection to the pool.
     */
    private boolean addConnection()
    {
        Connection connection = null;
        try
        {
            // Speculative increment of totalConnections with expectation of success (first time through)
            if (totalConnections.incrementAndGet() > configuration.getMaximumPoolSize())
            {
                totalConnections.decrementAndGet();
                return true;
            }

            connection = (username == null && password == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);  

            transactionIsolation = (transactionIsolation < 0 ? connection.getTransactionIsolation() : transactionIsolation);

            if (connectionCustomizer != null)
            {
                connectionCustomizer.customize(connection);
            }

            PoolUtilities.executeSqlAutoCommit(connection, configuration.getConnectionInitSql());

            IHikariConnectionProxy proxyConnection = ProxyFactory.getProxyConnection(this, connection, transactionIsolation, isAutoCommit, isReadOnly, catalog);
            proxyConnection.resetConnectionState();
            connectionBag.add(proxyConnection);
            lastConnectionFailure.set(null);
            return true;
        }
        catch (Exception e)
        {
            // We failed, so undo speculative increment of totalConnections
            totalConnections.decrementAndGet();

            if (connection != null)
            {
                PoolUtilities.quietlyCloseConnection(connection);
            }

            lastConnectionFailure.set(e);
            if (isDebug)
            {
                LOGGER.warn("Connection attempt to database {} failed (not every attempt is logged): {}", configuration.getPoolName(), e.getMessage(), (isDebug ? e : null));
            }
            return false;
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
            timeoutMs = Math.max(1000, timeoutMs);

            boolean valid = true;
            if (isJdbc4ConnectionTest)
            {
                valid = connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
            }
            else
            {
                Statement statement = connection.createStatement();
                try
                {
                    if (configuration.getConnectionTimeout() < Integer.MAX_VALUE)
                    {
                        statement.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
                    }
                    statement.executeQuery(configuration.getConnectionTestQuery());
                }
                finally
                {
                    statement.close();
                }
            }

            if (isIsolateInternalQueries && !isAutoCommit)
            {
                connection.rollback();
            }

            return valid;
        }
        catch (SQLException e)
        {
            LOGGER.warn("Exception during keep alive check, that means the connection must be dead.", e);
            return false;
        }
    }

    /**
     * Fill the pool up to the minimum size.
     */
    private void fillPool()
    {
        if (configuration.isInitializationFailFast())
        {
            for (int maxIters = configuration.getMinimumIdle(); maxIters > 0; maxIters--)
            {
                if (!addConnection())
                {
                    throw new RuntimeException("Fail-fast during pool initialization", lastConnectionFailure.getAndSet(null));
                }
            }
        }
        else if (configuration.getMinimumIdle() > 0)
        {
            addBagItem();
        }
    }

    private DataSource initializeDataSource()
    {
        String dsClassName = configuration.getDataSourceClassName();
        if (configuration.getDataSource() == null && dsClassName != null)
        {
            try
            {
                DataSource dataSource = PoolUtilities.createInstance(dsClassName, DataSource.class);
                PropertyBeanSetter.setTargetFromProperties(dataSource, configuration.getDataSourceProperties());
                return dataSource;
            }
            catch (Exception e)
            {
                throw new RuntimeException("Could not create datasource instance: " + dsClassName, e);
            }
        }
        else if (configuration.getJdbcUrl() != null)
        {
            return new DriverDataSource(configuration.getJdbcUrl(), configuration.getDataSourceProperties(), username, password);
        }

        return configuration.getDataSource();
    }

    private IConnectionCustomizer initializeCustomizer()
    {
        if (configuration.getConnectionCustomizerClassName() != null)
        {
            try
            {
                return PoolUtilities.createInstance(configuration.getConnectionCustomizerClassName(), IConnectionCustomizer.class);
            }
            catch (Exception e)
            {
                LOGGER.error("Connection customizer could not be created", e);
            }
        }

        return null;
    }

    private void logPoolState(String... prefix)
    {
        int total = totalConnections.get();
        int idle = getIdleConnections();
        LOGGER.debug("{}Pool stats {} (total={}, inUse={}, avail={}, waiting={})", (prefix.length > 0 ? prefix[0] : ""), configuration.getPoolName(), total, total - idle, idle,
                     getThreadsAwaitingConnection());
    }

    /**
     * The house keeping task to retire idle and maxAge connections.
     */
    private class HouseKeeper extends TimerTask
    {
        @Override
        public void run()
        {
            isDebug = LOGGER.isDebugEnabled();
            houseKeepingTimer.purge();

            logPoolState("Before pool cleanup ");

            final long now = System.currentTimeMillis();
            final long idleTimeout = configuration.getIdleTimeout();
            final long maxLifetime = configuration.getMaxLifetime();

            for (IHikariConnectionProxy connectionProxy : connectionBag.values(ConcurrentBag.STATE_NOT_IN_USE))
            {
                if (connectionBag.reserve(connectionProxy))
                {
                    if ((idleTimeout > 0 && now > connectionProxy.getLastAccess() + idleTimeout)
                        ||
                        (maxLifetime > 0 && now > connectionProxy.getCreationTime() + maxLifetime))
                    {
                        closeConnection(connectionProxy);
                        continue;
                    }
                    
                    connectionBag.unreserve(connectionProxy);
                }
            }
            
            logPoolState("After pool cleanup ");

            if (getIdleConnections() < configuration.getMinimumIdle() && totalConnections.get() < configuration.getMaximumPoolSize())
            {
                addBagItem();  // TRY to maintain minimum connections
            }
        }
    }
}
