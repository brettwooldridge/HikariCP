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

package com.zaxxer.hikari;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.BagEntry;
import com.zaxxer.hikari.util.LeakTask;
import com.zaxxer.hikari.util.PoolUtilities;

/**
 * @author Brett Wooldridge
 */
public class MiscTest
{
   @Test
   public void testLogWriter() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(4);
      config.setPoolName("test");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      TestElf.setConfigUnitTest();

      final HikariDataSource ds = new HikariDataSource(config);
      try {
         PrintWriter writer = new PrintWriter(System.out);
         ds.setLogWriter(writer);
         Assert.assertSame(writer, ds.getLogWriter());
         Assert.assertEquals("test", config.getPoolName());
      }
      finally
      {
         ds.close();
      }
   }

   @Test
   public void testInvalidIsolation()
   {
      try {
         PoolUtilities.getTransactionIsolation("INVALID");
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
         PoolUtilities.createInstance("invalid", null);
         Assert.fail();
      }
      catch (RuntimeException e) {
         Assert.assertTrue(e.getCause() instanceof ClassNotFoundException);
      }
   }

   @Test
   public void testBagDump()
   {
      ConcurrentBag<BagEntry> bag = new ConcurrentBag<BagEntry>(null);
      bag.dumpState();
   }

   @Test
   public void testLeakDetection() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(4);
      config.setPoolName("test");
      config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(1));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      TestElf.setConfigUnitTest();

      final HikariDataSource ds = new HikariDataSource(config);
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         PrintStream ps = new PrintStream(baos, true);
         TestElf.setSlf4jTargetStream(LeakTask.class, ps);

         Connection connection = ds.getConnection();
         PoolUtilities.quietlySleep(TimeUnit.SECONDS.toMillis(3));
         ps.close();
         connection.close();
         String s = new String(baos.toByteArray());
         Assert.assertTrue(s.contains("Connection leak detection"));
      }
      finally
      {
         ds.close();
      }
   }
}
