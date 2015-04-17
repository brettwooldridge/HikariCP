/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.PoolBagEntry;

/**
 * This class only supports realtime, not historical metrics.
 *
 * @author Brett Wooldridge
 */
public class MetricsTracker implements AutoCloseable
{
   public static final MetricsContext NO_CONTEXT = new MetricsContext();

   protected final HikariPool pool;

   public MetricsTracker(final HikariPool pool)
   {
      this.pool = pool;
   }

   public MetricsContext recordConnectionRequest(long requestTime)
   {
      return NO_CONTEXT;
   }

   public void recordConnectionUsage(final PoolBagEntry bagEntry)
   {
   }

   @Override
   public void close()
   {
   }

   /**
    * A base instance of a MetricsContext.  Classes extending this class should exhibit the
    * behavior of "starting" a timer upon contruction, and "stopping" the timer when the
    * {@link MetricsContext#stop()} method is called.
    *
    * @author Brett Wooldridge
    */
   public static class MetricsContext
   {
      public void stop()
      {
         // do nothing
      }

      /**
       * Set the lastOpenTime on the provided bag entry.
       *
       * @param bagEntry the bag entry
       * @param now the current timestamp
       */
      public void setConnectionLastOpen(final PoolBagEntry bagEntry, final long now)
      {
         // do nothing
      }
   }
}
