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
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class RampUpDown
{
   private static final long IDLE_TIMEOUT_MS = 1000;
   private static final long WAIT_TIMEOUT_MS = 10_000;

   @Test
   public void rampUpDownTest() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(60);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "250");

      try (HikariDataSource ds = new HikariDataSource(config)) {

         ds.setIdleTimeout(IDLE_TIMEOUT_MS);
         HikariPool pool = getPool(ds);

         awaitTotalConnections(pool, 5);

         assertPoolState(pool, 5, 5, 0);

         Connection[] connections = new Connection[ds.getMaximumPoolSize()];
         for (int i = 0; i < connections.length; i++)
         {
            connections[i] = ds.getConnection();
         }

         assertPoolState(pool, 60, 0, 60);

         for (Connection connection : connections)
         {
            connection.close();
         }

         assertPoolState(pool, 60, 60, 0);

         awaitTotalConnections(pool, 5);

         assertPoolState(pool, 5, 5, 0);
      }
   }

   private static void awaitTotalConnections(final HikariPool pool, final int expected)
   {
      final var deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
      while (pool.getTotalConnections() != expected && System.currentTimeMillis() < deadline) {
         quietlySleep(50);
      }

      assertEquals("Total connections not as expected", expected, pool.getTotalConnections());
   }

   private static void assertPoolState(final HikariPool pool, final int total, final int idle, final int active)
   {
      assertEquals("Total connections not as expected", total, pool.getTotalConnections());
      assertEquals("Idle connections not as expected", idle, pool.getIdleConnections());
      assertEquals("Active connections not as expected", active, pool.getActiveConnections());
   }
}