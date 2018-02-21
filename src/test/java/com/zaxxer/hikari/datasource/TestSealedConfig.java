package com.zaxxer.hikari.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.fail;

public class TestSealedConfig
{
   @Test(expected = IllegalStateException.class)
   public void testSealed1() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
         fail("Exception should have been thrown");
      }
   }

   @Test(expected = IllegalStateException.class)
   public void testSealed2() throws SQLException
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource closeable = ds) {
         try (Connection connection = ds.getConnection()) {
            ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
            fail("Exception should have been thrown");
         }
      }
   }

   @Test(expected = IllegalStateException.class)
   public void testSealed3() throws SQLException
   {
      HikariDataSource ds = new HikariDataSource();
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource closeable = ds) {
         try (Connection connection = ds.getConnection()) {
            ds.setAutoCommit(false);
            fail("Exception should have been thrown");
         }
      }
   }

   @Test
   public void testSealedAccessibleMethods() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         ds.setConnectionTimeout(5000);
         ds.setValidationTimeout(5000);
         ds.setIdleTimeout(30000);
         ds.setLeakDetectionThreshold(60000);
         ds.setMaxLifetime(1800000);
         ds.setMinimumIdle(5);
         ds.setMaximumPoolSize(8);
         ds.setPassword("password");
         ds.setUsername("username");
      }
   }
}
