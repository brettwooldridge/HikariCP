package com.zaxxer.hikari.pool;

import java.lang.reflect.Field;

import java.sql.Connection;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.util.ConcurrentBag;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class AddConnectionRaceConditionTest
{
   @Test
   public void testRaceCondition() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailFast(false);
      config.setPoolName("Test Pool");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      // Initialize HikariPool with no initial connections and room to grow

      HikariDataSource ds = new HikariDataSource(config);

      Connection connection = null;

      try {
         Field field = HikariDataSource.class.getDeclaredField("pool");

         field.setAccessible(true);

         _hikariPool = (HikariPool)field.get(ds);

         field = HikariPool.class.getDeclaredField("addConnectionExecutor");

         field.setAccessible(true);

         ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)field.get(_hikariPool);

         // At this point, HikariPool hasn't started adding connections, so this wrapper ThreadFactory will kick in

         threadPoolExecutor.setThreadFactory(new RaceThreadFactory(threadPoolExecutor.getThreadFactory()));

         field = HikariPool.class.getDeclaredField("connectionBag");

         field.setAccessible(true);

         ConcurrentBag<?> concurrentBag = (ConcurrentBag<?>)field.get(_hikariPool);

         field = ConcurrentBag.class.getDeclaredField("sharedList");

         field.setAccessible(true);

         // Get the list of connections in ConcurrentBag directly

         _sharedList = (List<PoolEntry>)field.get(concurrentBag);

         // Attempt to get a connection from the pool

        connection = _hikariPool.getConnection(5000);
      }
      catch (Exception e) {
         throw e;
      }
      finally {
         if (connection != null) {
            connection.close();
         }
      }

	}

   private HikariPool _hikariPool;
   private List<PoolEntry> _sharedList;

   protected class RaceThreadFactory implements ThreadFactory
   {
      public RaceThreadFactory(ThreadFactory threadFactory)
      {
         _threadFactory = threadFactory;
      }

      @Override
      public Thread newThread(Runnable r)
      {
         try {

            /*
             * Add a connection to the pool before the worker thread can begin.
             * This will cause getIdleConnections() <= minimumIdle to fail and not
             * add a new connection, and therefore not call QueuedSequenceSynchronizer.signal()
             */

            _sharedList.add(new PoolEntry(new StubConnection(), _hikariPool));
         }
         catch (Exception e) {
            throw new RuntimeException(e);
         }

         return _threadFactory.newThread(r);
      }
      private final ThreadFactory _threadFactory;
   }
}
