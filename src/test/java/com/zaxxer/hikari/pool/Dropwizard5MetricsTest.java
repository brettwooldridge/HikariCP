package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.dropwizard.Dropwizard5MetricsTrackerFactory;
import com.zaxxer.hikari.util.UtilityElf;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.Timer;
import org.junit.Test;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.newHikariDataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Test HikariCP/Dropwizard 5 metrics integration.
 */
public class Dropwizard5MetricsTest extends TestMetricsBase<MetricRegistry>
{
   @Override
   protected MetricsTrackerFactory metricsTrackerFactory(final MetricRegistry metricRegistry)
   {
      return new Dropwizard5MetricsTrackerFactory(metricRegistry);
   }

   @Override
   protected MetricRegistry metricRegistry()
   {
      return new MetricRegistry();
   }

   @Test
   public void testSetters1() throws Exception
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         MetricRegistry metricRegistry = metricRegistry();

         try {
            try (Connection connection = ds.getConnection()) {
               // close immediately
            }

            // After the pool as started, we can only set it once...
            ds.setMetricRegistry(metricRegistry);

            // and never again...
            ds.setMetricRegistry(metricRegistry);
            fail("Should not have been allowed to set registry after pool started");
         }
         catch (IllegalStateException ise) {
            // pass
         }
      }
   }

   @Test
   public void testSetters2() throws Exception
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         MetricRegistry metricRegistry = metricRegistry();

         ds.setMetricRegistry(metricRegistry);

         // before the pool is started, we can set it any number of times...
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
   public void testMetricWait() throws SQLException
   {
      MetricRegistry metricRegistry = new MetricRegistry();

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setMetricRegistry(metricRegistry);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         ds.getConnection().close();

         Timer timer = metricRegistry.getTimers(new MetricFilter() {
            /** {@inheritDoc} */
            @Override
            public boolean matches(MetricName name, Metric metric)
            {
               return name.equals(MetricRegistry.name("testMetricWait", "pool", "Wait"));
            }
         }).values().iterator().next();

         assertEquals(1, timer.getCount());
         assertTrue(timer.getMeanRate() > 0.0);
      }
   }

   @Test
   public void testMetricUsage() throws SQLException
   {
      assumeFalse(System.getProperty("os.name").contains("Windows"));
      MetricRegistry metricRegistry = new MetricRegistry();

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setMetricRegistry(metricRegistry);
      config.setInitializationFailTimeout(0);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         try (Connection connection = ds.getConnection()) {
            UtilityElf.quietlySleep(250L);
         }

         Histogram histo = metricRegistry.getHistograms(new MetricFilter() {
            /** {@inheritDoc} */
            @Override
            public boolean matches(MetricName name, Metric metric)
            {
               return name.equals(MetricRegistry.name("testMetricUsage", "pool", "Usage"));
            }
         }).values().iterator().next();

         assertEquals(1, histo.getCount());
         double seventyFifth = histo.getSnapshot().get75thPercentile();
         assertTrue("Seventy-fith percentile less than 250ms: " + seventyFifth, seventyFifth >= 250.0);
      }
   }

   @Test
   public void testMetricRegistrySubclassIsAllowed()
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         ds.setMetricRegistry(new MetricRegistry() {
            @Override
            public Timer timer(String name) {
               return super.timer(name);
            }
         });
      }
   }
}
