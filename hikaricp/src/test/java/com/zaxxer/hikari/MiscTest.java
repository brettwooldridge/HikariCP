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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import org.slf4j.spi.LocationAwareLogger;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.PoolBagEntry;
import com.zaxxer.hikari.util.ConcurrentBag;
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
      config.setMinimumIdle(0);
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
   public void testConcurrentBag() throws InterruptedException
   {
      ConcurrentBag<PoolBagEntry> bag = new ConcurrentBag<PoolBagEntry>(null);
      Assert.assertEquals(0, bag.values(8).size());

      PoolBagEntry reserved = new PoolBagEntry(null, 0);
      bag.add(reserved);
      bag.reserve(reserved);      // reserved

      PoolBagEntry inuse = new PoolBagEntry(null, 0);
      bag.add(inuse);
      bag.borrow(2L, TimeUnit.SECONDS); // in use
      
      PoolBagEntry notinuse = new PoolBagEntry(null, 0);
      bag.add(notinuse); // not in use
      
      bag.dumpState();

      try {
         bag.requite(reserved);
         Assert.fail();
      }
      catch (IllegalStateException e) {
         // pass
      }

      try {
         bag.remove(notinuse);
         Assert.fail();
      }
      catch (IllegalStateException e) {
         // pass
      }

      try {
         bag.unreserve(notinuse);
         Assert.fail();
      }
      catch (IllegalStateException e) {
         // pass
      }

      try {
         bag.remove(inuse);
         bag.remove(inuse);
         Assert.fail();
      }
      catch (IllegalStateException e) {
         // pass
      }

      bag.close();
      try {
         bag.add(new PoolBagEntry(null, 0));
         Assert.fail();
      }
      catch (IllegalStateException e) {
         // pass
      }

      Assert.assertNotNull(notinuse.toString());
   }

   @Test
   public void testLeakDetection() throws SQLException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(LeakTask.class, ps);

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(4);
      config.setPoolName("test");
      config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(1));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      TestElf.setConfigUnitTest();

      final HikariDataSource ds = new HikariDataSource(config);
      try {
         TestElf.setSlf4jLogLevel(HikariPool.class, LocationAwareLogger.DEBUG_INT);
         TestElf.getPool(ds).logPoolState();

         Connection connection = ds.getConnection();
         PoolUtilities.quietlySleep(TimeUnit.SECONDS.toMillis(4));
         connection.close();
         PoolUtilities.quietlySleep(TimeUnit.SECONDS.toMillis(1));
         ps.close();
         String s = new String(baos.toByteArray());
         Assert.assertNotNull("Exception string was null", s);
         Assert.assertTrue("Expected exception to contain 'Connection leak detection' but contains *" + s + "*", s.contains("Connection leak detection"));
      }
      finally
      {
         ds.close();
      }
   }
}
