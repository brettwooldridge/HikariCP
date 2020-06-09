package com.zaxxer.hikari.metrics.opentracing;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import io.opentracing.Tracer;

public class OpenTracingMetricsTrackerFactory implements MetricsTrackerFactory
{
   private final Tracer tracer;

   public OpenTracingMetricsTrackerFactory(Tracer tracer) {
      this.tracer = tracer;
   }

   @Override
   public IMetricsTracker create(String poolName, PoolStats poolStats)
   {
      return new OpenTracingMetricsTracker(tracer, poolName);
   }
}
