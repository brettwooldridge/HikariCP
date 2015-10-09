package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class AddConnectionRaceConditionTest
{
   private HikariPool _hikariPool;

   // @Test
   public void testRaceCondition() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(false);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      // Initialize HikariPool with no initial connections and room to grow
      try (final HikariDataSource ds = new HikariDataSource(config)) {
         _hikariPool = TestElf.getPool(ds);

         ExecutorService threadPool = Executors.newFixedThreadPool(2);
         for (int i = 0; i < 100000; i++) {
            Future<Exception> submit1 = threadPool.submit(new Callable<Exception>() {
               /** {@inheritDoc} */
               @Override
               public Exception call() throws Exception
               {
                  Connection c2;
                  try {
                     c2 = _hikariPool.getConnection(5000);
                     ds.evictConnection(c2);
                  }
                  catch (SQLException e) {
                     return e;
                  }
                  return null;
               }
            });

            Future<Exception> submit2 = threadPool.submit(new Callable<Exception>() {
               /** {@inheritDoc} */
               @Override
               public Exception call() throws Exception
               {
                  Connection c2;
                  try {
                     c2 = _hikariPool.getConnection(5000);
                     ds.evictConnection(c2);
                  }
                  catch (SQLException e) {
                     return e;
                  }
                  return null;
               }
            });

            if (submit1.get() != null) {
               throw submit1.get();
            }
            if (submit2.get() != null) {
               throw submit2.get();
            }
         }
      }
      catch (Exception e) {
         throw e;
      }
   }
}
