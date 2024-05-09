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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.fail;

/**
 * @author Yanming Zhou
 */
public class TestStates
{
   @Test
   public void testGetBeforeSet() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();
         StubConnection stub = conn.unwrap(StubConnection.class);
         stub.throwException = true;
         try {
            conn.isReadOnly();
            fail();
         }
         catch (SQLException e) {
            // pass
         }
         try {
            conn.getAutoCommit();
            fail();
         }
         catch (SQLException e) {
            // pass
         }
         try {
            conn.getTransactionIsolation();
            fail();
         }
         catch (SQLException e) {
            // pass
         }
         try {
            conn.getCatalog();
            fail();
         }
         catch (SQLException e) {
            // pass
         }
         try {
            conn.getNetworkTimeout();
            fail();
         }
         catch (SQLException e) {
            // pass
         }
         try {
            conn.getSchema();
            fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testGetAfterSet() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();
         StubConnection stub = conn.unwrap(StubConnection.class);

         conn.setReadOnly(true);
         conn.setAutoCommit(true);
         conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
         conn.setCatalog("catalog");
         conn.setNetworkTimeout(null, 10);
         conn.setSchema("schema");

         stub.throwException = true;

         // should not throw exception even if stub throws exception
         conn.isReadOnly();
         conn.getAutoCommit();
         conn.getTransactionIsolation();
         conn.getCatalog();
         conn.getNetworkTimeout();
         conn.getSchema();
      }
   }
}
