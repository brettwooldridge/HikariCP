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
import io.prometheus.client.CollectorRegistry;

/**
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
 * }</pre>
 */
public class PrometheusMetricsTrackerFactory implements MetricsTrackerFactory {

   private HikariCPCollector collector;

   private CollectorRegistry collectorRegistry;

   /**
    * Default Constructor. The Hikari metrics are registered to the default
    * collector registry ({@code CollectorRegistry.defaultRegistry}).
    */
   public PrometheusMetricsTrackerFactory() {
      this.collectorRegistry = CollectorRegistry.defaultRegistry;
   }

   /**
    * Constructor that allows to pass in a {@link CollectorRegistry} to which the
    * Hikari metrics are registered.
    */
   public PrometheusMetricsTrackerFactory(CollectorRegistry collectorRegistry) {
      this.collectorRegistry = collectorRegistry;
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats) {
      getCollector().add(poolName, poolStats);
      return new PrometheusMetricsTracker(poolName, this.collectorRegistry);
   }

   /**
    * initialize and register collector if it isn't initialized yet
    */
   private HikariCPCollector getCollector() {
      if (collector == null) {
         collector = new HikariCPCollector().register(this.collectorRegistry);
      }
      return collector;
   }
}
