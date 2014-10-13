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
   private static final Logger LOGGER = LoggerFactory.getLogger(LeakTask.class);

   private final ScheduledFuture<?> scheduledFuture;
   private StackTraceElement[] stackTrace;

   public LeakTask()
   {
      scheduledFuture = null;
   }

   public LeakTask(final long leakDetectionThreshold, final ScheduledExecutorService executorService)
   {
      this.stackTrace = new Exception().getStackTrace();
      
      scheduledFuture = executorService.schedule(this, leakDetectionThreshold, TimeUnit.MILLISECONDS);
   }

   /** {@inheritDoc} */
   @Override
   public void run()
   {
      StackTraceElement[] trace = new StackTraceElement[stackTrace.length - 3];
      System.arraycopy(stackTrace, 3, trace, 0, trace.length);

      LeakException e = new LeakException(trace);
      LOGGER.warn("Connection leak detection triggered, stack trace follows", e);
      stackTrace = null;
   }

   public void cancel()
   {
      stackTrace = null;
      if (scheduledFuture != null) {
         scheduledFuture.cancel(false);
      }
   }

   private static class LeakException extends Exception
   {
      private static final long serialVersionUID = -2021997004669670337L;

      /**
       * No-op constructor to avoid the call to fillInStackTrace() 
       */
      public LeakException(final StackTraceElement[] stackTrace)
      {
         super("Connection Leak", null, true, true);
         this.setStackTrace(stackTrace);
      }
   }
}
