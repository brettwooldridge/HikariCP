package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.util.UtilityElf.createInstance;
import static com.zaxxer.hikari.util.UtilityElf.createThreadPoolExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PropertyBeanSetter;

public final class PoolUtilities
{
   private static final Logger LOGGER = LoggerFactory.getLogger(PoolUtilities.class);

   private final ThreadPoolExecutor executorService;

   private String poolName;
   private volatile boolean isValidChecked; 
   private volatile boolean isValidSupported;
   private boolean isNetworkTimeoutSupported;
   private boolean isQueryTimeoutSupported;

   public PoolUtilities(final HikariConfig configuration)
   {
      this.poolName = configuration.getPoolName();
      this.isValidSupported = true;
      this.isNetworkTimeoutSupported = true;
      this.isQueryTimeoutSupported = true;
      this.executorService = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP utility thread (pool " + poolName + ")", configuration.getThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
   }

   /**
    * Close connection and eat any exception.
    *
    * @param connection the connection to close
    */
   public void quietlyCloseConnection(final Connection connection)
   {
      if (connection != null) {
         try {
            setNetworkTimeout(connection, TimeUnit.SECONDS.toMillis(30), true);
            connection.close();
         }
         catch (Exception e) {
            LOGGER.debug("{} - Exception closing connection {}", poolName, connection.toString(), e);
         }
      }
   }

   /**
    * Execute the user-specified init SQL.
    *
    * @param connection the connection to initialize
    * @param sql the SQL to execute
    * @param isAutoCommit whether to commit the SQL after execution or not
    * @throws SQLException throws if the init SQL execution fails
    */
   public void executeSql(final Connection connection, final String sql, final boolean isAutoCommit) throws SQLException
   {
      if (sql != null) {
         Statement statement = connection.createStatement();
         try {
            statement.execute(sql);
            if (!isAutoCommit) {
               connection.commit();
            }
         }
         finally {
            statement.close();
         }
      }
   }

   /**
    * Create/initialize the underlying DataSource.
    *
    * @param dsClassName a DataSource class name (optional)
    * @param dataSource a DataSource instance (optional)
    * @param dataSourceProperties a Properties instance of DataSource properties
    * @param jdbcUrl a JDBC connection URL (optional)
    * @param username a username (optional)
    * @param password a password (optional)
    * @return a DataSource instance
    */
   public DataSource initializeDataSource(final String dsClassName, DataSource dataSource, final Properties dataSourceProperties, final String jdbcUrl, final String username, final String password)
   {
      if (dataSource == null && dsClassName != null) {
         dataSource = createInstance(dsClassName, DataSource.class);
         PropertyBeanSetter.setTargetFromProperties(dataSource, dataSourceProperties);
         return dataSource;
      }
      else if (jdbcUrl != null) {
         return new DriverDataSource(jdbcUrl, dataSourceProperties, username, password);
      }

      return dataSource;
   }

   /**
    * Setup a connection intial state.
    *
    * @param connection a Connection
    * @param isAutoCommit auto-commit state
    * @param isReadOnly read-only state
    * @param transactionIsolation transaction isolation
    * @param catalog default catalog
    * @throws SQLException thrown from driver
    */
   public void setupConnection(final Connection connection, final boolean isAutoCommit, final boolean isReadOnly, final int transactionIsolation, final String catalog) throws SQLException
   {
      connection.setAutoCommit(isAutoCommit);
      connection.setReadOnly(isReadOnly);
      if (transactionIsolation != connection.getTransactionIsolation()) {
         connection.setTransactionIsolation(transactionIsolation);
      }
      if (catalog != null) {
         connection.setCatalog(catalog);
      }
   }

   /**
    * Return true if the driver appears to be JDBC 4.0 compliant.
    *
    * @param connection a Connection to check
    * @return true if JDBC 4.1 compliance, false otherwise
    * @throws SQLException re-thrown exception from Connection.getNetworkTimeout()
    */
   public boolean isJdbc4ValidationSupported(final Connection connection)
   {
      if (!isValidChecked) {
         try {
            connection.isValid(5);  // This will throw various exceptions in the case of a non-JDBC 4.0 compliant driver
         }
         catch (Throwable e) {
            isValidSupported = false;
            LOGGER.debug("{} - JDBC4 Connection.isValid() not supported", poolName);
         }

         isValidChecked = true;
      }
      
      return isValidSupported;
   }

   /**
    * Set the query timeout, if it is supported by the driver.
    *
    * @param statement a statement to set the query timeout on
    * @param timeoutSec the number of seconds before timeout
    * @throws SQLException re-thrown exception from Statement.setQueryTimeout()
    */
   public void setQueryTimeout(final Statement statement, final int timeoutSec)
   {
      if (isQueryTimeoutSupported) {
         try {
            statement.setQueryTimeout(timeoutSec);
         }
         catch (Throwable e) {
            isQueryTimeoutSupported = false;
            LOGGER.debug("{} - Statement.setQueryTimeout() not supported", poolName);
         }
      }
   }

   /**
    * Set the network timeout, if <code>isUseNetworkTimeout</code> is <code>true</code>, and return the
    * pre-existing value of the network timeout.
    *
    * @param executor an Executor
    * @param connection the connection to set the network timeout on
    * @param timeoutMs the number of milliseconds before timeout
    * @param isUseNetworkTimeout true if the network timeout should be set, false otherwise
    * @return the pre-existing network timeout value
    * @throws SQLException thrown if the network timeout cannot be set
    */
   public int setNetworkTimeout(final Connection connection, final long timeoutMs, final boolean isUseNetworkTimeout)
   {
      if (isUseNetworkTimeout && isNetworkTimeoutSupported) {
         try {
            final int networkTimeout = connection.getNetworkTimeout();
            connection.setNetworkTimeout(executorService, (int) timeoutMs);
            return networkTimeout;
         }
         catch (Throwable e) {
            isNetworkTimeoutSupported = false;
            LOGGER.debug("{} - Connection.setNetworkTimeout() not supported", poolName);
         }
      }

      return 0;
   }

   /**
    * Set the loginTimeout on the specified DataSource.
    *
    * @param dataSource the DataSource
    * @param connectionTimeout the timeout in milliseconds
    * @param logger a logger to use for a warning
    */
   public void setLoginTimeout(final DataSource dataSource, final long connectionTimeout)
   {
      if (connectionTimeout != Integer.MAX_VALUE) {
         try {
            dataSource.setLoginTimeout((int) TimeUnit.MILLISECONDS.toSeconds(Math.max(1000L, connectionTimeout)));
         }
         catch (SQLException e) {
            LOGGER.warn("Unable to set DataSource login timeout", e);
         }
      }
   }
}
