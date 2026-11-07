/*
 * Copyright (C) 2026 Brett Wooldridge
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
import com.zaxxer.hikari.mocks.StubDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.*;

public class SchemaStateTest
{
   @Test
   public void testSchemaReadIsDeferredAndCached() throws SQLException
   {
      try (HikariDataSource ds = createDataSource("default"); Connection connection = ds.getConnection()) {
         SchemaConnection delegate = connection.unwrap(SchemaConnection.class);
         connection.setSchema("tenant");
         assertEquals(0, delegate.schemaReads);

         assertEquals("tenant", connection.getSchema());
         assertEquals("tenant", connection.getSchema());
         assertEquals(1, delegate.schemaReads);

         connection.setSchema("other");
         assertEquals(1, delegate.schemaReads);
         assertEquals("other", connection.getSchema());
         assertEquals(2, delegate.schemaReads);
      }
   }

   @Test
   public void testNullSchemaIsResolvedAndCached() throws SQLException
   {
      try (HikariDataSource ds = createDataSource("default")) {
         try (Connection connection = ds.getConnection()) {
            SchemaConnection delegate = connection.unwrap(SchemaConnection.class);
            connection.setSchema("missing");
            assertEquals(0, delegate.schemaReads);
            assertNull(connection.getSchema());
            assertNull(connection.getSchema());
            assertEquals(1, delegate.schemaReads);
         }
         try (Connection connection = ds.getConnection()) {
            assertEquals("default", connection.getSchema());
         }
      }
   }

   @Test
   public void testUnreadSchemaIsResetOnClose() throws SQLException
   {
      try (HikariDataSource ds = createDataSource("default")) {
         SchemaConnection delegate;
         try (Connection connection = ds.getConnection()) {
            delegate = connection.unwrap(SchemaConnection.class);
            connection.setSchema("tenant");
         }
         assertEquals(0, delegate.schemaReads);
         try (Connection connection = ds.getConnection()) {
            assertSame(delegate, connection.unwrap(SchemaConnection.class));
            assertEquals("default", connection.getSchema());
         }
      }
   }

   @Test
   public void testFailedSchemaReadCanBeRetried() throws SQLException
   {
      try (HikariDataSource ds = createDataSource("default"); Connection connection = ds.getConnection()) {
         SchemaConnection delegate = connection.unwrap(SchemaConnection.class);
         connection.setSchema("tenant");
         delegate.throwException = true;
         try {
            assertThrows(SQLException.class, connection::getSchema);
         }
         finally {
            delegate.throwException = false;
         }
         assertEquals("tenant", connection.getSchema());
         assertEquals("tenant", connection.getSchema());
         assertEquals(2, delegate.schemaReads);
      }
   }

   @Test
   public void testFailedSchemaChangePreservesCachedSchema() throws SQLException
   {
      try (HikariDataSource ds = createDataSource("default"); Connection connection = ds.getConnection()) {
         SchemaConnection delegate = connection.unwrap(SchemaConnection.class);
         connection.setSchema("tenant");
         assertEquals("tenant", connection.getSchema());

         delegate.throwException = true;
         try {
            assertThrows(SQLException.class, () -> connection.setSchema("other"));
            assertEquals("tenant", connection.getSchema());
         }
         finally {
            delegate.throwException = false;
         }
         assertEquals(1, delegate.schemaReads);
      }
   }

   @Test
   public void testSchemaIsNotResetWithoutConfiguredSchema() throws SQLException
   {
      try (HikariDataSource ds = createDataSource(null)) {
         SchemaConnection delegate;
         try (Connection connection = ds.getConnection()) {
            delegate = connection.unwrap(SchemaConnection.class);
            connection.setSchema("tenant");
         }
         assertEquals(0, delegate.schemaReads);
         try (Connection connection = ds.getConnection()) {
            assertSame(delegate, connection.unwrap(SchemaConnection.class));
            assertEquals("tenant", connection.getSchema());
         }
      }
   }

   private HikariDataSource createDataSource(String schema)
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setSchema(schema);
      config.setDataSource(new StubDataSource() {
         @Override
         public Connection getConnection() {
            return new SchemaConnection();
         }
      });
      return new HikariDataSource(config);
   }

   private static class SchemaConnection extends StubConnection
   {
      private int schemaReads;

      @Override
      public String getSchema() throws SQLException {
         schemaReads++;
         return super.getSchema();
      }

      @Override
      public void setSchema(String schema) throws SQLException {
         super.setSchema("missing".equals(schema) ? null : schema);
      }
   }
}
