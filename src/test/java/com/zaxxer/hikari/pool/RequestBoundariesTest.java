package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;

import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;

public class RequestBoundariesTest {

   private static final HikariConfig config;
   static {
      config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
   }

   private HikariPool getHikariPool(boolean enableRequestBoundaries) {
      System.setProperty("com.zaxxer.hikari.enableRequestBoundaries", String.valueOf(enableRequestBoundaries));
      HikariDataSource ds = new HikariDataSource(config);
      HikariPool pool = getPool(ds);
      return pool;
   }

   @Test
   public void requestBoundaryEnabledTest() throws Exception {
      HikariPool pool = getHikariPool(true);
      Connection conn = pool.getConnection();
      StubConnection stubConnection = conn.unwrap(StubConnection.class);
      Assert.assertTrue("Begin request called", stubConnection.beginRequestCalled);
      Assert.assertFalse("End request called", stubConnection.endRequestCalled);
      conn.close();
      Assert.assertTrue("Begin request called", stubConnection.beginRequestCalled);
      Assert.assertTrue("End request called", stubConnection.endRequestCalled);
   }

   @Test
   public void requestBoundaryDisabledTest() throws Exception {
      HikariPool pool = getHikariPool(false);
      Connection conn = pool.getConnection();
      StubConnection stubConnection = conn.unwrap(StubConnection.class);
      Assert.assertFalse("Begin request called", stubConnection.beginRequestCalled);
      Assert.assertFalse("End request called", stubConnection.endRequestCalled);
      conn.close();
      Assert.assertFalse("Begin request called", stubConnection.beginRequestCalled);
      Assert.assertFalse("End request called", stubConnection.endRequestCalled);
   }
}
