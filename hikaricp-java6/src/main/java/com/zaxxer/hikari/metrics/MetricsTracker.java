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

/**
 * This class does absolutely nothing.
 *
 * @author Brett Wooldridge
 */
public class MetricsTracker implements IMetricsTracker
{
   public static final MetricsContext NO_CONTEXT = new MetricsContext();

   public MetricsTracker()
   {
   }

   public MetricsTracker(String poolName)
   {
   }

   public MetricsContext recordConnectionRequest(long requestTime)
   {
      return NO_CONTEXT;
   }

   public void recordConnectionUsage(long usageMilleseconds)
   {
   }
}
