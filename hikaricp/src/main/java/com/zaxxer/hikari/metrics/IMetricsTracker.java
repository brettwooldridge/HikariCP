/*
 * Copyright (C) 2014 Brett Wooldridge
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

import com.zaxxer.hikari.pool.PoolBagEntry;

/**
 *
 * @author Brett Wooldridge
 */
public interface IMetricsTracker
{
   /**
    * This method is called when a connection request starts.  The {@link MetricsContext#stop()}
    * method will be called at the completion of the connection request, whether or not an
    * exception occurred.
    * 
    * @param startTime the timestamp of the start time as returned by System.currentTimeMillis() 
    * @return an instance of MetricsContext
    */
   MetricsContext recordConnectionRequest(long startTime);

   /**
    * This method is called when a Connection is closed, with the total time in milliseconds
    * that the Connection was out of the pool.
    *
    * @param bagEntry the entry to calculate usage for
    */
   void recordConnectionUsage(PoolBagEntry bagEntry);

   /**
    * Get the current number of idle connections.
    *
    * @return the number of idle connections in the pool
    */
   int getIdleConnections();

   /**
    * Get the current number of active (in-use) connections.
    *
    * @return the number of active connections in the pool
    */
   int getActiveConnections();

   /**
    * Get the current total number of connections.
    *
    * @return the total number of connections in the pool
    */
   int getTotalConnections();

   /**
    * Get the current number of threads awaiting a connection.
    *
    * @return the number of awaiting threads
    */
   int getThreadsAwaitingConnection();

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