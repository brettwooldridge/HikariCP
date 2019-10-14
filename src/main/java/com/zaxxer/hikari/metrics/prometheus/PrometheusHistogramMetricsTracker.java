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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory.RegistrationStatus.REGISTERED;

/**
 * Alternative Prometheus metrics tracker using a Histogram instead of Summary
 * <p>
 * This is an alternative metrics tracker that doesn't use a {@link io.prometheus.client.Summary}. Summaries require
 * heavy locks that might cause performance issues. Source: https://github.com/prometheus/client_java/issues/328
 *
 * @see PrometheusMetricsTracker
 */
class PrometheusHistogramMetricsTracker implements IMetricsTracker
{
   private static final Counter CONNECTION_TIMEOUT_COUNTER = Counter.build()
      .name("hikaricp_connection_timeout_total")
      .labelNames("pool")
      .help("Connection timeout total count")
      .create();

   private static final Histogram ELAPSED_ACQUIRED_HISTOGRAM =
      createHistogram("hikaricp_connection_acquired_nanos", "Connection acquired time (ns)", 1_000);

   private static final Histogram ELAPSED_BORROWED_HISTOGRAM =
      createHistogram("hikaricp_connection_usage_millis", "Connection usage (ms)", 1);

   private static final Histogram ELAPSED_CREATION_HISTOGRAM =
      createHistogram("hikaricp_connection_creation_millis", "Connection creation (ms)", 1);

   private final static Map<CollectorRegistry, RegistrationStatus> registrationStatuses = new ConcurrentHashMap<>();

   private final Counter.Child connectionTimeoutCounterChild;

   private final Histogram.Child elapsedAcquiredHistogramChild;
   private final Histogram.Child elapsedBorrowedHistogramChild;
   private final Histogram.Child elapsedCreationHistogramChild;

   PrometheusHistogramMetricsTracker(String poolName, CollectorRegistry collectorRegistry)
   {
      registerMetrics(collectorRegistry);
      this.connectionTimeoutCounterChild = CONNECTION_TIMEOUT_COUNTER.labels(poolName);
      this.elapsedAcquiredHistogramChild = ELAPSED_ACQUIRED_HISTOGRAM.labels(poolName);
      this.elapsedBorrowedHistogramChild = ELAPSED_BORROWED_HISTOGRAM.labels(poolName);
      this.elapsedCreationHistogramChild = ELAPSED_CREATION_HISTOGRAM.labels(poolName);
   }

   private void registerMetrics(CollectorRegistry collectorRegistry)
   {
      if (registrationStatuses.putIfAbsent(collectorRegistry, REGISTERED) == null) {
         CONNECTION_TIMEOUT_COUNTER.register(collectorRegistry);
         ELAPSED_ACQUIRED_HISTOGRAM.register(collectorRegistry);
         ELAPSED_BORROWED_HISTOGRAM.register(collectorRegistry);
         ELAPSED_CREATION_HISTOGRAM.register(collectorRegistry);
      }
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos)
   {
      elapsedAcquiredHistogramChild.observe(elapsedAcquiredNanos);
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis)
   {
      elapsedBorrowedHistogramChild.observe(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis)
   {
      elapsedCreationHistogramChild.observe(connectionCreatedMillis);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutCounterChild.inc();
   }

   private static Histogram createHistogram(String name, String help, double bucketStart)
   {
      return Histogram.build()
            .name(name)
            .labelNames("pool")
            .help(help)
            .exponentialBuckets(bucketStart, 2.0, 11)
            .create();
   }
}
