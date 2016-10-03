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
import com.zaxxer.hikari.mocks.StubConnection;

import io.prometheus.client.CollectorRegistry;
import org.junit.Test;

import java.sql.Connection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HikariCPCollectorTest {
   @Test
   public void noConnection() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setPoolName("no_connection");
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config)) {
         assertThat(getValue("hikaricp_active_connections", "no_connection"), is(0.0));
         assertThat(getValue("hikaricp_idle_connections", "no_connection"), is(0.0));
         assertThat(getValue("hikaricp_pending_threads", "no_connection"), is(0.0));
         assertThat(getValue("hikaricp_connections", "no_connection"), is(0.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void noConnectionWithoutPoolName() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config)) {
         assertThat(getValue("hikaricp_active_connections", "HikariPool-1"), is(0.0));
         assertThat(getValue("hikaricp_idle_connections", "HikariPool-1"), is(0.0));
         assertThat(getValue("hikaricp_pending_threads", "HikariPool-1"), is(0.0));
         assertThat(getValue("hikaricp_connections", "HikariPool-1"), is(0.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void connection1() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setPoolName("connection1");
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config);
         Connection connection1 = ds.getConnection()) {

         assertThat(getValue("hikaricp_active_connections", "connection1"), is(1.0));
         assertThat(getValue("hikaricp_idle_connections", "connection1"), is(0.0));
         assertThat(getValue("hikaricp_pending_threads", "connection1"), is(0.0));
         assertThat(getValue("hikaricp_connections", "connection1"), is(1.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void connectionClosed() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setPoolName("connectionClosed");
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config)) {
         try (Connection connection1 = ds.getConnection()) {
            // close immediately
         }
         
         assertThat(getValue("hikaricp_active_connections", "connectionClosed"), is(0.0));
         assertThat(getValue("hikaricp_idle_connections", "connectionClosed"), is(1.0));
         assertThat(getValue("hikaricp_pending_threads", "connectionClosed"), is(0.0));
         assertThat(getValue("hikaricp_connections", "connectionClosed"), is(1.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   private double getValue(String name, String poolName) {
      String[] labelNames = {"pool"};
      String[] labelValues = {poolName};
      return CollectorRegistry.defaultRegistry.getSampleValue(name, labelNames, labelValues);
   }

}
