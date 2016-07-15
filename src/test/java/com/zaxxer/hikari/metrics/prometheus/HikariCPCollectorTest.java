package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
      config.setJdbcUrl("jdbc:h2:mem:");

      new HikariDataSource(config);

      assertThat(getValue("hikaricp_active_connections", "no_connection"), is(0.0));
      assertThat(getValue("hikaricp_idle_connections", "no_connection"), is(0.0));
      assertThat(getValue("hikaricp_pending_threads", "no_connection"), is(0.0));
      assertThat(getValue("hikaricp_connections", "no_connection"), is(0.0));
   }

   @Test
   public void noConnectionWithoutPoolName() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setJdbcUrl("jdbc:h2:mem:");

      new HikariDataSource(config);

      assertThat(getValue("hikaricp_active_connections", "HikariPool-1"), is(0.0));
      assertThat(getValue("hikaricp_idle_connections", "HikariPool-1"), is(0.0));
      assertThat(getValue("hikaricp_pending_threads", "HikariPool-1"), is(0.0));
      assertThat(getValue("hikaricp_connections", "HikariPool-1"), is(0.0));
   }

   @Test
   public void connection1() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setPoolName("connection1");
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setJdbcUrl("jdbc:h2:mem:");

      HikariDataSource ds = new HikariDataSource(config);
      Connection connection1 = ds.getConnection();

      assertThat(getValue("hikaricp_active_connections", "connection1"), is(1.0));
      assertThat(getValue("hikaricp_idle_connections", "connection1"), is(0.0));
      assertThat(getValue("hikaricp_pending_threads", "connection1"), is(0.0));
      assertThat(getValue("hikaricp_connections", "connection1"), is(1.0));

      connection1.close();
   }

   @Test
   public void connectionClosed() throws Exception {
      HikariConfig config = new HikariConfig();
      config.setPoolName("connectionClosed");
      config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
      config.setJdbcUrl("jdbc:h2:mem:");
      config.setMaximumPoolSize(20);

      HikariDataSource ds = new HikariDataSource(config);
      Connection connection1 = ds.getConnection();
      connection1.close();

      assertThat(getValue("hikaricp_active_connections", "connectionClosed"), is(0.0));
      assertThat(getValue("hikaricp_idle_connections", "connectionClosed"), is(1.0));
      assertThat(getValue("hikaricp_pending_threads", "connectionClosed"), is(0.0));
      assertThat(getValue("hikaricp_connections", "connectionClosed"), is(1.0));
   }

   private double getValue(String name, String poolName) {
      String[] labelNames = {"pool"};
      String[] labelValues = {poolName};
      return CollectorRegistry.defaultRegistry.getSampleValue(name, labelNames, labelValues);
   }

}
