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
 * <p>
 * Use default CollectorRegistry and default HikariCPCollector:
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
 * }</pre>
 * </p>
 * <p>
 * Use provided CollectorRegistry and default HikariCPCollector:
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(registry));
 * }</pre>
 * </p>
 * <p>
 * Use provided CollectorRegistry and provided HikariCPCollector:
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(registry, collector));
 * }</pre>
 * </p>
 */
public class PrometheusMetricsTrackerFactory implements MetricsTrackerFactory {

   private HikariCPCollector collector;
   private CollectorRegistry registry;

   private static volatile HikariCPCollector globalCollector;

   /**
    * If nothing is provided, use {@link CollectorRegistry.defaultRegistry} and {@link globalCollector} as default
    */
   public PrometheusMetricsTrackerFactory() {
      this(CollectorRegistry.defaultRegistry);
   }

   /**
    * If a CollectorRegistry is provided, use provided CollectorRegistry and {@link globalCollector} as default
    */
   public PrometheusMetricsTrackerFactory(CollectorRegistry registry) {
      if (globalCollector == null) {
         synchronized (PrometheusMetricsTrackerFactory.class) {
            if (globalCollector == null) {
               globalCollector = new HikariCPCollector();
            }
         }
      }
      this.registry = registry;
      this.collector = globalCollector.register(registry);
   }

   /**
    * If both a CollectorRegistry and a Collector are provided, use provided CollectorRegistry and Collector
    */
   public PrometheusMetricsTrackerFactory(CollectorRegistry registry, HikariCPCollector collector) {
      this.registry = registry;
      this.collector = collector.register(registry);
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats) {
      getCollector().add(poolName, poolStats);
      return new PrometheusMetricsTracker(poolName, registry);
   }

   public static void setGlobalCollector(HikariCPCollector globalCollector) {
      PrometheusMetricsTrackerFactory.globalCollector = globalCollector;
   }

   /**
    * initialize and register collector if it isn't initialized yet
    */
   private HikariCPCollector getCollector() {
      if (collector == null) {
         if (globalCollector == null) {
            synchronized (PrometheusMetricsTrackerFactory.class) {
               if (globalCollector == null) {
                  globalCollector = new HikariCPCollector();
               }
            }
         }
         collector = globalCollector;
      }
      return collector;
   }
}
