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
import com.zaxxer.hikari.mocks.StubDataSource;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class ConnectionPoolSizeVsThreadsTest {

   public static final int ITERATIONS = 50_000;

   @Test
   public void testPoolSizeAboutSameSizeAsThreadCount() throws Exception {
      final int threadCount = 50;
      final Counts counts = testPoolSize(2, 100, threadCount, 1, 0, 20, ITERATIONS, 1000);
      // maxActive may never make it to threadCount but it shouldn't be any higher
      Assert.assertEquals(threadCount, counts.getMaxActive() - 5, 5);
      Assert.assertEquals(threadCount, counts.getMaxTotal(), 5);
   }

   @Test
   public void testSlowConnectionTimeBurstyWork() throws Exception {
      // setup a bursty work load, 50 threads all needing to do around 100 units of work.
      // Using a more realistic time for connection startup of 250 ms and only 5 seconds worth of work will mean that we end up finishing
      // all of the work before we actually have setup 50 connections even though we have requested 50 connections
      final int threadCount = 50;
      final int workItems = threadCount * 100;
      final int workTime = 0;
      final int connectionAcquisitionTime = 250;
      final Counts counts = testPoolSize(2, 100, threadCount, workTime, 0, connectionAcquisitionTime, workItems, 3000);

      // hard to put exact bounds on how many thread we will use but we can put an upper bound on usage (if there was only one thread)
      long totalWorkTime = workItems * workTime;
      long connectionMax = totalWorkTime / connectionAcquisitionTime;
      Assert.assertTrue(connectionMax <= counts.getMaxActive());
      Assert.assertEquals(connectionMax, counts.getMaxTotal(), 2 + 2);
   }

   private Counts testPoolSize(int minIdle, int maxPoolSize, int threadCount, final int workTime, final int restTime,
                               int connectionAcquisitionTime, int iterations, int postTestTime) throws Exception {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(minIdle);
      config.setMaximumPoolSize(maxPoolSize);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTimeout(2500);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final AtomicReference<Exception> ref = new AtomicReference<>(null);

      // Initialize HikariPool with no initial connections and room to grow
      try (final HikariDataSource ds = new HikariDataSource(config)) {
         final StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         // connection acquisition takes more than 0 ms in a real system
         stubDataSource.setConnectionAcquistionTime(connectionAcquisitionTime);

         ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
         final CountDownLatch allThreadsDone = new CountDownLatch(iterations);
         for (int i = 0; i < iterations; i++) {
            threadPool.submit(new Callable<Exception>() {
               /** {@inheritDoc} */
               @Override
               public Exception call() throws Exception {
                  if (ref.get() == null) {
                     Connection c2;
                     try {
                        if (restTime > 0) {
                           Thread.sleep(restTime);
                        }
                        c2 = ds.getConnection();
                        if (workTime > 0) {
                           Thread.sleep(workTime);
                        }
                        c2.close();
                     } catch (Exception e) {
                        ref.set(e);
                     }
                  }
                  allThreadsDone.countDown();
                  return null;
               }
            });
         }

         HikariPool pool = TestElf.getPool(ds);

         // collect pool usage data while work is still being done
         Counts underLoad = new Counts();
         while (allThreadsDone.getCount() > 0 || pool.getTotalConnections() < minIdle) {
            Thread.sleep(100);
            underLoad.updateMaxCounts(pool);
         }

         // wait for long enough any pending acquisitions have already been done
         System.out.println("Test Over, waiting for post delay time: " + postTestTime);
         Thread.sleep(connectionAcquisitionTime + workTime + restTime);

         // collect pool data while there is no work to do.
         Counts postLoad = new Counts();
         if (postTestTime > 0) {
            long doneTime = System.currentTimeMillis() + postTestTime;
            while (System.currentTimeMillis() < doneTime) {
               Thread.sleep(100);
               postLoad.updateMaxCounts(pool);
            }
         }

         allThreadsDone.await();

         threadPool.shutdown();
         threadPool.awaitTermination(30, TimeUnit.SECONDS);

         if (ref.get() != null) {
            LoggerFactory.getLogger(ConnectionPoolSizeVsThreadsTest.class).error("Task failed", ref.get());
            Assert.fail("Task failed");
         }
         System.out.println("Under Load " + underLoad.toString());
         System.out.println("Idle After Work " + postLoad.toString());

         // verify that the no connections created after the work has stopped
         if (postTestTime > 0) {
            if (postLoad.getMaxActive() != 0) {
               Assert.fail("Max Active was greater than 0 after test was done");
            }
            int createdAfterWorkAllFinished = postLoad.getMaxTotal() - underLoad.getMaxTotal();
            Assert.assertEquals("Connections were created when there was no waiting consumers", 0, createdAfterWorkAllFinished);
         }
         return underLoad;
      }
   }

   private static class Counts {
      private int maxTotal = 0;
      private int maxActive = 0;

      public int getMaxTotal() {
         return maxTotal;
      }

      public int getMaxActive() {
         return maxActive;
      }

      public void updateMaxCounts(HikariPool pool) {
         maxTotal = Math.max(pool.getTotalConnections(), maxTotal);
         maxActive = Math.max(pool.getActiveConnections(), maxActive);
      }

      @Override
      public String toString() {
         return "Counts{" +
            "maxTotal=" + maxTotal +
            ", maxActive=" + maxActive +
            '}';
      }
   }
}
