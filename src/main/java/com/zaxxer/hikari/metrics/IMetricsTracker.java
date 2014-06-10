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
   public MetricsContext recordConnectionRequest(long startTime);

   /**
    * This method is called when a Connection is closed, with the total time in milliseconds
    * that the Connection was out of the pool.
    *
    * @param usageMilleseconds the Connection usage time in milliseconds
    */
   public void recordConnectionUsage(long usageMilleseconds);

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
   }
}