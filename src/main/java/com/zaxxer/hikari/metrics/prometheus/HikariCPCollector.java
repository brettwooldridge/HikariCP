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

   private MetricFamilySamples createSample(String name, String helpMessage, double value) {
      List<MetricFamilySamples.Sample> samples = Collections.singletonList(new MetricFamilySamples.Sample(name,
         labelNames,
         labelValues,
         value));

      return new MetricFamilySamples(name, Type.GAUGE, helpMessage, samples);
   }
}
