/*
 * Copyright (C) 2016 Brett Wooldridge
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

package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;

/**
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
 * }</pre>
 */
public class PrometheusMetricsTrackerFactory implements MetricsTrackerFactory {

   private static HikariCPCollector collector;

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats) {
      getCollector().add(poolName, poolStats);
      return new PrometheusMetricsTracker(poolName);
   }

   /**
    * initialize and register collector if it isn't initialized yet
    */
   private HikariCPCollector getCollector() {
      if (collector == null) {
         collector = new HikariCPCollector().register();
      }
      return collector;
   }
}
