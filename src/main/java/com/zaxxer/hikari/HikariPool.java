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
import java.util.concurrent.BlockingQueue;
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
    private final BlockingQueue<IHikariConnectionProxy> idleConnections;
    private final Set<IHikariConnectionProxy> inUseConnections;

    private final AtomicInteger totalConnections;
    private final AtomicInteger idleConnectionCount;
    private final DataSource dataSource;

    private final int maxLifeTime;
    private final boolean jdbc4ConnectionTest;

    /**
     * @param configuration
     */
    HikariPool(HikariConfig configuration)
    {
        configuration.validate();

        this.configuration = configuration;
        this.totalConnections = new AtomicInteger();
        this.idleConnectionCount = new AtomicInteger();
        this.idleConnections = new LinkedTransferQueue<IHikariConnectionProxy>();
        //this.inUseConnections = Collections.synchronizedSet(Collections.newSetFromMap(new LinkedHashMap<IHikariConnectionProxy, Boolean>()));
        this.inUseConnections = Collections.newSetFromMap(new ConcurrentHashMap<IHikariConnectionProxy, Boolean>(configuration.getMaximumPoolSize() * 2, 0.75f, 100));

        this.maxLifeTime = configuration.getMaxLifetimeMs();
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
            int timeout = configuration.getConnectionTimeoutMs();
            final long start = System.currentTimeMillis();
            do
            {
                if (idleConnectionCount.get() == 0 || totalConnections.get() < configuration.getMinimumPoolSize())
                {
                    fillPool();
                }
    
                IHikariConnectionProxy connectionProxy = idleConnections.poll(timeout, TimeUnit.MILLISECONDS);
                if (connectionProxy == null)
                {
                    LOGGER.error("Timeout of {}ms encountered waiting for connection", configuration.getConnectionTimeoutMs());
                    throw new SQLException("Timeout of encountered waiting for connection");
                }

                idleConnectionCount.decrementAndGet();

                if (maxLifeTime > 0 && start - connectionProxy.getCreationTime() > maxLifeTime)
                {
                    // Throw away the connection
                    totalConnections.decrementAndGet();
                    closeConnection(connectionProxy);
                    continue;
                }

                Connection connection = (Connection) connectionProxy; 
                if (!isConnectionAlive(connection, timeout))
                {
                    // Throw away the connection, and nap for a few ms
                    totalConnections.decrementAndGet();
                    Thread.sleep(50l);
                    timeout -= (System.currentTimeMillis() - start);
                    if (timeout < 0)
                    {
                        throw new SQLException("Timeout of encountered waiting for connection");    
                    }
                    continue;
                }
    
                if (configuration.getLeakDetectionThresholdMs() > 0)
                {
                    connectionProxy.captureStack();
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
            idleConnections.add(connection);
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
                IHikariConnectionProxy connection = createConnection();
                boolean alive = isConnectionAlive((Connection) connection, configuration.getConnectionTimeoutMs());
                if (alive)
                {
                    idleConnectionCount.incrementAndGet();
                    totalConnections.incrementAndGet();
                    idleConnections.add(connection);
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
                    Thread.sleep(configuration.getAcquireRetryDelayMs());
                }
                catch (InterruptedException e1)
                {
                    break;
                }
            }
        }
    }

    private boolean isConnectionAlive(Connection connection, int timeoutMs)
    {
        try
        {
            if (jdbc4ConnectionTest)
            {
                return connection.isValid(timeoutMs * 1000);
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
            connectionProxy.getDelegate().close();
        }
        catch (SQLException e)
        {
            return;
        }
    }
    
    private IHikariConnectionProxy createConnection() throws SQLException
    {
        Connection connection = dataSource.getConnection();
        return (IHikariConnectionProxy) ProxyFactory.INSTANCE.getProxyConnection(this, connection);
    }
}
