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

package com.zaxxer.hikari.pool;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PropertyElf;
import com.zaxxer.hikari.util.UtilityElf;
import com.zaxxer.hikari.util.UtilityElf.DefaultThreadFactory;

import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_AUTOCOMMIT;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_CATALOG;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_ISOLATION;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_NETTIMEOUT;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_READONLY;
import static com.zaxxer.hikari.util.UtilityElf.createInstance;

abstract class PoolBase
{
   private final Logger LOGGER = LoggerFactory.getLogger(PoolBase.class);

   protected final HikariConfig config;
   protected final String poolName;
   protected long connectionTimeout;
   protected long validationTimeout;
   protected MetricsTrackerDelegate metricsTracker;

   private static final String[] RESET_STATES = {"readOnly", "autoCommit", "isolation", "catalog", "netTimeout"};
   private static final int UNINITIALIZED = -1;
   private static final int TRUE = 1;
   private static final int FALSE = 0;

   private int networkTimeout;
   private int isNetworkTimeoutSupported;
   private int isQueryTimeoutSupported;
   private int defaultTransactionIsolation;
   private int transactionIsolation;
   private Executor netTimeoutExecutor;
   private DataSource dataSource;

   private final String catalog;
   private final boolean isReadOnly;
   private final boolean isAutoCommit;

   private final boolean isUseJdbc4Validation;
   private final boolean isIsolateInternalQueries;
   private final AtomicReference<Throwable> lastConnectionFailure;

   private volatile boolean isValidChecked;

   PoolBase(final HikariConfig config)
   {
      this.config = config;

      this.networkTimeout = UNINITIALIZED;
      this.catalog = config.getCatalog();
      this.isReadOnly = config.isReadOnly();
      this.isAutoCommit = config.isAutoCommit();
      this.transactionIsolation = UtilityElf.getTransactionIsolation(config.getTransactionIsolation());

      this.isQueryTimeoutSupported = UNINITIALIZED;
      this.isNetworkTimeoutSupported = UNINITIALIZED;
      this.isUseJdbc4Validation = config.getConnectionTestQuery() == null;
      this.isIsolateInternalQueries = config.isIsolateInternalQueries();

      this.poolName = config.getPoolName();
      this.connectionTimeout = config.getConnectionTimeout();
      this.validationTimeout = config.getValidationTimeout();
      this.lastConnectionFailure = new AtomicReference<>();

      initializeDataSource();
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return poolName;
   }

   abstract void recycle(final PoolEntry poolEntry);

   // ***********************************************************************
   //                           JDBC methods
   // ***********************************************************************

   void quietlyCloseConnection(final Connection connection, final String closureReason)
   {
      if (connection != null) {
         try {
            LOGGER.debug("{} - Closing connection {}: {}", poolName, connection, closureReason);
            try {
               setNetworkTimeout(connection, SECONDS.toMillis(15));
            }
            finally {
               connection.close(); // continue with the close even if setNetworkTimeout() throws
            }
         }
         catch (Throwable e) {
            LOGGER.debug("{} - Closing connection {} failed", poolName, connection, e);
         }
      }
   }

   boolean isConnectionAlive(final Connection connection)
   {
      try {
         try {
            if (isUseJdbc4Validation) {
               return connection.isValid((int) MILLISECONDS.toSeconds(Math.max(1000L, validationTimeout)));
            }

            setNetworkTimeout(connection, validationTimeout);

            try (Statement statement = connection.createStatement()) {
               if (isNetworkTimeoutSupported != TRUE) {
                  setQueryTimeout(statement, (int) MILLISECONDS.toSeconds(Math.max(1000L, validationTimeout)));
               }

               statement.execute(config.getConnectionTestQuery());
            }
         }
         finally {
            if (isIsolateInternalQueries && !isAutoCommit) {
               connection.rollback();
            }
         }

         setNetworkTimeout(connection, networkTimeout);

         return true;
      }
      catch (Exception e) {
         lastConnectionFailure.set(e);
         LOGGER.warn("{} - Failed to validate connection {} ({})", poolName, connection, e.getMessage());
         return false;
      }
   }

   Throwable getLastConnectionFailure()
   {
      return lastConnectionFailure.get();
   }

