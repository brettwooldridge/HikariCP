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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.UtilityElf;

/**
 * This test is meant to be run manually and interactively and was
 * build for issue #159.
 *
 * @author Brett Wooldridge
 */
public class PostgresTest
{
   //@Test
   public void testCase1() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(3000);
      config.setIdleTimeout(TimeUnit.SECONDS.toMillis(10));
      config.setValidationTimeout(TimeUnit.SECONDS.toMillis(2));

      config.setJdbcUrl("jdbc:pgsql://localhost:5432/test");
      config.setUsername("brettw");

      try (final HikariDataSource ds = new HikariDataSource(config)) {
         final long start = ClockSource.INSTANCE.currentTime();
         do {
            Thread t = new Thread() {
               public void run() {
                  try (Connection connection = ds.getConnection()) {
                     System.err.println("Obtained connection " + connection);
                     UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis((long)(10 + (Math.random() * 20))));
                  }
                  catch (SQLException e) {
                     e.printStackTrace();
                  }
               }
            };
            t.setDaemon(true);
            t.start();

            UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis((long)((Math.random() * 20))));
         } while (ClockSource.INSTANCE.elapsedMillis(start) < TimeUnit.MINUTES.toMillis(15));
      }
   }

   //@Test
   public void testCase2() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(1000);
      config.setIdleTimeout(TimeUnit.SECONDS.toMillis(60));

      config.setJdbcUrl("jdbc:pgsql://localhost:5432/test");
      config.setUsername("brettw");

      try (HikariDataSource ds = new HikariDataSource(config)) {

         try (Connection conn = ds.getConnection()) {
            System.err.println("\nGot a connection, and released it.  Now, enable the firewall.");
         }

         TestElf.getPool(ds).logPoolState();
         UtilityElf.quietlySleep(5000L);

         System.err.println("\nNow attempting another getConnection(), expecting a timeout...");

         long start = ClockSource.INSTANCE.currentTime();
         try (Connection conn = ds.getConnection()) {
            System.err.println("\nOpps, got a connection.  Did you enable the firewall? " + conn);
            Assert.fail("Opps, got a connection.  Did you enable the firewall?");
         }
         catch (SQLException e)
         {
            Assert.assertTrue("Timeout less than expected " + ClockSource.INSTANCE.elapsedMillis(start) + "ms", ClockSource.INSTANCE.elapsedMillis(start) > 5000);
         }

         System.err.println("\nOk, so far so good.  Now, disable the firewall again.  Attempting connection in 5 seconds...");
         UtilityElf.quietlySleep(5000L);
         TestElf.getPool(ds).logPoolState();

         try (Connection conn = ds.getConnection()) {
            System.err.println("\nGot a connection, and released it.");
         }
      }

      System.err.println("\nPassed.");
   }

   //@Test
   public void testCase3() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(1000);
      config.setIdleTimeout(TimeUnit.SECONDS.toMillis(60));

      config.setJdbcUrl("jdbc:pgsql://localhost:5432/test");
      config.setUsername("brettw");

      try (final HikariDataSource ds = new HikariDataSource(config)) {
         for (int i = 0; i < 10; i++) {
            new Thread() {
               public void run() {
                  try (Connection conn = ds.getConnection()) {
                     System.err.println("ERROR: should not have acquired connection.");
                  }
                  catch (SQLException e) {
                     // expected
                  }
               }
            }.start();
         }

         UtilityElf.quietlySleep(5000L);

         System.err.println("Now, bring the DB online.  Checking pool in 15 seconds.");
         UtilityElf.quietlySleep(15000L);

         TestElf.getPool(ds).logPoolState();
      }
   }

   // @Test
   public void testCase4() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(15);
      config.setConnectionTimeout(10000);
      config.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
      config.setMaxLifetime(TimeUnit.MINUTES.toMillis(2));
      config.setRegisterMbeans(true);

      config.setJdbcUrl("jdbc:postgresql://localhost:5432/netld");
      config.setUsername("brettw");

      try (final HikariDataSource ds = new HikariDataSource(config)) {

         countdown(20);
         List<Thread> threads = new ArrayList<>();
         for (int i = 0; i < 20; i++) {
            threads.add(new Thread() {
               public void run() {
                  UtilityElf.quietlySleep((long)(Math.random() * 2500L));
                  final long start = ClockSource.INSTANCE.currentTime();
                  do {
                     try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                        try (ResultSet rs = stmt.executeQuery("SELECT * FROM device WHERE device_id=0 ORDER BY device_id LIMIT 1 OFFSET 0")) {
                           rs.next();
                        }
                        UtilityElf.quietlySleep(100L); //Math.max(50L, (long)(Math.random() * 250L)));
                     }
                     catch (SQLException e) {
                        e.printStackTrace();
                        // throw new RuntimeException(e);
                     }

                     // UtilityElf.quietlySleep(10L); //Math.max(50L, (long)(Math.random() * 250L)));
                  } while (ClockSource.INSTANCE.elapsedMillis(start) < TimeUnit.MINUTES.toMillis(5));
               }
            });
         }

//         threads.forEach(t -> t.start());
//         threads.forEach(t -> { try { t.join(); } catch (InterruptedException e) {} });
      }
   }

   @Before
   public void before()
   {
      System.err.println("\n");
   }

   private void countdown(int seconds)
   {
      do {
         System.out.printf("Starting in %d seconds...\n", seconds);
         if (seconds > 10) {
            UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis(10));
            seconds -= 10;
         }
         else {
            UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis(1));
            seconds -= 1;
         }
      } while (seconds > 0);
   }
}
