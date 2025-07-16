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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.zaxxer.hikari.metrics.PoolStats;

import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;

class HikariCPCollector implements MultiCollector {

   private final Map<String, PoolStats> poolStatsMap = new ConcurrentHashMap<>();

   public MetricSnapshots collect()
   {
      return new MetricSnapshots(Arrays.asList(
         createGauge("hikaricp_active_connections", "Active connections",
            PoolStats::getActiveConnections),
         createGauge("hikaricp_idle_connections", "Idle connections",
            PoolStats::getIdleConnections),
         createGauge("hikaricp_pending_threads", "Pending threads",
            PoolStats::getPendingThreads),
         createGauge("hikaricp_connections", "The number of current connections",
            PoolStats::getTotalConnections),
         createGauge("hikaricp_max_connections", "Max connections",
            PoolStats::getMaxConnections),
         createGauge("hikaricp_min_connections", "Min connections",
            PoolStats::getMinConnections)
      ));
   }

   void add(String name, PoolStats poolStats)
   {
      poolStatsMap.put(name, poolStats);
   }

   void remove(String name)
   {
      poolStatsMap.remove(name);
   }

   private GaugeSnapshot createGauge(String metric, String help,
                                                            Function<PoolStats, Integer> metricValueFunction)
   {
      Collection<GaugeSnapshot.GaugeDataPointSnapshot> gaugeDataPointSnapshots = new ArrayList<>();
      poolStatsMap.forEach((k, v) -> gaugeDataPointSnapshots.add(
         new GaugeSnapshot.GaugeDataPointSnapshot(metricValueFunction.apply(v), Labels.of("pool", k), null)
      ));
      return new GaugeSnapshot(new MetricMetadata(metric, help), gaugeDataPointSnapshots);
   }
}
