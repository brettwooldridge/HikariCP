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

package com.zaxxer.hikari.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.zaxxer.hikari.pool.PoolBagEntry;
import com.zaxxer.hikari.util.ClockSource;

public final class CodaHaleMetricsTracker extends MetricsTracker
{
   private static final ClockSource clockSource = ClockSource.INSTANCE;

   private final String poolName;
   private final Timer connectionObtainTimer;
   private final Histogram connectionUsage;
   private final MetricRegistry registry;

   public CodaHaleMetricsTracker(final String poolName, final PoolStats poolStats, final MetricRegistry registry)
   {
      this.poolName = poolName;
      this.registry = registry;
      this.connectionObtainTimer = registry.timer(MetricRegistry.name(poolName, "pool", "Wait"));
      this.connectionUsage = registry.histogram(MetricRegistry.name(poolName, "pool", "Usage"));

      registry.register(MetricRegistry.name(poolName, "pool", "TotalConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getTotalConnections();
                           }
                        });

      registry.register(MetricRegistry.name(poolName, "pool", "IdleConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getIdleConnections();
                           }
                        });

      registry.register(MetricRegistry.name(poolName, "pool", "ActiveConnections"),
                        new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                              return poolStats.getActiveConnections();
                           }
                        });

      registry.register(MetricRegistry.name(poolName, "pool", "PendingConnections"),
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
      registry.remove(MetricRegistry.name(poolName, "pool", "Wait"));
      registry.remove(MetricRegistry.name(poolName, "pool", "Usage"));
      registry.remove(MetricRegistry.name(poolName, "pool", "TotalConnections"));
      registry.remove(MetricRegistry.name(poolName, "pool", "IdleConnections"));
      registry.remove(MetricRegistry.name(poolName, "pool", "ActiveConnections"));
      registry.remove(MetricRegistry.name(poolName, "pool", "PendingConnections"));
   }

   /** {@inheritDoc} */
   @Override
   public Context recordConnectionRequest()
   {
      return new Context(connectionObtainTimer);
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionUsage(final PoolBagEntry bagEntry)
   {
      connectionUsage.update(clockSource.elapsedMillis(bagEntry.lastOpenTime));
   }

   public Timer getConnectionAcquisitionTimer()
   {
      return connectionObtainTimer;
   }

   public Histogram getConnectionDurationHistogram()
   {
      return connectionUsage;
   }

   public static final class Context extends MetricsContext
   {
      final Timer.Context innerContext;

      Context(Timer timer) {
         innerContext = timer.time();
      }

      /** {@inheritDoc} */
      @Override
      public void stop()
      {
         innerContext.stop();
      }

      /** {@inheritDoc} */
      @Override
      public void setConnectionLastOpen(final PoolBagEntry bagEntry, final long now)
      {
         bagEntry.lastOpenTime = now;
      }
   }
}
