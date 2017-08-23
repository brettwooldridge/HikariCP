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

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.prometheus.client.CollectorRegistry;

public class PrometheusMetricsTrackerTest {

   @Test
   public void recordConnectionTimeout() throws Exception {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(250);

      String[] labelNames = {"pool"};
      String[] labelValues = {config.getPoolName()};

      try (HikariDataSource hikariDataSource = new HikariDataSource(config)) {
         try (Connection connection1 = hikariDataSource.getConnection();
            Connection connection2 = hikariDataSource.getConnection()) {
            try (Connection connection3 = hikariDataSource.getConnection()) {
            }
            catch (SQLTransientConnectionException ignored) {
            }
         }

         assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
            "hikaricp_connection_timeout_total",
            labelNames,
            labelValues), is(1.0));
         assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
            "hikaricp_connection_acquired_nanos_count",
            labelNames,
            labelValues), is(equalTo(3.0)));
         assertTrue(CollectorRegistry.defaultRegistry.getSampleValue(
            "hikaricp_connection_acquired_nanos_sum",
            labelNames,
            labelValues) > 0.0);
         assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
            "hikaricp_connection_usage_millis_count",
            labelNames,
            labelValues), is(equalTo(2.0)));
         assertTrue(CollectorRegistry.defaultRegistry.getSampleValue(
            "hikaricp_connection_usage_millis_sum",
            labelNames,
            labelValues) > 0.0);
      }
   }

   @Test
   public void testMultiplePoolName() throws Exception {
      String[] labelNames = {"pool"};

      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setPoolName("first");
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(250);
      String[] labelValues1 = {config.getPoolName()};

      try (HikariDataSource ignored = new HikariDataSource(config)) {
         assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
            "hikaricp_connection_timeout_total",
            labelNames,
            labelValues1), is(0.0));

         HikariConfig config2 = newHikariConfig();
         config2.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
         config2.setPoolName("second");
         config2.setJdbcUrl("jdbc:h2:mem:");
         config2.setMaximumPoolSize(4);
         config2.setConnectionTimeout(250);
         String[] labelValues2 = {config2.getPoolName()};

         try (HikariDataSource ignored2 = new HikariDataSource(config2)) {
            assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
               "hikaricp_connection_timeout_total",
               labelNames,
               labelValues2), is(0.0));
         }
      }
   }
}
