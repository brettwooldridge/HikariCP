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

import com.zaxxer.hikari.metrics.HikariMetricsConfig;
import com.zaxxer.hikari.metrics.PoolStats;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class HikariCPCollector extends Collector
{

   private static final List<String> LABEL_NAMES = Collections.singletonList("pool");

   private final Map<String, PoolStats> poolStatsMap = new ConcurrentHashMap<>();

   private HikariMetricsConfig hikariMetricsConfig;

   public HikariCPCollector()
   {
      this(new HikariMetricsConfig());
   }

   public HikariCPCollector(HikariMetricsConfig config)
   {
      this.hikariMetricsConfig = config;
   }

   @Override
   public List<MetricFamilySamples> collect()
   {
      MetricDef[] metrics = MetricDef.values();
      List<MetricFamilySamples> metricFamilySamples = new ArrayList<>(metrics.length);
      for (MetricDef metricDef : metrics) {
         metricFamilySamples.add(createGauge(metricDef));
      }
      return metricFamilySamples;
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

   private GaugeMetricFamily createGauge(MetricDef metricDef)
   {
      String metricName = metricDef.metricName;
      if (hikariMetricsConfig.isMetricNaming2()) {
         metricName = metricDef.metricName2;
      }
      return createGauge(metricName, metricDef.help, metricDef.metricValueFunction);
   }

   private enum MetricDef
   {
      active("hikaricp_active_connections", "hikaricp_connections_active", "Active connections", PoolStats::getActiveConnections),
      idle("hikaricp_idle_connections", "hikaricp_connections_idle", "Idle connections",
         PoolStats::getIdleConnections),
      pending("hikaricp_pending_threads", "hikaricp_connections_pending", "Pending threads", PoolStats::getPendingThreads),

      current("hikaricp_connections", "hikaricp_connections", "The number of current connections",
         PoolStats::getTotalConnections),
      max("hikaricp_max_connections", "hikaricp_connections_max", "Max connections",
         PoolStats::getMaxConnections),
      min("hikaricp_min_connections", "hikaricp_connections_min", "Min connections",
         PoolStats::getMinConnections);

      private String metricName;

      private String metricName2;

      private String help;

      private Function<PoolStats, Integer> metricValueFunction;

      MetricDef(String metricName, String metricName2, String help, Function<PoolStats, Integer> metricValueFunction)
      {
         this.metricName = metricName;
         this.metricName2 = metricName2;
         this.help = help;
         this.metricValueFunction = metricValueFunction;
      }
   }
}
