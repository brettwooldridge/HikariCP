package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.mocks.StubPoolStats;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrometheusMetricsTrackerFactoryTest
{

   @After
   public void clearCollectorRegistry()
   {
//      PrometheusRegistry.defaultRegistry.clear();
   }

   @Test
   public void registersToProvidedCollectorRegistry()
   {
      PrometheusRegistry collectorRegistry = new PrometheusRegistry();
      PrometheusMetricsTrackerFactory factory = new PrometheusMetricsTrackerFactory(collectorRegistry);
      factory.create("testpool-1", new StubPoolStats(0));
      assertHikariMetricsAreNotPresent(PrometheusRegistry.defaultRegistry);
      assertHikariMetricsArePresent(collectorRegistry);
   }

   @Test
   public void registersToDefaultCollectorRegistry()
   {
      PrometheusMetricsTrackerFactory factory = new PrometheusMetricsTrackerFactory();
      factory.create("testpool-2", new StubPoolStats(0));
      assertHikariMetricsArePresent(PrometheusRegistry.defaultRegistry);
   }

   private void assertHikariMetricsArePresent(PrometheusRegistry collectorRegistry)
   {
      List<String> registeredMetrics = toMetricNames(collectorRegistry.scrape());
      assertTrue(registeredMetrics.contains("hikaricp_active_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_idle_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_pending_threads"));
      assertTrue(registeredMetrics.contains("hikaricp_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_max_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_min_connections"));
   }

   private void assertHikariMetricsAreNotPresent(PrometheusRegistry collectorRegistry)
   {
      List<String> registeredMetrics = toMetricNames(collectorRegistry.scrape());
      assertFalse(registeredMetrics.contains("hikaricp_active_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_idle_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_pending_threads"));
      assertFalse(registeredMetrics.contains("hikaricp_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_max_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_min_connections"));
   }

   private List<String> toMetricNames(MetricSnapshots metricSnapshots)
   {
      return metricSnapshots.stream().map(metricSnapshot -> metricSnapshot.getMetadata().getName()).collect(Collectors.toList());

   }
}
