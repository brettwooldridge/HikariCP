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
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class PrometheusHistogramMetricsTrackerTest {

   private CollectorRegistry defaultCollectorRegistry;
   private CollectorRegistry customCollectorRegistry;

   private static final String POOL_LABEL_NAME = "pool";
   private static final String[] LABEL_NAMES = {POOL_LABEL_NAME};

   @Before
   public void setupCollectorRegistry() {
      this.defaultCollectorRegistry = new CollectorRegistry();
      this.customCollectorRegistry = new CollectorRegistry();
   }

   @Test
   public void recordConnectionTimeout() throws Exception {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(defaultCollectorRegistry));
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

         Double total = defaultCollectorRegistry.getSampleValue(
            "hikaricp_connection_timeout_total",
            LABEL_NAMES,
            labelValues
         );
         assertThat(total, is(1.0));
      }
   }

   @Test
   public void connectionAcquisitionMetrics() {
      checkSummaryMetricFamily("hikaricp_connection_acquired_nanos");
   }

   @Test
   public void connectionUsageMetrics() {
      checkSummaryMetricFamily("hikaricp_connection_usage_millis");
   }

   @Test
   public void connectionCreationMetrics() {
      checkSummaryMetricFamily("hikaricp_connection_creation_millis");
   }

   @Test
   public void testMultiplePoolNameWithOneCollectorRegistry()
   {
      HikariConfig configFirstPool = newHikariConfig();
      configFirstPool.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(defaultCollectorRegistry));
      configFirstPool.setPoolName("first");
      configFirstPool.setJdbcUrl("jdbc:h2:mem:");
      configFirstPool.setMaximumPoolSize(2);
      configFirstPool.setConnectionTimeout(250);

      HikariConfig configSecondPool = newHikariConfig();
      configSecondPool.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(defaultCollectorRegistry));
      configSecondPool.setPoolName("second");
      configSecondPool.setJdbcUrl("jdbc:h2:mem:");
      configSecondPool.setMaximumPoolSize(4);
      configSecondPool.setConnectionTimeout(250);

      String[] labelValuesFirstPool = {configFirstPool.getPoolName()};
      String[] labelValuesSecondPool = {configSecondPool.getPoolName()};

      try (HikariDataSource ignoredFirstPool = new HikariDataSource(configFirstPool)) {
         assertThat(defaultCollectorRegistry.getSampleValue(
            "hikaricp_connection_timeout_total", LABEL_NAMES, labelValuesFirstPool),
            is(0.0));

         try (HikariDataSource ignoredSecondPool = new HikariDataSource(configSecondPool)) {
            assertThat(defaultCollectorRegistry.getSampleValue(
               "hikaricp_connection_timeout_total", LABEL_NAMES, labelValuesSecondPool),
               is(0.0));
         }
      }
   }

   @Test
   public void testMultiplePoolNameWithDifferentCollectorRegistries()
   {
      HikariConfig configFirstPool = newHikariConfig();
      configFirstPool.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(defaultCollectorRegistry));
      configFirstPool.setPoolName("first");
      configFirstPool.setJdbcUrl("jdbc:h2:mem:");
      configFirstPool.setMaximumPoolSize(2);
      configFirstPool.setConnectionTimeout(250);

      HikariConfig configSecondPool = newHikariConfig();
      configSecondPool.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(customCollectorRegistry));
      configSecondPool.setPoolName("second");
      configSecondPool.setJdbcUrl("jdbc:h2:mem:");
      configSecondPool.setMaximumPoolSize(4);
      configSecondPool.setConnectionTimeout(250);

      String[] labelValuesFirstPool = {configFirstPool.getPoolName()};
      String[] labelValuesSecondPool = {configSecondPool.getPoolName()};

      try (HikariDataSource ignoredFirstPool = new HikariDataSource(configFirstPool)) {
         assertThat(defaultCollectorRegistry.getSampleValue(
            "hikaricp_connection_timeout_total", LABEL_NAMES, labelValuesFirstPool),
            is(0.0));

         try (HikariDataSource ignoredSecondPool = new HikariDataSource(configSecondPool)) {
            assertThat(customCollectorRegistry.getSampleValue(
               "hikaricp_connection_timeout_total", LABEL_NAMES, labelValuesSecondPool),
               is(0.0));
         }
      }
   }

   private void checkSummaryMetricFamily(String metricName) {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusHistogramMetricsTrackerFactory(defaultCollectorRegistry));
      config.setJdbcUrl("jdbc:h2:mem:");

      try (HikariDataSource ignored = new HikariDataSource(config)) {
         Double count = defaultCollectorRegistry.getSampleValue(
            metricName + "_count",
            LABEL_NAMES,
            new String[]{config.getPoolName()}
         );
         assertNotNull(count);

         Double sum = defaultCollectorRegistry.getSampleValue(
            metricName + "_sum",
            LABEL_NAMES,
            new String[]{config.getPoolName()}
         );
         assertNotNull(sum);
      }
   }
}
