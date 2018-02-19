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

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.metrics.dropwizard.CodahaleMetricsTrackerFactory;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;

public interface MetricsTrackerFactory
{
   /**
    * Create an instance of an IMetricsTracker.
    *
    * @param poolName the name of the pool
    * @param poolStats a PoolStats instance to use
    * @return a IMetricsTracker implementation instance
    */
   IMetricsTracker create(String poolName, PoolStats poolStats);

   static MetricsTrackerFactory from(MetricRegistry metricRegistry) {
      return new CodahaleMetricsTrackerFactory(metricRegistry);
   }

   static MetricsTrackerFactory from(MeterRegistry meterRegistry) {
      return new MicrometerMetricsTrackerFactory(meterRegistry);
   }

}
