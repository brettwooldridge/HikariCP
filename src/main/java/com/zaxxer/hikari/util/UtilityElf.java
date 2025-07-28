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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * UtilityElf is a utility class that provides various helper methods
 * for string manipulation, thread management, and JDBC URL handling.
 * It includes methods for masking passwords in JDBC URLs, creating
 * instances of classes, and creating thread pool executors.
 *
 * @author Brett Wooldridge
 * @hidden
 */
public final class UtilityElf
{
   private static final Logger LOGGER = LoggerFactory.getLogger(UtilityElf.class);

   /**
    * A pattern to match and mask passwords in JDBC URLs.
    * It looks for the "password" parameter in the URL and replaces its value with "<masked>".
    */
   private static final Pattern PASSWORD_MASKING_PATTERN = Pattern.compile("([?&;][^&#;=]*[pP]assword=)[^&#;]*");

   private UtilityElf()
   {
      // non-constructable
   }

   public static String maskPasswordInJdbcUrl(String jdbcUrl)
   {
      return PASSWORD_MASKING_PATTERN.matcher(jdbcUrl).replaceAll("$1<masked>");
   }

   /**
    * Get a trimmed string or null if the string is null or empty.
    *
    * @param text the string to check
    * @return null if string is null or empty, trimmed string otherwise
   */
   public static String getNullIfEmpty(final String text)
   {
      return text == null ? null : text.trim().isEmpty() ? null : text.trim();
   }

   /**
    * Sleep and suppress InterruptedException (but re-signal it).
    *
    * @param millis the number of milliseconds to sleep
    */
   public static void quietlySleep(final long millis)
   {
      try {
         Thread.sleep(millis);
      }
      catch (InterruptedException e) {
         // I said be quiet!
         currentThread().interrupt();
      }
   }

   /**
    * Checks whether an object is an instance of given type without throwing exception when the class is not loaded.
    * @param obj the object to check
    * @param className String class
    * @return true if object is assignable from the type, false otherwise or when the class cannot be loaded
    */
   public static boolean safeIsAssignableFrom(Object obj, String className) {
      try {
         var clazz = Class.forName(className);
         return clazz.isAssignableFrom(obj.getClass());
      } catch (ClassNotFoundException ignored) {
         return false;
      }
   }

   public static <T> T createInstance(final String className, final Class<T> clazz)
   {
      return createInstance(className, clazz, new Object[0]);
   }

   /**
    * Create and instance of the specified class using the constructor matching the specified
    * arguments.
    *
    * @param <T> the class type
    * @param className the name of the class to instantiate
    * @param clazz a class to cast the result as
    * @param args arguments to a constructor
    * @return an instance of the specified class
    */
   public static <T> T createInstance(final String className, final Class<T> clazz, final Object... args)
   {
      if (className == null) {
         return null;
      }

      try {
         var loaded = attemptFromContextLoader(className);
         if (loaded == null) {
            loaded = UtilityElf.class.getClassLoader().loadClass(className);
            LOGGER.debug("Class {} loaded from classloader {}", className, UtilityElf.class.getClassLoader());
         }
         var totalArgs = args.length;

         if (totalArgs == 0) {
            return clazz.cast(loaded.getDeclaredConstructor().newInstance());
         }

         var argClasses = new Class<?>[totalArgs];
         for (int i = 0; i < totalArgs; i++) {
            argClasses[i] = args[i].getClass();
         }

         Constructor<?> constructor = Arrays.stream(loaded.getConstructors())
            .filter(c -> {
               if (c.getParameterCount() != totalArgs) return false;

               Class<?>[] params = c.getParameterTypes();
               return IntStream.range(0, totalArgs)
                  .allMatch(i -> params[i].isAssignableFrom(argClasses[i]));
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No suitable constructor found for class " + className + " with arguments " + Arrays.toString(args)));

         return clazz.cast(constructor.newInstance(args));
      }
      catch (Exception e) {
         throw new RuntimeException("Failed to load class " + className, e);
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
      return createThreadPoolExecutor(new LinkedBlockingQueue<>(queueSize), threadName, threadFactory, policy);
   }

   /**
    * Create a ThreadPoolExecutor.
    *
    * @param queue the BlockingQueue to use
    * @param threadName the thread name
    * @param threadFactory an optional ThreadFactory
    * @param policy the RejectedExecutionHandler policy
    * @return a ThreadPoolExecutor
    */
   public static ThreadPoolExecutor createThreadPoolExecutor(final BlockingQueue<Runnable> queue, final String threadName, ThreadFactory threadFactory, final RejectedExecutionHandler policy)
   {
      if (threadFactory == null) {
         threadFactory = new DefaultThreadFactory(threadName);
      }

      var executor = new ThreadPoolExecutor(1 /*core*/, 1 /*max*/, 5 /*keepalive*/, SECONDS, queue, threadFactory, policy);
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }

   // ***********************************************************************
   //                       Misc. public methods
   // ***********************************************************************

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
            // use the english locale to avoid the infamous turkish locale bug
            final var upperCaseIsolationLevelName = transactionIsolationName.toUpperCase(Locale.ENGLISH);
            return IsolationLevel.valueOf(upperCaseIsolationLevelName).getLevelId();
         } catch (IllegalArgumentException e) {
            // legacy support for passing an integer version of the isolation level
            try {
               final var level = Integer.parseInt(transactionIsolationName);
               for (var iso : IsolationLevel.values()) {
                  if (iso.getLevelId() == level) {
                     return iso.getLevelId();
                  }
               }

               throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
            }
            catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName, nfe);
            }
         }
      }

      return -1;
   }

   /**
    * Custom RejectedExecutionHandler that does nothing when a task is rejected.
    *
    * @see java.util.concurrent.RejectedExecutionHandler
    * @see java.util.concurrent.ThreadPoolExecutor
    * @hidden
    */
   public static class CustomDiscardPolicy implements RejectedExecutionHandler
   {
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      }
   }

   /**
    * Default ThreadFactory implementation that creates daemon threads with a specified name.
    *
    * @see java.util.concurrent.ThreadFactory
    * @hidden
    */
   public static final class DefaultThreadFactory implements ThreadFactory
   {
      private final String threadName;
      private final boolean daemon;

      public DefaultThreadFactory(String threadName) {
         this.threadName = threadName;
         this.daemon = true;
      }

      @Override
      @SuppressWarnings("NullableProblems")
      public Thread newThread(Runnable r) {
         var thread = new Thread(r, threadName);
         thread.setDaemon(daemon);
         return thread;
      }
   }

   // ***********************************************************************
   //                          Private methods
   // ***********************************************************************

   private static Class<?> attemptFromContextLoader(final String className) {
      final var threadContextClassLoader = Thread.currentThread().getContextClassLoader();
      if (threadContextClassLoader != null) {
         try {
            final var clazz = threadContextClassLoader.loadClass(className);
            LOGGER.debug("Class {} found in Thread context class loader {}", className, threadContextClassLoader);
            return clazz;
         } catch (ClassNotFoundException e) {
            LOGGER.debug("Class {} not found in Thread context class loader {}, trying classloader {}",
               className, threadContextClassLoader, UtilityElf.class.getClassLoader());
         }
      }

      return null;
   }
}
