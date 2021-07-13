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

import com.zaxxer.hikari.metrics.PoolStats;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class HikariCPCollector extends Collector
{

   private static final List<String> LABEL_NAMES = Collections.singletonList("pool");

   private final Map<String, PoolStats> poolStatsMap = new ConcurrentHashMap<>();

   @Override
   public List<MetricFamilySamples> collect()
   {
      return Arrays.asList(
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
      );
   }

   void add(String name, PoolStats poolStats)
   {
      poolStatsMap.put(name, poolStats);
   }

   void remove(String name)
   {
      poolStatsMap.remove(name);
   }

   private GaugeMetricFamily createGauge(String metric, String help,
                                         Function<PoolStats, Integer> metricValueFunction)
   {
      var metricFamily = new GaugeMetricFamily(metric, help, LABEL_NAMES);
      poolStatsMap.forEach((k, v) -> metricFamily.addMetric(
         Collections.singletonList(k),
         metricValueFunction.apply(v)
      ));
      return metricFamily;
   }
}
