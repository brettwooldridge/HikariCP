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

import static com.zaxxer.hikari.pool.TestElf.newHikariDataSource;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;

public class IsolationTest
{
   @Test
   public void testIsolation() throws SQLException
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setIsolateInternalQueries(true);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         try (Connection connection = ds.getConnection()) {
            connection.close();

            try (Connection connection2 = ds.getConnection()) {
               connection2.close();

               assertNotSame(connection, connection2);
               assertSame(connection.unwrap(Connection.class), connection2.unwrap(Connection.class));
            }
         }
      }
   }

   @Test
   public void testNonIsolation() throws SQLException
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setIsolateInternalQueries(false);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         try (Connection connection = ds.getConnection()) {
            connection.close();

            try (Connection connection2 = ds.getConnection()) {
               connection2.close();

               assertSame(connection.unwrap(Connection.class), connection2.unwrap(Connection.class));
            }
         }
      }
   }
}
