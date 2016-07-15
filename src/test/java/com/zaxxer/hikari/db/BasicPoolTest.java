/*
 * Copyright (C) 2016 Brett Wooldridge
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

package com.zaxxer.hikari.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.TestElf;

/**
 * @author brettw
 *
 */
public class BasicPoolTest
{
   @Before
   public void setup() throws SQLException
   {
       HikariConfig config = new HikariConfig();
       config.setMinimumIdle(1);
       config.setMaximumPoolSize(2);
       config.setConnectionTestQuery("SELECT 1");
       config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
       config.addDataSourceProperty("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

       try (HikariDataSource ds = new HikariDataSource(config);
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement()) {
          stmt.executeUpdate("DROP TABLE IF EXISTS basic_pool_test");
          stmt.executeUpdate("CREATE TABLE basic_pool_test ("
                            + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
                            + "timestamp TIMESTAMP, "
                            + "string VARCHAR(128), "
                            + "string_from_number NUMERIC "
                            + ")");
       }
   }

   @Test
   public void testIdleTimeout() throws InterruptedException, SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(10);
      config.setConnectionTestQuery("SELECT 1");
      config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
      config.addDataSourceProperty("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "1000");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         System.clearProperty("com.zaxxer.hikari.housekeeping.periodMs");

         TimeUnit.SECONDS.sleep(1);

         HikariPool pool = TestElf.getPool(ds);

         ds.setIdleTimeout(3000);

         Assert.assertSame("Total connections not as expected", 5, pool.getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 5, pool.getIdleConnections());

         Connection connection = ds.getConnection();
         Assert.assertNotNull(connection);

         TimeUnit.MILLISECONDS.sleep(1500);

         Assert.assertSame("Second total connections not as expected", 6, pool.getTotalConnections());
         Assert.assertSame("Second idle connections not as expected", 5, pool.getIdleConnections());
         connection.close();

         Assert.assertSame("Idle connections not as expected", 6, pool.getIdleConnections());

         TimeUnit.SECONDS.sleep(2);

         Assert.assertSame("Third total connections not as expected", 5, pool.getTotalConnections());
         Assert.assertSame("Third idle connections not as expected", 5, pool.getIdleConnections());
      }
   }

   @Test
   public void testIdleTimeout2() throws InterruptedException, SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMaximumPoolSize(50);
      config.setConnectionTestQuery("SELECT 1");
      config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
      config.addDataSourceProperty("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "1000");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         System.clearProperty("com.zaxxer.hikari.housekeeping.periodMs");

         TimeUnit.SECONDS.sleep(1);

         HikariPool pool = TestElf.getPool(ds);

         ds.setIdleTimeout(3000);

         Assert.assertSame("Total connections not as expected", 50, pool.getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 50, pool.getIdleConnections());

         Connection connection = ds.getConnection();
         Assert.assertNotNull(connection);

         TimeUnit.MILLISECONDS.sleep(1500);

         Assert.assertSame("Second total connections not as expected", 50, pool.getTotalConnections());
         Assert.assertSame("Second idle connections not as expected", 49, pool.getIdleConnections());
         connection.close();

         Assert.assertSame("Idle connections not as expected", 50, pool.getIdleConnections());

         TimeUnit.SECONDS.sleep(3);

         Assert.assertSame("Third total connections not as expected", 50, pool.getTotalConnections());
         Assert.assertSame("Third idle connections not as expected", 50, pool.getIdleConnections());
      }
   }
}
