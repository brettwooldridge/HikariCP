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

import static com.zaxxer.hikari.util.UtilityElf.IS_JAVA7;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.PoolBagEntry;
import com.zaxxer.hikari.util.ConcurrentBag;
import com.zaxxer.hikari.util.IConcurrentBagEntry;
import com.zaxxer.hikari.util.Java8ConcurrentBag;

/**
 *
 * @author Brett Wooldridge
 */
public class TestConcurrentBag
{
   private static HikariDataSource ds;

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
   }

   @AfterClass
   public static void teardown()
   {
      ds.close();
   }

   @Test
   public void testConcurrentBag() throws InterruptedException
   {
      ConcurrentBag<PoolBagEntry> bag = new Java8ConcurrentBag(null);
      Assert.assertEquals(0, bag.values(8).size());

      HikariPool pool = TestElf.getPool(ds);
      PoolBagEntry reserved = new PoolBagEntry(null, TestElf.getPool(ds));
      bag.add(reserved);
      bag.reserve(reserved);      // reserved

      PoolBagEntry inuse = new PoolBagEntry(null, pool);
      bag.add(inuse);
      bag.borrow(2L, TimeUnit.SECONDS); // in use
      
      PoolBagEntry notinuse = new PoolBagEntry(null, pool);
      bag.add(notinuse); // not in use
      
      bag.dumpState();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(Java8ConcurrentBag.class, ps);
      
      bag.requite(reserved);
      Assert.assertTrue(new String(baos.toByteArray()).contains("does not exist"));

      bag.remove(notinuse);
      Assert.assertTrue(new String(baos.toByteArray()).contains("not borrowed or reserved"));

      bag.unreserve(notinuse);
      Assert.assertTrue(new String(baos.toByteArray()).contains("was not reserved"));

      bag.remove(inuse);
      bag.remove(inuse);
      Assert.assertTrue(new String(baos.toByteArray()).contains("not borrowed or reserved"));

      bag.close();
      try {
         PoolBagEntry bagEntry = new PoolBagEntry(null, pool);
         bag.add(bagEntry);
         Assert.assertNotEquals(bagEntry, bag.borrow(100, TimeUnit.MILLISECONDS));
      }
      catch (IllegalStateException e) {
         Assert.assertTrue(new String(baos.toByteArray()).contains("ignoring add()"));
      }

      Assert.assertNotNull(notinuse.toString());
   }

   @Test
   public void testConcurrentBag2() throws InterruptedException
   {
      ConcurrentBag<PoolBagEntry> bag = new FauxJava6ConcurrentBag();
      Assert.assertEquals(0, bag.values(IConcurrentBagEntry.STATE_IN_USE).size());
      Assert.assertEquals(0, bag.getCount(IConcurrentBagEntry.STATE_IN_USE));
   }

   private static class FauxJava6ConcurrentBag extends ConcurrentBag<PoolBagEntry>
   {
      /**
       * @param listener
       */
      public FauxJava6ConcurrentBag() {
         super(null);
      }

      @Override
      protected AbstractQueuedLongSynchronizer createQueuedSynchronizer()
      {
         return new Synchronizer();
      }
   }

   /**
    * Our private synchronizer that handles notify/wait type semantics.
    */
   private static final class Synchronizer extends AbstractQueuedLongSynchronizer
   {
      private static final long serialVersionUID = 104753538004341218L;

      @Override
      protected long tryAcquireShared(long seq)
      {
         return getState() > seq && !java67hasQueuedPredecessors() ? 1L : -1L;
      }

      /** {@inheritDoc} */
      @Override
      protected boolean tryReleaseShared(long updateSeq)
      {
         setState(updateSeq);

         return true;
      }

      private boolean java67hasQueuedPredecessors()
      {
         if (IS_JAVA7) {
            return hasQueuedPredecessors();
         }

         return false;
      }
   }
}
