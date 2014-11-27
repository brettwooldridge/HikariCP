package com.zaxxer.hikari.util;

import static com.zaxxer.hikari.util.UtilityElf.createInstance;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;

public final class PoolUtilities
{
   private volatile boolean IS_JDBC40;
   private volatile boolean IS_JDBC41;
   private volatile boolean jdbc40checked; 
   private volatile boolean jdbc41checked; 
   private volatile boolean queryTimeoutSupported = true;

   /**
    * Close connection and eat any exception.
    *
    * @param connection the connection to close
    */
   public void quietlyCloseConnection(final Connection connection)
   {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (SQLException e) {
            return;
         }
      }
   }

   /**
    * Execute the user-specified init SQL.
    *
    * @param connection the connection to initialize
    * @param sql the SQL to execute
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
    * @return the DataSource
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
   public boolean isJdbc40Compliant(final Connection connection) throws SQLException
   {
      if (!jdbc40checked) {
         try {
            connection.isValid(5);  // This will throw AbstractMethodError or SQLException in the case of a non-JDBC 41 compliant driver
            IS_JDBC40 = true;
         }
         catch (SQLFeatureNotSupportedException e) {
            IS_JDBC40 = false;
         }
         catch (AbstractMethodError e) {
            IS_JDBC40 = false;
         }
         catch (NoSuchMethodError e) {
            IS_JDBC40 = false;
         }

         jdbc40checked = true;
      }
      
      return IS_JDBC40;
   }

   /**
    * Set the query timeout, if it is supported by the driver.
    *
    * @param statement a statement to set the query timeout on
    * @param timeoutSec the number of seconds before timeout
    * @throws SQLException re-thrown exception from Statement.setQueryTimeout()
    */
   public void setQueryTimeout(final Statement statement, final int timeoutSec) throws SQLException
   {
      if (queryTimeoutSupported) {
         try {
            statement.setQueryTimeout(timeoutSec);
         }
         catch (SQLFeatureNotSupportedException e) {
            queryTimeoutSupported = false;
         }
         catch (AbstractMethodError e) {
            queryTimeoutSupported = false;
         }
         catch (NoSuchMethodError e) {
            queryTimeoutSupported = false;
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
   public int setNetworkTimeout(final Executor executor, final Connection connection, final long timeoutMs, final boolean isUseNetworkTimeout) throws SQLException
   {
      if ((IS_JDBC41 || !jdbc41checked) && isUseNetworkTimeout) {
         try {
            final int networkTimeout = connection.getNetworkTimeout();
            connection.setNetworkTimeout(executor, (int) timeoutMs);
            IS_JDBC41 = true;
            return networkTimeout;
         }
         catch (SQLFeatureNotSupportedException e) {
            IS_JDBC41 = false;
         }
         catch (AbstractMethodError e) {
            IS_JDBC41 = false;
         }
         catch (NoSuchMethodError e) {
            IS_JDBC41 = false;
         }
         finally {
            jdbc41checked = true;
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
   public void setLoginTimeout(final DataSource dataSource, final long connectionTimeout, final Logger logger)
   {
      if (connectionTimeout != Integer.MAX_VALUE) {
         try {
            dataSource.setLoginTimeout((int) TimeUnit.MILLISECONDS.toSeconds(Math.max(1000L, connectionTimeout)));
         }
         catch (SQLException e) {
            logger.warn("Unable to set DataSource login timeout", e);
         }
      }
   }
}
