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

import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.UtilityElf.createThreadPoolExecutor;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.IBagStateListener;
import com.zaxxer.hikari.util.Java8ConcurrentBag;

/**
 * This is the primary connection pool class that provides the basic
 * pooling behavior for HikariCP.
 *
 * @author Brett Wooldridge
 */
public final class HikariPool extends BaseHikariPool
{
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
      super(configuration, username, password);
   }

   // ***********************************************************************
   //                        IBagStateListener callback
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public void addBagItem()
   {
      addConnectionExecutor.execute( () -> {
         long sleepBackoff = 200L;
         final int maxPoolSize = configuration.getMaximumPoolSize();
         final int minIdle = configuration.getMinimumIdle();
         while (!isPoolSuspended && !isShutdown && totalConnections.get() < maxPoolSize && (minIdle == 0 || getIdleConnections() < minIdle)) {
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
      });
   }

   // ***********************************************************************
   //                        HikariPoolMBean methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public void softEvictConnections()
   {
      connectionBag.values(STATE_IN_USE).forEach(bagEntry -> bagEntry.evicted = true);
      connectionBag.values(STATE_NOT_IN_USE).stream().filter(p -> connectionBag.reserve(p)).forEach(bagEntry -> closeConnection(bagEntry));
   }

   // ***********************************************************************
   //                           Private methods
   // ***********************************************************************

   /**
    * Permanently close the real (underlying) connection (eat any exception).
    *
    * @param connectionProxy the connection to actually close
    */
   @Override
   protected void closeConnection(final PoolBagEntry bagEntry)
   {
      bagEntry.cancelMaxLifeTermination();
      if (connectionBag.remove(bagEntry)) {
         final int tc = totalConnections.decrementAndGet();
         if (tc < 0) {
            LOGGER.warn("Internal accounting inconsistency, totalConnections={}", tc, new Exception());
         }
         closeConnectionExecutor.execute(() -> { poolUtils.quietlyCloseConnection(bagEntry.connection); });
      }
   }

   /**
    * Check whether the connection is alive or not.
    *
    * @param connection the connection to test
    * @param timeoutMs the timeout before we consider the test a failure
    * @return true if the connection is alive, false if it is not alive or we timed out
    */
   protected boolean isConnectionAlive(final Connection connection, final long timeoutMs)
   {
      try {
         final boolean timeoutEnabled = (connectionTimeout != Integer.MAX_VALUE);
         int timeoutSec = timeoutEnabled ? (int) Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(timeoutMs)) : 0;

         if (isUseJdbc4Validation) {
            return connection.isValid(timeoutSec);
         }

         final int networkTimeout = poolUtils.setNetworkTimeout(houseKeepingExecutorService, connection, Math.max(1000, (int) timeoutMs), timeoutEnabled);

         try (Statement statement = connection.createStatement()) {
            poolUtils.setQueryTimeout(statement, timeoutSec);
            statement.executeQuery(configuration.getConnectionTestQuery());
         }

         if (isIsolateInternalQueries && !isAutoCommit) {
            connection.rollback();
         }

         poolUtils.setNetworkTimeout(houseKeepingExecutorService, connection, networkTimeout, timeoutEnabled);

         return true;
      }
      catch (SQLException e) {
         LOGGER.warn("Exception during keep alive check, that means the connection ({}) must be dead.", connection, e);
         return false;
      }
   }

   /**
    * Attempt to abort() active connections on Java7+, or close() them on Java6.
    *
    * @throws InterruptedException 
    */
   protected void abortActiveConnections() throws InterruptedException
   {
      ExecutorService assassinExecutor = createThreadPoolExecutor(configuration.getMaximumPoolSize(), "HikariCP connection assassin", configuration.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
      connectionBag.values(STATE_IN_USE).stream().forEach(bagEntry -> {
         try {
            bagEntry.aborted = bagEntry.evicted = true;
            bagEntry.connection.abort(assassinExecutor);
         }
         catch (SQLException | NoSuchMethodError | AbstractMethodError e) {
            poolUtils.quietlyCloseConnection(bagEntry.connection);
         }
         finally {
            if (connectionBag.remove(bagEntry)) {
               totalConnections.decrementAndGet();
            }
         }
      });

      assassinExecutor.shutdown();
      assassinExecutor.awaitTermination(5L, TimeUnit.SECONDS);
   }

   /** {@inheritDoc} */
   @Override
   protected Runnable getHouseKeeper()
   {
      return new HouseKeeper();
   }

   /** {@inheritDoc} */
   @Override
   protected ConcurrentBag<PoolBagEntry> createConcurrentBag(IBagStateListener listener)
   {
      return new Java8ConcurrentBag(listener);
   }

   /**
    * The house keeping task to retire idle connections.
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

         connectionBag.values(STATE_NOT_IN_USE).stream().filter(p -> connectionBag.reserve(p)).forEach(bagEntry -> {
            if (bagEntry.evicted || (idleTimeout > 0L && now > bagEntry.lastAccess + idleTimeout)) {
               closeConnection(bagEntry);
            }
            else {
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
