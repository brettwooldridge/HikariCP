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
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.util.List;

import com.zaxxer.hikari.metrics.PoolStats;
import io.prometheus.client.Collector;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubConnection;

import io.prometheus.client.CollectorRegistry;

public class HikariCPCollectorTest
{

   private CollectorRegistry collectorRegistry;

   @Before
   public void setupCollectorRegistry()
   {
      this.collectorRegistry = new CollectorRegistry();
   }

   @Test
   public void noConnection()
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(this.collectorRegistry));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config)) {
         assertThat(getValue("hikaricp_active_connections", "noConnection"), is(0.0));
         assertThat(getValue("hikaricp_idle_connections", "noConnection"), is(0.0));
         assertThat(getValue("hikaricp_pending_threads", "noConnection"), is(0.0));
         assertThat(getValue("hikaricp_connections", "noConnection"), is(0.0));
         assertThat(getValue("hikaricp_max_connections", "noConnection"), is(10.0));
         assertThat(getValue("hikaricp_min_connections", "noConnection"), is(0.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void noConnectionWithoutPoolName()
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(this.collectorRegistry));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config)) {
         String poolName = ds.getHikariConfigMXBean().getPoolName();
         assertThat(getValue("hikaricp_active_connections", poolName), is(0.0));
         assertThat(getValue("hikaricp_idle_connections", poolName), is(0.0));
         assertThat(getValue("hikaricp_pending_threads", poolName), is(0.0));
         assertThat(getValue("hikaricp_connections", poolName), is(0.0));
         assertThat(getValue("hikaricp_max_connections", poolName), is(10.0));
         assertThat(getValue("hikaricp_min_connections", poolName), is(0.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void connection1() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(this.collectorRegistry));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config);
         Connection connection1 = ds.getConnection()) {

         quietlySleep(1000);

         assertThat(getValue("hikaricp_active_connections", "connection1"), is(1.0));
         assertThat(getValue("hikaricp_idle_connections", "connection1"), is(0.0));
         assertThat(getValue("hikaricp_pending_threads", "connection1"), is(0.0));
         assertThat(getValue("hikaricp_connections", "connection1"), is(1.0));
         assertThat(getValue("hikaricp_max_connections", "connection1"), is(1.0));
         assertThat(getValue("hikaricp_min_connections", "connection1"), is(1.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void connectionClosed() throws Exception
   {
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(this.collectorRegistry));
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
         assertThat(getValue("hikaricp_max_connections", "connectionClosed"), is(1.0));
         assertThat(getValue("hikaricp_min_connections", "connectionClosed"), is(1.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }
   }

   @Test
   public void poolStatsRemovedAfterShutDown() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setPoolName("shutDownPool");
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory(this.collectorRegistry));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);

      StubConnection.slowCreate = true;
      try (HikariDataSource ds = new HikariDataSource(config)) {
         try (Connection connection1 = ds.getConnection()) {
            // close immediately
         }

         assertThat(getValue("hikaricp_active_connections", "shutDownPool"), is(0.0));
         assertThat(getValue("hikaricp_idle_connections", "shutDownPool"), is(1.0));
         assertThat(getValue("hikaricp_pending_threads", "shutDownPool"), is(0.0));
         assertThat(getValue("hikaricp_connections", "shutDownPool"), is(1.0));
         assertThat(getValue("hikaricp_max_connections", "shutDownPool"), is(1.0));
         assertThat(getValue("hikaricp_min_connections", "shutDownPool"), is(1.0));
      }
      finally {
         StubConnection.slowCreate = false;
      }

      assertNull(getValue("hikaricp_active_connections", "shutDownPool"));
      assertNull(getValue("hikaricp_idle_connections", "shutDownPool"));
      assertNull(getValue("hikaricp_pending_threads", "shutDownPool"));
      assertNull(getValue("hikaricp_connections", "shutDownPool"));
      assertNull(getValue("hikaricp_max_connections", "shutDownPool"));
      assertNull(getValue("hikaricp_min_connections", "shutDownPool"));
   }

   @Test
   public void testHikariCPCollectorGaugesMetricsInitialization()
   {
      HikariCPCollector hikariCPCollector = new HikariCPCollector();
      hikariCPCollector.add("collectorTestPool", poolStatsWithPredefinedValues());
      List<Collector.MetricFamilySamples> metrics = hikariCPCollector.collect();
      hikariCPCollector.register(collectorRegistry);

      assertThat(metrics.size(), is(6));
      assertThat(metrics.stream().filter(metricFamilySamples -> metricFamilySamples.type == Collector.Type.GAUGE).count(), is(6L));
      assertThat(getValue("hikaricp_active_connections", "collectorTestPool"), is(58.0));
      assertThat(getValue("hikaricp_idle_connections", "collectorTestPool"), is(42.0));
      assertThat(getValue("hikaricp_pending_threads", "collectorTestPool"), is(1.0));
      assertThat(getValue("hikaricp_connections", "collectorTestPool"), is(100.0));
      assertThat(getValue("hikaricp_max_connections", "collectorTestPool"), is(100.0));
      assertThat(getValue("hikaricp_min_connections", "collectorTestPool"), is(3.0));
   }

   private Double getValue(String name, String poolName)
   {
      String[] labelNames = {"pool"};
      String[] labelValues = {poolName};
      return this.collectorRegistry.getSampleValue(name, labelNames, labelValues);
   }

   private PoolStats poolStatsWithPredefinedValues()
   {
      return new PoolStats(0) {
         @Override
         protected void update() {
            totalConnections = 100;
            idleConnections = 42;
            activeConnections = 58;
            pendingThreads = 1;
            maxConnections = 100;
            minConnections = 3;
         }
      };
   }

}
