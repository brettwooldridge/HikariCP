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

import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.setSlf4jLogLevel;
import static com.zaxxer.hikari.pool.TestElf.setSlf4jTargetStream;
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.mocks.StubDataSource;

public class TestConnectionTimeoutRetry
{
   @Test
   public void testConnectionRetries() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2800);
      config.setValidationTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         long start = currentTime();
         try (Connection connection = ds.getConnection()) {
            connection.close();
            fail("Should not have been able to get a connection.");
         }
         catch (SQLException e) {
            long elapsed = elapsedMillis(start);
            long timeout = config.getConnectionTimeout();
            assertTrue("Didn't wait long enough for timeout", (elapsed >= timeout));
         }
      }
   }

   @Test
   public void testConnectionRetries2() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2800);
      config.setValidationTimeout(2800);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
         scheduler.schedule(new Runnable() {
            @Override
            public void run()
            {
               stubDataSource.setThrowException(null);
            }
         }, 300, TimeUnit.MILLISECONDS);

         long start = currentTime();
         try {
            try (Connection connection = ds.getConnection()) {
               // close immediately
            }

            long elapsed = elapsedMillis(start);
            assertTrue("Connection returned too quickly, something is wrong.", elapsed > 250);
            assertTrue("Waited too long to get a connection.", elapsed < config.getConnectionTimeout());
         }
         catch (SQLException e) {
            fail("Should not have timed out: " + e.getMessage());
         }
         finally {
            scheduler.shutdownNow();
         }
      }
   }

   @Test
   public void testConnectionRetries3() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(2800);
      config.setValidationTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final Connection connection1 = ds.getConnection();
         final Connection connection2 = ds.getConnection();
         assertNotNull(connection1);
         assertNotNull(connection2);

         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
         scheduler.schedule(new Runnable() {
            @Override
            public void run()
            {
               try {
                  connection1.close();
               }
               catch (Exception e) {
                  e.printStackTrace(System.err);
               }
            }
         }, 800, MILLISECONDS);

         long start = currentTime();
         try {
            try (Connection connection3 = ds.getConnection()) {
               // close immediately
            }

            long elapsed = elapsedMillis(start);
            assertTrue("Waited too long to get a connection.", (elapsed >= 700) && (elapsed < 950));
         }
         catch (SQLException e) {
            fail("Should not have timed out.");
         }
         finally {
            scheduler.shutdownNow();
         }
      }
   }

   @Test
   public void testConnectionRetries5() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final Connection connection1 = ds.getConnection();

         long start = currentTime();

         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
         scheduler.schedule(new Runnable() {
            @Override
            public void run()
            {
               try {
                  connection1.close();
               }
               catch (Exception e) {
                  e.printStackTrace(System.err);
               }
            }
         }, 250, MILLISECONDS);

         StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         try {
            try (Connection connection2 = ds.getConnection()) {
               // close immediately
            }

            long elapsed = elapsedMillis(start);
            assertTrue("Waited too long to get a connection.", (elapsed >= 250) && (elapsed < config.getConnectionTimeout()));
         }
         catch (SQLException e) {
            fail("Should not have timed out.");
         }
         finally {
            scheduler.shutdownNow();
         }
      }
   }

   @Test
   public void testConnectionIdleFill() throws Exception
   {
      StubConnection.slowCreate = false;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(2000);
      config.setValidationTimeout(2000);
      config.setConnectionTestQuery("VALUES 2");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "400");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      setSlf4jTargetStream(HikariPool.class, ps);

      try (HikariDataSource ds = new HikariDataSource(config)) {
         setSlf4jLogLevel(HikariPool.class, Level.DEBUG);

         HikariPool pool = getPool(ds);
         try (
            Connection connection1 = ds.getConnection();
            Connection connection2 = ds.getConnection();
            Connection connection3 = ds.getConnection();
            Connection connection4 = ds.getConnection();
            Connection connection5 = ds.getConnection();
            Connection connection6 = ds.getConnection();
            Connection connection7 = ds.getConnection()) {

            sleep(1300);

            assertSame("Total connections not as expected", 10, pool.getTotalConnections());
            assertSame("Idle connections not as expected", 3, pool.getIdleConnections());
         }

         assertSame("Total connections not as expected", 10, pool.getTotalConnections());
         assertSame("Idle connections not as expected", 10, pool.getIdleConnections());
      }
   }

   @Before
   public void before()
   {
      setSlf4jLogLevel(HikariPool.class, Level.INFO);
   }

   @After
   public void after()
   {
      System.getProperties().remove("com.zaxxer.hikari.housekeeping.periodMs");
      setSlf4jLogLevel(HikariPool.class, Level.INFO);
   }
}
