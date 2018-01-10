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
import static com.zaxxer.hikari.pool.TestElf.getPool;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class StatementTest
{
   private HikariDataSource ds;

   @Before
   public void setup()
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(2);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      ds = new HikariDataSource(config);
   }

   @After
   public void teardown()
   {
      ds.close();
   }

   @Test
   public void testStatementClose() throws SQLException
   {
      ds.getConnection().close();

      HikariPool pool = getPool(ds);
      assertTrue("Total connections not as expected", pool.getTotalConnections() >= 1);
      assertTrue("Idle connections not as expected", pool.getIdleConnections() >= 1);

      try (Connection connection = ds.getConnection()) {
         assertNotNull(connection);

         assertTrue("Total connections not as expected", pool.getTotalConnections() >= 1);
         assertTrue("Idle connections not as expected", pool.getIdleConnections() >= 0);

         Statement statement = connection.createStatement();
         assertNotNull(statement);

         connection.close();

         assertTrue(statement.isClosed());
      }
   }

   @Test
   public void testAutoStatementClose() throws SQLException
   {
      try (Connection connection = ds.getConnection()) {
         assertNotNull(connection);

         Statement statement1 = connection.createStatement();
         assertNotNull(statement1);
         Statement statement2 = connection.createStatement();
         assertNotNull(statement2);

         connection.close();

         assertTrue(statement1.isClosed());
         assertTrue(statement2.isClosed());
      }
   }

   @Test
   public void testStatementResultSetProxyClose() throws SQLException {
      try (Connection connection = ds.getConnection()) {
         assertNotNull(connection);

         Statement statement1 = connection.createStatement();
         assertNotNull(statement1);
         Statement statement2 = connection.createStatement();
         assertNotNull(statement2);

         statement1.getResultSet().getStatement().close();
         statement2.getGeneratedKeys().getStatement().close();

         assertTrue(statement1.isClosed());
         assertTrue(statement2.isClosed());
      }
   }

   @Test
   public void testDoubleStatementClose() throws SQLException
   {
      try (Connection connection = ds.getConnection();
            Statement statement1 = connection.createStatement()) {
         statement1.close();
         statement1.close();
      }
   }

   @Test
   public void testOutOfOrderStatementClose() throws SQLException
   {
      try (Connection connection = ds.getConnection();
            Statement statement1 = connection.createStatement();
            Statement statement2 = connection.createStatement()) {
         statement1.close();
         statement2.close();
      }
   }
}
