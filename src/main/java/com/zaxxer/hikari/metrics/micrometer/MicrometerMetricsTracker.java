package com.zaxxer.hikari.metrics.micrometer;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.stats.quantile.WindowSketchQuantiles;

import java.util.concurrent.TimeUnit;

import static io.micrometer.core.instrument.stats.hist.CumulativeHistogram.buckets;
import static io.micrometer.core.instrument.stats.hist.CumulativeHistogram.linear;

public class MicrometerMetricsTracker implements IMetricsTracker
{
   private static final String METRIC_CATEGORY = "pool";
   private static final String METRIC_NAME_WAIT = "Wait";
   private static final String METRIC_NAME_USAGE = "Usage";
   private static final String METRIC_NAME_CONNECT = "ConnectionCreation";
   private static final String METRIC_NAME_TIMEOUT_RATE = "ConnectionTimeoutRate";
   private static final String METRIC_NAME_TOTAL_CONNECTIONS = "TotalConnections";
   private static final String METRIC_NAME_IDLE_CONNECTIONS = "IdleConnections";
   private static final String METRIC_NAME_ACTIVE_CONNECTIONS = "ActiveConnections";
   private static final String METRIC_NAME_PENDING_CONNECTIONS = "PendingConnections";

   private final Timer connectionObtainTimer;
   private final DistributionSummary connectionTimeoutMeter;
   private final DistributionSummary connectionUsage;
   private final DistributionSummary connectionCreation;
   @SuppressWarnings({"FieldCanBeLocal", "unused"})
   private final Gauge totalConnectionGauge;
   @SuppressWarnings({"FieldCanBeLocal", "unused"})
   private final Gauge idleConnectionGauge;
   @SuppressWarnings({"FieldCanBeLocal", "unused"})
   private final Gauge activeConnectionGauge;
   @SuppressWarnings({"FieldCanBeLocal", "unused"})
   private final Gauge pendingConnectionGauge;

   MicrometerMetricsTracker(final String poolName, final PoolStats poolStats, final MeterRegistry meterRegistry)
   {
      this.connectionObtainTimer = meterRegistry
         .timerBuilder(METRIC_NAME_WAIT)
         .tags(METRIC_CATEGORY, poolName)
         .create();

      this.connectionCreation = meterRegistry
         .summaryBuilder(METRIC_NAME_CONNECT)
         .tags(METRIC_CATEGORY, poolName)
         .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
         .histogram(buckets(linear(0, 10, 20), TimeUnit.MILLISECONDS))
         .create();

      this.connectionUsage = meterRegistry
         .summaryBuilder(METRIC_NAME_USAGE)
         .tags(poolName, METRIC_CATEGORY)
         .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
         .histogram(buckets(linear(0, 10, 20), TimeUnit.MILLISECONDS))
         .create();

      this.connectionTimeoutMeter = meterRegistry
         .summaryBuilder(METRIC_NAME_TIMEOUT_RATE)
         .tags(METRIC_CATEGORY, poolName)
         .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
         .histogram(buckets(linear(0, 10, 20), TimeUnit.MILLISECONDS))
         .create();

      this.totalConnectionGauge = meterRegistry
         .gaugeBuilder(METRIC_NAME_TOTAL_CONNECTIONS, Integer.class, (i) -> poolStats.getTotalConnections())
         .tags(METRIC_CATEGORY, poolName)
         .create();

      this.idleConnectionGauge = meterRegistry
         .gaugeBuilder(METRIC_NAME_IDLE_CONNECTIONS, Integer.class, (i) -> poolStats.getIdleConnections())
         .tags(METRIC_CATEGORY, poolName)
         .create();

      this.activeConnectionGauge = meterRegistry
         .gaugeBuilder(METRIC_NAME_ACTIVE_CONNECTIONS, Integer.class, (i) -> poolStats.getActiveConnections())
         .tags(METRIC_CATEGORY, poolName)
         .create();

      this.pendingConnectionGauge = meterRegistry
         .gaugeBuilder(METRIC_NAME_PENDING_CONNECTIONS, Integer.class, (i) -> poolStats.getPendingThreads())
         .tags(METRIC_CATEGORY, poolName)
         .create();
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos)
   {
      connectionObtainTimer.record(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionUsageMillis(final long elapsedBorrowedMillis)
   {
      connectionUsage.record(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutMeter.count();
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis)
   {
      connectionCreation.record(connectionCreatedMillis);
   }
}
