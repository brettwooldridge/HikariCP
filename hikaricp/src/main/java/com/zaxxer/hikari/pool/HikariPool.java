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

import static com.zaxxer.hikari.util.ConcurrentBag.STATE_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.PoolUtilities.createInstance;
import static com.zaxxer.hikari.util.PoolUtilities.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.PoolUtilities.elapsedTimeMs;
import static com.zaxxer.hikari.util.PoolUtilities.executeSqlAutoCommit;
import static com.zaxxer.hikari.util.PoolUtilities.quietlyCloseConnection;
import static com.zaxxer.hikari.util.PoolUtilities.quietlySleep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.IConnectionCustomizer;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.IMetricsTracker.MetricsContext;
import com.zaxxer.hikari.metrics.MetricsFactory;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PoolUtilities;
import com.zaxxer.hikari.util.PropertyBeanSetter;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public final class HikariPool implements HikariPoolMBean, IBagStateListener
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HikariPool.class);

   public int transactionIsolation;
   public final String catalog;
   public final boolean isAutoCommit;
   public final boolean isReadOnly;

   private final DataSource dataSource;

   private final IConnectionCustomizer connectionCustomizer;
   private final HikariConfig configuration;
   private final ConcurrentBag<PoolBagEntry> connectionBag;
   private final ThreadPoolExecutor addConnectionExecutor;
   private final ThreadPoolExecutor closeConnectionExecutor;
   private final IMetricsTracker metricsTracker;

   private final AtomicReference<Throwable> lastConnectionFailure;
   private final AtomicInteger totalConnections;
   private final ScheduledThreadPoolExecutor houseKeepingExecutorService;

   private final boolean isIsolateInternalQueries;
   private final boolean isRecordMetrics;
   private final boolean isRegisteredMbeans;
   private final boolean isJdbc4ConnectionTest;
   private final long leakDetectionThreshold;
   private final String username;
   private final String password;

   private volatile long connectionTimeout;
   private volatile boolean isShutdown;

   /**
    * Construct a HikariPool with the specified configuration.
    *
    * @param configuration a HikariConfig instance
    */
   public HikariPool(HikariConfig configuration)
   {
      this(configuration, configuration.getUsername(), configuration.getPassword());
   }

   /**
    * Construct a HikariPool with the specified configuration.
    *
    * @param configuration a HikariConfig instance
    * @param username authentication username
    * @param password authentication password
    */
   public HikariPool(HikariConfig configuration, String username, String password)
   {
      this.configuration = configuration;
      this.username = username;
      this.password = password;

      this.totalConnections = new AtomicInteger();
      this.connectionBag = new ConcurrentBag<PoolBagEntry>(this);
      this.lastConnectionFailure = new AtomicReference<Throwable>();
      this.connectionTimeout = configuration.getConnectionTimeout();

      this.catalog = configuration.getCatalog();
      this.connectionCustomizer = initializeCustomizer();
      this.isAutoCommit = configuration.isAutoCommit();
      this.isIsolateInternalQueries = configuration.isIsolateInternalQueries();
      this.isReadOnly = configuration.isReadOnly();
      this.isRegisteredMbeans = configuration.isRegisterMbeans();
      this.isJdbc4ConnectionTest = configuration.isJdbc4ConnectionTest();
      this.leakDetectionThreshold = configuration.getLeakDetectionThreshold();
      this.transactionIsolation = PoolUtilities.getTransactionIsolation(configuration.getTransactionIsolation());
      this.isRecordMetrics = configuration.isRecordMetrics();
      this.metricsTracker = MetricsFactory.createMetricsTracker((isRecordMetrics ? configuration.getMetricsTrackerClassName()
            : "com.zaxxer.hikari.metrics.MetricsTracker"), this);

      this.dataSource = initializeDataSource();

      if (isRegisteredMbeans) {
         HikariMBeanElf.registerMBeans(configuration, this);
      }

      addConnectionExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection filler (pool " + configuration.getPoolName() + ")", configuration.getThreadFactory());
      closeConnectionExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection closer (pool " + configuration.getPoolName() + ")", configuration.getThreadFactory());

      fillPool();

      long delayPeriod = Long.getLong("com.zaxxer.hikari.housekeeping.periodMs", TimeUnit.SECONDS.toMillis(30L));
      houseKeepingExecutorService = new ScheduledThreadPoolExecutor(1, configuration.getThreadFactory() != null ? configuration.getThreadFactory() : new DefaultThreadFactory("Hikari Housekeeping Timer (pool " + configuration.getPoolName() + ")", true));
      houseKeepingExecutorService.setRemoveOnCancelPolicy(true);
      houseKeepingExecutorService.scheduleAtFixedRate(new HouseKeeper(), delayPeriod, delayPeriod, TimeUnit.MILLISECONDS);
   }

   /**
    * Get a connection from the pool, or timeout trying.
    *
    * @return a java.sql.Connection instance
    * @throws SQLException thrown if a timeout occurs trying to obtain a connection
    */
   public Connection getConnection() throws SQLException
   {
      final long start = System.currentTimeMillis();
      final MetricsContext context = (isRecordMetrics ? metricsTracker.recordConnectionRequest(start) : MetricsTracker.NO_CONTEXT);
      long timeout = connectionTimeout;

      try {
         do {
            final PoolBagEntry bagEntry = connectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
            if (bagEntry == null) {
               break; // We timed out... break and throw exception
            }

            final long now = System.currentTimeMillis();
            if (now > bagEntry.expirationTime || (now - bagEntry.lastAccess > 1000L && !isConnectionAlive(bagEntry.connection, timeout))) {
               closeConnection(bagEntry); // Throw away the dead connection and try again
               timeout = connectionTimeout - elapsedTimeMs(start);
               continue;
            }

            final IHikariConnectionProxy proxyConnection = ProxyFactory.getProxyConnection(this, bagEntry);

            if (leakDetectionThreshold != 0) {
               proxyConnection.captureStack(leakDetectionThreshold, houseKeepingExecutorService);
            }

            if (isRecordMetrics) {
               bagEntry.lastOpenTime = now;
            }

            return proxyConnection;
         }
         while (timeout > 0L);
      }
      catch (InterruptedException e) {
         throw new SQLException("Interrupted during connection acquisition", e);
      }
      finally {
         context.stop();
      }

      logPoolState("Timeout failure ");
      throw new SQLException(String.format("Timeout of %dms encountered waiting for connection.", configuration.getConnectionTimeout()),
                             lastConnectionFailure.getAndSet(null));
   }

   /**
    * Release a connection back to the pool, or permanently close it if it is broken.
    *
    * @param bagEntry the PoolBagEntry to release back to the pool
    * @param isBroken true if the connection was detected as broken
    */
   public void releaseConnection(final PoolBagEntry bagEntry, final boolean isBroken)
   {
      if (isRecordMetrics) {
         metricsTracker.recordConnectionUsage(elapsedTimeMs(bagEntry.lastOpenTime));
      }

      if (isBroken || isShutdown) {
         LOGGER.debug("Connection returned to pool {} is broken, or the pool is shutting down.  Closing connection.", configuration.getPoolName());
         closeConnection(bagEntry);
         return;
      }

      bagEntry.lastAccess = System.currentTimeMillis();
      connectionBag.requite(bagEntry);
   }

   /**
    * Shutdown the pool, closing all idle connections and aborting or closing
    * active connections.
    *
    * @throws InterruptedException thrown if the thread is interrupted during shutdown
    */
   public void shutdown() throws InterruptedException
   {
      if (!isShutdown) {
         isShutdown = true;
         LOGGER.info("HikariCP pool {} is shutting down.", configuration.getPoolName());

         logPoolState("Before shutdown ");
         houseKeepingExecutorService.shutdownNow();
         addConnectionExecutor.shutdownNow();

         final long start = System.currentTimeMillis();
         do {
            closeIdleConnections();
            abortActiveConnections();
         }
         while ((getIdleConnections() > 0 || getActiveConnections() > 0) && elapsedTimeMs(start) < TimeUnit.SECONDS.toMillis(5));

         closeConnectionExecutor.shutdown();
         closeConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);

         logPoolState("After shutdown ");

         if (isRegisteredMbeans) {
            HikariMBeanElf.unregisterMBeans(configuration, this);
         }
      }
   }

   public void evictConnection(IHikariConnectionProxy proxyConnection) {
      closeConnection(proxyConnection.getPoolBagEntry());
   }

   /**
    * Get the wrapped DataSource.
    *
    * @return the wrapped DataSource
    */
   public DataSource getDataSource()
   {
      return dataSource;
   }

   /**
    * Get the pool configuration object.
    *
    * @return the {@link HikariConfig} for this pool
    */
   public HikariConfig getConfiguration()
   {
      return configuration;
   }

   /**
    * Get the metrics tracker for this pool.
    *
    * @return the metrics tracker
    */
   public IMetricsTracker getMetricsTracker()
   {
      return metricsTracker;
   }

   @Override
   public String toString()
   {
      return configuration.getPoolName();
   }

   // ***********************************************************************
   //                        IBagStateListener callback
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public void addBagItem()
   {
      addConnectionExecutor.submit( () -> {
         long sleepBackoff = 200L;
         final int maxPoolSize = configuration.getMaximumPoolSize();
         final int minIdle = configuration.getMinimumIdle();
         while (!isShutdown && totalConnections.get() < maxPoolSize && (minIdle == 0 || getIdleConnections() < minIdle)) {
            if (!addConnection()) {
               quietlySleep(sleepBackoff);
               sleepBackoff = Math.min(connectionTimeout / 2, (long) ((double) sleepBackoff * 1.5));
               continue;
            }

            if (minIdle == 0) {
               break; // This break is here so we only add one connection when there is no min. idle
            }
         }
      });
   }

   // ***********************************************************************
   //                        HikariPoolMBean methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public int getActiveConnections()
   {
      return Math.min(configuration.getMaximumPoolSize(), totalConnections.get() - getIdleConnections());
   }

   /** {@inheritDoc} */
   @Override
   public int getIdleConnections()
   {
      return connectionBag.getCount(STATE_NOT_IN_USE);
   }

   /** {@inheritDoc} */
   @Override
   public int getTotalConnections()
   {
      return totalConnections.get();
   }

   /** {@inheritDoc} */
   @Override
   public int getThreadsAwaitingConnection()
   {
      return connectionBag.getPendingQueue();
   }

   /** {@inheritDoc} */
   @Override
   public void closeIdleConnections()
   {
      connectionBag.values(STATE_NOT_IN_USE).forEach(bagEntry -> {
         if (connectionBag.reserve(bagEntry)) {
            closeConnection(bagEntry);
         }
      });
   }

   /** {@inheritDoc} */
   @Override
   public void dumpPoolState()
   {
      connectionBag.dumpState();
   }

   // ***********************************************************************
   //                           Private methods
   // ***********************************************************************

   /**
    * Permanently close the real (underlying) connection (eat any exception).
    *
    * @param connectionProxy the connection to actually close
    */
   private void closeConnection(final PoolBagEntry bagEntry)
   {
      int tc = totalConnections.decrementAndGet();
      connectionBag.remove(bagEntry);
      if (tc < 0) {
         LOGGER.warn("Internal accounting inconsistency, totalConnections={}", tc, new Exception());
      }
      closeConnectionExecutor.submit(() -> { quietlyCloseConnection(bagEntry.connection); });
   }

   /**
    * Create and add a single connection to the pool.
    */
   private boolean addConnection()
   {
      Connection connection = null;
      try {
         // Speculative increment of totalConnections with expectation of success
         if (totalConnections.incrementAndGet() > configuration.getMaximumPoolSize() || isShutdown) {
            totalConnections.decrementAndGet();
            return true;
         }

         connection = (username == null && password == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         transactionIsolation = (transactionIsolation < 0 ? connection.getTransactionIsolation() : transactionIsolation);
         connectionCustomizer.customize(connection);

         executeSqlAutoCommit(connection, configuration.getConnectionInitSql());

         connection.setAutoCommit(isAutoCommit);
         connection.setTransactionIsolation(transactionIsolation);
         connection.setReadOnly(isReadOnly);
         if (catalog != null) {
            connection.setCatalog(catalog);
         }

         PoolBagEntry bagEntry = new PoolBagEntry(connection, configuration.getMaxLifetime());

         connectionBag.add(bagEntry);
         lastConnectionFailure.set(null);
         return true;
      }
      catch (Exception e) {
         // We failed, so undo speculative increment of totalConnections
         totalConnections.decrementAndGet();

         lastConnectionFailure.set(e);
         quietlyCloseConnection(connection);
         LOGGER.debug("Connection attempt to database {} failed: {}", configuration.getPoolName(), e.getMessage(), e);
         return false;
      }
   }

   /**
    * Check whether the connection is alive or not.
    *
    * @param connection the connection to test
    * @param timeoutMs the timeout before we consider the test a failure
    * @return true if the connection is alive, false if it is not alive or we timed out
    */
   private boolean isConnectionAlive(final Connection connection, long timeoutMs)
   {
      try {
         final boolean timeoutEnabled = (configuration.getConnectionTimeout() != Integer.MAX_VALUE);
         timeoutMs = timeoutEnabled ? Math.max(1000L, timeoutMs) : 0L;

         if (isJdbc4ConnectionTest) {
            return connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
         }

         try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
            statement.executeQuery(configuration.getConnectionTestQuery());
            return true;
         }
         finally {
            if (isIsolateInternalQueries && !isAutoCommit) {
               connection.rollback();
            }
         }
      }
      catch (SQLException e) {
         LOGGER.warn("Exception during keep alive check, that means the connection (" + connection + ") must be dead.", e);
         return false;
      }
   }

   /**
    * Fill the pool up to the minimum size.
    */
   private void fillPool()
   {
      if (configuration.isInitializationFailFast()) {
         for (int maxIters = configuration.getMinimumIdle(); maxIters > 0; maxIters--) {
            if (!addConnection()) {
               throw new RuntimeException("Fail-fast during pool initialization", lastConnectionFailure.getAndSet(null));
            }
         }
      }
      else if (configuration.getMinimumIdle() > 0) {
         addBagItem();
      }
   }

   /**
    * Attempt to abort() active connections on Java7+, or close() them on Java6.
    *
    * @throws InterruptedException 
    */
   private void abortActiveConnections() throws InterruptedException
   {
      ExecutorService assassinExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection assassin", configuration.getThreadFactory());
      connectionBag.values(STATE_IN_USE).parallelStream().forEach(bagEntry -> {
         try {
            bagEntry.connection.abort(assassinExecutor);
         }
         catch (SQLException | AbstractMethodError e) {
            quietlyCloseConnection(bagEntry.connection);
         }
         finally {
            try {
               connectionBag.remove(bagEntry);
               totalConnections.decrementAndGet();
            }
            catch (IllegalStateException ise) {
               return;
            }
         }
      });

      assassinExecutor.shutdown();
      assassinExecutor.awaitTermination(5L, TimeUnit.SECONDS);
   }

   /**
    * Create/initialize the underlying DataSource.
    *
    * @return the DataSource
    */
   private DataSource initializeDataSource()
   {
      String dsClassName = configuration.getDataSourceClassName();
      if (configuration.getDataSource() == null && dsClassName != null) {
         DataSource dataSource = createInstance(dsClassName, DataSource.class);
         PropertyBeanSetter.setTargetFromProperties(dataSource, configuration.getDataSourceProperties());
         return dataSource;
      }
      else if (configuration.getJdbcUrl() != null) {
         return new DriverDataSource(configuration.getJdbcUrl(), configuration.getDataSourceProperties(), username, password);
      }

      return configuration.getDataSource();
   }

   private IConnectionCustomizer initializeCustomizer()
   {
      if (configuration.getConnectionCustomizerClassName() != null) {
         return createInstance(configuration.getConnectionCustomizerClassName(), IConnectionCustomizer.class);
      }

      return configuration.getConnectionCustomizer();
   }

   private void logPoolState(String... prefix)
   {
      int total = totalConnections.get();
      int idle = getIdleConnections();
      LOGGER.debug("{}pool stats {} (total={}, inUse={}, avail={}, waiting={})", (prefix.length > 0 ? prefix[0] : ""), configuration.getPoolName(), total,
                   total - idle, idle, getThreadsAwaitingConnection());
   }

   /**
    * The house keeping task to retire idle and maxAge connections.
    */
   private class HouseKeeper implements Runnable
   {
      @Override
      public void run()
      {
         logPoolState("Before cleanup ");

         connectionTimeout = configuration.getConnectionTimeout(); // refresh member in case it changed

         final long now = System.currentTimeMillis();
         final long idleTimeout = configuration.getIdleTimeout();

         connectionBag.values(STATE_NOT_IN_USE).forEach(bagEntry -> {
            if (connectionBag.reserve(bagEntry)) {
               if ((idleTimeout > 0L && now > bagEntry.lastAccess + idleTimeout) || (now > bagEntry.expirationTime)) {
                  closeConnection(bagEntry);
                  return;
               }

               connectionBag.unreserve(bagEntry);
            }
         });

         logPoolState("After cleanup ");

         if (configuration.getMinimumIdle() > 0) {
            addBagItem(); // Try to maintain minimum connections
         }
      }
   }
}
