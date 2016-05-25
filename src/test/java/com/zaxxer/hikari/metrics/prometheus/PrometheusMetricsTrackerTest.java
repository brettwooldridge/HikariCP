package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.CollectorRegistry;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PrometheusMetricsTrackerTest {
   @Test
   public void recordConnectionTimeout() throws Exception {
      String poolName = "record";

      HikariConfig config = new HikariConfig();
      config.setPoolName(poolName);
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(250);

      String[] labelNames = {"pool"};
      String[] labelValues = {poolName};

      HikariDataSource hikariDataSource = new HikariDataSource(config);
      Connection connection = hikariDataSource.getConnection();
      try {
         hikariDataSource.getConnection();
      } catch (SQLTransientConnectionException ignored) {
      }
      connection.close();

      assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
         "hikaricp_connection_timeout_count",
         labelNames,
         labelValues), is(1.0));
      assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
         "hikaricp_connection_acquired_nanos_count",
         labelNames,
         labelValues), is(equalTo(1.0)));
      assertTrue(CollectorRegistry.defaultRegistry.getSampleValue(
         "hikaricp_connection_acquired_nanos_sum",
         labelNames,
         labelValues) > 0.0);
      assertThat(CollectorRegistry.defaultRegistry.getSampleValue(
         "hikaricp_connection_usage_millis_count",
         labelNames,
         labelValues), is(equalTo(1.0)));
      assertTrue(CollectorRegistry.defaultRegistry.getSampleValue(
         "hikaricp_connection_usage_millis_sum",
         labelNames,
         labelValues) > 0.0);
   }
}
