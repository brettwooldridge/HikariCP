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
import java.sql.SQLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.DriverDataSource;

public class JdbcDriverTest
{
   private HikariDataSource ds;

   @After
   public void teardown()
   {
      if (ds != null) {
         ds.close();
      }
   }

   @Test
   public void driverTest1() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDriverClassName("com.zaxxer.hikari.mocks.StubDriver");
      config.setJdbcUrl("jdbc:stub");
      config.addDataSourceProperty("user", "bart");
      config.addDataSourceProperty("password", "simpson");

      ds = new HikariDataSource(config);

      Assert.assertTrue(ds.isWrapperFor(DriverDataSource.class));

      DriverDataSource unwrap = ds.unwrap(DriverDataSource.class);
      Assert.assertNotNull(unwrap);

      Connection connection = ds.getConnection();
      connection.close();
   }

   @Test
   public void driverTest2() throws SQLException
   {
      HikariConfig config = new HikariConfig();

      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDriverClassName("com.zaxxer.hikari.mocks.StubDriver");
      config.setJdbcUrl("jdbc:invalid");

      try {
         ds = new HikariDataSource(config);
      }
      catch (RuntimeException e) {
         Assert.assertTrue(e.getMessage().contains("claims to not accept"));
      }
   }
}
