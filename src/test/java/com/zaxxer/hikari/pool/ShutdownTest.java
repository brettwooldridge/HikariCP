/*
 * Copyright (C) 2014 Brett Wooldridge
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

import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.setSlf4jLogLevel;
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedMillis;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.mocks.StubDataSource;
import com.zaxxer.hikari.util.UtilityElf;

/**
 * @author Brett Wooldridge
 */
public class ShutdownTest
{
   @Before
   public void beforeTest()
   {
      setSlf4jLogLevel(PoolBase.class, Level.DEBUG);
      setSlf4jLogLevel(HikariPool.class, Level.DEBUG);
      StubConnection.count.set(0);
   }

   @After
   public void afterTest()
   {
      setSlf4jLogLevel(PoolBase.class, Level.WARN);
      setSlf4jLogLevel(HikariPool.class, Level.WARN);
      StubConnection.slowCreate = false;
   }

   @Test
   public void testShutdown1() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = true;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         Thread[] threads = new Thread[10];
         for (int i = 0; i < 10; i++) {
            threads[i] = new Thread() {
               @Override
               public void run()
               {
                  try {
                     if (ds.getConnection() != null) {
                        quietlySleep(SECONDS.toMillis(1));
                     }
                  }
                  catch (SQLException e) {
                  }
               }
            };
            threads[i].setDaemon(true);
         }
         for (int i = 0; i < 10; i++) {
            threads[i].start();
         }

         quietlySleep(1800L);

         assertTrue("Total connection count not as expected, ", pool.getTotalConnections() > 0);

         ds.close();

         assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
         assertTrue(ds.isClosed());
      }
   }

   @Test
   public void testShutdown2() throws SQLException
   {
      assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = true;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(10);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         quietlySleep(1200L);

         assertTrue("Total connection count not as expected, ", pool.getTotalConnections() > 0);

         ds.close();

         assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
         assertTrue(ds.toString().startsWith("HikariDataSource (") && ds.toString().endsWith(")"));
      }
   }

   @Test
   public void testShutdown3() throws SQLException
   {
      assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      StubConnection.slowCreate = false;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         quietlySleep(1200L);

         assertTrue("Total connection count not as expected, ", pool.getTotalConnections() == 5);

         ds.close();

         assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
      }
   }

   @Test
   public void testShutdown4() throws SQLException
   {
      StubConnection.slowCreate = true;

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(10);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         quietlySleep(500L);

         ds.close();

         long startTime = currentTime();
         while (elapsedMillis(startTime) < SECONDS.toMillis(5) && threadCount() > 0) {
            quietlySleep(250);
         }

         assertSame("Unreleased connections after shutdown", 0, getPool(ds).getTotalConnections());
      }
   }

   @Test
   public void testShutdown5() throws SQLException
   {
      Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariPool pool = getPool(ds);

         Connection[] connections = new Connection[5];
         for (int i = 0; i < 5; i++) {
            connections[i] = ds.getConnection();
         }

         Assert.assertTrue("Total connection count not as expected, ", pool.getTotalConnections() == 5);

         ds.close();

         Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
         Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
         Assert.assertSame("Total connection count not as expected, ", 0, pool.getTotalConnections());
      }
   }

   @Test
   public void testShutdownWithInterruptedCaller() throws Exception
   {
      checkInterruptedShutdown(false, true);
   }

   @Test
   public void testShutdownWithInterruptedCallerAndPendingConnection() throws Exception
   {
      checkInterruptedShutdown(true, true);
   }

   @Test
   public void testNewInterruptDuringShutdownIsPreserved() throws Exception
   {
      checkInterruptedShutdown(true, false);
   }

   private void checkInterruptedShutdown(boolean pendingConnection, boolean interruptedAtEntry) throws Exception
   {
      final var creationEntered = new CountDownLatch(1);
      final var releaseCreation = new CountDownLatch(1);
      final var connectionClosed = new CountDownLatch(1);
      final var acquisitionCount = new AtomicInteger();
      final List<StubConnection> physicalConnections = new CopyOnWriteArrayList<>();
      final List<Thread> poolThreads = new CopyOnWriteArrayList<>();
      final var config = newHikariConfig();
      config.setMinimumIdle(pendingConnection ? 2 : 1);
      config.setMaximumPoolSize(pendingConnection ? 2 : 1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSource(new StubDataSource() {
         @Override
         public Connection getConnection() throws SQLException
         {
            if (pendingConnection && acquisitionCount.incrementAndGet() == 2) {
               creationEntered.countDown();
               try {
                  if (!releaseCreation.await(5, SECONDS)) {
                     throw new SQLException("Timed out waiting to release connection creation");
                  }
               }
               catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  throw new SQLException(e);
               }
            }
            final var connection = new StubConnection() {
               private volatile boolean closed;

               @Override
               public void close()
               {
                  closed = true;
                  connectionClosed.countDown();
               }

               @Override
               public boolean isClosed()
               {
                  return closed;
               }
            };
            physicalConnections.add(connection);
            return connection;
         }
      });
      config.setThreadFactory(task -> {
         final var thread = new Thread(task, "interrupted-shutdown-" + poolThreads.size());
         thread.setDaemon(true);
         poolThreads.add(thread);
         return thread;
      });

      try (var ds = new HikariDataSource(config)) {
         final var pool = getPool(ds);
         final var interruptPreserved = new AtomicBoolean();
         final var failure = new AtomicReference<Throwable>();
         final var closeStarted = new CountDownLatch(1);
         final var closer = new Thread(() -> {
            try {
               if (interruptedAtEntry) {
                  Thread.currentThread().interrupt();
               }
               closeStarted.countDown();
               ds.close();
               interruptPreserved.set(Thread.currentThread().isInterrupted());
            }
            catch (Throwable e) {
               failure.set(e);
            }
         });
         try {
            if (pendingConnection) {
               assertTrue("Connection creation did not start", creationEntered.await(5, SECONDS));
            }
            closer.start();
            assertTrue("Close caller did not start", closeStarted.await(5, SECONDS));
            if (interruptedAtEntry) {
               closer.join(100);
            }
            else {
               assertTrue("Shutdown did not reach connection eviction", connectionClosed.await(5, SECONDS));
               closer.interrupt();
            }
            releaseCreation.countDown();
            closer.join(5000);
            Assert.assertFalse("Close caller did not finish", closer.isAlive());
            Assert.assertNull(failure.get());
            assertTrue("Caller interrupt was cleared", interruptPreserved.get());
            if (!interruptedAtEntry) {
               return;
            }
            for (Thread thread : poolThreads) {
               thread.join(1000);
               Assert.assertFalse("Pool thread survived close: " + thread.getName(), thread.isAlive());
            }
            for (Connection connection : physicalConnections) {
               assertTrue("Physical connection survived close", connection.isClosed());
            }
            assertSame("Connections remained in the pool", 0, pool.getTotalConnections());
         }
         finally {
            releaseCreation.countDown();
            closer.join(5000);
            pool.shutdown();
         }
      }
   }

   @Test
   public void testAfterShutdown() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(5);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         ds.close();
         try {
            ds.getConnection();
         }
         catch (SQLException e) {
            Assert.assertTrue(e.getMessage().contains("has been closed."));
         }
      }
   }

   @Test
   public void testShutdownDuringInit() throws Exception
   {
      final HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         StubConnection.slowCreate = true;
         UtilityElf.quietlySleep(3000L);
      }
   }

   @Test
   public void testThreadedShutdown() throws Exception
   {
      final HikariConfig config = newHikariConfig();
      config.setMinimumIdle(5);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(1000);
      config.setValidationTimeout(1000);
      config.setInitializationFailTimeout(0);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      for (int i = 0; i < 4; i++) {
         try (final HikariDataSource ds = new HikariDataSource(config)) {
            Thread t = new Thread() {
               @Override
               public void run()
               {
                  try (Connection connection = ds.getConnection()) {
                     for (int i = 0; i < 10; i++) {
                        Connection connection2 = null;
                        try {
                           connection2 = ds.getConnection();
                           PreparedStatement stmt = connection2.prepareStatement("SOMETHING");
                           UtilityElf.quietlySleep(20);
                           stmt.getMaxFieldSize();
                        }
                        catch (SQLException e) {
                           try {
                              if (connection2 != null) {
                                 connection2.close();
                              }
                           }
                           catch (SQLException e2) {
                              if (e2.getMessage().contains("shutdown") || e2.getMessage().contains("evicted")) {
                                 break;
                              }
                           }
                        }
                     }
                  }
                  catch (Exception e) {
                     Assert.fail(e.getMessage());
                  }
                  finally {
                     ds.close();
                  }
               }
            };
            t.start();

            Thread t2 = new Thread() {
               @Override
               public void run()
               {
                  UtilityElf.quietlySleep(100);
                  try {
                     ds.close();
                  }
                  catch (IllegalStateException e) {
                     Assert.fail(e.getMessage());
                  }
               }
            };
            t2.start();

            t.join();
            t2.join();

            ds.close();
         }
      }
   }

   private int threadCount()
   {
      Thread[] threads = new Thread[Thread.activeCount() * 2];
      Thread.enumerate(threads);

      int count = 0;
      for (Thread thread : threads) {
         count += (thread != null && thread.getName().startsWith("Hikari")) ? 1 : 0;
      }

      return count;
   }
}
