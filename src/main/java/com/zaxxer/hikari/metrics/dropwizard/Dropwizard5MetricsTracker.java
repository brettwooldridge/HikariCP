package com.zaxxer.hikari.metrics.dropwizard;

import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Meter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.Timer;

import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_CATEGORY;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_ACTIVE_CONNECTIONS;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_CONNECT;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_IDLE_CONNECTIONS;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_MAX_CONNECTIONS;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_MIN_CONNECTIONS;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_PENDING_CONNECTIONS;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_TIMEOUT_RATE;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_TOTAL_CONNECTIONS;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_USAGE;
import static com.zaxxer.hikari.metrics.dropwizard.DropwizardCommon.METRIC_NAME_WAIT;

public class Dropwizard5MetricsTracker implements IMetricsTracker
{
   private final String poolName;
   private final Timer connectionObtainTimer;
   private final Histogram connectionUsage;
   private final Histogram connectionCreation;
   private final Meter connectionTimeoutMeter;
   private final MetricRegistry registry;

   Dropwizard5MetricsTracker(final String poolName, final PoolStats poolStats, final MetricRegistry registry)
   {
      this.poolName = poolName;
      this.registry = registry;
      this.connectionObtainTimer = registry.timer(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_WAIT));
      this.connectionUsage = registry.histogram(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_USAGE));
      this.connectionCreation = registry.histogram(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_CONNECT));
      this.connectionTimeoutMeter = registry.meter(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TIMEOUT_RATE));

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TOTAL_CONNECTIONS),
         (Gauge<Integer>) poolStats::getTotalConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_IDLE_CONNECTIONS),
         (Gauge<Integer>) poolStats::getIdleConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_ACTIVE_CONNECTIONS),
         (Gauge<Integer>) poolStats::getActiveConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_PENDING_CONNECTIONS),
         (Gauge<Integer>) poolStats::getPendingThreads);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MAX_CONNECTIONS),
         (Gauge<Integer>) poolStats::getMaxConnections);

      registry.register(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MIN_CONNECTIONS),
         (Gauge<Integer>) poolStats::getMinConnections);
   }

   /** {@inheritDoc} */
   @Override
   public void close()
   {
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_WAIT));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_USAGE));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_CONNECT));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TIMEOUT_RATE));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_TOTAL_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_IDLE_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_ACTIVE_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_PENDING_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MAX_CONNECTIONS));
      registry.remove(MetricRegistry.name(poolName, METRIC_CATEGORY, METRIC_NAME_MIN_CONNECTIONS));
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos)
   {
      connectionObtainTimer.update(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionUsageMillis(final long elapsedBorrowedMillis)
   {
      connectionUsage.update(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionTimeout()
   {
      connectionTimeoutMeter.mark();
   }

   @Override
   public void recordConnectionCreatedMillis(final long connectionCreatedMillis)
   {
      connectionCreation.update(connectionCreatedMillis);
   }

   public Timer getConnectionAcquisitionTimer()
   {
      return connectionObtainTimer;
   }

   public Histogram getConnectionDurationHistogram()
   {
      return connectionUsage;
   }

   public Histogram getConnectionCreationHistogram()
   {
      return connectionCreation;
   }
}
