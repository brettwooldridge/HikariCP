package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.pool.PoolElf;

public class ConnectionStateTest
{
   @Test
   public void testAutoCommit() throws SQLException
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setAutoCommit(true);
      ds.setMinimumIdle(1);
      ds.setMaximumPoolSize(1);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      ds.addDataSourceProperty("user", "bar");
      ds.addDataSourceProperty("password", "secret");
      ds.addDataSourceProperty("url", "baf");
      ds.addDataSourceProperty("loginTimeout", "10");

      try {
         Connection connection = ds.getConnection();
         Connection unwrap = connection.unwrap(Connection.class);
         connection.setAutoCommit(false);
         connection.close();

         Assert.assertTrue(unwrap.getAutoCommit());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testTransactionIsolation() throws SQLException
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
      ds.setMinimumIdle(1);
      ds.setMaximumPoolSize(1);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try {
         Connection connection = ds.getConnection();
         Connection unwrap = connection.unwrap(Connection.class);
         connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
         connection.close();

         Assert.assertEquals(Connection.TRANSACTION_READ_COMMITTED, unwrap.getTransactionIsolation());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testIsolation() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
      config.validate();

      int transactionIsolation = PoolElf.getTransactionIsolation(config.getTransactionIsolation());
      Assert.assertSame(Connection.TRANSACTION_REPEATABLE_READ, transactionIsolation);
   }

   @Test
   public void testReadOnly() throws Exception
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setCatalog("test");
      ds.setMinimumIdle(1);
      ds.setMaximumPoolSize(1);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try {
         Connection connection = ds.getConnection();
         Connection unwrap = connection.unwrap(Connection.class);
         connection.setReadOnly(true);
         connection.close();

         Assert.assertFalse(unwrap.isReadOnly());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testCatalog() throws SQLException
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setCatalog("test");
      ds.setMinimumIdle(1);
      ds.setMaximumPoolSize(1);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try {
         Connection connection = ds.getConnection();
         Connection unwrap = connection.unwrap(Connection.class);
         connection.setCatalog("other");
         connection.close();

         Assert.assertEquals("test", unwrap.getCatalog());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testCommitTracking() throws SQLException
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setMinimumIdle(1);
      ds.setMaximumPoolSize(1);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try {
         Connection connection = ds.getConnection();

         Statement statement = connection.createStatement();
         statement.execute("SELECT something");
         Assert.assertTrue(TestElf.getConnectionCommitDirtyState(connection));

         connection.commit();
         Assert.assertFalse(TestElf.getConnectionCommitDirtyState(connection));

         statement.execute("SELECT something", Statement.NO_GENERATED_KEYS);
         Assert.assertTrue(TestElf.getConnectionCommitDirtyState(connection));

         connection.rollback();
         Assert.assertFalse(TestElf.getConnectionCommitDirtyState(connection));

         ResultSet resultSet = statement.executeQuery("SELECT something");
         Assert.assertTrue(TestElf.getConnectionCommitDirtyState(connection));

         connection.rollback(null);
         Assert.assertFalse(TestElf.getConnectionCommitDirtyState(connection));

         resultSet.updateRow();
         Assert.assertTrue(TestElf.getConnectionCommitDirtyState(connection));

         connection.close();
      }
      finally {
         ds.close();
      }
      
   }
}
