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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.mocks.StubDataSource;
import org.junit.Test;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static com.zaxxer.hikari.pool.TestElf.getUnsealedConfig;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestMBean
{
   @Test
   public void testMBeanRegistration() {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setRegisterMbeans(true);
      config.setConnectionTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      new HikariDataSource(config).close();
   }

   @Test
   public void testMBeanReporting() throws SQLException, InterruptedException, MalformedObjectNameException {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(5);
      config.setRegisterMbeans(true);
      config.setConnectionTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "100");

      try (HikariDataSource ds = new HikariDataSource(config)) {

         TimeUnit.SECONDS.sleep(1);

         getUnsealedConfig(ds).setIdleTimeout(3000);

         MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
         ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (testMBeanReporting)");
         HikariPoolMXBean hikariPoolMXBean = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);

         assertEquals(0, hikariPoolMXBean.getActiveConnections());
         assertEquals(3, hikariPoolMXBean.getIdleConnections());

         try (Connection ignored = ds.getConnection()) {
            assertEquals(1, hikariPoolMXBean.getActiveConnections());

            TimeUnit.SECONDS.sleep(1);

            assertEquals(3, hikariPoolMXBean.getIdleConnections());
            assertEquals(4, hikariPoolMXBean.getTotalConnections());
         }

         TimeUnit.SECONDS.sleep(3);

         assertEquals(0, hikariPoolMXBean.getActiveConnections());
         assertEquals(3, hikariPoolMXBean.getIdleConnections());
         assertEquals(3, hikariPoolMXBean.getTotalConnections());
      }
      finally {
         System.clearProperty("com.zaxxer.hikari.housekeeping.periodMs");
      }

      System.setProperty("hikaricp.jmx.register2.0", "true");

      try (HikariDataSource ds = new HikariDataSource(config)) {

         getUnsealedConfig(ds).setIdleTimeout(3000);

         TimeUnit.SECONDS.sleep(1);

         MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
         ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool,name=testMBeanReporting");
         HikariPoolMXBean hikariPoolMXBean = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);

         assertEquals(0, hikariPoolMXBean.getActiveConnections());
         assertEquals(3, hikariPoolMXBean.getIdleConnections());
      }
      finally {
         System.clearProperty("hikaricp.jmx.register2.0");
      }
   }

   @Test
   public void testMBeanChange() {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(5);
      config.setRegisterMbeans(true);
      config.setConnectionTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariConfigMXBean hikariConfigMXBean = ds.getHikariConfigMXBean();
         hikariConfigMXBean.setIdleTimeout(3000);

         assertEquals(3000, ds.getIdleTimeout());
      }
   }

   @Test
   public void testMBeanConnectionTimeoutChange() throws SQLException {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(2);
      config.setRegisterMbeans(true);
      config.setConnectionTimeout(2800);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "250");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         HikariConfigMXBean hikariConfigMXBean = ds.getHikariConfigMXBean();
         assertEquals(2800, hikariConfigMXBean.getConnectionTimeout());

         final StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
         // connection acquisition takes more than 0 ms in a real system
         stubDataSource.setConnectionAcquisitionTime(1200);

         hikariConfigMXBean.setConnectionTimeout(1000);

         quietlySleep(500);

         try (Connection conn1 = ds.getConnection();
              Connection conn2 = ds.getConnection()) {
            fail("Connection should have timed out.");
         }
         catch (SQLException e) {
            assertEquals(1000, ds.getConnectionTimeout());
         }
      }
      finally {
         System.clearProperty("com.zaxxer.hikari.housekeeping.periodMs");
      }
   }
}
