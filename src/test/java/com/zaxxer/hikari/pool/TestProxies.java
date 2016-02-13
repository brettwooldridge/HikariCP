package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
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
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();

         Assert.assertNotNull(conn.createStatement(ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE));
         Assert.assertNotNull(conn.createStatement(ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.HOLD_CURSORS_OVER_COMMIT));
         Assert.assertNotNull(conn.prepareCall("some sql"));
         Assert.assertNotNull(conn.prepareCall("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE));
         Assert.assertNotNull(conn.prepareCall("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.HOLD_CURSORS_OVER_COMMIT));
         Assert.assertNotNull(conn.prepareStatement("some sql", PreparedStatement.NO_GENERATED_KEYS));
         Assert.assertNotNull(conn.prepareStatement("some sql", new int[3]));
         Assert.assertNotNull(conn.prepareStatement("some sql", new String[3]));
         Assert.assertNotNull(conn.prepareStatement("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE));
         Assert.assertNotNull(conn.prepareStatement("some sql", ResultSet.FETCH_FORWARD, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.HOLD_CURSORS_OVER_COMMIT));
         Assert.assertNotNull(conn.toString());

         Assert.assertTrue(conn.isWrapperFor(Connection.class));
         Assert.assertTrue(conn.isValid(10));
         Assert.assertFalse(conn.isClosed());
         Assert.assertTrue(conn.unwrap(StubConnection.class) instanceof StubConnection);
         try {
            conn.unwrap(TestProxies.class);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testStatementProxy() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();

         PreparedStatement stmt = conn.prepareStatement("some sql");
         stmt.executeQuery();
         stmt.executeQuery("some sql");
         Assert.assertFalse(stmt.isClosed());
         Assert.assertNotNull(stmt.getGeneratedKeys());
         Assert.assertNotNull(stmt.getResultSet());
         Assert.assertNotNull(stmt.getConnection());
         Assert.assertTrue(stmt.unwrap(StubStatement.class) instanceof StubStatement);
         try {
            stmt.unwrap(TestProxies.class);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testStatementExceptions() throws SQLException
   {
      HikariConfig config = new HikariConfig();
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
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.createStatement(0, 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.createStatement(0, 0, 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareCall("");
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareCall("", 0, 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareCall("", 0, 0, 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("");
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", new int[0]);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", new String[0]);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", 0, 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.prepareStatement("", 0, 0, 0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }
      }
   }

   @Test
   public void testOtherExceptions() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection conn = ds.getConnection();
         StubConnection stubConnection = conn.unwrap(StubConnection.class);
         stubConnection.throwException = true;

         try {
            conn.setTransactionIsolation(Connection.TRANSACTION_NONE);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.isReadOnly();
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.setReadOnly(false);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.setCatalog("");
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.setAutoCommit(false);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.clearWarnings();
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.isValid(0);
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.isWrapperFor(getClass());
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.unwrap(getClass());
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            conn.close();
            Assert.fail();
         }
         catch (SQLException e) {
            // pass
         }

         try {
            Assert.assertFalse(conn.isValid(0));
         }
         catch (SQLException e) {
            Assert.fail();
         }
      }
   }
}
