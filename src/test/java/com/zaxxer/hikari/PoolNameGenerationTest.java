/*
 * Copyright (C) 2024 Brett Wooldridge
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

package com.zaxxer.hikari;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards the auto-generated pool-name counter (see #2104): generation no longer holds the
 * {@code System.getProperties()} monitor, but the read-increment-write of the VM-global
 * counter must stay atomic so that concurrently generated pool numbers never overlap.
 */
public class PoolNameGenerationTest
{
   private static Method generatePoolName() throws Exception
   {
      Method m = HikariConfig.class.getDeclaredMethod("generatePoolName");
      m.setAccessible(true);
      return m;
   }

   @Test
   public void testGeneratedPoolNamesAreUniqueUnderConcurrency() throws Exception
   {
      final Method generate = generatePoolName();
      final int threads = 16;
      final int perThread = 250;
      final int total = threads * perThread;

      final ConcurrentLinkedQueue<String> names = new ConcurrentLinkedQueue<>();
      final CountDownLatch start = new CountDownLatch(1);
      final CountDownLatch done = new CountDownLatch(threads);
      final Thread[] workers = new Thread[threads];

      for (int t = 0; t < threads; t++) {
         workers[t] = new Thread(() -> {
            try {
               final HikariConfig config = new HikariConfig();
               start.await();
               for (int i = 0; i < perThread; i++) {
                  names.add((String) generate.invoke(config));
               }
            }
            catch (Exception e) {
               throw new RuntimeException(e);
            }
            finally {
               done.countDown();
            }
         });
         workers[t].start();
      }

      start.countDown();
      assertTrue("pool name generation did not complete in time", done.await(30, TimeUnit.SECONDS));

      assertEquals(total, names.size());

      // No two concurrently generated names may collide: a lost update on the shared counter
      // would produce a duplicate.
      final Set<String> unique = new HashSet<>(names);
      assertEquals("generated pool names must be unique (no lost counter updates)", total, unique.size());

      // Every name must follow the documented "HikariPool-<n>" shape.
      for (String name : names) {
         assertTrue("unexpected pool name: " + name, name.startsWith("HikariPool-"));
      }
   }

   @Test
   public void testGenerationDoesNotDeadlockWhilePropertiesMonitorIsContended() throws Exception
   {
      // A regression guard for the class of deadlock in #2104: other threads constantly hold the
      // System.getProperties() monitor (as the JVM and third-party code routinely do). Generation
      // must still make progress rather than wedging behind that foreign monitor.
      final Method generate = generatePoolName();
      final HikariConfig config = new HikariConfig();

      final CountDownLatch stop = new CountDownLatch(1);
      final Thread contender = new Thread(() -> {
         while (stop.getCount() > 0) {
            synchronized (System.getProperties()) {
               System.getProperties().getProperty("java.version");
            }
         }
      });
      contender.setDaemon(true);
      contender.start();

      try {
         final Set<String> names = Collections.synchronizedSet(new HashSet<>());
         for (int i = 0; i < 500; i++) {
            names.add((String) generate.invoke(config));
         }
         assertEquals("generated pool names must be unique", 500, names.size());
      }
      finally {
         stop.countDown();
      }
   }
}
