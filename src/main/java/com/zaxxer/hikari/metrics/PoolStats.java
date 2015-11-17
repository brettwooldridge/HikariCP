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

package com.zaxxer.hikari.metrics;

import java.util.concurrent.atomic.AtomicLong;

import com.zaxxer.hikari.util.ClockSource;

/**
 *
 * @author Brett Wooldridge
 */
public abstract class PoolStats
{
   private final ClockSource clock;
   private final AtomicLong reloadAt;
   private final long timeoutMs;

   protected volatile int totalConnections;
   protected volatile int idleConnections;
   protected volatile int activeConnections;
   protected volatile int pendingThreads;

   public PoolStats(final long timeoutMs)
   {
      this.timeoutMs = timeoutMs;
      this.reloadAt = new AtomicLong();
      this.clock = ClockSource.INSTANCE;
   }
   
   public int getTotalConnections()
   {
      if (shouldLoad()) {
         update();
      }

      return totalConnections;
   }

   public int getIdleConnections()
   {
      if (shouldLoad()) {
         update();
      }

      return idleConnections;
   }

   public int getActiveConnections()
   {
      if (shouldLoad()) {
         update();
      }

      return activeConnections;
   }

   public int getPendingThreads()
   {
      if (shouldLoad()) {
         update();
      }

      return pendingThreads;
   }

   protected abstract void update();

   private boolean shouldLoad()
   {
      for (; ; ) {
          final long now = clock.currentTime();
          final long reloadTime = reloadAt.get();
          if (reloadTime > now) {
              return false;
          }
          else if (reloadAt.compareAndSet(reloadTime, clock.plusMillis(now, timeoutMs))) {
              return true;
          }
      }
  }
}
