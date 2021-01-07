package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.metrics.PoolStats;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrometheusHistogramMetricsTrackerFactoryTest {

   @Test
   public void registersToProvidedCollectorRegistry() {
      CollectorRegistry collectorRegistry = new CollectorRegistry();
      PrometheusHistogramMetricsTrackerFactory factory =
         new PrometheusHistogramMetricsTrackerFactory(collectorRegistry);
      factory.create("testpool-1", poolStats());
      assertHikariMetricsAreNotPresent(CollectorRegistry.defaultRegistry);
      assertHikariMetricsArePresent(collectorRegistry);
   }

   @Test
   public void registersToDefaultCollectorRegistry() {
      PrometheusHistogramMetricsTrackerFactory factory = new PrometheusHistogramMetricsTrackerFactory();
      factory.create("testpool-2", poolStats());
      assertHikariMetricsArePresent(CollectorRegistry.defaultRegistry);
   }

   @After
   public void clearCollectorRegistry(){
      CollectorRegistry.defaultRegistry.clear();
   }

   private void assertHikariMetricsArePresent(CollectorRegistry collectorRegistry) {
      List<String> registeredMetrics = toMetricNames(collectorRegistry.metricFamilySamples());
      assertTrue(registeredMetrics.contains("hikaricp_active_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_idle_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_pending_threads"));
      assertTrue(registeredMetrics.contains("hikaricp_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_max_connections"));
      assertTrue(registeredMetrics.contains("hikaricp_min_connections"));
   }

   private void assertHikariMetricsAreNotPresent(CollectorRegistry collectorRegistry) {
      List<String> registeredMetrics = toMetricNames(collectorRegistry.metricFamilySamples());
      assertFalse(registeredMetrics.contains("hikaricp_active_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_idle_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_pending_threads"));
      assertFalse(registeredMetrics.contains("hikaricp_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_max_connections"));
      assertFalse(registeredMetrics.contains("hikaricp_min_connections"));
   }

   private List<String> toMetricNames(Enumeration<Collector.MetricFamilySamples> enumeration) {
      List<String> list = new ArrayList<>();
      while (enumeration.hasMoreElements()) {
         list.add(enumeration.nextElement().name);
      }
      return list;
   }

   private PoolStats poolStats() {
      return new PoolStats(0) {
         @Override
         protected void update() {
            // do nothing
         }
      };
   }

}
