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
import static org.junit.Assert.assertEquals;

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

         getUnsealedConfig(ds).setIdleTimeout(3000);

         TimeUnit.SECONDS.sleep(1);

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

         TimeUnit.SECONDS.sleep(2);

         assertEquals(0, hikariPoolMXBean.getActiveConnections());
         assertEquals(3, hikariPoolMXBean.getIdleConnections());
         assertEquals(3, hikariPoolMXBean.getTotalConnections());

      }
      finally {
         System.clearProperty("com.zaxxer.hikari.housekeeping.periodMs");
      }
   }

   @Test(expected = IllegalStateException.class)
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

         ds.setIdleTimeout(1000);
      }
   }
}
