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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ExceptionTest
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
   public void testException1() throws SQLException
   {
      try (Connection connection = ds.getConnection()) {
         assertNotNull(connection);

         PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
         assertNotNull(statement);

         ResultSet resultSet = statement.executeQuery();
         assertNotNull(resultSet);

         try {
            statement.getMaxFieldSize();
            fail();
         }
         catch (Exception e) {
            assertSame(SQLException.class, e.getClass());
         }
      }

      HikariPool pool = getPool(ds);
      assertTrue("Total (3) connections not as expected", pool.getTotalConnections() >= 0);
      assertTrue("Idle (3) connections not as expected", pool.getIdleConnections() >= 0);
   }

   @Test
   public void testUseAfterStatementClose() throws SQLException
   {
      Connection connection = ds.getConnection();
      assertNotNull(connection);

      try (Statement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?")) {
         statement.close();
         statement.getMoreResults();

         fail();
      }
      catch (SQLException e) {
         assertSame("Connection is closed", e.getMessage());
      }
   }

   @Test
   public void testUseAfterClose() throws SQLException
   {
      try (Connection connection = ds.getConnection()) {
         assertNotNull(connection);
         connection.close();

         try (Statement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?")) {
            fail();
         }
         catch (SQLException e) {
            assertSame("Connection is closed", e.getMessage());
         }
      }
   }
}
