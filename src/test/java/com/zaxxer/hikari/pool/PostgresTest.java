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
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedMillis;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTest
{
   private static final DockerImageName IMAGE_NAME = DockerImageName.parse("postgres:9.6.20");

   private PostgreSQLContainer<?> postgres;

   @Before
   public void beforeTest() {
     postgres = new PostgreSQLContainer<>(IMAGE_NAME);
     postgres.start();
   }

   @After
   public void afterTest() {
     postgres.stop();
   }

   @Test
   public void testCase1() throws Exception {
      HikariConfig config = createConfig(postgres);
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(3000);
      config.setIdleTimeout(SECONDS.toMillis(10));
      config.setValidationTimeout(SECONDS.toMillis(2));

      exerciseConfig(config, 3);
   }

   @Test
   public void testCase2() throws Exception
   {
      HikariConfig config = createConfig(postgres);
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(1000);
      config.setIdleTimeout(SECONDS.toMillis(20));

      exerciseConfig(config, 3);
   }

   @Test
   public void testCase3() throws Exception
   {
      HikariConfig config = createConfig(postgres);
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(1000);
      config.setIdleTimeout(SECONDS.toMillis(20));

      exerciseConfig(config, 3);
   }

   @Test
   public void testCase4() throws Exception
   {
      HikariConfig config = createConfig(postgres);
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(15);
      config.setConnectionTimeout(10000);
      config.setIdleTimeout(2000);
      config.setMaxLifetime(5);
      config.setRegisterMbeans(true);

      exerciseConfig(config, 3);
   }

   static private void exerciseConfig(HikariConfig config, int numThreads) {
      try (final HikariDataSource ds = new HikariDataSource(config)) {
         assertTrue(ds.isRunning());
         exerciseDataSource(ds, numThreads);
         assertTrue(ds.isRunning());
      }
   }

   static private void exerciseDataSource(HikariDataSource ds, int numThreads) {
      final long start = currentTime();
      List<PostgresWorkerThread> threads = startThreads(ds, numThreads);
      do {
         quietlySleep(SECONDS.toMillis(1));
         assertZeroErrors(threads);
      } while (elapsedMillis(start) < SECONDS.toMillis(15));
      stopThreads(threads);
   }

   static private void assertZeroErrors(List<PostgresWorkerThread> threads) {
     for (PostgresWorkerThread t : threads) {
        assertEquals(0, t.getErrorCount());
     }
   }

   static List<PostgresWorkerThread> startThreads(HikariDataSource ds, int numThreads) {
     List<PostgresWorkerThread> threads = new ArrayList<>();
     for (int i = 0; i < numThreads; i++) {
        PostgresWorkerThread t = new PostgresWorkerThread(ds);
        t.start();
        threads.add(t);
     }
     return threads;
   }

   static void stopThreads(List<PostgresWorkerThread> threads) {
      for (PostgresWorkerThread t : threads) {
         t.requestStop();
      }
   }

   static class PostgresWorkerThread extends Thread {
      static private final AtomicInteger id = new AtomicInteger(0);
      private final HikariDataSource dataSource;
      private int errorCount = 0;
      private AtomicBoolean stopRequested = new AtomicBoolean(false);

      public PostgresWorkerThread(HikariDataSource ds) {
         this.dataSource = ds;
         this.setName(getClass().getSimpleName() + "-" + id.getAndIncrement());
         this.setDaemon(true);
      }

      public void requestStop() {
        stopRequested.set(true);
         System.err.println("[" + getName() + "] stopRequested()");
      }

      public void run() {
         System.err.println("[" + getName() + "] run()");
         while (!stopRequested.get()) {
            try (Connection connection = dataSource.getConnection()) {
               quietlySleep(5);
            } catch (Exception e) {
               e.printStackTrace();
               errorCount++;
            }
         }
      }

      public int getErrorCount() {
         return errorCount;
      }
   }

   @Before
   public void before()
   {
      System.err.println("\n");
   }

   static private HikariConfig createConfig(PostgreSQLContainer<?> postgres) {
      HikariConfig config = newHikariConfig();
      config.setJdbcUrl(postgres.getJdbcUrl());
      config.setUsername(postgres.getUsername());
      config.setPassword(postgres.getPassword());
      config.setDriverClassName(postgres.getDriverClassName());
      return config;
   }
}
