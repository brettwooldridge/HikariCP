package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;

public class IsolationTest
{
   @Test
   public void testIsolation() throws SQLException
   {
      try (HikariDataSource ds = new HikariDataSource()) {
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setIsolateInternalQueries(true);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         Connection connection = ds.getConnection();
         connection.close();

         Connection connection2 = ds.getConnection();
         connection2.close();

         Assert.assertNotSame(connection, connection2);
         Assert.assertSame(connection.unwrap(Connection.class), connection2.unwrap(Connection.class));
      }
   }

   @Test
   public void testNonIsolation() throws SQLException
   {
      try (HikariDataSource ds = new HikariDataSource()) {
         ds.setMinimumIdle(1);
         ds.setMaximumPoolSize(1);
         ds.setIsolateInternalQueries(false);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         Connection connection = ds.getConnection();
         connection.close();

         Connection connection2 = ds.getConnection();
         connection2.close();

         Assert.assertSame(connection.unwrap(Connection.class), connection2.unwrap(Connection.class));
      }
   }
}
