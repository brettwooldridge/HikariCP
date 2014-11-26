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

package com.zaxxer.hikari.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Runnable that is scheduled in the future to report leaks.  The ScheduledFuture is
 * cancelled if the connection is closed before the leak time expires.
 *
 * @author Brett Wooldridge
 */
public class LeakTask implements Runnable
{
   public static final LeakTask NO_LEAK;
   private static final Logger LOGGER = LoggerFactory.getLogger(LeakTask.class);

   private ScheduledExecutorService executorService;
   private long leakDetectionThreshold;
   private ScheduledFuture<?> scheduledFuture;
   private Exception exception;

   static
   {
      NO_LEAK = new LeakTask() {
         @Override
         public void cancel() {};

         @Override
         public LeakTask start()
         {
            return this;
         }
      };
   }

   public LeakTask(final long leakDetectionThreshold, final ScheduledExecutorService executorService)
   {
      this.executorService = executorService;
      this.leakDetectionThreshold = leakDetectionThreshold;
   }

   private LeakTask()
   {
   }
   
   private LeakTask(final LeakTask parent)
   {
      exception = new Exception();
      scheduledFuture = parent.executorService.schedule(this, parent.leakDetectionThreshold, TimeUnit.MILLISECONDS);
   }

   public LeakTask start()
   {
      return new LeakTask(this);
   }

   /** {@inheritDoc} */
   @Override
   public void run()
   {
      final StackTraceElement[] stackTrace = exception.getStackTrace(); 
      final StackTraceElement[] trace = new StackTraceElement[stackTrace.length - 3];
      System.arraycopy(stackTrace, 3, trace, 0, trace.length);

      exception.setStackTrace(trace);
      LOGGER.warn("Connection leak detection triggered, stack trace follows", exception);
   }

   public void cancel()
   {
      scheduledFuture.cancel(false);
   }
}
