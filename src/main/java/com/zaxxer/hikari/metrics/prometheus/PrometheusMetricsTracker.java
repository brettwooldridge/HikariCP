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

package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory.RegistrationStatus;
import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.datapoints.DistributionDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory.RegistrationStatus.REGISTERED;

class PrometheusMetricsTracker implements IMetricsTracker
{
   private final static Counter CONNECTION_TIMEOUT_COUNTER = Counter.builder()
      .name("hikaricp_connection_timeout_total")
      .labelNames("pool")
      .help("Connection timeout total count")
      .register();

   private final static Summary ELAPSED_ACQUIRED_SUMMARY =
      createSummary("hikaricp_connection_acquired_nanos", "Connection acquired time (ns)");

   private final static Summary ELAPSED_USAGE_SUMMARY =
      createSummary("hikaricp_connection_usage_millis", "Connection usage (ms)");

   private final static Summary ELAPSED_CREATION_SUMMARY =
      createSummary("hikaricp_connection_creation_millis", "Connection creation (ms)");

   private final static Map<PrometheusRegistry, RegistrationStatus> registrationStatuses = new ConcurrentHashMap<>();

   private final String poolName;
   private final HikariCPCollector hikariCPCollector;

   private final CounterDataPoint connectionTimeoutCounterChild;

   private final DistributionDataPoint elapsedAcquiredSummaryChild;
   private final DistributionDataPoint elapsedUsageSummaryChild;
   private final DistributionDataPoint elapsedCreationSummaryChild;

   PrometheusMetricsTracker(String poolName, PrometheusRegistry collectorRegistry, HikariCPCollector hikariCPCollector)
   {
      registerMetrics(collectorRegistry);
      this.poolName = poolName;
      this.hikariCPCollector = hikariCPCollector;
      this.connectionTimeoutCounterChild = CONNECTION_TIMEOUT_COUNTER.labelValues(poolName);
      this.elapsedAcquiredSummaryChild = ELAPSED_ACQUIRED_SUMMARY.labelValues(poolName);
      this.elapsedUsageSummaryChild = ELAPSED_USAGE_SUMMARY.labelValues(poolName);
      this.elapsedCreationSummaryChild = ELAPSED_CREATION_SUMMARY.labelValues(poolName);
   }

   private void registerMetrics(PrometheusRegistry collectorRegistry)
   {
      if (registrationStatuses.putIfAbsent(collectorRegistry, REGISTERED) == null) {
         collectorRegistry.register(CONNECTION_TIMEOUT_COUNTER);
         collectorRegistry.register(ELAPSED_ACQUIRED_SUMMARY);
         collectorRegistry.register(ELAPSED_USAGE_SUMMARY);
         collectorRegistry.register(ELAPSED_CREATION_SUMMARY);
      }
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos)
   {
      elapsedAcquiredSummaryChild.observe(elapsedAcquiredNanos);
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis)
   {
      elapsedUsageSummaryChild.observe(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis)
   {
      elapsedCreationSummaryChild.observe(connectionCreatedMillis);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutCounterChild.inc();
   }

   private static Summary createSummary(String name, String help)
   {
      return Summary.builder()
         .name(name)
         .labelNames("pool")
         .help(help)
         .quantile(0.5, 0.05)
         .quantile(0.95, 0.01)
         .quantile(0.99, 0.001)
         .maxAgeSeconds(TimeUnit.MINUTES.toSeconds(5))
         .numberOfAgeBuckets(5)
         .register();
   }

   @Override
   public void close()
   {
      hikariCPCollector.remove(poolName);
      CONNECTION_TIMEOUT_COUNTER.remove(poolName);
      ELAPSED_ACQUIRED_SUMMARY.remove(poolName);
      ELAPSED_USAGE_SUMMARY.remove(poolName);
      ELAPSED_CREATION_SUMMARY.remove(poolName);
   }
}
