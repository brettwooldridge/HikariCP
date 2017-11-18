package com.zaxxer.hikari.metrics.micrometer;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

public class MicrometerMetricsTracker implements IMetricsTracker
{
   private static final String METRIC_CATEGORY = "pool";
   private static final String METRIC_NAME_WAIT = "hikaricp.connections.acquire";
   private static final String METRIC_NAME_USAGE = "hikaricp.connections.usage";
   private static final String METRIC_NAME_CONNECT = "hikaricp.connections.creation";

   private static final String METRIC_NAME_TIMEOUT_RATE = "hikaricp.connections.timeout";
   private static final String METRIC_NAME_TOTAL_CONNECTIONS = "hikaricp.connections";
   private static final String METRIC_NAME_IDLE_CONNECTIONS = "hikaricp.connections.idle";
   private static final String METRIC_NAME_ACTIVE_CONNECTIONS = "hikaricp.connections.active";
   private static final String METRIC_NAME_PENDING_CONNECTIONS = "hikaricp.connections.pending";

   private final Timer connectionObtainTimer;
   private final Counter connectionTimeoutCounter;
   private final Timer connectionUsage;
   private final Timer connectionCreation;
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
      this.connectionObtainTimer = Timer.builder(METRIC_NAME_WAIT)
         .description("Connection acquire time")
         .publishPercentiles(0.95)
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.connectionCreation = Timer.builder(METRIC_NAME_CONNECT)
         .description("Connection creation time")
         .publishPercentiles(0.95)
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.connectionUsage = Timer.builder(METRIC_NAME_USAGE)
         .description("Connection usage time")
         .publishPercentiles(0.95)
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.connectionTimeoutCounter = Counter.builder(METRIC_NAME_TIMEOUT_RATE)
         .description("Connection timeout total count")
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.totalConnectionGauge = Gauge.builder(METRIC_NAME_TOTAL_CONNECTIONS, poolStats, PoolStats::getTotalConnections)
         .description("Total connections")
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.idleConnectionGauge = Gauge.builder(METRIC_NAME_IDLE_CONNECTIONS, poolStats, PoolStats::getIdleConnections)
         .description("Idle connections")
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.activeConnectionGauge = Gauge.builder(METRIC_NAME_ACTIVE_CONNECTIONS, poolStats, PoolStats::getActiveConnections)
         .description("Active connections")
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

      this.pendingConnectionGauge = Gauge.builder(METRIC_NAME_PENDING_CONNECTIONS, poolStats, PoolStats::getPendingThreads)
         .description("Pending threads")
         .tags(METRIC_CATEGORY, poolName)
         .register(meterRegistry);

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
      connectionUsage.record(elapsedBorrowedMillis, TimeUnit.MILLISECONDS);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutCounter.increment();
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis)
   {
      connectionCreation.record(connectionCreatedMillis, TimeUnit.MILLISECONDS);
   }
}
