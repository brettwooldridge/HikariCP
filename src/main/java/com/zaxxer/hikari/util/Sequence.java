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
@SuppressWarnings("serial")
public interface Sequence
{
   /**
    * Increments the current sequence by one.
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
   final class Factory
   {
      public static Sequence create()
      {
         if (!Boolean.getBoolean("com.zaxxer.hikari.useAtomicLongSequence")) {
            return new Java8Sequence();
         }
         else {
            return new Java7Sequence();
         }
      }
   }

   final class Java7Sequence extends AtomicLong implements Sequence {
      @Override
      public void increment() {
         this.incrementAndGet();
      }
   }

   final class Java8Sequence extends LongAdder implements Sequence {
      @Override
      public long get() {
         return this.sum();
      }
   }
}
