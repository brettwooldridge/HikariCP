package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_AUTOCOMMIT;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_CATALOG;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_ISOLATION;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_NETTIMEOUT;
import static com.zaxxer.hikari.pool.ProxyConnection.DIRTY_BIT_READONLY;
import static com.zaxxer.hikari.util.UtilityElf.createInstance;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PropertyElf;
import com.zaxxer.hikari.util.UtilityElf;

abstract class PoolBase
{
   private final Logger LOGGER = LoggerFactory.getLogger(PoolBase.class);
   protected final HikariConfig config;
   protected final String poolName;
   protected long connectionTimeout;

   private static final String[] RESET_STATES = {"readOnly", "autoCommit", "isolation", "catalog", "netTimeout"};
   private static final int UNINITIALIZED = -1;
   private static final int TRUE = 1;
   private static final int FALSE = 0;

   private int networkTimeout;
   private int transactionIsolation;
   private int isNetworkTimeoutSupported;
   private int isQueryTimeoutSupported;
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

      this.networkTimeout = -1;
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
      this.lastConnectionFailure = new AtomicReference<>();

      initializeDataSource();
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return poolName;
   }

   abstract void releaseConnection(final PoolEntry poolEntry);

   // ***********************************************************************
   //                           JDBC methods
   // ***********************************************************************

   void quietlyCloseConnection(final Connection connection, final String closureReason)
   {
      if (connection == null) {
         return;
      }

      try {
         LOGGER.debug("{} - Closing connection {}: {}", poolName, connection, closureReason);
         try {
            setNetworkTimeout(connection, TimeUnit.SECONDS.toMillis(15));
         }
         finally {
            // continue with the close even if setNetworkTimeout() throws (due to driver poorly behaving drivers)
            connection.close();
         }
      }
      catch (Throwable e) {
         LOGGER.debug("{} - Closing connection {} failed", poolName, connection, e);
      }
   }

   boolean isConnectionAlive(final Connection connection)
   {
      try {
         final long validationTimeout = config.getValidationTimeout();

         if (isUseJdbc4Validation) {
            return connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(validationTimeout));
         }

         final int originalTimeout = getAndSetNetworkTimeout(connection, validationTimeout);

         try (Statement statement = connection.createStatement()) {
            if (isNetworkTimeoutSupported != TRUE) {
               setQueryTimeout(statement, (int) TimeUnit.MILLISECONDS.toSeconds(validationTimeout));
            }

            statement.execute(config.getConnectionTestQuery());
         }

         if (isIsolateInternalQueries && !isReadOnly && !isAutoCommit) {
            connection.rollback();
         }

         setNetworkTimeout(connection, originalTimeout);

         return true;
      }
      catch (SQLException e) {
         lastConnectionFailure.set(e);
         LOGGER.warn("{} - Connection {} failed alive test with exception {}", poolName, connection, e.getMessage());
         return false;
      }
   }

   Throwable getLastConnectionFailure()
   {
      return lastConnectionFailure.getAndSet(null);
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

      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("{} - Reset ({}) on connection {}", poolName, resetBits != 0 ? stringFromResetBits(resetBits) : "nothing", connection);
      }
   }

   void shutdownNetworkTimeoutExecutor()
   {
      if (netTimeoutExecutor != null && netTimeoutExecutor instanceof ThreadPoolExecutor) {
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
            LOGGER.error("{} - You cannot use the same pool name for separate pool instances.", poolName);
         }
      }
      catch (Exception e) {
         LOGGER.warn("{} - Unable to register management beans.", poolName, e);
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
         LOGGER.warn("{} - Unable to unregister management beans.", poolName, e);
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
         setLoginTimeout(dataSource, connectionTimeout);
         createNetworkTimeoutExecutor(dataSource, dsClassName, jdbcUrl);
      }

      this.dataSource = dataSource;
   }

   Connection newConnection() throws Exception
   {
      Connection connection = null;
      try {
         String username = config.getUsername();
         String password = config.getPassword();

         connection = (username == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         setupConnection(connection);
         lastConnectionFailure.set(null);
         return connection;
      }
      catch (Exception e) {
         lastConnectionFailure.set(e);
         String reason = e.getMessage();
         quietlyCloseConnection(connection, reason != null ? reason : "(Failed to create/set connection)");
         throw e;
      }
   }

   /**
    * Setup a connection initial state.
    *
    * @param connection a Connection
    * @throws SQLException thrown from driver
    */
   private void setupConnection(final Connection connection) throws SQLException
   {
      networkTimeout = getAndSetNetworkTimeout(connection, connectionTimeout);

      checkValidationMode(connection);

      connection.setAutoCommit(isAutoCommit);
      connection.setReadOnly(isReadOnly);

      final int defaultLevel = connection.getTransactionIsolation();
      transactionIsolation = (transactionIsolation < 0 || defaultLevel == Connection.TRANSACTION_NONE)
                           ? defaultLevel
                           : transactionIsolation;
      if (transactionIsolation != defaultLevel) {
         connection.setTransactionIsolation(transactionIsolation);
      }

      if (catalog != null) {
         connection.setCatalog(catalog);
      }

      executeSql(connection, config.getConnectionInitSql(), isIsolateInternalQueries && !isAutoCommit, false);

      setNetworkTimeout(connection, networkTimeout);
   }

   /**
    * Execute isValid() or connection test query.
    *
    * @param connection a Connection to check
    */
   private void checkValidationMode(final Connection connection) throws SQLException
   {
      if (!isValidChecked) {
         if (isUseJdbc4Validation) {
            try {
               connection.isValid(1);
            }
            catch (Throwable e) {
               LOGGER.debug("{} - Connection.isValid() is not supported, configure connection test query. ({})", poolName, e.getMessage());
               throw e;
            }
         }
         else {
            try {
               executeSql(connection, config.getConnectionTestQuery(), false, isIsolateInternalQueries && !isAutoCommit);
            }
            catch (Throwable e) {
               LOGGER.debug("{} - Failed to execute connection test query. ({})", poolName, e.getMessage());
               throw e;
            }
         }
         isValidChecked = true;
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
               LOGGER.debug("{} - Statement.setQueryTimeout() is not supported ({})", poolName, e.getMessage());
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
               LOGGER.debug("{} - Connection.setNetworkTimeout() is not supported ({})", poolName, e.getMessage());
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
    * @param isAutoCommit whether to commit the SQL after execution or not
    * @throws SQLException throws if the init SQL execution fails
    */
   private void executeSql(final Connection connection, final String sql, final boolean isCommit, final boolean isRollback) throws SQLException
   {
      if (sql != null) {
         try (Statement statement = connection.createStatement()) {

            //con created few ms before, set query timeout is omitted
            statement.execute(sql);

            if (!isReadOnly) {
               if (isCommit) {
                  connection.commit();
               }
               else if (isRollback) {
                  connection.rollback();
               }
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
         ThreadFactory threadFactory = config.getThreadFactory() != null ? config.getThreadFactory() : new DefaultThreadFactory("Hikari JDBC-timeout executor", true);
         ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
         executor.allowCoreThreadTimeOut(true);
         executor.setKeepAliveTime(15, TimeUnit.SECONDS);
         netTimeoutExecutor = executor;
      }
   }

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
            LoggerFactory.getLogger(PoolBase.class).debug("Exception executing {}", command, t);
         }
      }
   }

   /**
    * Set the loginTimeout on the specified DataSource.
    *
    * @param dataSource the DataSource
    * @param connectionTimeout the timeout in milliseconds
    */
   private void setLoginTimeout(final DataSource dataSource, final long connectionTimeout)
   {
      if (connectionTimeout != Integer.MAX_VALUE) {
         try {
            dataSource.setLoginTimeout((int) TimeUnit.MILLISECONDS.toSeconds(Math.max(1000L, connectionTimeout)));
         }
         catch (Throwable e) {
            LOGGER.warn("{} - Unable to set DataSource login timeout", poolName, e);
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
   }

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
   }
}
