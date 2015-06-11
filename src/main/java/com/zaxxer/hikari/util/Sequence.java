/*
 * Copyright (C) 2015 Brett Wooldridge
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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * A monotonically increasing long sequence.
 *
 * @author brettw
 */
public interface Sequence
{
   /**
    * Adds the given value to the current sequence.  If delta is negative,
    * the Sequential Consistency of this Sequence cannot be guarenteed.
    *
    * @param delta the value to add
    */
   void increment();

   /**
    * Get the current sequence.
    *
    * @return the current sequence.
    */
   long get();

   /**
    * Factory class used to create a platform-specific ClockSource. 
    */
   class Factory
   {
      @SuppressWarnings("serial")
      public static Sequence create()
      {
         class Java7Sequence extends AtomicLong implements Sequence {
            Java7Sequence() {
            }

            public void increment() {
               this.incrementAndGet();
            }
         }

         class Java8Sequence extends LongAdder implements Sequence {
            public Java8Sequence() {
            }

            @Override
            public long get() {
               return this.sum();
            }
         }

         Sequence sequence = null;
         try {
            if (Sequence.class.getClassLoader().loadClass("java.util.concurrent.atomic.LongAdder") != null) {
               sequence = new Java8Sequence();
            }
         }
         catch (ClassNotFoundException e) {
         }

         return sequence != null ? sequence : new Java7Sequence();
      }
   }
}
