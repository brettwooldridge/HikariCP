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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Assert;
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
      HikariConfig config = new HikariConfig();
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
         Assert.assertNotNull(connection);

         PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
         Assert.assertNotNull(statement);

         ResultSet resultSet = statement.executeQuery();
         Assert.assertNotNull(resultSet);

         try {
            statement.getMaxFieldSize();
            Assert.fail();
         }
         catch (Exception e) {
            Assert.assertSame(SQLException.class, e.getClass());
         }
      }

      HikariPool pool = TestElf.getPool(ds);
      Assert.assertTrue("Total (3) connections not as expected", pool.getTotalConnections() >= 0);
      Assert.assertTrue("Idle (3) connections not as expected", pool.getIdleConnections() >= 0);
   }

   @Test
   public void testUseAfterStatementClose() throws SQLException
   {
      Connection connection = ds.getConnection();
      Assert.assertNotNull(connection);

      try (Statement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?")) {
         statement.close();
         statement.getMoreResults();

         Assert.fail();
      }
      catch (SQLException e) {
         Assert.assertSame("Connection is closed", e.getMessage());
      }
   }

   @Test
   public void testUseAfterClose() throws SQLException
   {
      try (Connection connection = ds.getConnection()) {
         Assert.assertNotNull(connection);
         connection.close();

         try (Statement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?")) {
            Assert.fail();
         }
         catch (SQLException e) {
            Assert.assertSame("Connection is closed", e.getMessage());
         }
      }
   }
}
