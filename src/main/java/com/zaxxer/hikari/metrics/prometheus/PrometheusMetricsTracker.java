/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

import com.zaxxer.hikari.metrics.MetricsTracker;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

class PrometheusMetricsTracker extends MetricsTracker
{
   private final Counter.Child connectionTimeoutCounter;
   private final Summary.Child elapsedAcquiredSummary;
   private final Summary.Child elapsedBorrowedSummary;
   private final Summary.Child elapsedCreationSummary;

   private final Counter ctCounter;
   private final Summary eaSummary;
   private final Summary ebSummary;
   private final Summary ecSummary;
   private final Collector collector;

   PrometheusMetricsTracker(String poolName, Collector collector)
   {
      this.collector = collector;

      ctCounter = Counter.build()
         .name("hikaricp_connection_timeout_count")
         .labelNames("pool")
         .help("Connection timeout count")
         .register();

      this.connectionTimeoutCounter = ctCounter.labels(poolName);

      eaSummary = Summary.build()
         .name("hikaricp_connection_acquired_nanos")
         .labelNames("pool")
         .help("Connection acquired time (ns)")
         .register();
      this.elapsedAcquiredSummary = eaSummary.labels(poolName);

      ebSummary = Summary.build()
         .name("hikaricp_connection_usage_millis")
         .labelNames("pool")
         .help("Connection usage (ms)")
         .register();
      this.elapsedBorrowedSummary = ebSummary.labels(poolName);

      ecSummary = Summary.build()
            .name("hikaricp_connection_creation_millis")
            .labelNames("pool")
            .help("Connection creation (ms)")
            .register();
      this.elapsedCreationSummary = ecSummary.labels(poolName);
   }

   @Override
   public void close()
   {
      CollectorRegistry.defaultRegistry.unregister(ctCounter);
      CollectorRegistry.defaultRegistry.unregister(eaSummary);
      CollectorRegistry.defaultRegistry.unregister(ebSummary);
      CollectorRegistry.defaultRegistry.unregister(ecSummary);
      CollectorRegistry.defaultRegistry.unregister(collector);
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos)
   {
      elapsedAcquiredSummary.observe(elapsedAcquiredNanos);
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis)
   {
      elapsedBorrowedSummary.observe(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis)
   {
      elapsedCreationSummary.observe(connectionCreatedMillis);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutCounter.inc();
   }
}
