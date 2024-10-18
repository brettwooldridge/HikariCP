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
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory.RegistrationStatus;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory.RegistrationStatus.REGISTERED;

/**
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory());
 * }</pre>
 */
public class PrometheusHistogramMetricsTrackerFactory implements MetricsTrackerFactory {

   private final static Map<PrometheusRegistry, RegistrationStatus> registrationStatuses = new ConcurrentHashMap<>();

   private final HikariCPCollector collector = new HikariCPCollector();

   private final PrometheusRegistry collectorRegistry;

   /**
    * Default Constructor. The Hikari metrics are registered to the default
    * collector registry ({@code CollectorRegistry.defaultRegistry}).
    */
   public PrometheusHistogramMetricsTrackerFactory() {
      this(new PrometheusRegistry());
   }

   /**
    * Constructor that allows to pass in a {@link PrometheusRegistry} to which the
    * Hikari metrics are registered.
    */
   public PrometheusHistogramMetricsTrackerFactory(PrometheusRegistry collectorRegistry) {
      this.collectorRegistry = collectorRegistry;
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats) {
      registerCollector(this.collector, this.collectorRegistry);
      this.collector.add(poolName, poolStats);
      return new PrometheusHistogramMetricsTracker(poolName, this.collectorRegistry, this.collector);
   }

   private void registerCollector(MultiCollector collector, PrometheusRegistry collectorRegistry) {
      if (registrationStatuses.putIfAbsent(collectorRegistry, REGISTERED) == null) {
         collectorRegistry.register(collector);
      }
   }
}