   public DataSource getUnwrappedDataSource()
   {
      return dataSource;
   }

   // ***********************************************************************
   //                         PoolEntry methods
   // ***********************************************************************

   PoolEntry newPoolEntry() throws Exception
   {
      return new PoolEntry(newConnection(), this, isReadOnly, isAutoCommit);
   }

   void resetConnectionState(final Connection connection, final ProxyConnection proxyConnection, final int dirtyBits) throws SQLException
   {
      int resetBits = 0;

      if ((dirtyBits & DIRTY_BIT_READONLY) != 0 && proxyConnection.getReadOnlyState() != isReadOnly) {
         connection.setReadOnly(isReadOnly);
         resetBits |= DIRTY_BIT_READONLY;
      }

      if ((dirtyBits & DIRTY_BIT_AUTOCOMMIT) != 0 && proxyConnection.getAutoCommitState() != isAutoCommit) {
         connection.setAutoCommit(isAutoCommit);
         resetBits |= DIRTY_BIT_AUTOCOMMIT;
      }

      if ((dirtyBits & DIRTY_BIT_ISOLATION) != 0 && proxyConnection.getTransactionIsolationState() != transactionIsolation) {
         connection.setTransactionIsolation(transactionIsolation);
         resetBits |= DIRTY_BIT_ISOLATION;
      }

      if ((dirtyBits & DIRTY_BIT_CATALOG) != 0 && catalog != null && !catalog.equals(proxyConnection.getCatalogState())) {
         connection.setCatalog(catalog);
         resetBits |= DIRTY_BIT_CATALOG;
      }

      if ((dirtyBits & DIRTY_BIT_NETTIMEOUT) != 0 && proxyConnection.getNetworkTimeoutState() != networkTimeout) {
         setNetworkTimeout(connection, networkTimeout);
         resetBits |= DIRTY_BIT_NETTIMEOUT;
      }

      if (resetBits != 0 && LOGGER.isDebugEnabled()) {
         LOGGER.debug("{} - Reset ({}) on connection {}", poolName, stringFromResetBits(resetBits), connection);
      }
   }

   void shutdownNetworkTimeoutExecutor()
   {
      if (netTimeoutExecutor instanceof ThreadPoolExecutor) {
         ((ThreadPoolExecutor) netTimeoutExecutor).shutdownNow();
      }
   }

   // ***********************************************************************
   //                       JMX methods
   // ***********************************************************************

