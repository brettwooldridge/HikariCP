/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.util.PropertyBeanSetter;
import com.zaxxer.hikari.util.UtilityElf;

public abstract class AbstractHikariConfig implements HikariConfigMBean
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HikariConfig.class);

   private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
   private static final long IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
   private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);

   private static int poolNumber;
   private static boolean unitTest;

   // Properties changeable at runtime through the MBean
   //
   private volatile long connectionTimeout;
   private volatile long idleTimeout;
   private volatile long leakDetectionThreshold;
   private volatile long maxLifetime;
   private volatile int maxPoolSize;
   private volatile int minIdle;

   // Properties NOT changeable at runtime
   //
   private String catalog;
   private String connectionCustomizerClassName;
   private String connectionInitSql;
   private String connectionTestQuery;
   private String dataSourceClassName;
   private String dataSourceJndiName;
   private String driverClassName;
   private String jdbcUrl;
   private String password;
   private String poolName;
   private String transactionIsolationName;
   private String username;
   private boolean isAutoCommit;
   private boolean isReadOnly;
   private boolean isInitializationFailFast;
   private boolean isIsolateInternalQueries;
   private boolean isRegisterMbeans;
   private boolean isAllowPoolSuspension;
   private DataSource dataSource;
   private Properties dataSourceProperties;
   private IConnectionCustomizer customizer;
   private ThreadFactory threadFactory;
   private Object metricRegistry;

   /**
    * Default constructor
    */
   public AbstractHikariConfig()
   {
      dataSourceProperties = new Properties();

      connectionTimeout = CONNECTION_TIMEOUT;
      idleTimeout = IDLE_TIMEOUT;
      isAutoCommit = true;
      isInitializationFailFast = true;
      minIdle = -1;
      maxPoolSize = 10;
      maxLifetime = MAX_LIFETIME;
      customizer = new IConnectionCustomizer() {
         @Override
         public void customize(Connection connection) throws SQLException
         {
         }
      };

      String systemProp = System.getProperty("hikaricp.configurationFile");
      if ( systemProp != null) {
         loadProperties(systemProp);
      }
   }

   /**
    * Construct a HikariConfig from the specified properties object.
    *
    * @param properties the name of the property file
    */
   public AbstractHikariConfig(Properties properties)
   {
      this();
      PropertyBeanSetter.setTargetFromProperties(this, properties);
   }

   /**
    * Construct a HikariConfig from the specified property file name.  <code>propertyFileName</code>
    * will first be treated as a path in the file-system, and if that fails the 
    * ClassLoader.getResourceAsStream(propertyFileName) will be tried.
    *
    * @param propertyFileName the name of the property file
    */
   public AbstractHikariConfig(String propertyFileName)
   {
      this();

      loadProperties(propertyFileName);
   }

   /**
    * Get the default catalog name to be set on connections.
    *
    * @return the default catalog name
    */
   public String getCatalog()
   {
      return catalog;
   }

   /**
    * Set the default catalog name to be set on connections.
    *
    * @param catalog the catalog name, or null
    */
   public void setCatalog(String catalog)
   {
      this.catalog = catalog;
   }

   /**
    * Get the name of the connection customizer class to instantiate and execute
    * on all new connections.
    *
    * @return the name of the customizer class, or null
    */
   public String getConnectionCustomizerClassName()
   {
      return connectionCustomizerClassName;
   }

   /**
    * Set the name of the connection customizer class to instantiate and execute
    * on all new connections.
    *
    * @param connectionCustomizerClassName the name of the customizer class
    */
   public void setConnectionCustomizerClassName(String connectionCustomizerClassName)
   {
      this.connectionCustomizerClassName = connectionCustomizerClassName;
   }

   /**
    * Get the customizer instance specified by the user.
    *
    * @return an instance of IConnectionCustomizer
    */
   public IConnectionCustomizer getConnectionCustomizer()
   {
      return customizer;
   }

   /**
    * Set the connection customizer to be used by the pool.
    *
    * @param customizer an instance of IConnectionCustomizer
    */
   public void setConnectionCustomizer(IConnectionCustomizer customizer)
   {
      this.customizer = customizer;
   }

   /**
    * Get the SQL query to be executed to test the validity of connections.
    * 
    * @return the SQL query string, or null 
    */
   public String getConnectionTestQuery()
   {
      return connectionTestQuery;
   }

   /**
    * Set the SQL query to be executed to test the validity of connections. Using
    * the JDBC4 <code>Connection.isValid()</code> method to test connection validity can
    * be more efficient on some databases and is recommended.  See 
    * {@link HikariConfig#setJdbc4ConnectionTest(boolean)}.
    *
    * @param connectionTestQuery a SQL query string
    */
   public void setConnectionTestQuery(String connectionTestQuery)
   {
      this.connectionTestQuery = connectionTestQuery;
   }

   /**
    * Get the SQL string that will be executed on all new connections when they are
    * created, before they are added to the pool.
    *
    * @return the SQL to execute on new connections, or null
    */
   public String getConnectionInitSql()
   {
      return connectionInitSql;
   }

   /**
    * Set the SQL string that will be executed on all new connections when they are
    * created, before they are added to the pool.  If this query fails, it will be
    * treated as a failed connection attempt.
    *
    * @param connectionInitSql the SQL to execute on new connections
    */
   public void setConnectionInitSql(String connectionInitSql)
   {
      this.connectionInitSql = connectionInitSql;
   }

   /** {@inheritDoc} */
   @Override
   public long getConnectionTimeout()
   {
      return connectionTimeout;
   }

   /** {@inheritDoc} */
   @Override
   public void setConnectionTimeout(long connectionTimeoutMs)
   {
      if (connectionTimeoutMs == 0) {
         this.connectionTimeout = Integer.MAX_VALUE;
      }
      else if (connectionTimeoutMs < 100) {
         throw new IllegalArgumentException("connectionTimeout cannot be less than 100ms");
      }
      else {
         this.connectionTimeout = connectionTimeoutMs;
      }
   }

   /**
    * Get the {@link DataSource} that has been explicitly specified to be wrapped by the
    * pool.
    *
    * @return the {@link DataSource} instance, or null
    */
   public DataSource getDataSource()
   {
      return dataSource;
   }

   /**
    * Set a {@link DataSource} for the pool to explicitly wrap.  This setter is not
    * available through property file based initialization.
    *
    * @param dataSource a specific {@link DataSource} to be wrapped by the pool
    */
   public void setDataSource(DataSource dataSource)
   {
      this.dataSource = dataSource;
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

   public String getDataSourceJNDI()
   {
      return this.dataSourceJndiName;
   }

   public void setDataSourceJNDI(String jndiDataSource)
   {
      this.dataSourceJndiName = jndiDataSource;
   }

   public Properties getDataSourceProperties()
   {
      return dataSourceProperties;
   }

   public void setDataSourceProperties(Properties dsProperties)
   {
      dataSourceProperties.putAll(dsProperties);
   }

   public void setDriverClassName(String driverClassName)
   {
      try {
         Class<?> driverClass = this.getClass().getClassLoader().loadClass(driverClassName);
         driverClass.newInstance();
         this.driverClassName = driverClassName;
      }
      catch (Exception e) {
         throw new RuntimeException("driverClassName specified class '" + driverClassName + "' could not be loaded", e);
      }
   }

   /** {@inheritDoc} */
   @Override
   public long getIdleTimeout()
   {
      return idleTimeout;
   }

   /** {@inheritDoc} */
   @Override
   public void setIdleTimeout(long idleTimeoutMs)
   {
      if (idleTimeoutMs < 0) {
         throw new IllegalArgumentException("idleTimeout cannot be negative");
      }
      this.idleTimeout = idleTimeoutMs;
   }

   public String getJdbcUrl()
   {
      return jdbcUrl;
   }

   public void setJdbcUrl(String jdbcUrl)
   {
      this.jdbcUrl = jdbcUrl;
   }

   /**
    * Get the default auto-commit behavior of connections in the pool.
    *
    * @return the default auto-commit behavior of connections
    */
   public boolean isAutoCommit()
   {
      return isAutoCommit;
   }

   /**
    * Set the default auto-commit behavior of connections in the pool.
    *
    * @param isAutoCommit the desired auto-commit default for connections
    */
   public void setAutoCommit(boolean isAutoCommit)
   {
      this.isAutoCommit = isAutoCommit;
   }

   /**
    * Get the pool suspension behavior (allowed or disallowed).
    *
    * @return the pool suspension behavior
    */
   public boolean isAllowPoolSuspension()
   {
      return isAllowPoolSuspension;
   }

   /**
    * Set whether or not pool suspension is allowed.  There is a performance
    * impact when pool suspension is enabled.  Unless you need it (for a
    * redundancy system for example) do not enable it.
    *
    * @param isAllowPoolSuspension the desired pool suspension allowance
    */
   public void setAllowPoolSuspension(boolean isAllowPoolSuspension)
   {
      this.isAllowPoolSuspension = isAllowPoolSuspension;
   }

   /**
    * Get whether or not the construction of the pool should throw an exception
    * if the minimum number of connections cannot be created.
    *
    * @return whether or not initialization should fail on error immediately
    */
   public boolean isInitializationFailFast()
   {
      return isInitializationFailFast;
   }

   /**
    * Set whether or not the construction of the pool should throw an exception
    * if the minimum number of connections cannot be created.
    *
    * @param failFast true if the pool should fail if the minimum connections cannot be created
    */
   public void setInitializationFailFast(boolean failFast)
   {
      isInitializationFailFast = failFast;
   }

   public boolean isIsolateInternalQueries()
   {
      return isIsolateInternalQueries;
   }

   public void setIsolateInternalQueries(boolean isolate)
   {
      this.isIsolateInternalQueries = isolate;
   }

   @Deprecated
   public boolean isJdbc4ConnectionTest()
   {
      return false;
   }

   @Deprecated
   public void setJdbc4ConnectionTest(boolean useIsValid)
   {
      // ignored deprecated property
      LOGGER.warn("The jdbcConnectionTest property is now deprecated, see the documentation for connectionTestQuery");
   }

   /**
    * Get the Codahale MetricRegistry, could be null.
    *
    * @return the codahale MetricRegistry instance
    */
   public Object getMetricRegistry()
   {
      return metricRegistry;
   }

   /**
    * Set a Codahale MetricRegistry to use for HikariCP.
    *
    * @param metricRegistry the Codahale MetricRegistry to set
    */
   public void setMetricRegistry(Object metricRegistry)
   {
      if (metricRegistry != null && !metricRegistry.getClass().getName().contains("MetricRegistry")) {
         throw new IllegalArgumentException("Class must be an instance of com.codahale.metrics.MetricRegistry");
      }
      this.metricRegistry = metricRegistry;
   }

   public boolean isReadOnly()
   {
      return isReadOnly;
   }

   public void setReadOnly(boolean readOnly)
   {
      this.isReadOnly = readOnly;
   }

   public boolean isRegisterMbeans()
   {
      return isRegisterMbeans;
   }

   public void setRegisterMbeans(boolean register)
   {
      this.isRegisterMbeans = register;
   }

   /** {@inheritDoc} */
   @Override
   public long getLeakDetectionThreshold()
   {
      return leakDetectionThreshold;
   }

   /** {@inheritDoc} */
   @Override
   public void setLeakDetectionThreshold(long leakDetectionThresholdMs)
   {
      this.leakDetectionThreshold = leakDetectionThresholdMs;
   }

   /** {@inheritDoc} */
   @Override
   public long getMaxLifetime()
   {
      return maxLifetime;
   }

   /** {@inheritDoc} */
   @Override
   public void setMaxLifetime(long maxLifetimeMs)
   {
      this.maxLifetime = maxLifetimeMs;
   }

   /** {@inheritDoc} */
   @Override
   public int getMaximumPoolSize()
   {
      return maxPoolSize;
   }

   /** {@inheritDoc} */
   @Override
   public void setMaximumPoolSize(int maxPoolSize)
   {
      if (maxPoolSize < 1) {
         throw new IllegalArgumentException("maxPoolSize cannot be less than 1");
      }
      this.maxPoolSize = maxPoolSize;
   }

   /** {@inheritDoc} */
   @Override
   public int getMinimumIdle()
   {
      return minIdle;
   }

   /** {@inheritDoc} */
   @Override
   public void setMinimumIdle(int minIdle)
   {
      if (minIdle < 0) {
         throw new IllegalArgumentException("minimumIdle cannot be negative");
      }
      this.minIdle = minIdle;
   }

   /**
    * Get the default password to use for DataSource.getConnection(username, password) calls.
    * @return the password
    */
   public String getPassword()
   {
      return password;
   }

   /**
    * Set the default password to use for DataSource.getConnection(username, password) calls.
    * @param password the password
    */
   public void setPassword(String password)
   {
      this.password = password;
   }

   /** {@inheritDoc} */
   @Override
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

   public String getTransactionIsolation()
   {
      return transactionIsolationName;
   }

   /**
    * Set the default transaction isolation level.  The specified value is the
    * constant name from the <code>Connection</code> class, eg. 
    * <code>TRANSACTION_REPEATABLE_READ</code>.
    *
    * @param isolationLevel the name of the isolation level
    */
   public void setTransactionIsolation(String isolationLevel)
   {
      this.transactionIsolationName = isolationLevel;
   }

   /**
    * Get the default username used for DataSource.getConnection(username, password) calls.
    *
    * @return the username
    */
   public String getUsername()
   {
      return username;
   }

   /**
    * Set the default username used for DataSource.getConnection(username, password) calls.
    *
    * @param username the username
    */
   public void setUsername(String username)
   {
      this.username = username;
   }

   /**
    * Get the thread factory used to create threads.
    *
    * @return the thread factory (may be null, in which case the default thread factory is used)
    */
   public ThreadFactory getThreadFactory()
   {
      return threadFactory;
   }

   /**
    * Set the thread factory to be used to create threads.
    *
    * @param threadFactory the thread factory (setting to null causes the default thread factory to be used)
    */
   public void setThreadFactory(ThreadFactory threadFactory)
   {
      this.threadFactory = threadFactory;
   }

   public void validate()
   {
      Logger logger = LoggerFactory.getLogger(getClass());

      validateNumerics();

      if (connectionCustomizerClassName != null) {
         try {
            getClass().getClassLoader().loadClass(connectionCustomizerClassName);
         }
         catch (Exception e) {
            logger.warn("connectionCustomizationClass specified class '" + connectionCustomizerClassName + "' could not be loaded", e);
            connectionCustomizerClassName = null;
         }
      }

      if (driverClassName != null && jdbcUrl == null) {
         logger.error("when specifying driverClassName, jdbcUrl must also be specified");
         throw new IllegalStateException("when specifying driverClassName, jdbcUrl must also be specified");
      }
      else if (driverClassName != null && dataSourceClassName != null) {
         logger.error("both driverClassName and dataSourceClassName are specified, one or the other should be used");
         throw new IllegalStateException("both driverClassName and dataSourceClassName are specified, one or the other should be used");
      }
      else if (jdbcUrl != null) {
         // OK
      }
      else if (dataSource == null && dataSourceClassName == null) {
         logger.error("one of either dataSource, dataSourceClassName, or jdbcUrl and driverClassName must be specified");
         throw new IllegalArgumentException("one of either dataSource or dataSourceClassName must be specified");
      }
      else if (dataSource != null && dataSourceClassName != null) {
         logger.warn("both dataSource and dataSourceClassName are specified, ignoring dataSourceClassName");
      }

      if (transactionIsolationName != null) {
         UtilityElf.getTransactionIsolation(transactionIsolationName);
      }

      if (poolName == null) {
         poolName = "HikariPool-" + poolNumber++;
      }

      if (LOGGER.isDebugEnabled() || unitTest) {
         logConfiguration();
      }
   }

   private void validateNumerics()
   {
      Logger logger = LoggerFactory.getLogger(getClass());

      if (connectionTimeout == Integer.MAX_VALUE) {
         logger.warn("No connection wait timeout is set, this might cause an infinite wait.");
      }

      if (minIdle < 0 || minIdle > maxPoolSize) {
         minIdle = maxPoolSize;
      }

      if (maxLifetime < 0) {
         logger.error("maxLifetime cannot be negative.");
         throw new IllegalArgumentException("maxLifetime cannot be negative.");
      }
      else if (maxLifetime > 0 && maxLifetime < TimeUnit.SECONDS.toMillis(120)) {
         logger.warn("maxLifetime is less than 120000ms, using default {}ms.", MAX_LIFETIME);
         maxLifetime = MAX_LIFETIME;
      }

      if (idleTimeout != 0 && idleTimeout < TimeUnit.SECONDS.toMillis(30)) {
         logger.warn("idleTimeout is less than 30000ms, using default {}ms.", IDLE_TIMEOUT);
         idleTimeout = IDLE_TIMEOUT;
      }
      else if (idleTimeout > maxLifetime && maxLifetime > 0) {
         logger.warn("idleTimeout is greater than maxLifetime, setting to maxLifetime.");
         idleTimeout = maxLifetime;
      }

      if (leakDetectionThreshold != 0 && leakDetectionThreshold < TimeUnit.SECONDS.toMillis(2) && !unitTest) {
         logger.warn("leakDetectionThreshold is less than 2000ms, setting to minimum 2000ms.");
         leakDetectionThreshold = 2000L;
      }
   }

   private void logConfiguration()
   {
      LOGGER.debug("HikariCP pool {} configuration:", poolName);
      final Set<String> propertyNames = new TreeSet<String>(PropertyBeanSetter.getPropertyNames(HikariConfig.class));
      for (String prop : propertyNames) {
         try {
            Object value = PropertyBeanSetter.getProperty(prop, this);
            if ("dataSourceProperties".equals(prop)) {
               Properties dsProps = PropertyBeanSetter.copyProperties(dataSourceProperties);
               dsProps.setProperty("password", "<masked>");
               value = dsProps;
            }
            value = (prop.contains("password") ? "<masked>" : value);
            LOGGER.debug((prop + "................................................").substring(0, 32) + (value != null ? value : ""));
         }
         catch (Exception e) {
            continue;
         }
      }
   }

   abstract protected void loadProperties(String propertyFileName);

   void copyState(AbstractHikariConfig other)
   {
      for (Field field : AbstractHikariConfig.class.getDeclaredFields()) {
         if (!Modifier.isFinal(field.getModifiers())) {
            field.setAccessible(true);
            try {
               field.set(other, field.get(this));
            }
            catch (Exception e) {
               throw new RuntimeException("Exception copying HikariConfig state: " + e.getMessage(), e);
            }
         }
      }
   }
}
