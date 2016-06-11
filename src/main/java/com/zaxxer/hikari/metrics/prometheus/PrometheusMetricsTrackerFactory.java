package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;

/**
 * <pre>{@code
 * HikariConfig config = new HikariConfig();
 * config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
 * }</pre>
 */
public class PrometheusMetricsTrackerFactory implements MetricsTrackerFactory {
   @Override
   public MetricsTracker create(String poolName, PoolStats poolStats) {
      new HikariCPCollector(poolName, poolStats).register();
      return new PrometheusMetricsTracker(poolName);
   }
}
