/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

package com.zaxxer.hikari.proxy;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

/**
 * A Runnable that is scheduled in the future to report leaks.  The ScheduledFuture is
 * cancelled if the connection is closed before the leak time expires.
 *
 * @author Brett Wooldridge
 */
public class LeakTask implements Runnable
{
   private final long leakTime;
   private final ScheduledFuture<?> scheduledFuture;
   private StackTraceElement[] stackTrace;

   public LeakTask()
   {
      leakTime = 0;
      scheduledFuture = null;
   }

   public LeakTask(final long leakDetectionThreshold, final ScheduledExecutorService executorService)
   {
      this.stackTrace = Thread.currentThread().getStackTrace();
      
      this.leakTime = System.currentTimeMillis() + leakDetectionThreshold;
      scheduledFuture = executorService.schedule(this, leakDetectionThreshold, TimeUnit.MILLISECONDS);
   }

   /** {@inheritDoc} */
   @Override
   public void run()
   {
      if (System.currentTimeMillis() > leakTime) {
         StackTraceElement[] trace = new StackTraceElement[stackTrace.length - 3];
         System.arraycopy(stackTrace, 4, trace, 0, trace.length);

         Exception e = new Exception();
         e.setStackTrace(trace);
         LoggerFactory.getLogger(LeakTask.class).warn("Connection leak detection triggered, stack trace follows", e);
         stackTrace = null;
      }
   }

   public void cancel()
   {
      stackTrace = null;
      if (scheduledFuture != null) {
         scheduledFuture.cancel(false);
      }
   }
}
