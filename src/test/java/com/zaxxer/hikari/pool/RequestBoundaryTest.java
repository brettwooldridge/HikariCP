package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;

public class RequestBoundaryTest {

   private static final HikariConfig config;
   static {
      config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
   }

   private HikariPool getHikariPool(boolean enableRequestBoundary) {
      System.setProperty("com.zaxxer.hikari.enableRequestBoundary", String.valueOf(enableRequestBoundary));
      HikariDataSource ds = new HikariDataSource(config);
      HikariPool pool = getPool(ds);
      return pool;
   }

   @Test
   public void RequestBoundaryEnabledTest() throws Exception {
      HikariPool pool = getHikariPool(true);
      Connection conn = pool.getConnection();
      Assert.assertTrue("Begin request called", ((StubConnection)conn).beginRequestCalled);
      Assert.assertFalse("End request called", ((StubConnection)conn).beginRequestCalled);
      conn.close();
      Assert.assertTrue("Begin request called", ((StubConnection)conn).beginRequestCalled);
      Assert.assertTrue("End request called", ((StubConnection)conn).beginRequestCalled);
   }

   @Test
   public void RequestBoundaryDisabledTest() throws Exception {
      HikariPool pool = getHikariPool(false);
      Connection conn = pool.getConnection();
      Assert.assertFalse("Begin request called", ((StubConnection)conn).beginRequestCalled);
      Assert.assertFalse("End request called", ((StubConnection)conn).beginRequestCalled);
      conn.close();
      Assert.assertFalse("Begin request called", ((StubConnection)conn).beginRequestCalled);
      Assert.assertFalse("End request called", ((StubConnection)conn).beginRequestCalled);
   }

}
