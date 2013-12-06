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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.util.PropertyBeanSetter;

public final class HikariConfig implements HikariConfigMBean
{
	private static final long CONNECTION_TIMEOUT = 5000L;
	private static final long ACQUIRE_RETRY_DELAY = 750L;
	private static final long IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
	private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);

    private static int poolNumber;

    // Properties changeable at runtime through the MBean
    //
    private volatile int acquireIncrement;
    private volatile  int acquireRetries;
    private volatile  long acquireRetryDelay;
    private volatile  long connectionTimeout;
    private volatile long idleTimeout;
    private volatile long leakDetectionThreshold;
    private volatile long maxLifetime;
    private volatile int minPoolSize;
    private volatile int maxPoolSize;

    // Properties NOT changeable at runtime
    //
    private String poolName;
    private String connectionTestQuery;
    private String dataSourceClassName;
    private String shadedCodexMapping;
    private String connectionInitSql;
    private boolean isJdbc4connectionTest;
    private boolean isAutoCommit;
    private boolean isUseInstrumentation;
    private Properties dataSourceProperties;

    /**
     * Default constructor
     */
    public HikariConfig()
    {
        dataSourceProperties = new Properties();

        acquireIncrement = 1;
        acquireRetries = 3;
        acquireRetryDelay = ACQUIRE_RETRY_DELAY;
        connectionTimeout = CONNECTION_TIMEOUT;
        idleTimeout = IDLE_TIMEOUT;
        isAutoCommit = true;
        isJdbc4connectionTest = true;
        isUseInstrumentation = true;
        minPoolSize = 10;
        maxPoolSize = 60;
        maxLifetime = MAX_LIFETIME;
        poolName = "HikariPool-" + poolNumber++;
    }

    /**
     * Construct a HikariConfig from the specified property file name.
     *
     * @param propertyFileName the name of the property file
     */
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
            PropertyBeanSetter.setTargetFromProperties(this, props);
        }
        catch (IOException io)
        {
            throw new RuntimeException("Error loading properties file", io);
        }
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

    public String getConnectionInitSql()
    {
        return connectionInitSql;
    }

    public void setConnectionInitSql(String connectionInitSql)
    {
        this.connectionInitSql = connectionInitSql;
    }

    /** {@inheritDoc} */
    public long getConnectionTimeout()
    {
        return connectionTimeout;
    }

    /** {@inheritDoc} */
    public void setConnectionTimeout(long connectionTimeoutMs)
    {
        if (connectionTimeoutMs < 100)
        {
            throw new IllegalArgumentException("connectionTimeout cannot be less than 100ms");
        }
        if (connectionTimeoutMs == 0)
        {
            this.connectionTimeout = Integer.MAX_VALUE;
        }
        this.connectionTimeout = connectionTimeoutMs;
    }

    public String getDataSourceClassName()
    {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String className)
    {
        this.dataSourceClassName = className;
    }

    public void addDataSourceProperty(String propertyName, Object value)
    {
        dataSourceProperties.put(propertyName, value);
    }

    public Properties getDataSourceProperties()
    {
        return dataSourceProperties;
    }

    public void setDataSourceProperties(Properties dsProperties)
    {
        dataSourceProperties.putAll(dsProperties);
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

    public boolean isAutoCommit()
    {
        return isAutoCommit;
    }

    public void setAutoCommit(boolean isAutoCommit)
    {
        this.isAutoCommit = isAutoCommit;
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

    public boolean isUseInstrumentation()
    {
        return isUseInstrumentation;
    }

    public void setUseInstrumentation(boolean useInstrumentation)
    {
        this.isUseInstrumentation = useInstrumentation;
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

    public String getShadedCodexMapping()
    {
        return shadedCodexMapping;
    }

    /**
     * Set a package mapping for "shaded" drivers so that we can find the DataSource
     * in the instrumentation codex even though the package/class name is different.
     * The mapping should be of the form:<p>
     *    &lt;original package>:&lt;shaded package>
     * <p>
     * Where the original package name is a widely scoped as possible while still being
     * unique to the driver.  Typically, this is the first two segments of a package
     * name.  For example, take the DataSource <code>org.mariadb.jdbc.MySQLDataSource</code>,
     * that has been shaded to <code>com.other.maria.jdbc.MySQLDataSource</code>.  In this case,
     * the following mapping could be used:<p>
     *    org.mariadb:com.other.maria
     * <br>
     * @param mapping a mapping of the form: &lt;original package>:&lt;shaded package>
     */
    public void setShadedCodexMapping(String mapping)
    {
        this.shadedCodexMapping = mapping;
    }

    public void validate()
    {
        Logger logger = LoggerFactory.getLogger(getClass());

        if (acquireRetryDelay < 0)
        {
            logger.error("acquireRetryDelay cannot be negative.");
            throw new IllegalStateException("acquireRetryDelay cannot be negative.");
        }
        else if (acquireRetryDelay < 100)
        {
            logger.warn("acquireRetryDelay is less than 100ms, did you specify the wrong time unit?  Using default instead.");
            acquireRetryDelay = ACQUIRE_RETRY_DELAY;
        }

        if (connectionTimeout == Integer.MAX_VALUE)
        {
            logger.warn("No connection wait timeout is set, this might cause an infinite wait.");
        }
        else if (connectionTimeout < 100)
        {
            logger.warn("connectionTimeout is less than 100ms, did you specify the wrong time unit?  Using default instead.");
        	connectionTimeout = CONNECTION_TIMEOUT;
        }

        if (idleTimeout < 0)
        {
            logger.error("idleTimeout cannot be negative.");
            throw new IllegalStateException("idleTimeout cannot be negative.");
        }
        else if (idleTimeout < 30000)
        {
            logger.warn("idleTimeout is less than 30000ms, did you specify the wrong time unit?  Using default instead.");
            idleTimeout = IDLE_TIMEOUT;
        }

        if (!isJdbc4connectionTest && connectionTestQuery == null)
        {
            logger.error("Either jdbc4ConnectionTest must be enabled or a connectionTestQuery must be specified.");
            throw new IllegalStateException("Either jdbc4ConnectionTest must be enabled or a connectionTestQuery must be specified.");
        }

        if (leakDetectionThreshold != 0 && leakDetectionThreshold < 10000)
        {
            logger.warn("leakDetectionThreshold is less than 10000ms, did you specify the wrong time unit?  Disabling leak detection.");
            leakDetectionThreshold = 0;
        }

        if (minPoolSize < 0)
        {
            logger.error("minPoolSize cannot be negative.");
            throw new IllegalStateException("minPoolSize cannot be negative.");
        }

        if (maxPoolSize < minPoolSize)
        {
            logger.warn("maxPoolSize is less than minPoolSize, forcing them equal.");
            maxPoolSize = minPoolSize;
        }

        if (maxLifetime < 0)
        {
            logger.error("maxLifetime cannot be negative.");
            throw new IllegalStateException("maxLifetime cannot be negative.");
        }
        else if (maxLifetime < 120000)
        {
            logger.warn("maxLifetime is less than 120000ms, did you specify the wrong time unit?  Using default instead.");
            maxLifetime = MAX_LIFETIME;
        }
    }
}
