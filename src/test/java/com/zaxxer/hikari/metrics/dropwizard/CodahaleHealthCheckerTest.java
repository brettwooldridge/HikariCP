package com.zaxxer.hikari.metrics.dropwizard;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.assertTrue;

public class CodahaleHealthCheckerTest
{
   @Test
   public void testNoWaitForShutdownPool()
   {
      HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(2);
      config.setConnectionTestQuery("SELECT 1");
      config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
      config.addDataSourceProperty("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
      config.setHealthCheckRegistry(healthCheckRegistry);

      String poolName;

      try (HikariDataSource ds = new HikariDataSource(config)) {
         poolName = ds.getPoolName();
      }

      long currentTime = System.currentTimeMillis();
      healthCheckRegistry.runHealthCheck(poolName + ".pool.ConnectivityCheck");
      long elapsed = System.currentTimeMillis() - currentTime;

      assertTrue("Health check waited for timeout: " + elapsed + " ms", elapsed < 1000);
   }
}
