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

package com.zaxxer.hikari.metrics.dropwizard;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;

public final class CodaHaleMetricsTracker implements IMetricsTracker
{
   private final String poolName;
   private final Timer connectionObtainTimer;
   private final Histogram connectionUsage;
   private final Histogram connectionCreation;
   private final Meter connectionTimeoutMeter;
   private final MetricRegistry registry;

   private static final String METRIC_CATEGORY = "pool";
   private static final String METRIC_NAME_WAIT = "Wait";
   private static final String METRIC_NAME_USAGE = "Usage";
   private static final String METRIC_NAME_CONNECT = "ConnectionCreation";
   private static final String METRIC_NAME_TIMEOUT_RATE = "ConnectionTimeoutRate";
   private static final String METRIC_NAME_TOTAL_CONNECTIONS = "TotalConnections";
   private static final String METRIC_NAME_IDLE_CONNECTIONS = "IdleConnections";
   private static final String METRIC_NAME_ACTIVE_CONNECTIONS = "ActiveConnections";
   private static final String METRIC_NAME_PENDING_CONNECTIONS = "PendingConnections";
   private static final String METRIC_NAME_MAX_CONNECTIONS = "MaxConnections";
   private static final String METRIC_NAME_MIN_CONNECTIONS = "MinConnections";

   CodaHaleMetricsTracker(final String poolName, final PoolStats poolStats, final MetricRegistry registry)
   {
      this.poolName = poolName;
      this.registry = registry;
      this.connectionObtainTimer = registry.timer(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_WAIT));
      this.connectionUsage = registry.histogram(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_USAGE));
      this.connectionCreation = registry.histogram(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_CONNECT));
      this.connectionTimeoutMeter = registry.meter(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TIMEOUT_RATE));

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TOTAL_CONNECTIONS),
         (Gauge<Integer>) poolStats::getTotalConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_IDLE_CONNECTIONS),
         (Gauge<Integer>) poolStats::getIdleConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_ACTIVE_CONNECTIONS),
         (Gauge<Integer>) poolStats::getActiveConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_PENDING_CONNECTIONS),
         (Gauge<Integer>) poolStats::getPendingThreads);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MAX_CONNECTIONS),
         (Gauge<Integer>) poolStats::getMaxConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MIN_CONNECTIONS),
         (Gauge<Integer>) poolStats::getMinConnections);
   }

   /** {@inheritDoc} */
   @Override
   public void close()
   {
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_WAIT));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_USAGE));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_CONNECT));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TIMEOUT_RATE));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TOTAL_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_IDLE_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_ACTIVE_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_PENDING_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MAX_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MIN_CONNECTIONS));
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos)
   {
      connectionObtainTimer.update(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionUsageMillis(final long elapsedBorrowedMillis)
   {
      connectionUsage.update(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutMeter.mark();
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis)
   {
      connectionCreation.update(connectionCreatedMillis);
   }

   public Timer getConnectionAcquisitionTimer()
   {
      return connectionObtainTimer;
   }

   public Histogram getConnectionDurationHistogram()
   {
      return connectionUsage;
   }

   public Histogram getConnectionCreationHistogram()
   {
      return connectionCreation;
   }
}
