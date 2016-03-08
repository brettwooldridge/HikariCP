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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.UtilityElf;
import org.apache.logging.log4j.Level;

/**
 * @author Brett Wooldridge
 */
public class ShutdownTest
{
   @Before
   public void beforeTest()
   {
      TestElf.setSlf4jLogLevel(PoolBase.class, Level.DEBUG);
      TestElf.setSlf4jLogLevel(HikariPool.class, Level.DEBUG);
      StubConnection.count.set(0);
   }

   @After
   public void afterTest()
   {
      TestElf.setSlf4jLogLevel(PoolBase.class, Level.WARN);
      TestElf.setSlf4jLogLevel(HikariPool.class, Level.WARN);
      StubConnection.slowCreate = false;
   }

   @Test
   public void testShutdown1() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = true;

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(false);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final HikariDataSource ds = new HikariDataSource(config);
      HikariPool pool = TestElf.getPool(ds);

      Thread[] threads = new Thread[10];
      for (int i = 0; i < 10; i++) {
         threads[i] = new Thread() {
            @Override
            public void run()
            {
               try {
                  if (ds.getConnection() != null) {
                     UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis(1));
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

      UtilityElf.quietlySleep(1800L);

      Assert.assertTrue("Total connection count not as expected, ", pool.getTotalConnections() > 0);

      ds.close();

      Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
      Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
      Assert.assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
      Assert.assertTrue(ds.isClosed());
   }

   @Test
   public void testShutdown2() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = true;

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(10);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      HikariPool pool = TestElf.getPool(ds);

      UtilityElf.quietlySleep(1200L);

      Assert.assertTrue("Total connection count not as expected, ", pool.getTotalConnections() > 0);

      ds.close();

      Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
      Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
      Assert.assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
      Assert.assertTrue(ds.toString().startsWith("HikariDataSource (") && ds.toString().endsWith(")"));
   }

   @Test
   public void testShutdown3() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = false;

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      HikariPool pool = TestElf.getPool(ds);

      UtilityElf.quietlySleep(1200L);

      Assert.assertTrue("Total connection count not as expected, ", pool.getTotalConnections() == 5);

      ds.close();

      Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
      Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
      Assert.assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
   }

   @Test
   public void testShutdown4() throws SQLException
   {
      StubConnection.slowCreate = true;

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(10);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(false);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);

      UtilityElf.quietlySleep(500L);

      ds.close();

      long startTime = ClockSource.INSTANCE.currentTime();
      while (ClockSource.INSTANCE.elapsedMillis(startTime) < TimeUnit.SECONDS.toMillis(5) && threadCount() > 0) {
         UtilityElf.quietlySleep(250);
      }

      Assert.assertSame("Unreleased connections after shutdown", 0, TestElf.getPool(ds).getTotalConnections());
   }

   @Test
   public void testShutdown5() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      HikariPool pool = TestElf.getPool(ds);

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

   @Test
   public void testAfterShutdown() throws Exception
   {
       HikariConfig config = new HikariConfig();
       config.setMinimumIdle(0);
       config.setMaximumPoolSize(5);
       config.setInitializationFailFast(true);
       config.setConnectionTestQuery("VALUES 1");
       config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

       HikariDataSource ds = new HikariDataSource(config);
       ds.close();
       try
       {
           ds.getConnection();
       }
       catch (SQLException e) {
          Assert.assertTrue(e.getMessage().contains("has been closed."));
       }
   }

   @Test
   public void testShutdownDuringInit() throws Exception
   {
      final HikariConfig config = new HikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setInitializationFailFast(true);
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
      final HikariConfig config = new HikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      for (int i = 0; i < 4; i++) {
         final HikariDataSource ds = new HikariDataSource(config);
         Thread t = new Thread() {
            @Override
            public void run() {
               Connection connection = null;
               try {
                  connection = ds.getConnection();
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
                  try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }
                  ds.close();
               }
            }
         };
         t.start();

         Thread t2 = new Thread() {
            @Override
            public void run() {
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
