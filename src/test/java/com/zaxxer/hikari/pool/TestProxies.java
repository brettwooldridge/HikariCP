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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.mocks.StubStatement;

public class TestProxies
{
   @Test
   public void testProxyCreation() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();

         assertNotNull(conn.createStatement(ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE));
         assertNotNull(conn.createStatement(ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.HOLD_CURSORS_OVER_COMMIT));
         assertNotNull(conn.prepareCall("some sql"));
         assertNotNull(conn.prepareCall("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE));
         assertNotNull(conn.prepareCall("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.HOLD_CURSORS_OVER_COMMIT));
         assertNotNull(conn.prepareStatement("some sql", PreparedStatement.NO_GENERATED_KEYS));
         assertNotNull(conn.prepareStatement("some sql", new int[3]));
         assertNotNull(conn.prepareStatement("some sql", new String[3]));
         assertNotNull(conn.prepareStatement("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE));
         assertNotNull(conn.prepareStatement("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.HOLD_CURSORS_OVER_COMMIT));
         assertNotNull(conn.toString());

         assertTrue(conn.isWrapperFor(Connection.class));
         assertTrue(conn.isValid(10));
         assertFalse(conn.isClosed());
         assertTrue(conn.unwrap(StubConnection.class) instanceof StubConnection);
         try {
            conn.unwrap(TestProxies.class);
            fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testStatementProxy() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();

         PreparedStatement stmt = conn.prepareStatement("some sql");
         stmt.executeQuery();
         stmt.executeQuery("some sql");
         assertFalse(stmt.isClosed());
         assertNotNull(stmt.getGeneratedKeys());
         assertNotNull(stmt.getResultSet());
         assertNotNull(stmt.getConnection());
         assertTrue(stmt.unwrap(StubStatement.class) instanceof StubStatement);
         try {
            stmt.unwrap(TestProxies.class);
            fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testStatementExceptions() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(1));
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();
         StubConnection stubConnection = conn.unwrap(StubConnection.class);
         stubConnection.throwException = true;

         try {
            conn.createStatement();
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.createStatement(0, 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.createStatement(0, 0, 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareCall("");
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareCall("", 0, 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareCall("", 0, 0, 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("");
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", new int[0]);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", new String[0]);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", 0, 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", 0, 0, 0);
            fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testOtherExceptions() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         try (Connection conn = ds.getConnection()) {
            StubConnection stubConnection = conn.unwrap(StubConnection.class);
            stubConnection.throwException = true;

            try {
               conn.setTransactionIsolation(Connection.TRANSACTION_NONE);
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.isReadOnly();
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.setReadOnly(false);
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.setCatalog("");
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.setAutoCommit(false);
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.clearWarnings();
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.isValid(0);
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.isWrapperFor(getClass());
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.unwrap(getClass());
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               conn.close();
               fail();
            }
            catch (SQLException e) {
               // pass
            }

            try {
               assertFalse(conn.isValid(0));
            }
            catch (SQLException e) {
               fail();
            }
         }
      }
   }
}
