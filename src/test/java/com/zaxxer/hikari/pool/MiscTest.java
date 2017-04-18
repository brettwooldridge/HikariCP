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

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.setConfigUnitTest;
import static com.zaxxer.hikari.pool.TestElf.setSlf4jLogLevel;
import static com.zaxxer.hikari.pool.TestElf.setSlf4jTargetStream;
import static com.zaxxer.hikari.util.UtilityElf.createInstance;
import static com.zaxxer.hikari.util.UtilityElf.getTransactionIsolation;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Brett Wooldridge
 */
public class MiscTest
{
   @Test
   public void testLogWriter() throws SQLException
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(4);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      setConfigUnitTest(true);

      try (HikariDataSource ds = new HikariDataSource(config)) {
         PrintWriter writer = new PrintWriter(System.out);
         ds.setLogWriter(writer);
         assertSame(writer, ds.getLogWriter());
         assertEquals("testLogWriter", config.getPoolName());
      }
      finally
      {
         setConfigUnitTest(false);
      }
   }

   @Test
   public void testInvalidIsolation()
   {
      try {
         getTransactionIsolation("INVALID");
         fail();
      }
      catch (Exception e) {
         assertTrue(e instanceof IllegalArgumentException);
      }
   }

   @Test
   public void testCreateInstance()
   {
      try {
         createInstance("invalid", null);
         fail();
      }
      catch (RuntimeException e) {
         assertTrue(e.getCause() instanceof ClassNotFoundException);
      }
   }

   @Test
   public void testLeakDetection() throws Exception
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (PrintStream ps = new PrintStream(baos, true)) {
         setSlf4jTargetStream(Class.forName("com.zaxxer.hikari.pool.ProxyLeakTask"), ps);
         setConfigUnitTest(true);

         HikariConfig config = newHikariConfig();
         config.setMinimumIdle(0);
         config.setMaximumPoolSize(4);
         config.setThreadFactory(Executors.defaultThreadFactory());
         config.setMetricRegistry(null);
         config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(1));
         config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         try (HikariDataSource ds = new HikariDataSource(config)) {
            setSlf4jLogLevel(HikariPool.class, Level.DEBUG);
            getPool(ds).logPoolState();

            try (Connection connection = ds.getConnection()) {
               quietlySleep(SECONDS.toMillis(4));
               connection.close();
               quietlySleep(SECONDS.toMillis(1));
               ps.close();
               String s = new String(baos.toByteArray());
               assertNotNull("Exception string was null", s);
               assertTrue("Expected exception to contain 'Connection leak detection' but contains *" + s + "*", s.contains("Connection leak detection"));
            }
         }
         finally
         {
            setConfigUnitTest(false);
            setSlf4jLogLevel(HikariPool.class, Level.INFO);
         }
      }
   }
}
