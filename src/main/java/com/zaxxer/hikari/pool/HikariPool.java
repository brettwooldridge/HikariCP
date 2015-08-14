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

import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_REMOVED;
import static com.zaxxer.hikari.util.UtilityElf.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
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
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.metrics.CodahaleHealthChecker;
import com.zaxxer.hikari.metrics.CodahaleMetricsTrackerFactory;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker.MetricsContext;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import com.zaxxer.hikari.proxy.ConnectionProxy;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.PropertyElf;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public class HikariPool implements HikariPoolMXBean, IBagStateListener
{
   final Logger LOGGER = LoggerFactory.getLogger(getClass());

   private static final ClockSource clockSource = ClockSource.INSTANCE;

   private final long ALIVE_BYPASS_WINDOW_MS = Long.getLong("com.zaxxer.hikari.aliveBypassWindow", TimeUnit.SECONDS.toMillis(1));
   private final long HOUSEKEEPING_PERIOD_MS = Long.getLong("com.zaxxer.hikari.housekeeping.periodMs", TimeUnit.SECONDS.toMillis(30));

   private static final int POOL_NORMAL = 0;
   private static final int POOL_SUSPENDED = 1;
   private static final int POOL_SHUTDOWN = 2;

   final PoolElf poolElf;
   final HikariConfig config;
   final ConcurrentBag<PoolBagEntry> connectionBag;
   final ScheduledThreadPoolExecutor houseKeepingExecutorService;

   private final AtomicInteger totalConnections;
   private final ThreadPoolExecutor addConnectionExecutor;
   private final ThreadPoolExecutor closeConnectionExecutor;

   private volatile int poolState;
   private long connectionTimeout;

   private final String poolName;
   private final LeakTask leakTask;
   private final DataSource dataSource;
   private final SuspendResumeLock suspendResumeLock;
   private final AtomicReference<Throwable> lastConnectionFailure;

   private volatile MetricsTracker metricsTracker;
   private boolean isRecordMetrics;

   /**
    * Construct a HikariPool with the specified configuration.
    *
    * @param config a HikariConfig instance
    */
   public HikariPool(final HikariConfig config)
    {
      this.config = config;

      this.poolElf = new PoolElf(config);
      this.dataSource = poolElf.initializeDataSource();

      this.poolName = config.getPoolName();
      this.connectionBag = new ConcurrentBag<>(this);
      this.totalConnections = new AtomicInteger();
      this.connectionTimeout = config.getConnectionTimeout();
      this.lastConnectionFailure = new AtomicReference<>();
      this.suspendResumeLock = config.isAllowPoolSuspension() ? new SuspendResumeLock(true) : SuspendResumeLock.FAUX_LOCK;

      this.addConnectionExecutor = createThreadPoolExecutor(config.getMaximumPoolSize(), "Hikari connection filler (pool " + poolName + ")", config.getThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
      this.closeConnectionExecutor = createThreadPoolExecutor(4, "Hikari connection closer (pool " + poolName + ")", config.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

      if (config.getScheduledExecutorService() == null) {
         ThreadFactory threadFactory = config.getThreadFactory() != null ? config.getThreadFactory() : new DefaultThreadFactory("Hikari housekeeper (pool " + poolName + ")", true);
         this.houseKeepingExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
         this.houseKeepingExecutorService.scheduleAtFixedRate(new HouseKeeper(), HOUSEKEEPING_PERIOD_MS, HOUSEKEEPING_PERIOD_MS, TimeUnit.MILLISECONDS);
         this.houseKeepingExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
         this.houseKeepingExecutorService.setRemoveOnCancelPolicy(true);
      }
      else {
         this.houseKeepingExecutorService = config.getScheduledExecutorService();
      }

      this.leakTask = new LeakTask(config.getLeakDetectionThreshold(), houseKeepingExecutorService);
      
      if (config.getMetricsTrackerFactory() != null) {
         setMetricsTrackerFactory(config.getMetricsTrackerFactory());
      }
      else {
         setMetricRegistry(config.getMetricRegistry());
      }

      setHealthCheckRegistry(config.getHealthCheckRegistry());

      poolElf.registerMBeans(this);

      PropertyElf.flushCaches();

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
      final long startTime = clockSource.currentTime();

      try {
         long timeout = hardTimeout;
         final MetricsContext metricsContext = (isRecordMetrics ? metricsTracker.recordConnectionRequest() : MetricsTracker.NO_CONTEXT);
         do {
            final PoolBagEntry bagEntry = connectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
            if (bagEntry == null) {
               break; // We timed out... break and throw exception
            }

            final long now = clockSource.currentTime();
            if (bagEntry.evicted || (clockSource.elapsedMillis(bagEntry.lastAccess, now) > ALIVE_BYPASS_WINDOW_MS && !poolElf.isConnectionAlive(bagEntry.connection, lastConnectionFailure))) {
               closeConnection(bagEntry, "(connection evicted or dead)"); // Throw away the dead connection and try again
               timeout = hardTimeout - clockSource.elapsedMillis(startTime, now);
            }
            else {
               metricsContext.setConnectionLastOpen(bagEntry, now);
               metricsContext.stop();
               return ProxyFactory.getProxyConnection(bagEntry, leakTask.start(bagEntry), now);
            }
         }
         while (timeout > 0L);
      }
      catch (InterruptedException e) {
         throw new SQLException(poolName + " - Interrupted during connection acquisition", e);
      }
      finally {
         suspendResumeLock.release();
      }

      logPoolState("Timeout failure ");
      String sqlState = null;
      final Throwable originalException = lastConnectionFailure.getAndSet(null);
      if (originalException instanceof SQLException) {
         sqlState = ((SQLException) originalException).getSQLState();
      }
      throw new SQLTransientConnectionException(poolName + " - Connection is not available, request timed out after " + clockSource.elapsedMillis(startTime) + "ms.", sqlState, originalException);
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
         closeConnection(bagEntry, "(connection broken or evicted)");
      }
      else {
         connectionBag.requite(bagEntry);
      }
   }

   /**
    * Shutdown the pool, closing all idle connections and aborting or closing
    * active connections.
    *
    * @throws InterruptedException thrown if the thread is interrupted during shutdown
    */
   public final synchronized void shutdown() throws InterruptedException
   {
      try {
         poolState = POOL_SHUTDOWN;

         LOGGER.info("{} - is closing down.", poolName);
         logPoolState("Before closing ");

         connectionBag.close();
         softEvictConnections();
         addConnectionExecutor.shutdown();
         addConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         if (config.getScheduledExecutorService() == null) {
            houseKeepingExecutorService.shutdown();
            houseKeepingExecutorService.awaitTermination(5L, TimeUnit.SECONDS);
         }

         final ExecutorService assassinExecutor = createThreadPoolExecutor(config.getMaximumPoolSize(), "Hikari connection assassin",
                                                                           config.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
         try {
            final long start = clockSource.currentTime();
            do {
               softEvictConnections();
               abortActiveConnections(assassinExecutor);
            }
            while (getTotalConnections() > 0 && clockSource.elapsedMillis(start) < TimeUnit.SECONDS.toMillis(5));
         } finally {
            assassinExecutor.shutdown();
            assassinExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         }

         poolElf.shutdownTimeoutExecutor();
         closeConnectionExecutor.shutdown();
         closeConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);
      }
      finally {
         logPoolState("After closing ");

         poolElf.unregisterMBeans();
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
      closeConnection(proxyConnection.getPoolBagEntry(), "(connection evicted by user)");
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

   public void setMetricRegistry(Object metricRegistry)
   {
      this.isRecordMetrics = metricRegistry != null;
      if (isRecordMetrics) {
         setMetricsTrackerFactory(new CodahaleMetricsTrackerFactory((MetricRegistry) metricRegistry));
      }
      else {
         setMetricsTrackerFactory(null);
      }
   }

   public void setMetricsTrackerFactory(MetricsTrackerFactory metricsTrackerFactory)
   {
      this.isRecordMetrics = metricsTrackerFactory != null;
      if (isRecordMetrics) {
         this.metricsTracker = metricsTrackerFactory.create(config.getPoolName(), getPoolStats());
      }
      else {
         this.metricsTracker = new MetricsTracker();
      }
   }

   public void setHealthCheckRegistry(Object healthCheckRegistry)
   {
      if (healthCheckRegistry != null) {
         CodahaleHealthChecker.registerHealthChecks(this, config, (HealthCheckRegistry) healthCheckRegistry);
      }
   }

   /**
    * Log the current pool state at debug level.
    *
    * @param prefix an optional prefix to prepend the log message 
    */
   public final void logPoolState(String... prefix)
   {
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("{}pool {} stats (total={}, active={}, idle={}, waiting={})",
                      (prefix.length > 0 ? prefix[0] : ""), poolName,
                      getTotalConnections(), getActiveConnections(), getIdleConnections(), getThreadsAwaitingConnection());
      }
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return poolName;
   }

   // ***********************************************************************
   //                        IBagStateListener callback
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public Future<Boolean> addBagItem()
   {
      FutureTask<Boolean> future = new FutureTask<>(new Runnable() {
         @Override
         public void run()
         {
            long sleepBackoff = 200L;
            final int minimumIdle = config.getMinimumIdle();
            final int maxPoolSize = config.getMaximumPoolSize();
            while (poolState == POOL_NORMAL && totalConnections.get() < maxPoolSize && getIdleConnections() <= minimumIdle && !addConnection()) {
               // If we got into the loop, addConnection() failed, so we sleep and retry
               quietlySleep(sleepBackoff);
               sleepBackoff = Math.min(connectionTimeout / 2, (long) (sleepBackoff * 1.5));
            }
         }
      }, true);

      addConnectionExecutor.execute(future);
      return future;
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
   public void softEvictConnections()
   {
      for (PoolBagEntry bagEntry : connectionBag.values()) {
         bagEntry.evicted = true;
         if (connectionBag.reserve(bagEntry)) {
            closeConnection(bagEntry, "(connection evicted by user)");
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public final synchronized void suspendPool()
   {
      if (suspendResumeLock == SuspendResumeLock.FAUX_LOCK) {
         throw new IllegalStateException(poolName + " - is not suspendable");
      }
      else if (poolState != POOL_SUSPENDED) {
         suspendResumeLock.suspend();
         poolState = POOL_SUSPENDED;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final synchronized void resumePool()
   {
      if (poolState == POOL_SUSPENDED) {
         poolState = POOL_NORMAL;
         fillPool();
         suspendResumeLock.resume();
      }
   }

   // ***********************************************************************
   //                           Package methods
   // ***********************************************************************

   /**
    * Permanently close the real (underlying) connection (eat any exception).
    *
    * @param bagEntry the connection to actually close
    */
   void closeConnection(final PoolBagEntry bagEntry, final String closureReason)
   {
      final Connection connection = bagEntry.connection;
      bagEntry.close();
      if (connectionBag.remove(bagEntry)) {
         final int tc = totalConnections.decrementAndGet();
         if (tc < 0) {
            LOGGER.warn("{} - Internal accounting inconsistency, totalConnections={}", poolName, tc, new Exception());
         }
         
         closeConnectionExecutor.execute(new Runnable() {
            @Override
            public void run() {
               poolElf.quietlyCloseConnection(connection, closureReason);
            }
         });
      }
   }

   // ***********************************************************************
   //                           Private methods
   // ***********************************************************************

   /**
    * Create and add a single connection to the pool.
    */
   private boolean addConnection()
   {
      // Speculative increment of totalConnections with expectation of success
      if (totalConnections.incrementAndGet() > config.getMaximumPoolSize()) {
         totalConnections.decrementAndGet(); // Pool is maxed out, so undo speculative increment of totalConnections
         lastConnectionFailure.set(new SQLException(poolName + " - is at maximum capacity"));
         return true;
      }

      Connection connection = null;
      try {
         String username = config.getUsername();
         String password = config.getPassword();

         connection = (username == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         poolElf.setupConnection(connection, connectionTimeout);

         connectionBag.add(new PoolBagEntry(connection, this));
         lastConnectionFailure.set(null);
         LOGGER.debug("{} - Added connection {}", poolName, connection);

         return true;
      }
      catch (Exception e) {
         totalConnections.decrementAndGet(); // We failed, so undo speculative increment of totalConnections
         lastConnectionFailure.set(e);
         if (poolState == POOL_NORMAL) {
            LOGGER.debug("{} - Cannot acquire connection from data source", poolName, e);
         }
         poolElf.quietlyCloseConnection(connection, "(exception during connection creation)");
         return false;
      }
   }

   /**
    * Fill pool up from current idle connections (as they are perceived at the point of execution) to minimumIdle connections.
    */
   private void fillPool()
   {
      final int connectionsToAdd = Math.min(config.getMaximumPoolSize() - totalConnections.get(), config.getMinimumIdle() - getIdleConnections());
      for (int i = 0; i < connectionsToAdd; i++) {
         addBagItem();
      }

      if (connectionsToAdd > 0 && LOGGER.isDebugEnabled()) {
         addConnectionExecutor.execute(new Runnable() {
            @Override
            public void run() {
               logPoolState("After fill ");
            }
         });
      }
   }

   /**
    * Attempt to abort() active connections, or close() them.
    */
   private void abortActiveConnections(final ExecutorService assassinExecutor)
   {
      for (PoolBagEntry bagEntry : connectionBag.values(STATE_IN_USE)) {
         try {
            bagEntry.aborted = bagEntry.evicted = true;
            bagEntry.connection.abort(assassinExecutor);
         }
         catch (Throwable e) {
            poolElf.quietlyCloseConnection(bagEntry.connection, "(connection aborted during shutdown)");
         }
         finally {
            bagEntry.close();
            if (connectionBag.remove(bagEntry)) {
               totalConnections.decrementAndGet();
            }
         }
      }
   }
   
   /**
    * Fill the pool up to the minimum size.
    */
   private void initializeConnections()
   {
      if (config.isInitializationFailFast()) {
         try {
            if (!addConnection()) {
               throw lastConnectionFailure.getAndSet(null);
            }

            ConnectionProxy connection = (ConnectionProxy) getConnection();
            connection.getPoolBagEntry().evicted = (config.getMinimumIdle() == 0);
            connection.close();
         }
         catch (Throwable e) {
            try {
               shutdown();
            }
            catch (Throwable ex) {
               e.addSuppressed(ex);
            }

            throw new PoolInitializationException(e);
         }
      }

      fillPool();
   }

   private PoolStats getPoolStats()
   {
      return new PoolStats(TimeUnit.SECONDS.toMillis(1)) {
         @Override
         protected void update() {
            this.pendingThreads = HikariPool.this.getThreadsAwaitingConnection();
            this.idleConnections = HikariPool.this.getIdleConnections();
            this.totalConnections = HikariPool.this.getTotalConnections();
            this.activeConnections = HikariPool.this.getActiveConnections();
         }
      };
   }

   // ***********************************************************************
   //                      Non-anonymous Inner-classes
   // ***********************************************************************

   /**
    * The house keeping task to retire idle connections.
    */
   private class HouseKeeper implements Runnable
   {
      private volatile long previous = clockSource.currentTime();

      @Override
      public void run()
      {
         // refresh timeouts in case they changed via MBean
         connectionTimeout = config.getConnectionTimeout();
         poolElf.setValidationTimeout(config.getValidationTimeout());
         leakTask.updateLeakDetectionThreshold(config.getLeakDetectionThreshold());

         final long now = clockSource.currentTime();
         final long idleTimeout = config.getIdleTimeout();

         // Detect retrograde time as well as forward leaps of unacceptable duration
         if (now < previous || now > clockSource.plusMillis(previous, (2 * HOUSEKEEPING_PERIOD_MS))) {
            LOGGER.warn("{} - Unusual system clock change detected, soft-evicting connections from pool.", poolName);
            previous = now;
            softEvictConnections();
            fillPool();
            return;
         }
         else {
            previous = now;
         }

         logPoolState("Before cleanup ");
         final List<PoolBagEntry> bag = connectionBag.values(STATE_NOT_IN_USE);
         int removable = bag.size() - config.getMinimumIdle();
         for (PoolBagEntry bagEntry : bag) {
            if (connectionBag.reserve(bagEntry)) {
               if (removable <= 0) {
                  break;
               }
               if (idleTimeout > 0L && clockSource.elapsedMillis(bagEntry.lastAccess, now) > idleTimeout) {
                  closeConnection(bagEntry, "(connection passed idleTimeout)");
                  removable--;
               }
               else {
                  connectionBag.unreserve(bagEntry);
               }
            }
         }
         
         logPoolState("After cleanup ");

         fillPool(); // Try to maintain minimum connections
      }
   }
}
