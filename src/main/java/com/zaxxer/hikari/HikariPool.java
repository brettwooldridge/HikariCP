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
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HikariPool
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariPool.class);

    private final HikariConfig configuration;
    private final LinkedTransferQueue<IHikariConnectionProxy> idleConnections;
    private final Set<IHikariConnectionProxy> inUseConnections;

    private final AtomicInteger totalConnections;
    private final AtomicInteger idleConnectionCount;
    private final DataSource dataSource;

    private final long maxLifeTime;
    private final long leakDetectionThreshold;
    private final boolean jdbc4ConnectionTest;

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
        this.idleConnections = new LinkedTransferQueue<IHikariConnectionProxy>();
        this.inUseConnections = Collections.newSetFromMap(new ConcurrentHashMap<IHikariConnectionProxy, Boolean>(configuration.getMaximumPoolSize() * 2, 0.75f, 100));
        this.leakDetectionThreshold = configuration.getLeakDetectionThreshold();

        this.maxLifeTime = configuration.getMaxLifetime();
        this.jdbc4ConnectionTest = configuration.isJdbc4ConnectionTest();

        try
        {
            Class<?> clazz = ClassLoaderUtils.loadClass(configuration.getDataSourceClassName());
            this.dataSource = (DataSource) clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not create datasource class: " + configuration.getDataSourceClassName(), e);
        }

        houseKeepingTimer = new Timer("Hikari Housekeeping Timer", true);

        long idleTimeout = configuration.getIdleTimeout();
        if (idleTimeout > 0 || maxLifeTime > 0 || leakDetectionThreshold > 0)
        {
            houseKeepingTimer.scheduleAtFixedRate(new HouseKeeper(), TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
        }

        System.setProperty("hikariProxyGeneratorType", configuration.getProxyFactoryType());

        while (totalConnections.get() < configuration.getMinimumPoolSize())
        {
            fillPool();
        }
    }

    Connection getConnection() throws SQLException
    {
        try
        {
            long timeout = configuration.getConnectionTimeout();
            final long start = System.currentTimeMillis();
            do
            {
                if (idleConnectionCount.get() == 0)
                {
                    fillPool();
                }
    
                IHikariConnectionProxy connectionProxy = idleConnections.poll(timeout, TimeUnit.MILLISECONDS);
                if (connectionProxy == null)
                {
                    LOGGER.error("Timeout of {}ms encountered waiting for connection", configuration.getConnectionTimeout());
                    throw new SQLException("Timeout of encountered waiting for connection");
                }

                idleConnectionCount.decrementAndGet();

                if (maxLifeTime > 0 && start - connectionProxy.getCreationTime() > maxLifeTime)
                {
                    // Throw away the connection
                    closeConnection(connectionProxy);
                    timeout -= (System.currentTimeMillis() - start);
                    continue;
                }

                Connection connection = (Connection) connectionProxy; 
                if (!isConnectionAlive(connection, timeout))
                {
                    // Throw away the connection, and nap for a few ms
                    closeConnection(connectionProxy);
                    Thread.sleep(50l);
                    timeout -= (System.currentTimeMillis() - start);
                    if (timeout < 0)
                    {
                        throw new SQLException("Timeout of encountered waiting for connection");    
                    }
                    continue;
                }
    
                if (leakDetectionThreshold > 0)
                {
                    connectionProxy.captureStack(configuration.getLeakDetectionThreshold(), houseKeepingTimer);
                }

                inUseConnections.add(connectionProxy);

                return connection;

            } while (true);
        }
        catch (InterruptedException e)
        {
            return null;
        }
    }

    void releaseConnection(IHikariConnectionProxy connection)
    {
        boolean existing = inUseConnections.remove(connection);
        if (existing)
        {
            connection.setLastAccess(System.currentTimeMillis());
            idleConnections.put(connection);
            idleConnectionCount.incrementAndGet();
        }
        else
        {
            // Should never happen, just a precaution
            totalConnections.decrementAndGet();
        }
    }

    int getTotalConnectionCount()
    {
        return totalConnections.get();
    }

    int getIdleConnectionCount()
    {
        return idleConnectionCount.get();
    }

    private synchronized void fillPool()
    {
        final int max = configuration.getMaximumPoolSize();
        if (totalConnections.get() >= max)
        {
            return;
        }

        final int increment = configuration.getAcquireIncrement();
        for (int i = 0; i < increment && totalConnections.get() < max; i++)
        {
            addConnection();
        }
    }

    private void addConnection()
    {
        int retries = 0;
        while (true)
        {
            try
            {
                Connection connection = dataSource.getConnection();
                IHikariConnectionProxy proxyConnection = (IHikariConnectionProxy) ProxyFactory.INSTANCE.getProxyConnection(this, connection);

                boolean alive = isConnectionAlive((Connection) proxyConnection, configuration.getConnectionTimeout());
                if (alive)
                {
                    idleConnectionCount.incrementAndGet();
                    totalConnections.incrementAndGet();
                    idleConnections.add(proxyConnection);
                    LOGGER.trace("Added connection");
                    break;
                }
            }
            catch (SQLException e)
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

    private boolean isConnectionAlive(Connection connection, long timeoutMs)
    {
        try
        {
            if (jdbc4ConnectionTest)
            {
                return connection.isValid((int) timeoutMs * 1000);
            }

            Statement statement = connection.createStatement();
            try
            {
                statement.executeQuery(configuration.getConnectionTestQuery());
                return true;
            }
            finally
            {
                statement.close();
            }
        }
        catch (SQLException e)
        {
            LOGGER.error("Exception during keep alive check.  Connection must be dead.");
            return false;
        }
    }

    private void closeConnection(IHikariConnectionProxy connectionProxy)
    {
        try
        {
            totalConnections.decrementAndGet();
            connectionProxy.getDelegate().close();
        }
        catch (SQLException e)
        {
            return;
        }
    }

    private class HouseKeeper extends TimerTask
    {
        public void run()
        {
            houseKeepingTimer.purge();

            final long now = System.currentTimeMillis();
            final long idleTimeout = configuration.getIdleTimeout();
            final int idleCount = idleConnectionCount.get();

            for (int i = 0; i < idleCount; i++)
            {
                IHikariConnectionProxy connectionProxy = idleConnections.poll();
                if (connectionProxy == null)
                {
                    break;
                }

                if ((idleTimeout > 0 && now > connectionProxy.getLastAccess() + idleTimeout)
                    ||
                    (maxLifeTime > 0 && now > connectionProxy.getCreationTime() + maxLifeTime))
                {
                    idleConnectionCount.decrementAndGet();
                    closeConnection(connectionProxy);
                }
                else
                {
                    idleConnections.add(connectionProxy);
                    idleConnectionCount.incrementAndGet();
                }
            }

            int maxIters = configuration.getMinimumPoolSize() / configuration.getAcquireIncrement();
            while (totalConnections.get() < configuration.getMinimumPoolSize() && --maxIters > 0)
            {
                fillPool();
            }            
        }
    }
}
