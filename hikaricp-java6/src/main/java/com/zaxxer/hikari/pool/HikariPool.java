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
import static com.zaxxer.hikari.util.ConcurrentBag.STATE_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.STATE_REMOVED;
import static com.zaxxer.hikari.util.PoolUtilities.IS_JAVA7;
import static com.zaxxer.hikari.util.PoolUtilities.createInstance;
import static com.zaxxer.hikari.util.PoolUtilities.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.PoolUtilities.elapsedTimeMs;
import static com.zaxxer.hikari.util.PoolUtilities.executeSql;
import static com.zaxxer.hikari.util.PoolUtilities.getTransactionIsolation;
import static com.zaxxer.hikari.util.PoolUtilities.initializeDataSource;
import static com.zaxxer.hikari.util.PoolUtilities.isJdbc40Compliant;
import static com.zaxxer.hikari.util.PoolUtilities.quietlyCloseConnection;
import static com.zaxxer.hikari.util.PoolUtilities.quietlySleep;
import static com.zaxxer.hikari.util.PoolUtilities.setLoginTimeout;
import static com.zaxxer.hikari.util.PoolUtilities.setNetworkTimeout;
import static com.zaxxer.hikari.util.PoolUtilities.setQueryTimeout;
import static com.zaxxer.hikari.util.PoolUtilities.setupConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.IConnectionCustomizer;
import com.zaxxer.hikari.metrics.CodaHaleMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTracker.MetricsContext;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener;
import com.zaxxer.hikari.util.DefaultThreadFactory;
import com.zaxxer.hikari.util.FauxSemaphore;
import com.zaxxer.hikari.util.LeakTask;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public final class HikariPool implements HikariPoolMBean, IBagStateListener
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HikariPool.class);
   private static final long ALIVE_BYPASS_WINDOW = Long.getLong("com.zaxxer.hikari.aliveBypassWindow", 1000L);

   public final String catalog;
   public final boolean isReadOnly;
   public final boolean isAutoCommit;
   public int transactionIsolation;

   private final DataSource dataSource;

   private final HikariConfig configuration;
   private final MetricsTracker metricsTracker;
   private final ThreadPoolExecutor addConnectionExecutor;
   private final ConcurrentBag<PoolBagEntry> connectionBag;
   private final ThreadPoolExecutor closeConnectionExecutor;
   private final IConnectionCustomizer connectionCustomizer;
   private final Semaphore acquisitionSemaphore;

   private final LeakTask leakTask;
   private final AtomicInteger totalConnections;
   private final AtomicReference<Throwable> lastConnectionFailure;
   private final ScheduledThreadPoolExecutor houseKeepingExecutorService;

   private final String username;
   private final String password;
   private final boolean isRecordMetrics;
   private final boolean isIsolateInternalQueries;

   private volatile boolean isShutdown;
   private volatile long connectionTimeout;
   private volatile boolean isPoolSuspended;
   private volatile boolean isUseJdbc4Validation;

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
      this.username = username;
      this.password = password;
      this.configuration = configuration;

      this.connectionBag = new ConcurrentBag<PoolBagEntry>(this);
      this.totalConnections = new AtomicInteger();
      this.connectionTimeout = configuration.getConnectionTimeout();
      this.lastConnectionFailure = new AtomicReference<Throwable>();

      this.isReadOnly = configuration.isReadOnly();
      this.isAutoCommit = configuration.isAutoCommit();

      this.acquisitionSemaphore = configuration.isAllowPoolSuspension() ? new Semaphore(10000, true) : FauxSemaphore.FAUX_SEMAPHORE;

      this.catalog = configuration.getCatalog();
      this.connectionCustomizer = initializeCustomizer();
      this.transactionIsolation = getTransactionIsolation(configuration.getTransactionIsolation());
      this.isIsolateInternalQueries = configuration.isIsolateInternalQueries();

      this.isRecordMetrics = configuration.getMetricRegistry() != null;
      this.metricsTracker = (isRecordMetrics ? new CodaHaleMetricsTracker(this, (MetricRegistry) configuration.getMetricRegistry()) : new MetricsTracker(this));

      this.dataSource = initializeDataSource(configuration.getDataSourceClassName(), configuration.getDataSource(), configuration.getDataSourceProperties(), configuration.getJdbcUrl(), username, password);

      this.addConnectionExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection filler (pool " + configuration.getPoolName() + ")", configuration.getThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
      this.closeConnectionExecutor = createThreadPoolExecutor(4, "HikariCP connection closer (pool " + configuration.getPoolName() + ")", configuration.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

      long delayPeriod = Long.getLong("com.zaxxer.hikari.housekeeping.periodMs", TimeUnit.SECONDS.toMillis(30L));
      this.houseKeepingExecutorService = new ScheduledThreadPoolExecutor(1, configuration.getThreadFactory() != null ? configuration.getThreadFactory() : new DefaultThreadFactory("Hikari Housekeeping Timer (pool " + configuration.getPoolName() + ")", true));
      if (IS_JAVA7) {
         this.houseKeepingExecutorService.setRemoveOnCancelPolicy(true);
      }
      this.houseKeepingExecutorService.scheduleAtFixedRate(new HouseKeeper(), delayPeriod, delayPeriod, TimeUnit.MILLISECONDS);
      this.leakTask = (configuration.getLeakDetectionThreshold() == 0) ? LeakTask.NO_LEAK : new LeakTask(configuration.getLeakDetectionThreshold(), houseKeepingExecutorService);

      setLoginTimeout(dataSource, connectionTimeout, LOGGER);
      registerMBeans(configuration, this);
      fillPool();
   }

   /**
    * Get a connection from the pool, or timeout trying.
    *
    * @return a java.sql.Connection instance
    * @throws SQLException thrown if a timeout occurs trying to obtain a connection
    */
   public Connection getConnection() throws SQLException
   {
      acquisitionSemaphore.acquireUninterruptibly();
      long timeout = connectionTimeout;
      final long start = System.currentTimeMillis();
      final MetricsContext metricsContext = (isRecordMetrics ? metricsTracker.recordConnectionRequest(start) : MetricsTracker.NO_CONTEXT);

      try {
         do {
            final PoolBagEntry bagEntry = connectionBag.borrow(timeout, TimeUnit.MILLISECONDS);
            if (bagEntry == null) {
               break; // We timed out... break and throw exception
            }

            final long now = System.currentTimeMillis();
            if (now > bagEntry.expirationTime || (now - bagEntry.lastAccess > ALIVE_BYPASS_WINDOW && !isConnectionAlive(bagEntry.connection, timeout))) {
               closeConnection(bagEntry); // Throw away the dead connection and try again
               timeout = connectionTimeout - elapsedTimeMs(start);
            }
            else {
               metricsContext.setConnectionLastOpen(bagEntry, now);
               return ProxyFactory.getProxyConnection(this, bagEntry, leakTask.start());
            }
         }
         while (timeout > 0L);
      }
      catch (InterruptedException e) {
         throw new SQLException("Interrupted during connection acquisition", e);
      }
      finally {
         acquisitionSemaphore.release();
         metricsContext.stop();
      }

      logPoolState("Timeout failure ");
      throw new SQLException(String.format("Timeout after %dms of waiting for a connection.", elapsedTimeMs(start)), lastConnectionFailure.getAndSet(null));
   }

   /**
    * Release a connection back to the pool, or permanently close it if it is broken.
    *
    * @param bagEntry the PoolBagEntry to release back to the pool
    * @param isBroken true if the connection was detected as broken
    */
   public void releaseConnection(final PoolBagEntry bagEntry, final boolean isBroken)
   {
      metricsTracker.recordConnectionUsage(bagEntry);

      if (isBroken || bagEntry.evicted) {
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
   public void shutdown() throws InterruptedException
   {
      if (!isShutdown) {
         isShutdown = true;
         LOGGER.info("HikariCP pool {} is shutting down.", configuration.getPoolName());

         connectionBag.close();
         houseKeepingExecutorService.shutdownNow();
         addConnectionExecutor.shutdownNow();

         logPoolState("Before shutdown ");
         final long start = System.currentTimeMillis();
         do {
            softEvictConnections();
            abortActiveConnections();
         }
         while ((getIdleConnections() > 0 || getActiveConnections() > 0) && elapsedTimeMs(start) < TimeUnit.SECONDS.toMillis(5));

         closeConnectionExecutor.shutdown();
         closeConnectionExecutor.awaitTermination(5L, TimeUnit.SECONDS);
         logPoolState("After shutdown ");

         unregisterMBeans(configuration, this);
      }
   }

   /**
    * Evict a connection from the pool.
    *
    * @param proxyConnection the connection to evict
    */
   public void evictConnection(IHikariConnectionProxy proxyConnection)
   {
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
      class AddConnection implements Runnable
      {
         public void run()
         {
            long sleepBackoff = 200L;
            final int maxPoolSize = configuration.getMaximumPoolSize();
            final int minIdle = configuration.getMinimumIdle();
            while (!isShutdown && totalConnections.get() < maxPoolSize && (minIdle == 0 || getIdleConnections() < minIdle)) {
               if (addConnection()) {
                  if (minIdle == 0) {
                     break; // This break is here so we only add one connection when there is no min. idle
                  }
               }
               else {
                  if (minIdle == 0 && getThreadsAwaitingConnection() == 0) {
                     break;
                  }
                  
                  quietlySleep(sleepBackoff);
                  sleepBackoff = Math.min(connectionTimeout / 2, (long) ((double) sleepBackoff * 1.5));
               }
            }
         }
      }

      addConnectionExecutor.submit(new AddConnection());
   }

   // ***********************************************************************
   //                        HikariPoolMBean methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public int getActiveConnections()
   {
      return connectionBag.getCount(STATE_IN_USE);
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
      return connectionBag.size() - connectionBag.getCount(STATE_REMOVED);
   }

   /** {@inheritDoc} */
   @Override
   public int getThreadsAwaitingConnection()
   {
      return connectionBag.getPendingQueue();
   }

   /** {@inheritDoc} */
   @Override
   public void softEvictConnections()
   {
      for (PoolBagEntry bagEntry : connectionBag.values(STATE_IN_USE)) {
         bagEntry.evicted = true;
      }

      for (PoolBagEntry bagEntry : connectionBag.values(STATE_NOT_IN_USE)) {
         if (connectionBag.reserve(bagEntry)) {
            closeConnection(bagEntry);
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public void suspendPool()
   {
      if (!isPoolSuspended) {
         acquisitionSemaphore.acquireUninterruptibly(10000);
         isPoolSuspended = true;
      }
   }

   /** {@inheritDoc} */
   @Override
   public void resumePool()
   {
      if (isPoolSuspended) {
         acquisitionSemaphore.release(10000);
         isPoolSuspended = false;
      }
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
      connectionBag.remove(bagEntry);
      final int tc = totalConnections.decrementAndGet();
      if (tc < 0) {
         LOGGER.warn("Internal accounting inconsistency, totalConnections={}", tc, new Exception());
      }
      closeConnectionExecutor.submit(new Runnable() {
         public void run() {
            quietlyCloseConnection(bagEntry.connection);
         }
      });
   }

   /**
    * Create and add a single connection to the pool.
    */
   private boolean addConnection()
   {
      // Speculative increment of totalConnections with expectation of success
      if (totalConnections.incrementAndGet() > configuration.getMaximumPoolSize() || isShutdown || isPoolSuspended) {
         totalConnections.decrementAndGet();
         return true;
      }

      Connection connection = null;
      try {
         connection = (username == null && password == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);

         isUseJdbc4Validation = isJdbc40Compliant(connection) && configuration.getConnectionTestQuery() == null;
         if (!isUseJdbc4Validation && configuration.getConnectionTestQuery() == null) {
            LOGGER.error("JDBC4 Connection.isValid() method not supported, connection test query must be configured");
         }

         final boolean timeoutEnabled = (connectionTimeout != Integer.MAX_VALUE);
         final long timeoutMs = timeoutEnabled ? Math.max(250L, connectionTimeout) : 0L;
         final int originalTimeout = setNetworkTimeout(houseKeepingExecutorService, connection, timeoutMs, timeoutEnabled);

         transactionIsolation = (transactionIsolation < 0 ? connection.getTransactionIsolation() : transactionIsolation);
         
         setupConnection(connection, isAutoCommit, isReadOnly, transactionIsolation, catalog);
         connectionCustomizer.customize(connection);
         executeSql(connection, configuration.getConnectionInitSql(), isAutoCommit);
         setNetworkTimeout(houseKeepingExecutorService, connection, originalTimeout, timeoutEnabled);
         
         connectionBag.add(new PoolBagEntry(connection, configuration.getMaxLifetime()));
         lastConnectionFailure.set(null);
         return true;
      }
      catch (Exception e) {
         totalConnections.decrementAndGet(); // We failed, so undo speculative increment of totalConnections

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
   private boolean isConnectionAlive(final Connection connection, final long timeoutMs)
   {
      try {
         final boolean timeoutEnabled = (connectionTimeout != Integer.MAX_VALUE);
         int timeoutSec = timeoutEnabled ? (int) Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(timeoutMs)) : 0;

         if (isUseJdbc4Validation) {
            return connection.isValid(timeoutSec);
         }

         final int networkTimeout = setNetworkTimeout(houseKeepingExecutorService, connection, Math.max(250, (int) timeoutMs), timeoutEnabled);

         Statement statement = connection.createStatement();
         try {
            setQueryTimeout(statement, timeoutSec);
            statement.executeQuery(configuration.getConnectionTestQuery());
         }
         finally {
            statement.close();
         }

         if (isIsolateInternalQueries && !isAutoCommit) {
            connection.rollback();
         }

         setNetworkTimeout(houseKeepingExecutorService, connection, networkTimeout, timeoutEnabled);

         return true;
      }
      catch (SQLException e) {
         LOGGER.warn("Exception during keep alive check, that means the connection ({}) must be dead.", connection, e);
         return false;
      }
   }

   /**
    * Fill the pool up to the minimum size.
    */
   private void fillPool()
   {
      if (configuration.getMinimumIdle() > 0) {
         if (configuration.isInitializationFailFast() && !addConnection()) {
            throw new RuntimeException("Fail-fast during pool initialization", lastConnectionFailure.getAndSet(null));
         }

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
      ExecutorService assassinExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection assassin", configuration.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
      for (PoolBagEntry bagEntry : connectionBag.values(STATE_IN_USE)) {
         try {
            bagEntry.connection.abort(assassinExecutor);
         }
         catch (AbstractMethodError e) {
            quietlyCloseConnection(bagEntry.connection);
         }
         catch (NoSuchMethodError e) {
            quietlyCloseConnection(bagEntry.connection);
         }
         catch (SQLException e) {
            quietlyCloseConnection(bagEntry.connection);
         }
         finally {
            try {
               connectionBag.remove(bagEntry);
               totalConnections.decrementAndGet();
            }
            catch (IllegalStateException ise) {
               continue;
            }
         }
      }

      assassinExecutor.shutdown();
      assassinExecutor.awaitTermination(5L, TimeUnit.SECONDS);
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

   public void logPoolState(String... prefix)
   {
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("{}pool stats {} (total={}, inUse={}, avail={}, waiting={})",
                      (prefix.length > 0 ? prefix[0] : ""), configuration.getPoolName(),
                      getTotalConnections(), getActiveConnections(), getIdleConnections(), getThreadsAwaitingConnection());
      }
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

         for (PoolBagEntry bagEntry : connectionBag.values(STATE_NOT_IN_USE)) {
            if (connectionBag.reserve(bagEntry)) {
               if ((idleTimeout > 0L && now > bagEntry.lastAccess + idleTimeout) || (now > bagEntry.expirationTime)) {
                  closeConnection(bagEntry);
               }
               else {
                  connectionBag.unreserve(bagEntry);
               }
            }
         }

         logPoolState("After cleanup ");

         if (configuration.getMinimumIdle() > 0) {
            addBagItem(); // Try to maintain minimum connections
         }
      }
   }
}
