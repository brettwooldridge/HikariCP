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

package com.zaxxer.hikari.pool;

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
   private String connectionName;
   private Exception exception;

   static
   {
      NO_LEAK = new LeakTask() {
         @Override
         public void cancel() {}

         @Override
         public LeakTask start(final PoolBagEntry bagEntry)
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
   
   private LeakTask(final LeakTask parent, final PoolBagEntry bagEntry)
   {
      this.exception = new Exception("Apparent connection leak detected");
      this.connectionName = bagEntry.connection.toString();
      scheduledFuture = parent.executorService.schedule(this, parent.leakDetectionThreshold, TimeUnit.MILLISECONDS);
   }

   public LeakTask start(final PoolBagEntry bagEntry)
   {
      return new LeakTask(this, bagEntry);
   }

   /** {@inheritDoc} */
   @Override
   public void run()
   {
      final StackTraceElement[] stackTrace = exception.getStackTrace(); 
      final StackTraceElement[] trace = new StackTraceElement[stackTrace.length - 5];
      System.arraycopy(stackTrace, 5, trace, 0, trace.length);

      exception.setStackTrace(trace);
      LOGGER.warn("Connection leak detection triggered for connection {}, stack trace follows", connectionName, exception);
   }

   public void cancel()
   {
      scheduledFuture.cancel(false);
   }
}
