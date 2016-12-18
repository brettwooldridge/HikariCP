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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class HikariCPCollector extends Collector {
   private final PoolStats poolStats;
   private final List<String> labelNames;
   private final List<String> labelValues;

   HikariCPCollector(String poolName, PoolStats poolStats) {
      this.poolStats = poolStats;
      this.labelNames = Collections.singletonList("pool");
      this.labelValues = Collections.singletonList(poolName);
   }

   @Override
   public List<MetricFamilySamples> collect() {
      return Arrays.asList(
         createSample("hikaricp_active_connections", "Active connections", poolStats.getActiveConnections()),
         createSample("hikaricp_idle_connections", "Idle connections", poolStats.getIdleConnections()),
         createSample("hikaricp_pending_threads", "Pending threads", poolStats.getPendingThreads()),
         createSample("hikaricp_connections", "The number of current connections", poolStats.getTotalConnections())
      );
   }

   private MetricFamilySamples createSample(String name, String helpMessage, double value)
   {
      List<MetricFamilySamples.Sample> samples = Collections.singletonList(
         new MetricFamilySamples.Sample(name, labelNames, labelValues, value)
      );

      return new MetricFamilySamples(name, Type.GAUGE, helpMessage, samples);
   }
}
