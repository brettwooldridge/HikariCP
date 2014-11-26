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

package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Before;

import com.zaxxer.hikari.util.PoolUtilities;

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
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(5000);
      config.setConnectionTestQuery("VALUES 1");

      config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
      config.addDataSourceProperty("serverName", "192.168.0.114");
      config.addDataSourceProperty("portNumber", "5432");
      config.addDataSourceProperty("databaseName", "test");
      config.addDataSourceProperty("user", "brettw");
      config.addDataSourceProperty("tcpKeepAlive", true);

      try (HikariDataSource ds = new HikariDataSource(config)) {

         System.err.println("\nMake sure the firewall is enabled.  Attempting connection in 5 seconds...");
         PoolUtilities.quietlySleep(5000L);

         TestElf.getPool(ds).logPoolState();

         System.err.println("\nNow attempting getConnection(), expecting a timeout...");

         long start = System.currentTimeMillis();
         try (Connection conn = ds.getConnection()) {
            System.err.println("\nOpps, got a connection.  Are you sure the firewall is enabled?");
         }
         catch (SQLException e)
         {
            Assert.assertTrue("Timeout less than expected " + (System.currentTimeMillis() - start) + "ms", (System.currentTimeMillis() - start) > 5000);
         }
      }

      System.err.println("\nPassed.");
   }

   //@Test
   public void testCase2() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(5000);
      config.setConnectionTestQuery("VALUES 1");

      config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
      config.addDataSourceProperty("serverName", "192.168.0.114");
      config.addDataSourceProperty("portNumber", "5432");
      config.addDataSourceProperty("databaseName", "test");
      config.addDataSourceProperty("user", "brettw");
      config.addDataSourceProperty("tcpKeepAlive", true);

      try (HikariDataSource ds = new HikariDataSource(config)) {

         System.err.println("\nDisable the firewall, please.  Starting test in 5 seconds...");
         PoolUtilities.quietlySleep(5000L);

         try (Connection conn = ds.getConnection()) {
            System.err.println("\nGot a connection, and released it.  Now, enable the firewall.");
         }
         
         TestElf.getPool(ds).logPoolState();
         PoolUtilities.quietlySleep(5000L);

         System.err.println("\nNow attempting another getConnection(), expecting a timeout...");

         long start = System.currentTimeMillis();
         try (Connection conn = ds.getConnection()) {
            System.err.println("\nOpps, got a connection.  Did you enable the firewall? " + conn);
            Assert.fail("Opps, got a connection.  Did you enable the firewall?");
         }
         catch (SQLException e)
         {
            Assert.assertTrue("Timeout less than expected " + (System.currentTimeMillis() - start) + "ms", (System.currentTimeMillis() - start) > 5000);
         }

         System.err.println("\nOk, so far so good.  Now, disable the firewall again.  Attempting connection in 5 seconds...");
         PoolUtilities.quietlySleep(5000L);
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
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(false);
      config.setConnectionTimeout(2000);
      config.setConnectionTestQuery("VALUES 1");

      config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
      config.addDataSourceProperty("serverName", "192.168.0.114");
      config.addDataSourceProperty("portNumber", "5432");
      config.addDataSourceProperty("databaseName", "test");
      config.addDataSourceProperty("user", "brettw");
      config.addDataSourceProperty("tcpKeepAlive", true);

      System.err.println("\nShutdown the database, please.  Starting test in 15 seconds...");
      PoolUtilities.quietlySleep(15000L);

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
               };
            }.start();
         }

         PoolUtilities.quietlySleep(5000L);

         System.err.println("Now, bring the DB online.  Checking pool in 15 seconds.");
         PoolUtilities.quietlySleep(15000L);

         TestElf.getPool(ds).logPoolState();
      }
   }

   @Before
   public void before()
   {
      System.err.println("\n");
   }
}
