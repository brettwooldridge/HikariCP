/*
 * Copyright (C) 2013 Brett Wooldridge
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Brett Wooldridge
 */
class LeakTask implements Runnable
{
   private static final int CLOSING_NOT_STARTED = 0, CLOSING_STARTED = 1, CLOSING_CANCELLED = 2;

   private final long leakTime;
   private IHikariConnectionProxy connectionOpt;

   private StackTraceElement[] stackTrace;
   private final AtomicInteger closingState = new AtomicInteger(CLOSING_NOT_STARTED);


   public LeakTask(StackTraceElement[] stackTrace, long leakDetectionThreshold, IHikariConnectionProxy connectionOpt)
   {
      this.stackTrace = stackTrace;
      this.leakTime = System.currentTimeMillis() + leakDetectionThreshold;
      this.connectionOpt = connectionOpt;
   }

   /** {@inheritDoc} */
   @Override
   public void run()
   {
      if (System.currentTimeMillis() > leakTime) {
         Logger log = LoggerFactory.getLogger(LeakTask.class);

         Exception e = new Exception();
         e.setStackTrace(stackTrace);
         log.warn("Connection leak detection triggered, stack trace follows", e);
         stackTrace = null;

         if (connectionOpt != null && closingState.compareAndSet(CLOSING_NOT_STARTED, CLOSING_STARTED)) {
            log.warn("Closing leaked connection");
            try {
               connectionOpt.close();
            } catch (SQLException sqle) {
               log.warn(sqle.getMessage(), sqle);
            }
         }
      }
   }

   /**
    * Cancel leak detection task.
    *
    * @return false if leaked connection closing process has already started.
    */
   public boolean cancel()
   {
      if (closingState.compareAndSet(CLOSING_NOT_STARTED, CLOSING_CANCELLED)) {
         stackTrace = null;
         connectionOpt = null;
      }
      return closingState.get() == CLOSING_CANCELLED;
   }
}
