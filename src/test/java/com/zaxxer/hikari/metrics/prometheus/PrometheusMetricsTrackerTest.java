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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.CollectorRegistry;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

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
            "hikaricp_connection_timeout_count",
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
   public void testThatTheProperRegistryIsUsed() throws Exception {
      HikariConfig config = newHikariConfig();
      CollectorRegistry customRegistry = new CollectorRegistry();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(customRegistry));
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(2);
      config.setConnectionTimeout(250);

      String[] labelNames = {"pool"};
      String[] labelValues = {config.getPoolName()};

      try (HikariDataSource hikariDataSource = new HikariDataSource(config)) {
         assertThat(customRegistry.getSampleValue(
            "hikaricp_connection_timeout_count",
            labelNames,
            labelValues), is(0.0));
         assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
             "hikaricp_connection_usage_millis_sum",
             labelNames,
             labelValues), is(nullValue()));
      }
   }
}
