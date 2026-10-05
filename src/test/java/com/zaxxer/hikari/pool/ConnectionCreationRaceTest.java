package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ConcurrentBag;
import org.junit.Test;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.zaxxer.hikari.pool.TestElf.getConcurrentBag;
import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionCreationRaceTest
{
   @Test
   public void testOutOfOrderConnectionRequests() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(3);
      config.setConnectionTimeout(2000);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final var borrowers = Executors.newFixedThreadPool(2);
      final var creatorBlocked = new CountDownLatch(1);
      final var releaseCreator = new CountDownLatch(1);
      final var firstWaiting = new CountDownLatch(1);
      final var secondSubmitted = new CountDownLatch(1);
      final var requestsSubmitted = new CountDownLatch(2);
      final var connectionsAcquired = new CountDownLatch(2);
      final var releaseConnections = new CountDownLatch(1);

      try (HikariDataSource ds = new HikariDataSource(config);
           Connection held = ds.getConnection()) {
         HikariPool pool = getPool(ds);
         final var executorField = HikariPool.class.getDeclaredField("addConnectionExecutor");
         executorField.setAccessible(true);
         final var executor = (ThreadPoolExecutor) executorField.get(pool);

         // Keep creator tasks queued until both borrowers have requested a connection.
         executor.execute(() -> {
            creatorBlocked.countDown();
            await(releaseCreator);
         });
         await(creatorBlocked);

         final var listenerField = ConcurrentBag.class.getDeclaredField("listener");
         listenerField.setAccessible(true);
         listenerField.set(getConcurrentBag(ds), (ConcurrentBag.IBagStateListener) waiting -> {
            if (waiting == 1) {
               firstWaiting.countDown();
               await(secondSubmitted);
            }

            // Submit waiter 2 before waiter 1, preserving their original waiter counts.
            pool.addBagItem(waiting);
            if (waiting == 2) {
               secondSubmitted.countDown();
            }
            requestsSubmitted.countDown();
         });

         final var first = borrowers.submit(() -> {
            try (Connection connection = ds.getConnection()) {
               connectionsAcquired.countDown();
               await(releaseConnections);
            }
            return null;
         });
         await(firstWaiting);
         final var second = borrowers.submit(() -> {
            try (Connection connection = ds.getConnection()) {
               connectionsAcquired.countDown();
               await(releaseConnections);
            }
            return null;
         });

         try {
            await(requestsSubmitted);
            releaseCreator.countDown();
            final var acquired = connectionsAcquired.await(5, SECONDS);
            assertTrue("A borrower was stranded despite available pool capacity: total=" + pool.getTotalConnections(), acquired);
            assertEquals(3, pool.getActiveConnections());
         }
         finally {
            releaseCreator.countDown();
            releaseConnections.countDown();
         }

         first.get(5, SECONDS);
         second.get(5, SECONDS);
      }
      finally {
         secondSubmitted.countDown();
         releaseCreator.countDown();
         releaseConnections.countDown();
         borrowers.shutdownNow();
         assertTrue("Borrower executor did not terminate", borrowers.awaitTermination(5, SECONDS));
      }
   }

   private static void await(CountDownLatch latch)
   {
      try {
         assertTrue("Timed out waiting for the controlled race", latch.await(5, SECONDS));
      }
      catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new AssertionError(e);
      }
   }
}
