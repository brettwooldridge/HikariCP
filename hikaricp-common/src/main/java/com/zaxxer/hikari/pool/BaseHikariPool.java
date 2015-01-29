/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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

import static com.zaxxer.hikari.pool.HikariMBeanElf.registerMBeans;
import static com.zaxxer.hikari.pool.HikariMBeanElf.unregisterMBeans;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_REMOVED;
import static com.zaxxer.hikari.util.UtilityElf.createInstance;
import static com.zaxxer.hikari.util.UtilityElf.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.UtilityElf.elapsedTimeMs;
import static com.zaxxer.hikari.util.UtilityElf.getTransactionIsolation;
import static com.zaxxer.hikari.util.UtilityElf.setRemoveOnCancelPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.IConnectionCustomizer;
import com.zaxxer.hikari.metrics.CodaHaleMetricsTracker;
import com.zaxxer.hikari.metrics.CodahaleHealthChecker;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker.MetricsContext;
import com.zaxxer.hikari.proxy.ConnectionProxy;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.IBagStateListener;
import com.zaxxer.hikari.util.LeakTask;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public abstract class BaseHikariPool implements HikariPoolMBean, IBagStateListener
{
   protected static final Logger LOGGER = LoggerFactory.getLogger("HikariPool");
   private static final long ALIVE_BYPASS_WINDOW = Long.getLong("com.zaxxer.hikari.aliveBypassWindow", 1000L);

   protected static final int POOL_RUNNING = 0;
   protected static final int POOL_SUSPENDED = 1;
   protected static final int POOL_SHUTDOWN = 2;

   public final String catalog;
   public final boolean isReadOnly;
   public final boolean isAutoCommit;
   public int transactionIsolation;

   protected final PoolUtilities poolUtils;
   protected final HikariConfig configuration;
   protected final AtomicInteger totalConnections;
   protected final ConcurrentBag<PoolBagEntry> connectionBag;
   protected final ThreadPoolExecutor addConnectionExecutor;
   protected final ThreadPoolExecutor closeConnectionExecutor;
   protected final ScheduledThreadPoolExecutor houseKeepingExecutorService;

   protected final boolean isUseJdbc4Validation;
   protected final boolean isIsolateInternalQueries;

   protected volatile int poolState;
   protected volatile long connectionTimeout;
   protected volatile long validationTimeout;
   
   private final LeakTask leakTask;
   private final DataSource dataSource;
   private final MetricsTracker metricsTracker;
   private final GlobalPoolLock suspendResumeLock;
   private final IConnectionCustomizer connectionCustomizer;
   private final AtomicReference<Throwable> lastConnectionFailure;

   private final String username;
   private final String password;
   private final boolean isRecordMetrics;

   /**
    * Construct a HikariPool with the specified configuration.
    *
    * @param configuration a HikariConfig instance
    */
   public BaseHikariPool(HikariConfig configuration)
   {
      this(configuration, configuration.getUsername(), configuration.getPassword());
   }

   /**
    * Construct a HikariPool with the specified configuration.  We cache lots of configuration
    * items in class-local final members for speed.
    *
    * @param configuration a HikariConfig instance
    * @param username authentication username
    * @param password authentication password
    */
   public BaseHikariPool(HikariConfig configuration, String username, String password)
   {
      this.username = username;
      this.password = password;
      this.configuration = configuration;

      this.poolUtils = new PoolUtilities(configuration);
      this.connectionBag = createConcurrentBag(this);
      this.totalConnections = new AtomicInteger();
      this.connectionTimeout = configuration.getConnectionTimeout();
      this.validationTimeout = configuration.getValidationTimeout();
      this.lastConnectionFailure = new AtomicReference<Throwable>();

      this.isReadOnly = configuration.isReadOnly();
      this.isAutoCommit = configuration.isAutoCommit();

      this.suspendResumeLock = configuration.isAllowPoolSuspension() ? GlobalPoolLock.SUSPEND_RESUME_LOCK : GlobalPoolLock.FAUX_LOCK;

      this.catalog = configuration.getCatalog();
      this.connectionCustomizer = initializeCustomizer();
      this.transactionIsolation = getTransactionIsolation(configuration.getTransactionIsolation());
      this.isIsolateInternalQueries = configuration.isIsolateInternalQueries();
      this.isUseJdbc4Validation = configuration.getConnectionTestQuery() == null;

      this.isRecordMetrics = configuration.getMetricRegistry() != null;
      this.metricsTracker = (isRecordMetrics ? new CodaHaleMetricsTracker(this, (MetricRegistry) configuration.getMetricRegistry()) : new MetricsTracker(this));

      this.dataSource = poolUtils.initializeDataSource(configuration.getDataSourceClassName(), configuration.getDataSource(), configuration.getDataSourceProperties(), configuration.getJdbcUrl(), username, password);

      this.addConnectionExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection filler (pool " + configuration.getPoolName() + ")", configuration.getThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
      this.closeConnectionExecutor = createThreadPoolExecutor(4, "HikariCP connection closer (pool " + configuration.getPoolName() + ")", configuration.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

      long delayPeriod = Long.getLong("com.zaxxer.hikari.housekeeping.periodMs", TimeUnit.SECONDS.toMillis(30L));
      ThreadFactory threadFactory = configuration.getThreadFactory() != null ? configuration.getThreadFactory() : new DefaultThreadFactory("Hikari Housekeeping Timer (pool " + configuration.getPoolName() + ")", true);
      this.houseKeepingExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
      this.houseKeepingExecutorService.scheduleAtFixedRate(getHouseKeeper(), delayPeriod, delayPeriod, TimeUnit.MILLISECONDS);
      this.houseKeepingExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      this.leakTask = (configuration.getLeakDetectionThreshold() == 0) ? LeakTask.NO_LEAK : new LeakTask(configuration.getLeakDetectionThreshold(), houseKeepingExecutorService);

      if (configuration.getHealthCheckRegistry() != null) {
         CodahaleHealthChecker.registerHealthChecks(this, (HealthCheckRegistry) configuration.getHealthCheckRegistry());
      }

      setRemoveOnCancelPolicy(houseKeepingExecutorService);
      poolUtils.setLoginTimeout(dataSource, connectionTimeout);
      registerMBeans(configuration, this);
      initializeConnections();
   }

   /**
    * Get a connection from the pool, or timeout after connectionTimeout milliseconds.
    *
    * @return a java.sql.Connection instance
    * @throws SQLException thrown if a timeout occurs trying to obtain a connection
    */
   public final Connection getConnection() throws SQLException
   {
      return getConnection(connectionTimeout);
   }

   /**
    * Get a connection from the pool, or timeout after the specified number of milliseconds.
    *
    * @param hardTimeout the maximum time to wait for a connection from the pool
    * @return a java.sql.Connection instance
    * @throws SQLException thrown if a timeout occurs trying to obtain a connection
    */
   public final Connection getConnection(final long hardTimeout) throws SQLException
   {
      suspendResumeLock.acquire();
      long timeout = hardTimeout;
      final long start = System.currentTimeMillis();
      final MetricsContext metricsContext = (isRecordMetrics ? metricsTracker.recordConnectionRequest(start) : MetricsTracker.NO_CONTEXT);

      try {
         do {
            final PoolBagEntry bagEntry = connectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
            if (bagEntry == null) {
               break; // We timed out... break and throw exception
            }

            final long now = System.currentTimeMillis();
            if (bagEntry.evicted || (now - bagEntry.lastAccess > ALIVE_BYPASS_WINDOW && !isConnectionAlive(bagEntry.connection))) {
               closeConnection(bagEntry); // Throw away the dead connection and try again
               timeout = hardTimeout - elapsedTimeMs(start);
            }
            else {
               metricsContext.setConnectionLastOpen(bagEntry, now);
               metricsContext.stop();
               return ProxyFactory.getProxyConnection((HikariPool) this, bagEntry, leakTask.start());
            }
         }
         while (timeout > 0L);
      }
      catch (InterruptedException e) {
         throw new SQLException("Interrupted during connection acquisition", e);
      }
      finally {
         suspendResumeLock.release();
      }

      logPoolState("Timeout failure ");
      throw new SQLTimeoutException(String.format("Timeout after %dms of waiting for a connection.", elapsedTimeMs(start)), lastConnectionFailure.getAndSet(null));
   }


   /**
    * Release a connection back to the pool, or permanently close it if it is broken.
    *
    * @param bagEntry the PoolBagEntry to release back to the pool
    */
   public final void releaseConnection(final PoolBagEntry bagEntry)
   {
      metricsTracker.recordConnectionUsage(bagEntry);

      if (bagEntry.evicted) {
         LOGGER.debug("Connection returned to pool {} is broken or evicted.  Closing connection.", configuration.getPoolName());
         closeConnection(bagEntry);
      }
      else {
         bagEntry.lastAccess = System.currentTimeMillis();
         connectionBag.requite(bagEntry);
      }
   }

   /**
    * Shutdown the pool, closing all idle connections and aborting or closing
    * active connections.
    *
    * @throws InterruptedException thrown if the thread is interrupted during shutdown
    */
   public final void shutdown() throws InterruptedException
   {
      if (poolState != POOL_SHUTDOWN) {
         poolState = POOL_SHUTDOWN;
         LOGGER.info("HikariCP pool {} is shutting down.", configuration.getPoolName());

         logPoolState("Before shutdown ");
         connectionBag.close();
         softEvictConnections();
         houseKeepingExecutorService.shutdown();
         addConnectionExecutor.shutdown();
         houseKeepingExecutorService.awaitTermination(5L, TimeUnit.SECONDS);
         addConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);

         final ExecutorService assassinExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection assassin",
                                                                     configuration.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
         final long start = System.currentTimeMillis();
         do {
            softEvictConnections();
            abortActiveConnections(assassinExecutor);
         }
         while (getTotalConnections() > 0 && elapsedTimeMs(start) < TimeUnit.SECONDS.toMillis(5));

         assassinExecutor.shutdown();
         assassinExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         closeConnectionExecutor.shutdown();
         closeConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         logPoolState("After shutdown ");

         unregisterMBeans(configuration, this);
         metricsTracker.close();
      }
   }

   /**
    * Evict a connection from the pool.
    *
    * @param proxyConnection the connection to evict
    */
   public final void evictConnection(IHikariConnectionProxy proxyConnection)
   {
      closeConnection(proxyConnection.getPoolBagEntry());
   }

   /**
    * Get the wrapped DataSource.
    *
    * @return the wrapped DataSource
    */
   public final DataSource getDataSource()
   {
      return dataSource;
   }

   /**
    * Get the pool configuration object.
    *
    * @return the {@link HikariConfig} for this pool
    */
   public final HikariConfig getConfiguration()
   {
      return configuration;
   }

   @Override
   public String toString()
   {
      return configuration.getPoolName();
   }

   // ***********************************************************************
   //                        HikariPoolMBean methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public final int getActiveConnections()
   {
      return connectionBag.getCount(STATE_IN_USE);
   }

   /** {@inheritDoc} */
   @Override
   public final int getIdleConnections()
   {
      return connectionBag.getCount(STATE_NOT_IN_USE);
   }

   /** {@inheritDoc} */
   @Override
   public final int getTotalConnections()
   {
      return connectionBag.size() - connectionBag.getCount(STATE_REMOVED);
   }

   /** {@inheritDoc} */
   @Override
   public final int getThreadsAwaitingConnection()
   {
      return connectionBag.getPendingQueue();
   }

   /** {@inheritDoc} */
   @Override
   public final void suspendPool()
   {
      if (suspendResumeLock == GlobalPoolLock.FAUX_LOCK) {
         throw new IllegalStateException("Pool " + configuration.getPoolName() + " is not suspendable");
      }
      else if (poolState != POOL_SUSPENDED) {
         suspendResumeLock.suspend();
         poolState = POOL_SUSPENDED;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final void resumePool()
   {
      if (poolState == POOL_SUSPENDED) {
         poolState = POOL_RUNNING;
         addBagItem(); // re-populate the pool
         suspendResumeLock.resume();
      }
   }

   // ***********************************************************************
   //                           Protected methods
   // ***********************************************************************

   /**
    * Create and add a single connection to the pool.
    */
   protected final boolean addConnection()
   {
      // Speculative increment of totalConnections with expectation of success
      if (totalConnections.incrementAndGet() <= configuration.getMaximumPoolSize()) {
         Connection connection = null;
         try {
            connection = (username == null && password == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
            
            if (isUseJdbc4Validation && !poolUtils.isJdbc4ValidationSupported(connection)) {
               throw new SQLException("JDBC4 Connection.isValid() method not supported, connection test query must be configured");
            }
            
            final int originalTimeout = poolUtils.getAndSetNetworkTimeout(connection, connectionTimeout);
            
            transactionIsolation = (transactionIsolation < 0 ? connection.getTransactionIsolation() : transactionIsolation);
            
            poolUtils.setupConnection(connection, isAutoCommit, isReadOnly, transactionIsolation, catalog);
            connectionCustomizer.customize(connection);
            poolUtils.executeSql(connection, configuration.getConnectionInitSql(), isAutoCommit);
            poolUtils.setNetworkTimeout(connection, originalTimeout);
            
            connectionBag.add(new PoolBagEntry(connection, this));
            lastConnectionFailure.set(null);
            return true;
         }
         catch (Exception e) {
            lastConnectionFailure.set(e);
            poolUtils.quietlyCloseConnection(connection);
            LOGGER.debug("Connection attempt to database {} failed: {}", configuration.getPoolName(), e.getMessage(), e);
         }
      }
   
      totalConnections.decrementAndGet(); // We failed or pool is max, so undo speculative increment of totalConnections
      return false;
   }

   /**
    * Fill pool up from current idle connections (as they are perceived at the point of execution) to minimumIdle connections.
    */
   protected void fillPool()
   {
      final int connectionsToAdd = configuration.getMinimumIdle() - getIdleConnections();
      for (int i = 0; i < connectionsToAdd; i++) {
         addBagItem();
      }

      if (connectionsToAdd > 0 && LOGGER.isDebugEnabled()) {
         addConnectionExecutor.execute(new Runnable() {
            public void run() {
               logPoolState("After fill ");
            }
         });
      }
   }

   // ***********************************************************************
   //                           Abstract methods
   // ***********************************************************************

   /**
    * Permanently close the real (underlying) connection (eat any exception).
    *
    * @param connectionProxy the connection to actually close
    */
   protected abstract void closeConnection(final PoolBagEntry bagEntry);

   /**
    * Check whether the connection is alive or not.
    *
    * @param connection the connection to test
    * @return true if the connection is alive, false if it is not alive or we timed out
    */
   protected abstract boolean isConnectionAlive(final Connection connection);

   /**
    * Attempt to abort() active connections on Java7+, or close() them on Java6.
    * @param assassinExecutor 
    *
    * @throws InterruptedException 
    */
   protected abstract void abortActiveConnections(final ExecutorService assassinExecutor) throws InterruptedException;
   
   /**
    * Create the JVM version-specific ConcurrentBag instance used by the pool.
    *
    * @param listener the IBagStateListener instance
    * @return a ConcurrentBag instance
    */
   protected abstract ConcurrentBag<PoolBagEntry> createConcurrentBag(IBagStateListener listener);

   /**
    * Create the JVM version-specific Housekeeping runnable instance used by the pool.
    * @return the HouseKeeper instance
    */
   protected abstract Runnable getHouseKeeper();

   // ***********************************************************************
   //                           Private methods
   // ***********************************************************************

   /**
    * Fill the pool up to the minimum size.
    */
   private void initializeConnections()
   {
      if (configuration.isInitializationFailFast()) {
         try {
            try {
               if (!addConnection()) {
                  shutdown();
                  throw new RuntimeException("Fail-fast during pool initialization", lastConnectionFailure.getAndSet(null));
               }
   
               ConnectionProxy connection = (ConnectionProxy) getConnection();
               connection.getPoolBagEntry().evicted = (configuration.getMinimumIdle() == 0);
               connection.close();
            }
            catch (SQLException e) {
               shutdown();
               throw new RuntimeException("Fail-fast during pool initialization", e);
            }
         }
         catch (InterruptedException ie) {
            throw new RuntimeException("Fail-fast during pool initialization", ie);
         }
      }

      fillPool();
   }

   /**
    * Construct the user's connection customizer, if specified.
    *
    * @return an IConnectionCustomizer instance
    */
   private IConnectionCustomizer initializeCustomizer()
   {
      if (configuration.getConnectionCustomizerClassName() != null) {
         return createInstance(configuration.getConnectionCustomizerClassName(), IConnectionCustomizer.class);
      }

      return configuration.getConnectionCustomizer();
   }

   /**
    * @param healthCheckRegistry
    */
   private void registerHealthChecks(Object healthCheckRegistry)
   {
      if (healthCheckRegistry != null) {
         
      }
   }

   public final void logPoolState(String... prefix)
   {
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("{}pool stats {} (total={}, inUse={}, avail={}, waiting={})",
                      (prefix.length > 0 ? prefix[0] : ""), configuration.getPoolName(),
                      getTotalConnections(), getActiveConnections(), getIdleConnections(), getThreadsAwaitingConnection());
      }
   }
}
