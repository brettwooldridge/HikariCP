/*
 * Copyright (C) 2026 Brett Wooldridge
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ConcurrentBagVirtualThreadTest
{
   @Test
   public void detectsCurrentVirtualThread() throws Exception
   {
      assertFalse(ConcurrentBag.isCurrentThreadVirtual());

      final var ofVirtual = virtualThreadBuilderFactory();
      assumeTrue(ofVirtual != null);

      final var virtualThreadBuilder = ofVirtual.invoke(null);
      final var start = Class.forName("java.lang.Thread$Builder$OfVirtual").getMethod("start", Runnable.class);
      final var detected = new AtomicBoolean();

      final var virtualThread = (Thread) start.invoke(virtualThreadBuilder,
         (Runnable) () -> detected.set(ConcurrentBag.isCurrentThreadVirtual()));
      virtualThread.join();

      assertTrue(detected.get());
   }

   private static Method virtualThreadBuilderFactory()
   {
      try {
         return Thread.class.getMethod("ofVirtual");
      }
      catch (NoSuchMethodException e) {
         return null;
      }
   }
}
