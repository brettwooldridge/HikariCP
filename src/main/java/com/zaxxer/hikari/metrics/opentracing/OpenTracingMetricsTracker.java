package com.zaxxer.hikari.metrics.opentracing;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * {@link IMetricsTracker Metrics tracker} for OpenTracing.
 * This tracker records connection request/acquisition/return as logs on the currently active span.
 */
public class OpenTracingMetricsTracker implements IMetricsTracker
{
   public static final String HIKARI_METRIC_NAME_PREFIX = "hikaricp";

   private final Tracer tracer;
   private final String poolName;

   public OpenTracingMetricsTracker(Tracer tracer, String poolName)
   {
      this.tracer = tracer;
      this.poolName = poolName;
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos)
   {
      logEvent("connection-acquired");
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis)
   {
      logEvent("connection-returned");
   }

   @Override
   public void recordConnectionTimeout(long elapsedTimeoutMillis)
   {
      logEvent("connection-timeout");
   }

   @Override
   public void recordConnectionRequest()
   {
      logEvent("connection-requested");
   }

   private void logEvent(String eventName)
   {
      Span span = tracer.scopeManager().activeSpan();
      if (span == null) {
         return;
      }

      span.log(HIKARI_METRIC_NAME_PREFIX + "." + poolName + "." + eventName);
   }
}
