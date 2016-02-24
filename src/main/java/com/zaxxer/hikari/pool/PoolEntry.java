/*
 * Copyright (C) 2014 Brett Wooldridge
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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry;
import com.zaxxer.hikari.util.FastList;

/**
 * Entry used in the ConcurrentBag to track Connection instances.
 *
 * @author Brett Wooldridge
 */
final class PoolEntry implements IConcurrentBagEntry
{
   private static final Logger LOGGER = LoggerFactory.getLogger(PoolEntry.class);

   static final Comparator<PoolEntry> LASTACCESS_COMPARABLE;

   Connection connection;
   long lastAccessed;
   long lastBorrowed;
   private volatile boolean evict;

   private volatile ScheduledFuture<?> endOfLife;

   private final FastList<Statement> openStatements;
   private final HikariPool hikariPool;
   private final AtomicInteger state;

   private final boolean isReadOnly;
   private final boolean isAutoCommit;

   static
   {
      LASTACCESS_COMPARABLE = new Comparator<PoolEntry>() {
         @Override
         public int compare(final PoolEntry entryOne, final PoolEntry entryTwo) {
            return Long.compare(entryOne.lastAccessed, entryTwo.lastAccessed);
         }
      };
   }

   PoolEntry(final Connection connection, final PoolBase pool, final boolean isReadOnly, final boolean isAutoCommit)
   {
      this.connection = connection;
      this.hikariPool = (HikariPool) pool;
      this.isReadOnly = isReadOnly;
      this.isAutoCommit = isAutoCommit;
      this.state = new AtomicInteger();
      this.lastAccessed = ClockSource.INSTANCE.currentTime();
      this.openStatements = new FastList<>(Statement.class, 16);
   }

   /**
    * Release this entry back to the pool.
    *
    * @param lastAccessed last access time-stamp
    */
   void recycle(final long lastAccessed)
   {
      this.lastAccessed = lastAccessed;
      hikariPool.releaseConnection(this);
   }

   /**
    * @param endOfLife
    */
   void setFutureEol(final ScheduledFuture<?> endOfLife)
   {
      this.endOfLife = endOfLife;
   }

   Connection createProxyConnection(final ProxyLeakTask leakTask, final long now)
   {
      return ProxyFactory.getProxyConnection(this, connection, openStatements, leakTask, now, isReadOnly, isAutoCommit);
   }

   void resetConnectionState(final ProxyConnection proxyConnection, final int dirtyBits) throws SQLException
   {
      hikariPool.resetConnectionState(connection, proxyConnection, dirtyBits);
   }

   String getPoolName()
   {
      return hikariPool.toString();
   }

   boolean isMarkedEvicted()
   {
      return evict;
   }

   void markEvicted()
   {
      this.evict = true;
   }

   void evict(final String closureReason)
   {
      hikariPool.closeConnection(this, closureReason);
   }

   /** Returns millis since lastBorrowed */
   long getMillisSinceBorrowed()
   {
      return ClockSource.INSTANCE.elapsedMillis(lastBorrowed);
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      final long now = ClockSource.INSTANCE.currentTime();
      return connection
         + ", accessed " + ClockSource.INSTANCE.elapsedDisplayString(lastAccessed, now) + " ago, "
         + stateToString();
   }

   // ***********************************************************************
   //                      IConcurrentBagEntry methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public int getState()
   {
      return state.get();
   }

   /** {@inheritDoc} */
   @Override
   public boolean compareAndSet(int expect, int update)
   {
      return state.compareAndSet(expect, update);
   }

   /** {@inheritDoc} */
   @Override
   public void lazySet(int update)
   {
      state.lazySet(update);
   }

   Connection close()
   {
      ScheduledFuture<?> eol = endOfLife;
      if (eol != null && !eol.isDone() && !eol.cancel(false)) {
         LOGGER.warn("{} - maxLifeTime expiration task cancellation unexpectedly returned false for connection {}", getPoolName(), connection);
      }

      Connection con = connection;
      connection = null;
      endOfLife = null;
      return con;
   }

   private String stateToString()
   {
      switch (state.get()) {
      case STATE_IN_USE:
         return "IN_USE";
      case STATE_NOT_IN_USE:
         return "NOT_IN_USE";
      case STATE_REMOVED:
         return "REMOVED";
      case STATE_RESERVED:
         return "RESERVED";
      default:
         return "Invalid";
      }
   }
}
