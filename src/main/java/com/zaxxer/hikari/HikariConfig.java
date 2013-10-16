/**
 * 
 */
package com.zaxxer.hikari;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class HikariConfig implements HikariConfigMBean
{
    private static int poolNumber;

    private volatile int acquireIncrement;
    private volatile  int acquireRetries;
    private volatile  long acquireRetryDelay;
    private volatile  long connectionTimeout;
    private String connectionTestQuery;
    private String connectionUrl;
    private String dataSourceClassName;
    private volatile long idleTimeout;
    private boolean isJdbc4connectionTest;
    private volatile long leakDetectionThreshold;
    private volatile long maxLifetime;
    private volatile int minPoolSize;
    private volatile int maxPoolSize;
    private String poolName;

    private Properties driverProperties;

    /**
     * Default constructor
     */
    public HikariConfig()
    {
        driverProperties = new Properties();

        acquireIncrement = 5;
        acquireRetries = 3;
        acquireRetryDelay = 750;
        connectionTimeout = 5000;
        idleTimeout = TimeUnit.MINUTES.toMillis(10);
        isJdbc4connectionTest = true;
        minPoolSize = 10;
        maxPoolSize = 60;
        maxLifetime = TimeUnit.MINUTES.toMillis(30);
        poolName = "HikariPool-" + poolNumber++;
    }

    public HikariConfig(String propertyFileName)
    {
        this();

        File propFile = new File(propertyFileName);
        if (!propFile.isFile())
        {
            throw new IllegalArgumentException("Property file " + propertyFileName + " was not found.");
        }

        try
        {
            FileInputStream fis = new FileInputStream(propFile);
            Properties props = new Properties();
            props.load(fis);
        }
        catch (IOException io)
        {
            throw new RuntimeException("Error loading properties file", io);
        }
    }

    public void addDriverProperty(String propertyName, String value)
    {
        driverProperties.put(propertyName, value);
    }

    public Properties getDriverProperties()
    {
        return driverProperties;
    }

    /** {@inheritDoc} */
    public int getAcquireIncrement()
    {
        return acquireIncrement;
    }

    /** {@inheritDoc} */
    public void setAcquireIncrement(int acquireIncrement)
    {
        if (acquireIncrement < 1)
        {
            throw new IllegalArgumentException("acquireRetries cannot be less than 1");
        }
        this.acquireIncrement = acquireIncrement;
    }

    /** {@inheritDoc} */
    public int getAcquireRetries()
    {
        return acquireRetries;
    }

    /** {@inheritDoc} */
    public void setAcquireRetries(int acquireRetries)
    {
        if (acquireRetries < 0)
        {
            throw new IllegalArgumentException("acquireRetries cannot be negative");
        }
        this.acquireRetries = acquireRetries;
    }

    /** {@inheritDoc} */
    public long getAcquireRetryDelay()
    {
        return acquireRetryDelay;
    }

    /** {@inheritDoc} */
    public void setAcquireRetryDelay(long acquireRetryDelayMs)
    {
        if (acquireRetryDelayMs < 0)
        {
            throw new IllegalArgumentException("acquireRetryDelay cannot be negative");
        }
        this.acquireRetryDelay = acquireRetryDelayMs;
    }

    public String getConnectionTestQuery()
    {
        return connectionTestQuery;
    }

    public void setConnectionTestQuery(String connectionTestQuery)
    {
        this.connectionTestQuery = connectionTestQuery;
    }

    /** {@inheritDoc} */
    public long getConnectionTimeout()
    {
        return connectionTimeout;
    }

    /** {@inheritDoc} */
    public void setConnectionTimeout(long connectionTimeoutMs)
    {
        if (connectionTimeoutMs < 0)
        {
            throw new IllegalArgumentException("connectionTimeout cannot be negative");
        }
        if (connectionTimeoutMs == 0)
        {
            this.connectionTimeout = Integer.MAX_VALUE;
        }
        this.connectionTimeout = connectionTimeoutMs;
    }

    public String getConnectionUrl()
    {
        return connectionUrl;
    }

    public void setConnectionUrl(String url)
    {
        this.connectionUrl = url;
    }

    public String getDataSourceClassName()
    {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String className)
    {
        this.dataSourceClassName = className;
    }

    /** {@inheritDoc} */
    public long getIdleTimeout()
    {
        return idleTimeout;
    }

    /** {@inheritDoc} */
    public void setIdleTimeout(long idleTimeoutMs)
    {
        this.idleTimeout = idleTimeoutMs;
    }

    public boolean isJdbc4ConnectionTest()
    {
        return isJdbc4connectionTest;
    }

    public void setJdbc4ConnectionTest(boolean useIsValid)
    {
        this.isJdbc4connectionTest = useIsValid;
    }

    /** {@inheritDoc} */
    public long getLeakDetectionThreshold()
    {
        return leakDetectionThreshold;
    }

    /** {@inheritDoc} */
    public void setLeakDetectionThreshold(long leakDetectionThresholdMs)
    {
        this.leakDetectionThreshold = leakDetectionThresholdMs; 
    }

    /** {@inheritDoc} */
    public long getMaxLifetime()
    {
        return maxLifetime;
    }

    /** {@inheritDoc} */
    public void setMaxLifetime(long maxLifetimeMs)
    {
        this.maxLifetime = maxLifetimeMs;
    }

    /** {@inheritDoc} */
    public int getMinimumPoolSize()
    {
        return minPoolSize;
    }

    /** {@inheritDoc} */
    public void setMinimumPoolSize(int minPoolSize)
    {
        if (minPoolSize < 0)
        {
            throw new IllegalArgumentException("minPoolSize cannot be negative");
        }
        this.minPoolSize = minPoolSize;
    }

    /** {@inheritDoc} */
    public int getMaximumPoolSize()
    {
        return maxPoolSize;
    }

    /** {@inheritDoc} */
    public void setMaximumPoolSize(int maxPoolSize)
    {
        if (maxPoolSize < 0)
        {
            throw new IllegalArgumentException("maxPoolSize cannot be negative");
        }
        this.maxPoolSize = maxPoolSize;
    }

    /** {@inheritDoc} */
    public String getPoolName()
    {
        return poolName;
    }

    /**
     * Set the name of the connection pool.  This is primarily used for the MBean
     * to uniquely identify the pool configuration.
     *
     * @param poolName the name of the connection pool to use
     */
    public void setPoolName(String poolName)
    {
        this.poolName = poolName;
    }

    public void validate()
    {
        Logger logger = LoggerFactory.getLogger(getClass());

        if (!isJdbc4connectionTest && connectionTestQuery == null)
        {
            logger.error("Either jdbc4ConnectionTest must be enabled or a connectionTestQuery must be specified.");
            throw new IllegalStateException("Either jdbc4ConnectionTest must be enabled or a connectionTestQuery must be specified.");
        }

        if (minPoolSize < 0)
        {
            logger.error("minPoolSize cannot be negative.");
            throw new IllegalStateException("minPoolSize cannot be negative.");
        }

        if (maxLifetime < 0)
        {
            logger.error("maxLifetime cannot be negative.");
            throw new IllegalStateException("maxLifetime cannot be negative.");
        }

        if (idleTimeout < 0)
        {
            logger.error("idleTimeout cannot be negative.");
            throw new IllegalStateException("idleTimeout cannot be negative.");
        }

        if (acquireRetryDelay < 0)
        {
            logger.error("acquireRetryDelay cannot be negative.");
            throw new IllegalStateException("acquireRetryDelay cannot be negative.");
        }

        if (maxPoolSize < minPoolSize)
        {
            logger.warn("maxPoolSize is less than minPoolSize, forcing them equal.");
            maxPoolSize = minPoolSize;
        }

        if (connectionTimeout == Integer.MAX_VALUE)
        {
            logger.warn("No connection wait timeout is set, this might cause an infinite wait.");
        }
    }
}
