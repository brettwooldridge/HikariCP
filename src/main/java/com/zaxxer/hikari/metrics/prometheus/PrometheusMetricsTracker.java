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
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

import java.util.concurrent.TimeUnit;

class PrometheusMetricsTracker implements IMetricsTracker
{
   private static final Counter CONNECTION_TIMEOUT_COUNTER = Counter.build()
      .name("hikaricp_connection_timeout_total")
      .labelNames("pool")
      .help("Connection timeout total count")
      .register();

   private static final Summary ELAPSED_ACQUIRED_SUMMARY =
      registerSummary("hikaricp_connection_acquired_nanos", "Connection acquired time (ns)");

   private static final Summary ELAPSED_BORROWED_SUMMARY =
      registerSummary("hikaricp_connection_usage_millis", "Connection usage (ms)");

   private static final Summary ELAPSED_CREATION_SUMMARY =
      registerSummary("hikaricp_connection_creation_millis", "Connection creation (ms)");

   private final Counter.Child connectionTimeoutCounterChild;

   private static Summary registerSummary(String name, String help) {
      return Summary.build()
         .name(name)
         .labelNames("pool")
         .help(help)
         .quantile(0.5, 0.05)
         .quantile(0.95, 0.01)
         .quantile(0.99, 0.001)
         .maxAgeSeconds(TimeUnit.MINUTES.toSeconds(5))
         .ageBuckets(5)
         .register();
   }

   private final Summary.Child elapsedAcquiredSummaryChild;
   private final Summary.Child elapsedBorrowedSummaryChild;
   private final Summary.Child elapsedCreationSummaryChild;

   PrometheusMetricsTracker(String poolName) {
      this.connectionTimeoutCounterChild = CONNECTION_TIMEOUT_COUNTER.labels(poolName);
      this.elapsedAcquiredSummaryChild = ELAPSED_ACQUIRED_SUMMARY.labels(poolName);
      this.elapsedBorrowedSummaryChild = ELAPSED_BORROWED_SUMMARY.labels(poolName);
      this.elapsedCreationSummaryChild = ELAPSED_CREATION_SUMMARY.labels(poolName);
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos)
   {
      elapsedAcquiredSummaryChild.observe(elapsedAcquiredNanos);
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis)
   {
      elapsedBorrowedSummaryChild.observe(elapsedBorrowedMillis);
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
}
