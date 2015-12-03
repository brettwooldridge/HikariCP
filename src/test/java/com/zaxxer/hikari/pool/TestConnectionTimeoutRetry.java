package com.zaxxer.hikari.pool;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.mocks.StubDataSource;
import com.zaxxer.hikari.util.ClockSource;

public class TestConnectionTimeoutRetry
{
   @Test
   public void testConnectionRetries() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2800);
      config.setValidationTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         long start = ClockSource.INSTANCE.currentTime();
         try (Connection connection = ds.getConnection()) {
            connection.close();
            Assert.fail("Should not have been able to get a connection.");
         }
         catch (SQLException e) {
            long elapsed = ClockSource.INSTANCE.elapsedMillis(start);
            long timeout = config.getConnectionTimeout();
            Assert.assertTrue("Didn't wait long enough for timeout", (elapsed >= timeout));
         }
      }
   }

   @Test
   public void testConnectionRetries2() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2800);
      config.setValidationTimeout(2800);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
         scheduler.schedule(new Runnable() {
            @Override
            public void run()
            {
               stubDataSource.setThrowException(null);
            }
         }, 300, TimeUnit.MILLISECONDS);

         long start = ClockSource.INSTANCE.currentTime();
         try {
            Connection connection = ds.getConnection();
            connection.close();

            long elapsed = ClockSource.INSTANCE.elapsedMillis(start);
            Assert.assertTrue("Connection returned too quickly, something is wrong.", elapsed > 250);
            Assert.assertTrue("Waited too long to get a connection.", elapsed < config.getConnectionTimeout());
         }
         catch (SQLException e) {
            Assert.fail("Should not have timed out: " + e.getMessage());
         }
         finally {
            scheduler.shutdownNow();
         }
      }
   }

   @Test
   public void testConnectionRetries3() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(2800);
      config.setValidationTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final Connection connection1 = ds.getConnection();
         final Connection connection2 = ds.getConnection();
         Assert.assertNotNull(connection1);
         Assert.assertNotNull(connection2);

         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
         scheduler.schedule(new Runnable() {
            @Override
            public void run()
            {
               try {
                  connection1.close();
               }
               catch (Exception e) {
                  e.printStackTrace(System.err);
               }
            }
         }, 800, TimeUnit.MILLISECONDS);

         long start = ClockSource.INSTANCE.currentTime();
         try {
            Connection connection3 = ds.getConnection();
            connection3.close();

            long elapsed = ClockSource.INSTANCE.elapsedMillis(start);
            Assert.assertTrue("Waited too long to get a connection.", (elapsed >= 700) && (elapsed < 950));
         }
         catch (SQLException e) {
            Assert.fail("Should not have timed out.");
         }
         finally {
            scheduler.shutdownNow();
         }
      }
   }

   @Test
   public void testConnectionRetries4() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         long start = ClockSource.INSTANCE.currentTime();
         try {
            Connection connection = ds.getConnection();
            connection.close();
            Assert.fail("Should not have been able to get a connection.");
         }
         catch (SQLException e) {
            long elapsed = ClockSource.INSTANCE.elapsedMillis(start);
            Assert.assertTrue("Didn't wait long enough for timeout", (elapsed >= config.getConnectionTimeout()));
         }
      }
   }

   @Test
   public void testConnectionRetries5() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         final Connection connection1 = ds.getConnection();

         long start = ClockSource.INSTANCE.currentTime();

         ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
         scheduler.schedule(new Runnable() {
            @Override
            public void run()
            {
               try {
                  connection1.close();
               }
               catch (Exception e) {
                  e.printStackTrace(System.err);
               }
            }
         }, 250, TimeUnit.MILLISECONDS);

         StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         stubDataSource.setThrowException(new SQLException("Connection refused"));

         try {
            Connection connection2 = ds.getConnection();
            connection2.close();

            long elapsed = ClockSource.INSTANCE.elapsedMillis(start);
            Assert.assertTrue("Waited too long to get a connection.", (elapsed >= 250) && (elapsed < config.getConnectionTimeout()));
         }
         catch (SQLException e) {
            Assert.fail("Should not have timed out.");
         }
         finally {
            scheduler.shutdownNow();
         }
      }
   }

   @Test
   public void testConnectionIdleFill() throws Exception
   {
      StubConnection.slowCreate = false;

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(10);
      config.setConnectionTimeout(2000);
      config.setValidationTimeout(2000);
      config.setConnectionTestQuery("VALUES 2");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "400");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(HikariPool.class, ps);

      try (HikariDataSource ds = new HikariDataSource(config)) {
         TestElf.setSlf4jLogLevel(HikariPool.class, Level.DEBUG);

         HikariPool pool = TestElf.getPool(ds);
         Connection connection1 = ds.getConnection();
         Connection connection2 = ds.getConnection();
         Connection connection3 = ds.getConnection();
         Connection connection4 = ds.getConnection();
         Connection connection5 = ds.getConnection();
         Connection connection6 = ds.getConnection();
         Connection connection7 = ds.getConnection();

         Thread.sleep(1300);

         Assert.assertSame("Total connections not as expected", 10, pool.getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 3, pool.getIdleConnections());

         connection1.close();
         connection2.close();
         connection3.close();
         connection4.close();
         connection5.close();
         connection6.close();
         connection7.close();

         Assert.assertSame("Total connections not as expected", 10, pool.getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 10, pool.getIdleConnections());
      }
   }

   @Before
   public void before()
   {
      TestElf.setSlf4jLogLevel(HikariPool.class, Level.INFO);
   }

   @After
   public void after()
   {
      System.getProperties().remove("com.zaxxer.hikari.housekeeping.periodMs");
   }
}
