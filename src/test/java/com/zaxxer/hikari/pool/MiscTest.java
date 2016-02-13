/*
 * Copyright (C) 2013 Brett Wooldridge
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.UtilityElf;

/**
 * @author Brett Wooldridge
 */
public class MiscTest
{
   @Test
   public void testLogWriter() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(4);
      config.setPoolName("test");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      TestElf.setConfigUnitTest(true);

      try (HikariDataSource ds = new HikariDataSource(config)) {
         PrintWriter writer = new PrintWriter(System.out);
         ds.setLogWriter(writer);
         Assert.assertSame(writer, ds.getLogWriter());
         Assert.assertEquals("test", config.getPoolName());
      }
      finally
      {
         TestElf.setConfigUnitTest(false);
      }
   }

   @Test
   public void testInvalidIsolation()
   {
      try {
         UtilityElf.getTransactionIsolation("INVALID");
         Assert.fail();
      }
      catch (Exception e) {
         Assert.assertTrue(e instanceof IllegalArgumentException);
      }
   }

   @Test
   public void testCreateInstance()
   {
      try {
         UtilityElf.createInstance("invalid", null);
         Assert.fail();
      }
      catch (RuntimeException e) {
         Assert.assertTrue(e.getCause() instanceof ClassNotFoundException);
      }
   }

   @Test
   public void testLeakDetection() throws Exception
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(Class.forName("com.zaxxer.hikari.pool.ProxyLeakTask"), ps);
      TestElf.setConfigUnitTest(true);

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(4);
      config.setPoolName("test");
      config.setThreadFactory(Executors.defaultThreadFactory());
      config.setMetricRegistry(null);
      config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(1));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      try (HikariDataSource ds = new HikariDataSource(config)) {
         TestElf.setSlf4jLogLevel(HikariPool.class, Level.DEBUG);
         TestElf.getPool(ds).logPoolState();

         Connection connection = ds.getConnection();
         UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis(4));
         connection.close();
         UtilityElf.quietlySleep(TimeUnit.SECONDS.toMillis(1));
         ps.close();
         String s = new String(baos.toByteArray());
         Assert.assertNotNull("Exception string was null", s);
         Assert.assertTrue("Expected exception to contain 'Connection leak detection' but contains *" + s + "*", s.contains("Connection leak detection"));
      }
      finally
      {
         TestElf.setConfigUnitTest(false);
      }
   }
}
