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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression test for the concurrent double-close of a pooled connection.
 *
 * Two threads closing the same {@link Connection} at the same time must return
 * the underlying pool entry only once. If the entry is returned twice and a
 * waiting borrower takes it in between the two returns, the second return marks
 * the entry free while the borrower is still using it, and the next borrower is
 * handed the same physical connection.
 *
 * The interleaving is forced deterministically: the first {@code close()} is
 * held inside {@code ProxyConnection.close()} (via the {@code clearWarnings()}
 * hook of {@link StubConnection}, which is only invoked from that method),
 * after it has passed the "not already closed" check but before it returns the
 * entry to the pool. The second {@code close()} and a waiting borrower are then
 * given their chance to run before the first close is released.
 */
public class TestDoubleClose
{
   @Test
   public void testConcurrentCloseReturnsConnectionOnlyOnce() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);
      config.setMinimumIdle(1);
      config.setConnectionTimeout(2000);

      final var firstCloseEntered = new CountDownLatch(1);
      final var releaseFirstClose = new CountDownLatch(1);
      final var hookArmed = new AtomicBoolean(true);

      // Hold the first close() inside ProxyConnection.close(), after it has
      // passed the delegate != CLOSED_CONNECTION check but before recycle().
      StubConnection.clearWarningsHook = () -> {
         if (hookArmed.compareAndSet(true, false)) {
            firstCloseEntered.countDown();
            releaseFirstClose.await(30, TimeUnit.SECONDS);
         }
         return null;
      };

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final Connection connection = ds.getConnection();

         // Thread 1: starts closing the connection and is held inside close().
         Thread closer1 = new Thread(() -> quietlyClose(connection), "closer-1");
         closer1.start();
         assertTrue("first close() did not reach clearWarnings()",
                    firstCloseEntered.await(30, TimeUnit.SECONDS));

         // Thread W: waits in getConnection() for the pool's only connection.
         final var waiterConnection = new AtomicReference<Connection>();
         final var waiterError = new AtomicReference<Throwable>();
         final var waiterDone = new CountDownLatch(1);
         Thread waiter = new Thread(() -> {
            try {
               waiterConnection.set(ds.getConnection());
            }
            catch (Throwable t) {
               waiterError.set(t);
            }
            finally {
               waiterDone.countDown();
            }
         }, "waiter");
         waiter.start();
         awaitCondition(() -> ds.getHikariPoolMXBean().getThreadsAwaitingConnection() == 1,
                        "waiter thread did not queue in getConnection()");

         // Thread 2: closes the same connection while close #1 is in progress.
         // With the fix this is a no-op; without it, it returns the entry to the
         // pool (handing it to W) even though close #1 has not finished.
         Thread closer2 = new Thread(() -> quietlyClose(connection), "closer-2");
         closer2.start();
         closer2.join(TimeUnit.SECONDS.toMillis(30));
         assertFalse("second close() thread is stuck", closer2.isAlive());

         // Let close #1 finish. Without the fix this returns the entry a second
         // time, marking it free although W now holds it.
         releaseFirstClose.countDown();
         closer1.join(TimeUnit.SECONDS.toMillis(30));
         assertFalse("first close() thread is stuck", closer1.isAlive());

         assertTrue("waiter did not obtain the connection",
                    waiterDone.await(30, TimeUnit.SECONDS));
         if (waiterError.get() != null) {
            fail("waiter failed to obtain the connection: " + waiterError.get());
         }
         final Connection w = waiterConnection.get();
         assertNotNull(w);

         // W holds the pool's only connection, so another borrow must time out.
         Connection v = null;
         try {
            v = ds.getConnection();
         }
         catch (SQLException e) {
            // expected: connection timeout, the only connection is in use
         }
         if (v != null) {
            final boolean samePhysical = v.unwrap(Connection.class) == w.unwrap(Connection.class);
            v.close();
            fail("a second borrow succeeded while the only pooled connection was in use"
                 + " (same physical connection as the waiter's: " + samePhysical + ")");
         }

         w.close();
         waiter.join(TimeUnit.SECONDS.toMillis(30));
      }
      finally {
         StubConnection.clearWarningsHook = null;
      }
   }

   private static void quietlyClose(final Connection connection)
   {
      try {
         connection.close();
      }
      catch (SQLException e) {
         // ignored; assertions on pool state decide the outcome
      }
   }

   private static void awaitCondition(final BooleanSupplier condition, final String failureMessage) throws InterruptedException
   {
      final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      while (!condition.getAsBoolean()) {
         if (System.nanoTime() > deadline) {
            fail(failureMessage);
         }
         TimeUnit.MILLISECONDS.sleep(10);
      }
   }
}
