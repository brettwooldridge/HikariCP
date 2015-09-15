package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.util.UtilityElf.createInstance;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
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
import com.zaxxer.hikari.pool.Mediators.PoolEntryMediator;
import com.zaxxer.hikari.pool.Mediators.JdbcMediator;
import com.zaxxer.hikari.pool.Mediators.PoolMediator;
import com.zaxxer.hikari.proxy.ConnectionState;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PropertyElf;

public final class Mediator implements Mediators, JdbcMediator, PoolMediator, PoolEntryMediator
{
   private static final Logger LOGGER = LoggerFactory.getLogger(Mediator.class);
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

   private final HikariPool hikariPool;
   private final HikariConfig config;
   private final String poolName;
   private final String catalog;
   private final boolean isReadOnly;
   private final boolean isAutoCommit;
   private final boolean isUseJdbc4Validation;
   private final boolean isIsolateInternalQueries;
   private final AtomicReference<Throwable> lastConnectionFailure;

   private volatile boolean isValidChecked; 
   private volatile boolean isValidSupported;

   public Mediator(final HikariPool pool)
   {
      this.hikariPool = pool;
      this.config = pool.config;

      this.networkTimeout = -1;
      this.catalog = config.getCatalog();
      this.isReadOnly = config.isReadOnly();
      this.isAutoCommit = config.isAutoCommit();
      this.transactionIsolation = getTransactionIsolation(config.getTransactionIsolation());

      this.isValidSupported = true;
      this.isQueryTimeoutSupported = UNINITIALIZED;
      this.isNetworkTimeoutSupported = UNINITIALIZED;
      this.isUseJdbc4Validation = config.getConnectionTestQuery() == null;
      this.isIsolateInternalQueries = config.isIsolateInternalQueries();

      this.poolName = config.getPoolName();
      this.lastConnectionFailure = new AtomicReference<>();

      initializeDataSource();
   }

   // ***********************************************************************
   //                        JdbcMediator methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public void quietlyCloseConnection(final Connection connection, final String closureReason)
   {
      try {
         if (connection == null || connection.isClosed()) {
            return;
         }

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

   /** {@inheritDoc} */
   @Override
   public boolean isConnectionAlive(final Connection connection)
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
   
         if (isIsolateInternalQueries && !isAutoCommit) {
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

   /** {@inheritDoc} */
   @Override
   public DataSource getUnwrappedDataSource()
   {
      return dataSource;
   }

   /** {@inheritDoc} */
   @Override
   public Throwable getLastConnectionFailure()
   {
      return lastConnectionFailure.getAndSet(null);
   }


   // ***********************************************************************
   //                     ConnectionStateMediator methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public PoolEntry newPoolEntry() throws Exception
   {
      return new PoolEntry(newConnection(), hikariPool, this);
   }

   public void resetConnectionState(final Connection connection, final ConnectionState liveState, final int dirtyBits) throws SQLException
   {
      int resetBits = 0;

      if ((dirtyBits & 0b00001) != 0 && liveState.getReadOnlyState() != isReadOnly) {
         connection.setReadOnly(isReadOnly);
         resetBits |= 0b00001;
      }

      if ((dirtyBits & 0b00010) != 0 && liveState.getAutoCommitState() != isAutoCommit) {
         connection.setAutoCommit(isAutoCommit);
         resetBits |= 0b00010;
      }

      if ((dirtyBits & 0b00100) != 0 && liveState.getTransactionIsolationState() != transactionIsolation) {
         connection.setTransactionIsolation(transactionIsolation);
         resetBits |= 0b00100;
      }

      if ((dirtyBits & 0b01000) != 0) {
         final String currentCatalog = liveState.getCatalogState();
         if ((currentCatalog != null && !currentCatalog.equals(catalog)) || (currentCatalog == null && catalog != null)) {
            connection.setCatalog(catalog);
            resetBits |= 0b01000;
         }
      }

      if ((dirtyBits & 0b10000) != 0 && liveState.getNetworkTimeoutState() != networkTimeout) {
         setNetworkTimeout(connection, networkTimeout);
         resetBits |= 0b10000;
      }
      
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("{} - Reset ({}) on connection {}", poolName, resetBits != 0 ? stringFromResetBits(resetBits) : "nothing", connection);
      }
   }
   

   // ***********************************************************************
   //                       PoolMediator methods
   // ***********************************************************************

   /**
    * Register MBeans for HikariConfig and HikariPool.
    *
    * @param pool a HikariPool instance
    */
   public void registerMBeans()
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
   public void unregisterMBeans()
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

   public void shutdownTimeoutExecutor()
   {
      if (netTimeoutExecutor != null && netTimeoutExecutor instanceof ThreadPoolExecutor) {
         ((ThreadPoolExecutor) netTimeoutExecutor).shutdownNow();
      }
   }

   // ***********************************************************************
   //                        Mediators accessors
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public JdbcMediator getJdbcMediator()
   {
      return this;
   }


   /** {@inheritDoc} */
   @Override
   public PoolEntryMediator getConnectionStateMediator()
   {
      return this;
   }


   /** {@inheritDoc} */
   @Override
   public PoolMediator getPoolMediator()
   {
      return this;
   }

   // ***********************************************************************
   //                       Misc. public methods
   // ***********************************************************************

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
            final String upperName = transactionIsolationName.toUpperCase();
            if (upperName.startsWith("TRANSACTION_")) {
               Field field = Connection.class.getField(upperName);
               return field.getInt(null);
            }
            final int level = Integer.parseInt(transactionIsolationName);
            switch (level) {
               case Connection.TRANSACTION_READ_UNCOMMITTED:
               case Connection.TRANSACTION_READ_COMMITTED:
               case Connection.TRANSACTION_REPEATABLE_READ:
               case Connection.TRANSACTION_SERIALIZABLE:
               case Connection.TRANSACTION_NONE:
                  return level;
               default:
                  throw new IllegalArgumentException();
             }
         }
         catch (Exception e) {
            throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
         }
      }

