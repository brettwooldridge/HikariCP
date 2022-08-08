package com.zaxxer.hikari.metrics.dropwizard;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import io.dropwizard.metrics5.MetricRegistry;

public class Dropwizard5MetricsTrackerFactory implements MetricsTrackerFactory
{
   private final MetricRegistry registry;

   public Dropwizard5MetricsTrackerFactory(final MetricRegistry registry)
   {
      this.registry = registry;
   }

   public MetricRegistry getRegistry()
   {
      return registry;
   }

   @Override
   public IMetricsTracker create(final String poolName, final PoolStats poolStats)
   {
      return new Dropwizard5MetricsTracker(poolName, poolStats, registry);
   }
}
