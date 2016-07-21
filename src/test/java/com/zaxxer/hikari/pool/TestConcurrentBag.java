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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener;

/**
 *
 * @author Brett Wooldridge
 */
public class TestConcurrentBag
{
   private static HikariDataSource ds;
   private static HikariPool pool;

   @BeforeClass
   public static void setup()
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(2);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      ds = new HikariDataSource(config);
      pool = TestElf.getPool(ds);
   }

   @AfterClass
   public static void teardown()
   {
      ds.close();
   }

   @Test
   public void testConcurrentBag() throws Exception
   {
      ConcurrentBag<PoolEntry> bag = new ConcurrentBag<>(new IBagStateListener() {
         @Override
         public Future<Boolean> addBagItem()
         {
            return null;
         }
      });
      Assert.assertEquals(0, bag.values(8).size());

      PoolEntry reserved = pool.newPoolEntry();
      bag.add(reserved);
      bag.reserve(reserved);      // reserved

      PoolEntry inuse = pool.newPoolEntry();
      bag.add(inuse);
      bag.borrow(2, TimeUnit.MILLISECONDS); // in use

      PoolEntry notinuse = pool.newPoolEntry();
      bag.add(notinuse); // not in use

      bag.dumpState();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(ConcurrentBag.class, ps);

      bag.requite(reserved);

      bag.remove(notinuse);
      Assert.assertTrue(new String(baos.toByteArray()).contains("not borrowed or reserved"));

      bag.unreserve(notinuse);
      Assert.assertTrue(new String(baos.toByteArray()).contains("was not reserved"));

      bag.remove(inuse);
      bag.remove(inuse);
      Assert.assertTrue(new String(baos.toByteArray()).contains("not borrowed or reserved"));

      bag.close();
      try {
         PoolEntry bagEntry = pool.newPoolEntry();
         bag.add(bagEntry);
         Assert.assertNotEquals(bagEntry, bag.borrow(100, TimeUnit.MILLISECONDS));
      }
      catch (IllegalStateException e) {
         Assert.assertTrue(new String(baos.toByteArray()).contains("ignoring add()"));
      }

      Assert.assertNotNull(notinuse.toString());
   }
}