      return -1;
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
         setLoginTimeout(dataSource, config.getConnectionTimeout());
         createNetworkTimeoutExecutor(dataSource, dsClassName, jdbcUrl);
      }

      this.dataSource = dataSource;
   }

   private Connection newConnection() throws Exception
   {
      Connection connection = null;
      try {
         String username = config.getUsername();
         String password = config.getPassword();

         connection = (username == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         setupConnection(connection, config.getConnectionTimeout());
         lastConnectionFailure.set(null);
         return connection;
      }
      catch (Exception e) {
         quietlyCloseConnection(connection, "(exception during connection creation)");
         throw e;
      }
   }

   /**
    * Setup a connection initial state.
    *
    * @param connection a Connection
    * @param connectionTimeout the connection timeout
    * @throws SQLException thrown from driver
    */
   private void setupConnection(final Connection connection, final long connectionTimeout) throws SQLException
   {
      if (isUseJdbc4Validation && !isJdbc4ValidationSupported(connection)) {
         throw new SQLException("Connection.isValid() is not supported, configure connection test query.");
      }

      networkTimeout = getAndSetNetworkTimeout(connection, connectionTimeout);

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

      executeSql(connection, config.getConnectionInitSql(), isAutoCommit);

      setNetworkTimeout(connection, networkTimeout);
   }

   /**
    * Return true if the driver appears to be JDBC 4.0 compliant.
    *
    * @param connection a Connection to check
    * @return true if JDBC 4.1 compliance, false otherwise
    */
   private boolean isJdbc4ValidationSupported(final Connection connection)
   {
      if (!isValidChecked) {
         try {
            // We don't care how long the wait actually is here, just whether it returns without exception. This
            // call will throw various exceptions in the case of a non-JDBC 4.0 compliant driver
            connection.isValid(1);
         }
         catch (Throwable e) {
            isValidSupported = false;
            LOGGER.debug("{} - Connection.isValid() is not supported ({})", poolName, e.getMessage());
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
   private void executeSql(final Connection connection, final String sql, final boolean isAutoCommit) throws SQLException
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
            LOGGER.debug("Exception executing {}", command, t);
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
         catch (SQLException e) {
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
}