   /**
    * Register MBeans for HikariConfig and HikariPool.
    *
    * @param pool a HikariPool instance
    */
   void registerMBeans(final HikariPool hikariPool)
   {
      if (!config.isRegisterMbeans()) {
         return;
      }

      try {
         final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

         final ObjectName beanConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")");
         final ObjectName beanPoolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
         if (!mBeanServer.isRegistered(beanConfigName)) {
            mBeanServer.registerMBean(config, beanConfigName);
            mBeanServer.registerMBean(hikariPool, beanPoolName);
         }
         else {
            LOGGER.error("{} - JMX name ({}) is already registered.", poolName, poolName);
         }
      }
      catch (Exception e) {
         LOGGER.warn("{} - Failed to register management beans.", poolName, e);
      }
   }

   /**
    * Unregister MBeans for HikariConfig and HikariPool.
    */
   void unregisterMBeans()
   {
      if (!config.isRegisterMbeans()) {
         return;
      }

      try {
         final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

         final ObjectName beanConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")");
         final ObjectName beanPoolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
         if (mBeanServer.isRegistered(beanConfigName)) {
            mBeanServer.unregisterMBean(beanConfigName);
            mBeanServer.unregisterMBean(beanPoolName);
         }
      }
      catch (Exception e) {
         LOGGER.warn("{} - Failed to unregister management beans.", poolName, e);
      }
   }

   // ***********************************************************************
   //                          Private methods
   // ***********************************************************************

   /**
    * Create/initialize the underlying DataSource.
    *
    * @return a DataSource instance
    */
   private void initializeDataSource()
   {
      final String jdbcUrl = config.getJdbcUrl();
      final String username = config.getUsername();
      final String password = config.getPassword();
      final String dsClassName = config.getDataSourceClassName();
      final String driverClassName = config.getDriverClassName();
      final Properties dataSourceProperties = config.getDataSourceProperties();

      DataSource dataSource = config.getDataSource();
      if (dsClassName != null && dataSource == null) {
         dataSource = createInstance(dsClassName, DataSource.class);
         PropertyElf.setTargetFromProperties(dataSource, dataSourceProperties);
      }
      else if (jdbcUrl != null && dataSource == null) {
         dataSource = new DriverDataSource(jdbcUrl, driverClassName, dataSourceProperties, username, password);
      }

      if (dataSource != null) {
         setLoginTimeout(dataSource);
         createNetworkTimeoutExecutor(dataSource, dsClassName, jdbcUrl);
      }

      this.dataSource = dataSource;
   }

   /**
    * Obtain connection from data source.
    *
    * @return a Connection connection
    */
   Connection newConnection() throws Exception
   {
      final long start = ClockSource.INSTANCE.currentTime();

      Connection connection = null;
      try {
         String username = config.getUsername();
         String password = config.getPassword();

         connection = (username == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         if (connection == null) {
            throw new SQLTransientConnectionException("DataSource returned null unexpectedly");
         }

         setupConnection(connection);
         lastConnectionFailure.set(null);
         return connection;
      }
      catch (Exception e) {
         Exception cause = e;
         if (e instanceof ConnectionSetupException) {
            cause = (Exception) e.getCause();
         }

         if (connection != null) {
            quietlyCloseConnection(connection, "(Failed to create/setup connection)");
         }
         else if (getLastConnectionFailure() == null) {
            LOGGER.debug("{} - Failed to create/setup connection: {}", poolName, cause.getMessage());
         }

         lastConnectionFailure.set(cause);
         throw e;
      }
      finally {
         // tracker will be null during failFast check
         if (metricsTracker != null) {
            metricsTracker.recordConnectionCreated(ClockSource.INSTANCE.elapsedMillis(start));
         }
      }
   }

   /**
    * Setup a connection initial state.
    *
    * @param connection a Connection
    * @throws SQLException thrown from driver
    */
   private void setupConnection(final Connection connection) throws ConnectionSetupException
   {
      try {
         if (networkTimeout == UNINITIALIZED) {
            networkTimeout = getAndSetNetworkTimeout(connection, validationTimeout);
         }
         else {
            setNetworkTimeout(connection, validationTimeout);
         }

         connection.setReadOnly(isReadOnly);
         connection.setAutoCommit(isAutoCommit);

         checkDriverSupport(connection);

         if (transactionIsolation != defaultTransactionIsolation) {
            connection.setTransactionIsolation(transactionIsolation);
         }

         if (catalog != null) {
            connection.setCatalog(catalog);
         }

         executeSql(connection, config.getConnectionInitSql(), true);

         setNetworkTimeout(connection, networkTimeout);
      }
      catch (SQLException e) {
         throw new ConnectionSetupException(e);
      }
   }

   /**
    * Execute isValid() or connection test query.
    *
    * @param connection a Connection to check
    */
   private void checkDriverSupport(final Connection connection) throws SQLException
   {
      if (!isValidChecked) {
         try {
            if (isUseJdbc4Validation) {
               connection.isValid(1);
            }
            else {
               executeSql(connection, config.getConnectionTestQuery(), false);
            }
         }
         catch (Throwable e) {
            LOGGER.error("{} - Failed to execute" + (isUseJdbc4Validation ? " isValid() for connection, configure" : "") + " connection test query. ({})", poolName, e.getMessage());
            throw e;
         }

         try {
            defaultTransactionIsolation = connection.getTransactionIsolation();
            if (transactionIsolation == -1) {
               transactionIsolation = defaultTransactionIsolation;
            }
         }
         catch (SQLException e) {
            LOGGER.warn("{} - Default transaction isolation level detection failed. ({})", poolName, e.getMessage());
         }
         finally {
            isValidChecked = true;
         }
      }
   }

   /**
    * Set the query timeout, if it is supported by the driver.
    *
    * @param statement a statement to set the query timeout on
    * @param timeoutSec the number of seconds before timeout
    */
   private void setQueryTimeout(final Statement statement, final int timeoutSec)
   {
      if (isQueryTimeoutSupported != FALSE) {
         try {
            statement.setQueryTimeout(timeoutSec);
            isQueryTimeoutSupported = TRUE;
         }
         catch (Throwable e) {
            if (isQueryTimeoutSupported == UNINITIALIZED) {
               isQueryTimeoutSupported = FALSE;
               LOGGER.info("{} - Failed to set query timeout for statement. ({})", poolName, e.getMessage());
            }
         }
      }
   }

   /**
    * Set the network timeout, if <code>isUseNetworkTimeout</code> is <code>true</code> and the
    * driver supports it.  Return the pre-existing value of the network timeout.
    *
    * @param connection the connection to set the network timeout on
    * @param timeoutMs the number of milliseconds before timeout
    * @return the pre-existing network timeout value
    */
   private int getAndSetNetworkTimeout(final Connection connection, final long timeoutMs)
   {
      if (isNetworkTimeoutSupported != FALSE) {
         try {
            final int originalTimeout = connection.getNetworkTimeout();
            connection.setNetworkTimeout(netTimeoutExecutor, (int) timeoutMs);
            isNetworkTimeoutSupported = TRUE;
            return originalTimeout;
         }
         catch (Throwable e) {
            if (isNetworkTimeoutSupported == UNINITIALIZED) {
               isNetworkTimeoutSupported = FALSE;

               LOGGER.info("{} - Driver does not support get/set network timeout for connections. ({})", poolName, e.getMessage());
               if (validationTimeout < SECONDS.toMillis(1)) {
                  LOGGER.warn("{} - A validationTimeout of less than 1 second cannot be honored on drivers without setNetworkTimeout() support.", poolName);
               }
               else if (validationTimeout % SECONDS.toMillis(1) != 0) {
                  LOGGER.warn("{} - A validationTimeout with fractional second granularity cannot be honored on drivers without setNetworkTimeout() support.", poolName);
               }
            }
         }
      }

      return 0;
   }

   /**
    * Set the network timeout, if <code>isUseNetworkTimeout</code> is <code>true</code> and the
    * driver supports it.
    *
    * @param connection the connection to set the network timeout on
    * @param timeoutMs the number of milliseconds before timeout
    * @throws SQLException throw if the connection.setNetworkTimeout() call throws
    */
   private void setNetworkTimeout(final Connection connection, final long timeoutMs) throws SQLException
   {
      if (isNetworkTimeoutSupported == TRUE) {
         connection.setNetworkTimeout(netTimeoutExecutor, (int) timeoutMs);
      }
   }

   /**
    * Execute the user-specified init SQL.
    *
    * @param connection the connection to initialize
    * @param sql the SQL to execute
    * @param isCommit whether to commit the SQL after execution or not
    * @throws SQLException throws if the init SQL execution fails
    */
   private void executeSql(final Connection connection, final String sql, final boolean isCommit) throws SQLException
   {
      if (sql != null) {
         try (Statement statement = connection.createStatement()) {
            // connection was created a few milliseconds before, so set query timeout is omitted (we assume it will succeed)
            statement.execute(sql);
         }

         if (isIsolateInternalQueries && !isAutoCommit) {
            if (isCommit) {
               connection.commit();
            }
            else {
               connection.rollback();
            }
         }
      }
   }

   private void createNetworkTimeoutExecutor(final DataSource dataSource, final String dsClassName, final String jdbcUrl)
   {
      // Temporary hack for MySQL issue: http://bugs.mysql.com/bug.php?id=75615
      if ((dsClassName != null && dsClassName.contains("Mysql")) ||
          (jdbcUrl != null && jdbcUrl.contains("mysql")) ||
          (dataSource != null && dataSource.getClass().getName().contains("Mysql"))) {
         netTimeoutExecutor = new SynchronousExecutor();
      }
      else {
         ThreadFactory threadFactory = config.getThreadFactory();
         threadFactory = threadFactory != null ? threadFactory : new DefaultThreadFactory(poolName + " network timeout executor", true);
         ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
         executor.setKeepAliveTime(15, SECONDS);
         executor.allowCoreThreadTimeOut(true);
         netTimeoutExecutor = executor;
      }
   }

   /**
    * Set the loginTimeout on the specified DataSource.
    *
    * @param dataSource the DataSource
    */
   private void setLoginTimeout(final DataSource dataSource)
   {
      if (connectionTimeout != Integer.MAX_VALUE) {
         try {
            dataSource.setLoginTimeout(Math.max(1, (int) MILLISECONDS.toSeconds(500L + connectionTimeout)));
         }
         catch (Throwable e) {
            LOGGER.info("{} - Failed to set login timeout for data source. ({})", poolName, e.getMessage());
         }
      }
   }

   /**
    * This will create a string for debug logging. Given a set of "reset bits", this
    * method will return a concatenated string, for example:
    *
    * Input : 0b00110
    * Output: "autoCommit, isolation"
    *
    * @param bits a set of "reset bits"
    * @return a string of which states were reset
    */
   private String stringFromResetBits(final int bits)
   {
      final StringBuilder sb = new StringBuilder();
      for (int ndx = 0; ndx < RESET_STATES.length; ndx++) {
         if ( (bits & (0b1 << ndx)) != 0) {
            sb.append(RESET_STATES[ndx]).append(", ");
         }
      }

      sb.setLength(sb.length() - 2);  // trim trailing comma
      return sb.toString();
   }

   // ***********************************************************************
   //                      Private Static Classes
   // ***********************************************************************

   static class ConnectionSetupException extends Exception
   {
      private static final long serialVersionUID = 929872118275916521L;

      public ConnectionSetupException(Throwable t)
      {
         super(t);
      }
   }

   /**
    * Special executor used only to work around a MySQL issue that has not been addressed.
    * MySQL issue: http://bugs.mysql.com/bug.php?id=75615
    */
   private static class SynchronousExecutor implements Executor
   {
      /** {@inheritDoc} */
      @Override
      public void execute(Runnable command)
      {
         try {
            command.run();
         }
         catch (Throwable t) {
            LoggerFactory.getLogger(PoolBase.class).debug("Failed to execute: {}", command, t);
         }
      }
   }

   /**
    * A class that delegates to a MetricsTracker implementation.  The use of a delegate
    * allows us to use the NopMetricsTrackerDelegate when metrics are disabled, which in
    * turn allows the JIT to completely optimize away to callsites to record metrics.
    */
   static class MetricsTrackerDelegate implements AutoCloseable
   {
      final MetricsTracker tracker;

      protected MetricsTrackerDelegate()
      {
         this.tracker = null;
      }

      MetricsTrackerDelegate(MetricsTracker tracker)
      {
         this.tracker = tracker;
      }

      @Override
      public void close()
      {
         tracker.close();
      }

      void recordConnectionUsage(final PoolEntry poolEntry)
      {
         tracker.recordConnectionUsageMillis(poolEntry.getMillisSinceBorrowed());
      }

      void recordConnectionCreated(long connectionCreatedMillis)
      {
         tracker.recordConnectionCreatedMillis(connectionCreatedMillis);
      }

      /**
       * @param poolEntry
       * @param now
       */
      void recordBorrowStats(final PoolEntry poolEntry, final long startTime)
      {
         final long now = ClockSource.INSTANCE.currentTime();
         poolEntry.lastBorrowed = now;
         tracker.recordConnectionAcquiredNanos(ClockSource.INSTANCE.elapsedNanos(startTime, now));
      }

      void recordConnectionTimeout() {
         tracker.recordConnectionTimeout();
      }
   }

   /**
    * A no-op implementation of the MetricsTrackerDelegate that is used when metrics capture is
    * disabled.
    */
   static final class NopMetricsTrackerDelegate extends MetricsTrackerDelegate
   {
      @Override
      void recordConnectionUsage(final PoolEntry poolEntry)
      {
         // no-op
      }

      @Override
      public void close()
      {
         // no-op
      }

      @Override
      void recordBorrowStats(final PoolEntry poolEntry, final long startTime)
      {
         // no-op
      }

      @Override
      void recordConnectionTimeout()
      {
         // no-op
      }

      @Override
      void recordConnectionCreated(long connectionCreatedMillis)
      {
         // no-op
      }
   }
}
