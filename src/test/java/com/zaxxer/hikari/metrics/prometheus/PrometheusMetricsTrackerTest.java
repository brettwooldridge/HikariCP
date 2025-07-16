/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.mocks.StubPoolStats;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class PrometheusMetricsTrackerTest
{

   private PrometheusRegistry defaultCollectorRegistry;
   private PrometheusRegistry customCollectorRegistry;

   private static final String POOL_LABEL_NAME = "pool";
   private static final String[] LABEL_NAMES = {POOL_LABEL_NAME};

   private static final String QUANTILE_LABEL_NAME = "quantile";
   private static final String[] QUANTILE_LABEL_VALUES = new String[]{"0.5", "0.95", "0.99"};

   @Before
   public void setupCollectorRegistry()
   {
      this.defaultCollectorRegistry = new PrometheusRegistry();
      this.customCollectorRegistry = new PrometheusRegistry();
   }

   @Test
   public void recordConnectionTimeout() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(defaultCollectorRegistry));
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(250);

      String[] labelValues = {config.getPoolName()};

      try (HikariDataSource hikariDataSource = new HikariDataSource(config)) {
         try (Connection connection1 = hikariDataSource.getConnection();
              Connection connection2 = hikariDataSource.getConnection()) {
            try (Connection connection3 = hikariDataSource.getConnection()) {
            } catch (SQLTransientConnectionException ignored) {
            }
         }

         Double total = Samples.getSampleValue(defaultCollectorRegistry,
            "hikaricp_connection_timeout", LABEL_NAMES, labelValues
         );
         assertThat(total, is(1.0));
      }
   }

   @Test
   public void connectionAcquisitionMetrics()
   {
      checkSummaryMetricFamily("hikaricp_connection_acquired_nanos");
   }

   @Test
   public void connectionUsageMetrics()
   {
      checkSummaryMetricFamily("hikaricp_connection_usage_millis");
   }

   @Test
   public void connectionCreationMetrics()
   {
      checkSummaryMetricFamily("hikaricp_connection_creation_millis");
   }

   @Test
   public void testMultiplePoolNameWithOneCollectorRegistry()
   {
      HikariConfig configFirstPool = newHikariConfig();
      configFirstPool.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(defaultCollectorRegistry));
      configFirstPool.setPoolName("first");
      configFirstPool.setJdbcUrl("jdbc:h2:mem:");
      configFirstPool.setMaximumPoolSize(2);
      configFirstPool.setConnectionTimeout(250);

      HikariConfig configSecondPool = newHikariConfig();
      configSecondPool.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(defaultCollectorRegistry));
      configSecondPool.setPoolName("second");
      configSecondPool.setJdbcUrl("jdbc:h2:mem:");
      configSecondPool.setMaximumPoolSize(4);
      configSecondPool.setConnectionTimeout(250);

      String[] labelValuesFirstPool = {configFirstPool.getPoolName()};
      String[] labelValuesSecondPool = {configSecondPool.getPoolName()};

      try (HikariDataSource ignoredFirstPool = new HikariDataSource(configFirstPool)) {
         assertThat(Samples.getSampleValue(defaultCollectorRegistry,
            "hikaricp_connection_timeout", LABEL_NAMES, labelValuesFirstPool),
            is(0.0));

         try (HikariDataSource ignoredSecondPool = new HikariDataSource(configSecondPool)) {
            assertThat(Samples.getSampleValue(defaultCollectorRegistry,
               "hikaricp_connection_timeout", LABEL_NAMES, labelValuesSecondPool),
               is(0.0));
         }
      }
   }

   @Test
   public void testMultiplePoolNameWithDifferentCollectorRegistries()
   {
      HikariConfig configFirstPool = newHikariConfig();
      configFirstPool.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(defaultCollectorRegistry));
      configFirstPool.setPoolName("first");
      configFirstPool.setJdbcUrl("jdbc:h2:mem:");
      configFirstPool.setMaximumPoolSize(2);
      configFirstPool.setConnectionTimeout(250);

      HikariConfig configSecondPool = newHikariConfig();
      configSecondPool.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(customCollectorRegistry));
      configSecondPool.setPoolName("second");
      configSecondPool.setJdbcUrl("jdbc:h2:mem:");
      configSecondPool.setMaximumPoolSize(4);
      configSecondPool.setConnectionTimeout(250);

      String[] labelValuesFirstPool = {configFirstPool.getPoolName()};
      String[] labelValuesSecondPool = {configSecondPool.getPoolName()};

      try (HikariDataSource ignoredFirstPool = new HikariDataSource(configFirstPool)) {
         assertThat(Samples.getSampleValue(defaultCollectorRegistry,
            "hikaricp_connection_timeout", LABEL_NAMES, labelValuesFirstPool),
            is(0.0));

         try (HikariDataSource ignoredSecondPool = new HikariDataSource(configSecondPool)) {
            assertThat(Samples.getSampleValue(customCollectorRegistry,
               "hikaricp_connection_timeout", LABEL_NAMES, labelValuesSecondPool),
               is(0.0));
         }
      }
   }

   @Test
   public void testMetricsRemovedAfterShutDown()
   {
      HikariConfig configFirstPool = newHikariConfig();
      configFirstPool.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(defaultCollectorRegistry));
      configFirstPool.setPoolName("first");
      configFirstPool.setJdbcUrl("jdbc:h2:mem:");
      configFirstPool.setMaximumPoolSize(2);
      configFirstPool.setConnectionTimeout(250);

      HikariConfig configSecondPool = newHikariConfig();
      configSecondPool.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(customCollectorRegistry));
      configSecondPool.setPoolName("second");
      configSecondPool.setJdbcUrl("jdbc:h2:mem:");
      configSecondPool.setMaximumPoolSize(4);
      configSecondPool.setConnectionTimeout(250);

      String[] labelValuesFirstPool = {configFirstPool.getPoolName()};
      String[] labelValuesSecondPool = {configSecondPool.getPoolName()};

      try (HikariDataSource ignoredFirstPool = new HikariDataSource(configFirstPool)) {
         assertThat(Samples.getSampleValue(defaultCollectorRegistry,
            "hikaricp_connection_timeout", LABEL_NAMES, labelValuesFirstPool),
            is(0.0));

         try (HikariDataSource ignoredSecondPool = new HikariDataSource(configSecondPool)) {
            assertThat(Samples.getSampleValue(customCollectorRegistry,
               "hikaricp_connection_timeout", LABEL_NAMES, labelValuesSecondPool),
               is(0.0));
         }

         assertNull(Samples.getSampleValue(defaultCollectorRegistry,
            "hikaricp_connection_timeout", LABEL_NAMES, labelValuesSecondPool));

         assertThat(Samples.getSampleValue(defaultCollectorRegistry,
            "hikaricp_connection_timeout", LABEL_NAMES, labelValuesFirstPool),
            is(0.0));
      }
   }

   @Test
   public void testCloseMethod()
   {
      String[] labelValues = {"testPool"};
      PrometheusMetricsTrackerFactory prometheusFactory = new PrometheusMetricsTrackerFactory(defaultCollectorRegistry);
      IMetricsTracker prometheusTracker = prometheusFactory.create("testPool", new StubPoolStats(0));

      prometheusTracker.recordConnectionTimeout();
      prometheusTracker.recordConnectionAcquiredNanos(42L);
      prometheusTracker.recordConnectionUsageMillis(111L);
      prometheusTracker.recordConnectionCreatedMillis(101L);

      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_connection_timeout", LABEL_NAMES, labelValues),
         is(1.0));
      assertThat(Samples.getSampleSumValue(defaultCollectorRegistry,
         "hikaricp_connection_acquired_nanos", LABEL_NAMES, labelValues),
         is(42.0));
      assertThat(Samples.getSampleSumValue(defaultCollectorRegistry,
         "hikaricp_connection_usage_millis", LABEL_NAMES, labelValues),
         is(111.0));
      assertThat(Samples.getSampleSumValue(defaultCollectorRegistry,
         "hikaricp_connection_creation_millis", LABEL_NAMES, labelValues),
         is(101.0));
      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_active_connections", LABEL_NAMES, labelValues),
         is(0.0));
      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_idle_connections", LABEL_NAMES, labelValues),
         is(0.0));
      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_pending_threads", LABEL_NAMES, labelValues),
         is(0.0));
      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_connections", LABEL_NAMES, labelValues),
         is(0.0));
      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_max_connections", LABEL_NAMES, labelValues),
         is(0.0));
      assertThat(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_min_connections", LABEL_NAMES, labelValues),
         is(0.0));

      prometheusTracker.close();

      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_connection_timeout", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleSumValue(defaultCollectorRegistry,
         "hikaricp_connection_acquired_nanos", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleSumValue(defaultCollectorRegistry,
         "hikaricp_connection_usage_millis", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleSumValue(defaultCollectorRegistry,
         "hikaricp_connection_creation_millis", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_active_connections", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_idle_connections", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_pending_threads", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_connections", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_connections", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_max_connections", LABEL_NAMES, labelValues));
      assertNull(Samples.getSampleValue(defaultCollectorRegistry,
         "hikaricp_min_connections", LABEL_NAMES, labelValues));
   }

   private void checkSummaryMetricFamily(String metricName)
   {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(defaultCollectorRegistry));
      config.setJdbcUrl("jdbc:h2:mem:");

      try (HikariDataSource ignored = new HikariDataSource(config)) {
         Long count = Samples.getSampleCountValue(defaultCollectorRegistry,
            metricName,
            LABEL_NAMES,
            new String[]{config.getPoolName()}
         );
         assertNotNull(count);

         Double sum = Samples.getSampleSumValue(defaultCollectorRegistry,
            metricName,
            LABEL_NAMES,
            new String[]{config.getPoolName()}
         );
         assertNotNull(sum);

         for (String quantileLabelValue : QUANTILE_LABEL_VALUES) {
            Double quantileValue = Samples.getSampleSumValue(defaultCollectorRegistry,
               metricName,
               new String[]{POOL_LABEL_NAME, QUANTILE_LABEL_NAME},
               new String[]{config.getPoolName(), quantileLabelValue}
            );
            assertNull("q = " + quantileLabelValue, quantileValue);
         }
      }
   }
}
