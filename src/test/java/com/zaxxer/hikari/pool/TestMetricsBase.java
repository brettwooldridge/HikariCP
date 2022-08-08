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

package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.dropwizard.CodahaleMetricsTrackerFactory;
import com.zaxxer.hikari.util.UtilityElf;
import org.junit.Test;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.newHikariDataSource;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Test HikariCP/CodaHale/Dropwizard 5 metrics integration.
 *
 * <p>
 * This base test class contains tests common to Codahale metrics testing (pre-5) and Dropwizard 5 metrics testing.
 * That's the idea behind the registry type parameterization and abstract methods.
 * Include health checks when implemented for Dropwizard 5.
 * There's still a bit of duplication between the extending classes.
 *
 * @author Brett Wooldridge
 */
abstract class TestMetricsBase<M>
{
   protected abstract MetricsTrackerFactory metricsTrackerFactory(M metricRegistry);
   protected abstract M metricRegistry();

   @Test
   public void testSetters3() throws Exception
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         M metricRegistry = metricRegistry();
         MetricsTrackerFactory metricsTrackerFactory = metricsTrackerFactory(metricRegistry);

         try (Connection connection = ds.getConnection()) {

            // After the pool as started, we can only set them once...
            ds.setMetricsTrackerFactory(metricsTrackerFactory);

            // and never again...
            ds.setMetricsTrackerFactory(metricsTrackerFactory);
            fail("Should not have been allowed to set metricsTrackerFactory after pool started");
         }
         catch (IllegalStateException ise) {
            // pass
            try {
               // and never again... (even when calling another method)
               ds.setMetricRegistry(metricRegistry);
               fail("Should not have been allowed to set registry after pool started");
            }
            catch (IllegalStateException ise2) {
               // pass
            }
         }
      }
   }

   @Test
   public void testSetters4() throws Exception
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         M metricRegistry = metricRegistry();

         // before the pool is started, we can set it any number of times using either setter
         ds.setMetricRegistry(metricRegistry);
         ds.setMetricRegistry(metricRegistry);
         ds.setMetricRegistry(metricRegistry);

         try (Connection connection = ds.getConnection()) {

            // after the pool is started, we cannot set it any more
            ds.setMetricRegistry(metricRegistry);
            fail("Should not have been allowed to set registry after pool started");
         }
         catch (IllegalStateException ise) {
            // pass
         }
      }
   }

   @Test
   public void testSetters5() throws Exception
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         M metricRegistry = metricRegistry();
         MetricsTrackerFactory metricsTrackerFactory = metricsTrackerFactory(metricRegistry);

         // before the pool is started, we can set it any number of times using either setter
         ds.setMetricsTrackerFactory(metricsTrackerFactory);
         ds.setMetricsTrackerFactory(metricsTrackerFactory);
         ds.setMetricsTrackerFactory(metricsTrackerFactory);

         try (Connection connection = ds.getConnection()) {

            // after the pool is started, we cannot set it any more
            ds.setMetricsTrackerFactory(metricsTrackerFactory);
            fail("Should not have been allowed to set registry factory after pool started");
         }
         catch (IllegalStateException ise) {
            // pass
         }
      }
   }
}
