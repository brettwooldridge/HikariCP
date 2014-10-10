package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

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

       HikariDataSource ds = new HikariDataSource(config);
       try {
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
       finally {
          ds.close();
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

      HikariDataSource ds = new HikariDataSource(config);
      try {
         Connection conn = ds.getConnection();

         PreparedStatement stmt = conn.prepareStatement("some sql");
         stmt.executeQuery();
         stmt.executeQuery("some sql");
         Assert.assertFalse(stmt.isClosed());
         Assert.assertNotNull(stmt.getGeneratedKeys());
         Assert.assertNotNull(stmt.getResultSet());
         Assert.assertNotNull(stmt.getConnection());
         Assert.assertTrue(stmt.unwrap(StubStatement.class) instanceof StubStatement);
      }
      finally {
         ds.close();
      }
   }
}
