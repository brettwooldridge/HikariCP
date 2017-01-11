/*
 * Copyright (C) 2014 Brett Wooldridge
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
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedMillis;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.util.UtilityElf;

/**
 * @author Brett Wooldridge
 */
public class ShutdownTest
{
   @Before
   public void beforeTest()
   {
      setSlf4jLogLevel(PoolBase.class, Level.DEBUG);
      setSlf4jLogLevel(HikariPool.class, Level.DEBUG);
      StubConnection.count.set(0);
   }

   @After
   public void afterTest()
   {
      setSlf4jLogLevel(PoolBase.class, Level.WARN);
      setSlf4jLogLevel(HikariPool.class, Level.WARN);
      StubConnection.slowCreate = false;
   }

   @Test
   public void testShutdown1() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = true;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         Thread[] threads = new Thread[10];
         for (int i = 0; i < 10; i++) {
            threads[i] = new Thread() {
               @Override
               public void run()
               {
                  try {
                     if (ds.getConnection() != null) {
                        quietlySleep(SECONDS.toMillis(1));
                     }
                  }
                  catch (SQLException e) {
                  }
               }
            };
            threads[i].setDaemon(true);
         }
         for (int i = 0; i < 10; i++) {
            threads[i].start();
         }

         quietlySleep(1800L);

         assertTrue("Total connection count not as expected, ", pool.getTotalConnections() > 0);

         ds.close();

         assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
         assertTrue(ds.isClosed());
      }
   }

   @Test
   public void testShutdown2() throws SQLException
   {
      assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = true;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(10);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         quietlySleep(1200L);

         assertTrue("Total connection count not as expected, ", pool.getTotalConnections() > 0);

         ds.close();

         assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
         assertTrue(ds.toString().startsWith("HikariDataSource (") && ds.toString().endsWith(")"));
      }
   }

   @Test
   public void testShutdown3() throws SQLException
   {
      assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = false;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         quietlySleep(1200L);

         assertTrue("Total connection count not as expected, ", pool.getTotalConnections() == 5);

         ds.close();

         assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
      }
   }

   @Test
   public void testShutdown4() throws SQLException
   {
      StubConnection.slowCreate = true;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(10);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         quietlySleep(500L);

         ds.close();

         long startTime = currentTime();
         while (elapsedMillis(startTime) < SECONDS.toMillis(5) && threadCount() > 0) {
            quietlySleep(250);
         }

         assertSame("Unreleased connections after shutdown", 0, getPool(ds).getTotalConnections());
      }
   }

   @Test
   public void testShutdown5() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         Connection[] connections = new Connection[5];
         for (int i = 0; i < 5; i++) {
            connections[i] = ds.getConnection();
         }

         Assert.assertTrue("Total connection count not as expected, ", pool.getTotalConnections() == 5);

         ds.close();

         Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         Assert.assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
      }
   }

   @Test
   public void testAfterShutdown() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(5);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         ds.close();
         try {
            ds.getConnection();
         }
         catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("has been closed."));
         }
      }
   }

   @Test
   public void testShutdownDuringInit() throws Exception
   {
      final HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         StubConnection.slowCreate = true;
         UtilityElf.quietlySleep(3000L);
      }
   }

   @Test
   public void testThreadedShutdown() throws Exception
   {
      final HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      for (int i = 0; i < 4; i++) {
         try (final HikariDataSource ds = new HikariDataSource(config)) {
            Thread t = new Thread() {
               @Override
               public void run()
               {
                  try (Connection connection = ds.getConnection()) {
                     for (int i = 0; i < 10; i++) {
                        Connection connection2 = null;
                        try {
                           connection2 = ds.getConnection();
                           PreparedStatement stmt = connection2.prepareStatement("SOMETHING");
                           UtilityElf.quietlySleep(20);
                           stmt.getMaxFieldSize();
                        }
                        catch (SQLException e) {
                           try {
                              if (connection2 != null) {
                                 connection2.close();
                              }
                           }
                           catch (SQLException e2) {
                              if (e2.getMessage().contains("shutdown") || e2.getMessage().contains("evicted")) {
                                 break;
                              }
                           }
                        }
                     }
                  }
                  catch (Exception e) {
                     Assert.fail(e.getMessage());
                  }
                  finally {
                     ds.close();
                  }
               }
            };
            t.start();

            Thread t2 = new Thread() {
               @Override
               public void run()
               {
                  UtilityElf.quietlySleep(100);
                  try {
                     ds.close();
                  }
                  catch (IllegalStateException e) {
                     Assert.fail(e.getMessage());
                  }
               }
            };
            t2.start();

            t.join();
            t2.join();

            ds.close();
         }
      }
   }

   private int threadCount()
   {
      Thread[] threads = new Thread[Thread.activeCount() * 2];
      Thread.enumerate(threads);

      int count = 0;
      for (Thread thread : threads) {
         count += (thread != null && thread.getName().startsWith("Hikari")) ? 1 : 0;
      }

      return count;
   }
}
