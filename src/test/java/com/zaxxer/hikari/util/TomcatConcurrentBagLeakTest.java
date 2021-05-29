/*
 * Copyright (C) 2017 Brett Wooldridge
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

import com.zaxxer.hikari.pool.TestElf.FauxWebClassLoader;
import com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import static com.zaxxer.hikari.pool.TestElf.isJava11;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * @author Brett Wooldridge
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TomcatConcurrentBagLeakTest
{
   @Test
   public void testConcurrentBagForLeaks() throws Exception
   {
      assumeTrue(!isJava11());

      ClassLoader cl = new FauxWebClassLoader();
      Class<?> clazz = cl.loadClass(this.getClass().getName() + "$FauxWebContext");
      Object fauxWebContext = clazz.getDeclaredConstructor().newInstance();

      Method createConcurrentBag = clazz.getDeclaredMethod("createConcurrentBag");
      createConcurrentBag.invoke(fauxWebContext);

      Field failureException = clazz.getDeclaredField("failureException");
      Exception ex = (Exception) failureException.get(fauxWebContext);
      assertNull(ex);
   }

   @Test
   public void testConcurrentBagForLeaks2() throws Exception
   {
      assumeTrue(!isJava11());

      ClassLoader cl = this.getClass().getClassLoader();
      Class<?> clazz = cl.loadClass(this.getClass().getName() + "$FauxWebContext");
      Object fauxWebContext = clazz.getDeclaredConstructor().newInstance();

      Method createConcurrentBag = clazz.getDeclaredMethod("createConcurrentBag");
      createConcurrentBag.invoke(fauxWebContext);

      Field failureException = clazz.getDeclaredField("failureException");
      Exception ex = (Exception) failureException.get(fauxWebContext);
      assertNotNull(ex);
   }

   public static class PoolEntry implements IConcurrentBagEntry
   {
      private int state;

      @Override
      public boolean compareAndSet(int expectState, int newState)
      {
         this.state = newState;
         return true;
      }

      @Override
      public void setState(int newState)
      {
         this.state = newState;
      }

      @Override
      public int getState()
      {
         return state;
      }
   }

   public static class FauxWebContext
   {
      private static final Logger log = LoggerFactory.getLogger(FauxWebContext.class);

      @SuppressWarnings("WeakerAccess")
      public Exception failureException;

      @SuppressWarnings({"ResultOfMethodCallIgnored"})
      public void createConcurrentBag() throws InterruptedException
      {
         try (ConcurrentBag<PoolEntry> bag = new ConcurrentBag<>(x -> CompletableFuture.completedFuture(Boolean.TRUE))) {

            PoolEntry entry = new PoolEntry();
            bag.add(entry);

            PoolEntry borrowed = bag.borrow(100, MILLISECONDS);
            bag.requite(borrowed);

            PoolEntry removed = bag.borrow(100, MILLISECONDS);
            bag.remove(removed);
         }

         checkThreadLocalsForLeaks();
      }

      private void checkThreadLocalsForLeaks()
      {
         Thread[] threads = getThreads();

         try {
            // Make the fields in the Thread class that store ThreadLocals
            // accessible
            Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Field inheritableThreadLocalsField = Thread.class.getDeclaredField("inheritableThreadLocals");
            inheritableThreadLocalsField.setAccessible(true);
            // Make the underlying array of ThreadLoad.ThreadLocalMap.Entry objects
            // accessible
            Class<?> tlmClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = tlmClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Method expungeStaleEntriesMethod = tlmClass.getDeclaredMethod("expungeStaleEntries");
            expungeStaleEntriesMethod.setAccessible(true);

            for (Thread thread : threads) {
               Object threadLocalMap;
               if (thread != null) {

                  // Clear the first map
                  threadLocalMap = threadLocalsField.get(thread);
                  if (null != threadLocalMap) {
                     expungeStaleEntriesMethod.invoke(threadLocalMap);
                     checkThreadLocalMapForLeaks(threadLocalMap, tableField);
                  }

                  // Clear the second map
                  threadLocalMap = inheritableThreadLocalsField.get(thread);
                  if (null != threadLocalMap) {
                     expungeStaleEntriesMethod.invoke(threadLocalMap);
                     checkThreadLocalMapForLeaks(threadLocalMap, tableField);
                  }
               }
            }
         }
         catch (Throwable t) {
            log.warn("Failed to check for ThreadLocal references for web application [{}]", getContextName(), t);
            failureException = new Exception();
         }
      }

      private Object getContextName()
      {
         return this.getClass().getName();
      }

      // THE FOLLOWING CODE COPIED FROM APACHE TOMCAT (2017/01/08)

      /**
      * Analyzes the given thread local map object. Also pass in the field that
      * points to the internal table to save re-calculating it on every
      * call to this method.
      */
      private void checkThreadLocalMapForLeaks(Object map, Field internalTableField) throws IllegalAccessException, NoSuchFieldException
      {
         if (map != null) {
            Object[] table = (Object[]) internalTableField.get(map);
            if (table != null) {
               for (Object obj : table) {
                  if (obj != null) {
                     boolean keyLoadedByWebapp = false;
                     boolean valueLoadedByWebapp = false;
                     // Check the key
                     Object key = ((Reference<?>) obj).get();
                     if (this.equals(key) || loadedByThisOrChild(key)) {
                        keyLoadedByWebapp = true;
                     }
                     // Check the value
                     Field valueField = obj.getClass().getDeclaredField("value");
                     valueField.setAccessible(true);
                     Object value = valueField.get(obj);
                     if (this.equals(value) || loadedByThisOrChild(value)) {
                        valueLoadedByWebapp = true;
                     }
                     if (keyLoadedByWebapp || valueLoadedByWebapp) {
                        Object[] args = new Object[5];
                        args[0] = getContextName();
                        if (key != null) {
                           args[1] = getPrettyClassName(key.getClass());
                           try {
                              args[2] = key.toString();
                           } catch (Exception e) {
                              log.warn("Unable to determine string representation of key of type [{}]", args[1], e);
                              args[2] = "Unknown";
                           }
                        }
                        if (value != null) {
                           args[3] = getPrettyClassName(value.getClass());
                           try {
                              args[4] = value.toString();
                           } catch (Exception e) {
                              log.warn("webappClassLoader.checkThreadLocalsForLeaks.badValue {}", args[3], e);
                              args[4] = "Unknown";
                           }
                        }

                        if (valueLoadedByWebapp) {
                           log.error("The web application [{}] created a ThreadLocal with key of type [{}] " +
                              "(value [{}]) and a value of type [{}] (value [{}]) but failed to remove " +
                              "it when the web application was stopped. Threads are going to be renewed " +
                              "over time to try and avoid a probable memory leak.", args);
                           failureException = new Exception();
                        } else if (value == null) {
                           log.debug("The web application [{}] created a ThreadLocal with key of type [{}] " +
                              "(value [{}]). The ThreadLocal has been correctly set to null and the " +
                              "key will be removed by GC.", args);
                           failureException = new Exception();
                        } else {
                           log.debug("The web application [{}] created a ThreadLocal with key of type [{}] " +
                              "(value [{}]) and a value of type [{}] (value [{}]). Since keys are only " +
                              "weakly held by the ThreadLocal Map this is not a memory leak.", args);
                           failureException = new Exception();
                        }
                     }
                  }
               }
            }
         }
      }

      /**
       * @param o object to test, may be null
       * @return <code>true</code> if o has been loaded by the current classloader
       * or one of its descendants.
       */
      private boolean loadedByThisOrChild(Object o) {
         if (o == null) {
            return false;
         }

         Class<?> clazz;
         if (o instanceof Class) {
            clazz = (Class<?>) o;
         } else {
            clazz = o.getClass();
         }

         ClassLoader cl = clazz.getClassLoader();
         while (cl != null) {
            if (cl == this.getClass().getClassLoader()) {
               return true;
            }
            cl = cl.getParent();
         }

         if (o instanceof Collection<?>) {
            Iterator<?> iter = ((Collection<?>) o).iterator();
            try {
               while (iter.hasNext()) {
                  Object entry = iter.next();
                  if (loadedByThisOrChild(entry)) {
                     return true;
                  }
               }
            } catch (ConcurrentModificationException e) {
               log.warn("Failed to check for ThreadLocal references for web application [{}]", getContextName(), e);
            }
         }
         return false;
      }

      /*
      * Get the set of current threads as an array.
      */
      private Thread[] getThreads()
      {
         // Get the current thread group
         ThreadGroup tg = Thread.currentThread().getThreadGroup();
         // Find the root thread group
         try {
            while (tg.getParent() != null) {
               tg = tg.getParent();
            }
         }
         catch (SecurityException se) {
            log.warn("Unable to obtain the parent for ThreadGroup [{}]. It will not be possible to check all threads for potential memory leaks", tg.getName(), se);
         }

         int threadCountGuess = tg.activeCount() + 50;
         Thread[] threads = new Thread[threadCountGuess];
         int threadCountActual = tg.enumerate(threads);
         // Make sure we don't miss any threads
         while (threadCountActual == threadCountGuess) {
            threadCountGuess *= 2;
            threads = new Thread[threadCountGuess];
            // Note tg.enumerate(Thread[]) silently ignores any threads that
            // can't fit into the array
            threadCountActual = tg.enumerate(threads);
         }

         return threads;
      }

      private String getPrettyClassName(Class<?> clazz)
      {
         String name = clazz.getCanonicalName();
         if (name == null) {
            name = clazz.getName();
         }
         return name;
      }
   }
}
