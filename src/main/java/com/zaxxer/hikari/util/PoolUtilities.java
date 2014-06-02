package com.zaxxer.hikari.util;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

public final class PoolUtilities
{
   public static final boolean IS_JAVA7;

   static {
      boolean b = false;
      try {
         b = AbstractQueuedLongSynchronizer.class.getMethod("hasQueuedPredecessors", new Class<?>[0]) != null;
      }
      catch (Exception e) {
      }

      IS_JAVA7 = b;
   }

   public static void quietlyCloseConnection(Connection connection)
   {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (SQLException e) {
            return;
         }
      }
   }

   /**
    * Get the elapsed time in millisecond between the specified start time and now.
    *
    * @param start the start time
    * @return the elapsed milliseconds
    */
   public static long elapsedTimeMs(long start)
   {
      return System.currentTimeMillis() - start;
   }

   /**
    * Execute the user-specified init SQL.
    *
    * @param connection the connection to initialize
    * @param sql the SQL to execute
    * @throws SQLException throws if the init SQL execution fails
    */
   public static void executeSqlAutoCommit(Connection connection, String sql) throws SQLException
   {
      if (sql != null) {
         connection.setAutoCommit(true);
         Statement statement = connection.createStatement();
         try {
            statement.execute(sql);
         }
         finally {
            statement.close();
         }
      }
   }

   public static void quietlySleep(long millis)
   {
      try {
         Thread.sleep(millis);
      }
      catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> T createInstance(String className, Class<T> clazz, Object... args)
   {
      if (className == null) {
         return null;
      }

      try {
         Class<?> loaded = PoolUtilities.class.getClassLoader().loadClass(className);

         Class<?>[] argClasses = new Class<?>[args.length];
         for (int i = 0; i < args.length; i++) {
            argClasses[i] = args[i].getClass();
         }

         if (args.length > 0) {
            Constructor<?> constructor = loaded.getConstructor(argClasses);
            return (T) constructor.newInstance(args);
         }

         return (T) loaded.newInstance();
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static ThreadPoolExecutor createThreadPoolExecutor(final int queueSize, final String threadName)
   {
      ThreadFactory threadFactory = new ThreadFactory() {
         public Thread newThread(Runnable r)
         {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
         }
      };

      int processors = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueSize);
      ThreadPoolExecutor executor = new ThreadPoolExecutor(processors, processors, 2, TimeUnit.SECONDS, queue, threadFactory,
                                                           new ThreadPoolExecutor.DiscardPolicy());
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }
}
