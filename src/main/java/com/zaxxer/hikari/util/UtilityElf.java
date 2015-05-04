/*
 * Copyright (C) 2013 Brett Wooldridge
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

package com.zaxxer.hikari.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Brett Wooldridge
 */
public final class UtilityElf
{
   /**
    * Get the elapsed time in nanoseconds between the specified start time and now.
    *
    * @param startNanos the start time
    * @return the elapsed milliseconds
    */
   public static long elapsedNanos(final long startNanos)
   {
      return System.nanoTime() - startNanos;
   }

   /**
    * Get the elapsed time in millisecond between the specified start time and now.
    *
    * @param startNano the start time
    * @return the elapsed milliseconds
    */
   public static long elapsedTimeMs(final long startNano)
   {
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
   }

   /**
    * Sleep and transform an InterruptedException into a RuntimeException.
    *
    * @param millis the number of milliseconds to sleep
    */
   public static void quietlySleep(final long ticks, final TimeUnit unit)
   {
      try {
         unit.sleep(ticks);
      }
      catch (InterruptedException e) {
         // I said be quiet!
      }
   }

   /**
    * Sleep and transform an InterruptedException into a RuntimeException.
    *
    * @param millis the number of milliseconds to sleep
    */
   public static void quietlySleepMs(final long millis)
   {
      quietlySleep(millis, TimeUnit.MILLISECONDS);
   }

   /**
    * Create and instance of the specified class using the constructor matching the specified
    * arguments.
    *
    * @param <T> the class type
    * @param className the name of the classto instantiate
    * @param clazz a class to cast the result as
    * @param args arguments to a constructor
    * @return an instance of the specified class
    */
   @SuppressWarnings("unchecked")
   public static <T> T createInstance(final String className, final Class<T> clazz, final Object... args)
   {
      if (className == null) {
         return null;
      }

      try {
         Class<?> loaded = UtilityElf.class.getClassLoader().loadClass(className);

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

   /**
    * Create a ThreadPoolExecutor.
    *
    * @param queueSize the queue size
    * @param threadName the thread name
    * @param threadFactory an optional ThreadFactory
    * @param policy the RejectedExecutionHandler policy
    * @return a ThreadPoolExecutor
    */
   public static ThreadPoolExecutor createThreadPoolExecutor(final int queueSize, final String threadName, ThreadFactory threadFactory, final RejectedExecutionHandler policy)
   {
      if (threadFactory == null) {
         threadFactory = new DefaultThreadFactory(threadName, true);
      }

      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueSize);
      ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, queue, threadFactory, policy);
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }

   /**
    * Get the int value of a transaction isolation level by name.
    *
    * @param transactionIsolationName the name of the transaction isolation level
    * @return the int value of the isolation level or -1
    */
   public static int getTransactionIsolation(final String transactionIsolationName)
   {
      if (transactionIsolationName != null) {
         try {
            Field field = Connection.class.getField(transactionIsolationName);
            return field.getInt(null);
         }
         catch (Exception e) {
            throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
         }
      }

      return -1;
   }
}
