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
import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;

public final class CodaHaleMetricsTracker extends MetricsTracker
{
   private final String metricsPrefix;
   private final Timer connectionObtainTimer;
   private final Histogram connectionUsage;
   private final Meter connectionTimeoutMeter;
   private final MetricRegistry registry;

   public CodaHaleMetricsTracker(final String metricsPrefix, final PoolStats poolStats, final MetricRegistry registry)
   {
      this.metricsPrefix = metricsPrefix;
      this.registry = registry;
      this.connectionObtainTimer = registry.timer(MetricRegistry.name(metricsPrefix, "Wait"));
      this.connectionUsage = registry.histogram(MetricRegistry.name(metricsPrefix, "Usage"));
      this.connectionTimeoutMeter = registry.meter(MetricRegistry.name(metricsPrefix, "ConnectionTimeoutRate"));

      registry.register(MetricRegistry.name(metricsPrefix, "TotalConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getTotalConnections();
                           }
                        });

      registry.register(MetricRegistry.name(metricsPrefix, "IdleConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getIdleConnections();
                           }
                        });

      registry.register(MetricRegistry.name(metricsPrefix, "ActiveConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getActiveConnections();
                           }
                        });

      registry.register(MetricRegistry.name(metricsPrefix, "PendingConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getPendingThreads();
                           }
                        });
   }

   /** {@inheritDoc} */
   @Override
   public void close()
   {
      registry.remove(MetricRegistry.name(metricsPrefix, "Wait"));
      registry.remove(MetricRegistry.name(metricsPrefix, "Usage"));
      registry.remove(MetricRegistry.name(metricsPrefix, "TotalConnections"));
      registry.remove(MetricRegistry.name(metricsPrefix, "IdleConnections"));
      registry.remove(MetricRegistry.name(metricsPrefix, "ActiveConnections"));
      registry.remove(MetricRegistry.name(metricsPrefix, "PendingConnections"));
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

   public Timer getConnectionAcquisitionTimer()
   {
      return connectionObtainTimer;
   }

   public Histogram getConnectionDurationHistogram()
   {
      return connectionUsage;
   }
}
