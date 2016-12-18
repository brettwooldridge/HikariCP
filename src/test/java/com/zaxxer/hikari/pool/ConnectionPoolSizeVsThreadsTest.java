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

import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubDataSource;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class ConnectionPoolSizeVsThreadsTest {

   public static final int ITERATIONS = 50_000;

   @Test
   public void testPoolSizeAboutSameSizeAsThreadCount() throws Exception {
      {
         final int threadCount = 50;
         final Counts counts = testPoolSize(2, 100, threadCount, 1, 0, 20);
         Assert.assertEquals(threadCount, counts.getTotal(), 5);
      }
//      {
//         final int threadCount = 2;
//         final Counts counts = testPoolSize(2, 100, threadCount, 1, 0, 20);
//         Assert.assertEquals(threadCount, counts.getTotal());
//      }
   }

   private Counts testPoolSize(int minIdle, int maxPoolSize, int threadCount, final int workTime, final int restTime, int connectionAcquisitionTime) throws Exception {
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
         final int iterationCount = ITERATIONS;
         final CountDownLatch allThreadsDone = new CountDownLatch(iterationCount);
         for (int i = 0; i < iterationCount; i++) {
            threadPool.submit(new Callable<Exception>() {
               /** {@inheritDoc} */
               @Override
               public Exception call() throws Exception {
                  if (ref.get() == null) {
                     Connection c2;
                     try {
                        if(restTime > 0) {
                           Thread.sleep(restTime);
                        }
                        c2 = ds.getConnection();
                        if(workTime > 0) {
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

         int maxTotal = pool.getTotalConnections();
         int maxActive = pool.getActiveConnections();
         while (allThreadsDone.getCount() > 0) {
            Thread.sleep(100);
            maxTotal = Math.max(pool.getTotalConnections(), maxTotal);
            maxActive = Math.max(pool.getActiveConnections(), maxActive);
         }

         allThreadsDone.await();

         threadPool.shutdown();
         threadPool.awaitTermination(30, TimeUnit.SECONDS);

         if (ref.get() != null) {
            LoggerFactory.getLogger(ConnectionPoolSizeVsThreadsTest.class).error("Task failed", ref.get());
            Assert.fail("Task failed");
         }

         return new Counts(maxTotal, maxActive);
      }
   }

   private static class Counts {
      private final int total;

      public Counts(int total, int max) {
         this.total = total;
      }

      public int getTotal() {
         return total;
      }
   }
}
