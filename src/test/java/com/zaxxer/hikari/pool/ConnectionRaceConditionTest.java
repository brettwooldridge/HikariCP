package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ConcurrentBag;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class ConnectionRaceConditionTest
{
   @Test
   public void testRaceCondition() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(false);
      config.setConnectionTimeout(2500);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      TestElf.setSlf4jLogLevel(ConcurrentBag.class, Level.INFO);

      final AtomicReference<Exception> ref = new AtomicReference<>(null);

      // Initialize HikariPool with no initial connections and room to grow
      try (final HikariDataSource ds = new HikariDataSource(config)) {
         ExecutorService threadPool = Executors.newFixedThreadPool(2);
         for (int i = 0; i < 500_000; i++) {
            threadPool.submit(new Callable<Exception>() {
               /** {@inheritDoc} */
               @Override
               public Exception call() throws Exception
               {
                  if (ref.get() != null) {
                     Connection c2;
                     try {
                        c2 = ds.getConnection();
                        ds.evictConnection(c2);
                     }
                     catch (Exception e) {
                        ref.set(e);
                     }
                  }
                  return null;
               }
            });
         }

         threadPool.shutdown();
         threadPool.awaitTermination(30, TimeUnit.SECONDS);

         if (ref.get() != null) {
            LoggerFactory.getLogger(ConnectionRaceConditionTest.class).error("Task failed", ref.get());
            Assert.fail("Task failed");
         }
      }
      catch (Exception e) {
         throw e;
      }
   }

   @After
   public void after()
   {
      System.getProperties().remove("com.zaxxer.hikari.housekeeping.periodMs");

      TestElf.setSlf4jLogLevel(HikariPool.class, Level.WARN);
      TestElf.setSlf4jLogLevel(ConcurrentBag.class, Level.WARN);
   }
}
