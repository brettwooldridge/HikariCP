package com.zaxxer.hikari.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;

public final class PoolUtilities
{
   private static volatile boolean IS_JDBC40;
   private static volatile boolean IS_JDBC41;
   private static volatile boolean jdbc40checked; 
   private static volatile boolean jdbc41checked; 
   private static volatile boolean queryTimeoutSupported = true;

   /**
    * Close connection and eat any exception.
    *
    * @param connection the connection to close
    */
   public static void quietlyCloseConnection(final Connection connection)
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
    * Get the elapsed time in millisecond between the specified start time and now.
    *
    * @param start the start time
    * @return the elapsed milliseconds
    */
   public static long elapsedTimeMs(final long start)
   {
      return System.currentTimeMillis() - start;
   }

   /**
    * Execute the user-specified init SQL.
    *
    * @param connection the connection to initialize
    * @param sql the SQL to execute
    * @throws SQLException throws if the init SQL execution fails
    */
   public static void executeSql(final Connection connection, final String sql, final boolean isAutoCommit) throws SQLException
   {
      if (sql != null) {
         try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            if (!isAutoCommit) {
               connection.commit();
            }
         }
      }
   }

   /**
    * Sleep and transform an InterruptedException into a RuntimeException.
    *
    * @param millis the number of milliseconds to sleep
    */
   public static void quietlySleep(final long millis)
   {
      try {
         Thread.sleep(millis);
      }
      catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Create and instance of the specified class using the constructor matching the specified
    * arguments.
    *
    * @param className the name of the classto instantiate
    * @param clazz a class to cast the result as
    * @param args arguments to a constructor
    * @return an instance of the specified class
    */
   @SuppressWarnings("unchecked")
   public static <T> T createInstance(final String className, final Class<T> clazz, final Object... args)
   {
      if (className == null) {
         return null;
      }

      try {
         Class<?> loaded = PoolUtilities.class.getClassLoader().loadClass(className);

         Class<?>[] argClasses = new Class<?>[args.length];
         for (int i = 0; i < args.length; i++) {
            argClasses[i] = args[i].getClass();
         }

         if (args.length > 0) {
            Constructor<?> constructor = loaded.getConstructor(argClasses);
            return (T) constructor.newInstance(args);
         }

         return (T) loaded.newInstance();
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Create/initialize the underlying DataSource.
    *
    * @return the DataSource
    */
   public static DataSource initializeDataSource(final String dsClassName, DataSource dataSource, final Properties dataSourceProperties, final String jdbcUrl, final String username, final String password)
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
    * Get the int value of a transaction isolation level by name.
    *
    * @param transactionIsolationName the name of the transaction isolation level
    * @return the int value of the isolation level or -1
    */
   public static int getTransactionIsolation(final String transactionIsolationName)
   {
      if (transactionIsolationName != null) {
         try {
            Field field = Connection.class.getField(transactionIsolationName);
            return field.getInt(null);
         }
         catch (Exception e) {
            throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
         }
      }

      return -1;
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
   public static void setupConnection(final Connection connection, final boolean isAutoCommit, final boolean isReadOnly, final int transactionIsolation, final String catalog) throws SQLException
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
    * Create a ThreadPoolExecutor.
    *
    * @param queueSize the queue size
    * @param threadName the thread name
    * @param threadFactory an optional ThreadFactory
    * @return a ThreadPoolExecutor
    */
   public static ThreadPoolExecutor createThreadPoolExecutor(final int queueSize, final String threadName, ThreadFactory threadFactory, final RejectedExecutionHandler policy)
   {
      if (threadFactory == null) {
         threadFactory = new DefaultThreadFactory(threadName, true);
      }

      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueSize);
      ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, queue, threadFactory, policy);
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }

   /**
    * Return true if the driver appears to be JDBC 4.0 compliant.
    *
    * @param connection a Connection to check
    * @return true if JDBC 4.1 compliance, false otherwise
    * @throws SQLException re-thrown exception from Connection.getNetworkTimeout()
    */
   public static boolean isJdbc40Compliant(final Connection connection) throws SQLException
   {
      if (!jdbc40checked) {
         jdbc40checked = true;
         
         try {
            connection.isValid(5);  // This will throw AbstractMethodError or SQLException in the case of a non-JDBC 41 compliant driver
            IS_JDBC40 = true;
         }
         catch (NoSuchMethodError | AbstractMethodError | SQLFeatureNotSupportedException e) {
            IS_JDBC40 = false;
         }
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
   public static void setQueryTimeout(final Statement statement, final int timeoutSec) throws SQLException
   {
      if (queryTimeoutSupported) {
         try {
            statement.setQueryTimeout(timeoutSec);
         }
         catch (NoSuchMethodError | AbstractMethodError | SQLFeatureNotSupportedException e) {
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
   public static int setNetworkTimeout(final Executor executor, final Connection connection, final long timeoutMs, final boolean isUseNetworkTimeout) throws SQLException
   {
      if ((IS_JDBC41 || !jdbc41checked) && isUseNetworkTimeout) {
         jdbc41checked = true;

         try {
            final int networkTimeout = connection.getNetworkTimeout();
            connection.setNetworkTimeout(executor, (int) timeoutMs);
            IS_JDBC41 = true;
            return networkTimeout;
         }
         catch (SQLFeatureNotSupportedException | AbstractMethodError | NoSuchMethodError e) {
            IS_JDBC41 = false;
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
   public static void setLoginTimeout(final DataSource dataSource, final long connectionTimeout, final Logger logger)
   {
      if (connectionTimeout != Integer.MAX_VALUE) {
         try {
            dataSource.setLoginTimeout((int) TimeUnit.MILLISECONDS.toSeconds(Math.min(1000L, connectionTimeout)));
         }
         catch (SQLException e) {
            logger.warn("Unable to set DataSource login timeout", e);
         }
      }
   }
}
