package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ConcurrentBag;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.zaxxer.hikari.pool.TestElf.getConcurrentBag;
import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedMillis;
import static com.zaxxer.hikari.util.ClockSource.plusMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class ConnectionLifecycleRaceTest
{
   @Test
   public void healthyReturnWakesExistingWaiter() throws Exception
   {
      assertReturnUnblocksWaitingBorrower(false);
   }

   @Test
   public void retiredReturnWakesExistingWaiter() throws Exception
   {
      assertReturnUnblocksWaitingBorrower(true);
   }

   private void assertReturnUnblocksWaitingBorrower(boolean retireBeforeReturn) throws Exception
   {
      final var borrowers = Executors.newSingleThreadExecutor();
      try (HikariDataSource ds = dataSource(0, 1);
           Connection held = ds.getConnection()) {
         final var underlying = held.unwrap(Connection.class);
         final var requested = observeRequest(ds, null);
         final var borrower = borrowers.submit((Callable<Connection>) ds::getConnection);
         await(requested);
         drainExecutor(ds, "addConnectionExecutor");
         assertEquals(1, getPool(ds).getThreadsAwaitingConnection());

         if (retireBeforeReturn) {
            getPool(ds).softEvictConnections();
         }
         held.close();

         try (Connection acquired = borrower.get(5, SECONDS)) {
            if (retireBeforeReturn) {
               assertNotSame(underlying, acquired.unwrap(Connection.class));
            }
            else {
               assertSame(underlying, acquired.unwrap(Connection.class));
            }
            assertEquals(1, getPool(ds).getTotalConnections());
         }
      }
      finally {
         borrowers.shutdownNow();
         assertTrue(borrowers.awaitTermination(5, SECONDS));
      }
   }

   @Test
   public void healthyReturnBeforeWaiterPollDoesNotLoseHandoff() throws Exception
   {
      final var workers = Executors.newFixedThreadPool(2);
      final var allowPoll = new CountDownLatch(1);
      try (HikariDataSource ds = dataSource(0, 1);
           Connection held = ds.getConnection()) {
         final var underlying = held.unwrap(Connection.class);
         final var requested = observeRequest(ds, allowPoll);
         final var borrower = workers.submit((Callable<Connection>) ds::getConnection);
         await(requested);
         drainExecutor(ds, "addConnectionExecutor");
         final var returning = workers.submit(() -> { held.close(); return null; });

         final var started = currentTime();
         while (getPool(ds).getIdleConnections() != 1 && elapsedMillis(started) < 5000) {
            Thread.yield();
         }
         assertEquals(1, getPool(ds).getIdleConnections());
         assertFalse("Return must keep offering until the waiter starts polling", returning.isDone());
         allowPoll.countDown();
         try (Connection acquired = borrower.get(5, SECONDS)) {
            assertSame(underlying, acquired.unwrap(Connection.class));
            returning.get(5, SECONDS);
         }
      }
      finally {
         allowPoll.countDown();
         workers.shutdownNow();
         assertTrue(workers.awaitTermination(5, SECONDS));
      }
   }

   @Test
   public void unreserveWakesExistingWaiter() throws Exception
   {
      final var borrowers = Executors.newSingleThreadExecutor();
      try (HikariDataSource ds = dataSource(0, 1)) {
         try (Connection initial = ds.getConnection()) {}
         final var bag = bag(ds);
         final var entry = bag.values().get(0);
         assertTrue(bag.reserve(entry));
         final var requested = observeRequest(ds, null);
         final var borrower = borrowers.submit((Callable<Connection>) ds::getConnection);
         await(requested);
         drainExecutor(ds, "addConnectionExecutor");
         bag.unreserve(entry);
         try (Connection acquired = borrower.get(5, SECONDS)) {
            assertSame(entry.connection, acquired.unwrap(Connection.class));
         }
      }
      finally {
         borrowers.shutdownNow();
         assertTrue(borrowers.awaitTermination(5, SECONDS));
      }
   }

   @Test
   public void idleRetirementWakesWaiterThatArrivesAfterReservation() throws Exception
   {
      final var workers = Executors.newFixedThreadPool(2);
      final var reserved = new CountDownLatch(1);
      final var allowRemoval = new CountDownLatch(1);
      try (HikariDataSource ds = dataSource(0, 1)) {
         try (Connection initial = ds.getConnection()) {}
         final var bag = installSpy(ds);
         bag.values().get(0).lastAccessed = plusMillis(currentTime(), -20_000);
         doAnswer(invocation -> {
            boolean result = (boolean) invocation.callRealMethod();
            if (result) {
               reserved.countDown();
               await(allowRemoval);
            }
            return result;
         }).when(bag).reserve(any(PoolEntry.class));

         final var requested = observeRequest(ds, null);
         final var cleanup = workers.submit(houseKeeper(ds));
         await(reserved);
         final var borrower = workers.submit((Callable<Connection>) ds::getConnection);
         await(requested);
         drainExecutor(ds, "addConnectionExecutor");
         allowRemoval.countDown();
         cleanup.get(5, SECONDS);
         try (Connection acquired = borrower.get(5, SECONDS)) {
            assertEquals(1, getPool(ds).getTotalConnections());
         }
      }
      finally {
         allowRemoval.countDown();
         workers.shutdownNow();
         assertTrue(workers.awaitTermination(5, SECONDS));
      }
   }

   @Test
   public void idleTimeoutMustRecheckAgeAfterReservation() throws Exception
   {
      final var workers = Executors.newSingleThreadExecutor();
      final var reserving = new CountDownLatch(1);
      final var allowReserve = new CountDownLatch(1);
      try (HikariDataSource ds = dataSource(0, 1)) {
         try (Connection initial = ds.getConnection()) {}
         final var bag = installSpy(ds);
         final var entry = bag.values().get(0);
         entry.lastAccessed = plusMillis(currentTime(), -20_000);
         doAnswer(invocation -> {
            reserving.countDown();
            await(allowReserve);
            return invocation.callRealMethod();
         }).when(bag).reserve(any(PoolEntry.class));

         final var cleanup = workers.submit(houseKeeper(ds));
         await(reserving);
         try (Connection used = ds.getConnection()) {
            assertSame(entry.connection, used.unwrap(Connection.class));
         }
         assertTrue(elapsedMillis(entry.lastAccessed) < ds.getIdleTimeout());
         allowReserve.countDown();
         cleanup.get(5, SECONDS);
         drainExecutor(ds, "closeConnectionExecutor");
         assertEquals("Recently returned connection was retired before idleTimeout", 1, getPool(ds).getTotalConnections());
      }
      finally {
         allowReserve.countDown();
         workers.shutdownNow();
         assertTrue(workers.awaitTermination(5, SECONDS));
      }
   }

   @Test
   public void ordinaryIdleCleanupStopsAtMinimumIdle() throws Exception
   {
      try (HikariDataSource ds = dataSource(1, 3)) {
         try (Connection first = ds.getConnection();
              Connection second = ds.getConnection();
              Connection third = ds.getConnection()) {}
         drainExecutor(ds, "addConnectionExecutor");
         for (PoolEntry entry : bag(ds).values()) {
            entry.lastAccessed = plusMillis(currentTime(), -20_000);
         }
         houseKeeper(ds).run();
         drainExecutor(ds, "closeConnectionExecutor");
         drainExecutor(ds, "addConnectionExecutor");
         assertEquals(1, getPool(ds).getIdleConnections());
         assertEquals(1, getPool(ds).getTotalConnections());
      }
   }

   private static HikariDataSource dataSource(int minimumIdle, int maximumPoolSize) throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(minimumIdle);
      config.setMaximumPoolSize(maximumPoolSize);
      config.setConnectionTimeout(2000);
      config.setIdleTimeout(10_000);
      config.setMaxLifetime(0);
      config.setKeepaliveTime(0);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      final var ds = new HikariDataSource(config);
      ((ScheduledFuture<?>) field(HikariPool.class, "houseKeeperTask").get(getPool(ds))).cancel(false);
      return ds;
   }

   @SuppressWarnings("unchecked")
   private static ConcurrentBag<PoolEntry> bag(HikariDataSource ds)
   {
      return (ConcurrentBag<PoolEntry>) getConcurrentBag(ds);
   }

   private static ConcurrentBag<PoolEntry> installSpy(HikariDataSource ds) throws Exception
   {
      final var bag = spy(bag(ds));
      field(HikariPool.class, "connectionBag").set(getPool(ds), bag);
      return bag;
   }

   private static CountDownLatch observeRequest(HikariDataSource ds, CountDownLatch allowPoll) throws Exception
   {
      final var requested = new CountDownLatch(1);
      field(ConcurrentBag.class, "listener").set(bag(ds), (ConcurrentBag.IBagStateListener) waiting -> {
         getPool(ds).addBagItem(waiting);
         requested.countDown();
         if (allowPoll != null) {
            await(allowPoll);
         }
      });
      return requested;
   }

   private static Runnable houseKeeper(HikariDataSource ds) throws Exception
   {
      final var constructor = Class.forName("com.zaxxer.hikari.pool.HikariPool$HouseKeeper").getDeclaredConstructor(HikariPool.class);
      constructor.setAccessible(true);
      return (Runnable) constructor.newInstance(getPool(ds));
   }

   private static void drainExecutor(HikariDataSource ds, String name) throws Exception
   {
      final var executor = (ThreadPoolExecutor) field(HikariPool.class, name).get(getPool(ds));
      // Enqueue a barrier directly so the production discard policy cannot drop it.
      final var barrier = new FutureTask<Void>(() -> null);
      executor.prestartCoreThread();
      assertTrue(executor.getQueue().offer(barrier, 5, SECONDS));
      barrier.get(5, SECONDS);
   }

   private static Field field(Class<?> type, String name) throws Exception
   {
      final var field = type.getDeclaredField(name);
      field.setAccessible(true);
      return field;
   }

   private static void await(CountDownLatch latch)
   {
      try {
         assertTrue("Timed out waiting for controlled interleaving", latch.await(5, SECONDS));
      }
      catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new AssertionError(e);
      }
   }
}
