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

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.setSlf4jLogLevel;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ConcurrentBag;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class ConnectionRaceConditionTest
{

   public static final int ITERATIONS = 10_000;

   @Test
   public void testRaceCondition() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTimeout(5000);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      setSlf4jLogLevel(ConcurrentBag.class, Level.INFO);

      final AtomicReference<Exception> ref = new AtomicReference<>(null);

      // Initialize HikariPool with no initial connections and room to grow
      try (final HikariDataSource ds = new HikariDataSource(config)) {
         ExecutorService threadPool = Executors.newFixedThreadPool(2);
         for (int i = 0; i < ITERATIONS; i++) {
            threadPool.submit(new Callable<Exception>() {
               /** {@inheritDoc} */
               @Override
               public Exception call() throws Exception
               {
                  if (ref.get() == null) {
                     Connection c2;
                     try {
                        c2 = ds.getConnection();
                        ds.evictConnection(c2);
                     }
                     catch (Exception e) {
                        ref.set(e);
                     }
                  }
                  return null;
               }
            });
         }

         threadPool.shutdown();
         threadPool.awaitTermination(30, TimeUnit.SECONDS);

         if (ref.get() != null) {
            LoggerFactory.getLogger(ConnectionRaceConditionTest.class).error("Task failed", ref.get());
            fail("Task failed");
         }
      }
      catch (Exception e) {
         throw e;
      }
   }

   @After
   public void after()
   {
      System.getProperties().remove("com.zaxxer.hikari.housekeeping.periodMs");

      setSlf4jLogLevel(HikariPool.class, Level.WARN);
      setSlf4jLogLevel(ConcurrentBag.class, Level.WARN);
   }
}
