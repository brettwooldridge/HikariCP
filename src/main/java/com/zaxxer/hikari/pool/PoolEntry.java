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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.pool.Mediators.PoolEntryMediator;
import com.zaxxer.hikari.proxy.ConnectionState;
import com.zaxxer.hikari.proxy.ProxyFactory;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry;
import com.zaxxer.hikari.util.FastList;

/**
 * Entry used in the ConcurrentBag to track Connection instances.
 *
 * @author Brett Wooldridge
 */
public final class PoolEntry implements IConcurrentBagEntry
{
   private static final Logger LOGGER;
   private static final SimpleDateFormat DATE_FORMAT;

   public final long creationTime;

   public Connection connection;
   public long lastAccess;

   public volatile long lastOpenTime;
   public volatile boolean evict;

   private final FastList<Statement> openStatements;
   private final HikariPool hikariPool;
   private final PoolEntryMediator stateMediator;
   private final AtomicInteger state;

   private volatile ScheduledFuture<?> endOfLife;

   static
   {
      LOGGER = LoggerFactory.getLogger(PoolEntry.class);
      DATE_FORMAT = new SimpleDateFormat("MMM dd, HH:mm:ss.SSS");
   }

   PoolEntry(final Connection connection, final HikariPool pool, final PoolEntryMediator stateMediator)
   {
      this.connection = connection;
      this.hikariPool = pool;
      this.creationTime = System.currentTimeMillis();
      this.state = new AtomicInteger(STATE_NOT_IN_USE);
      this.lastAccess = ClockSource.INSTANCE.currentTime();
      this.openStatements = new FastList<>(Statement.class, 16);
      this.stateMediator = stateMediator;
   }

   /**
    * Release this entry back to the pool.
    *
    * @param lastAccess last access time-stamp
    */
   public void returnPoolEntry(final long lastAccess)
   {
      this.lastAccess = lastAccess;
      hikariPool.releaseConnection(this);
   }

   /**
    * @param endOfLife
    */
   public void setFutureEol(final ScheduledFuture<?> endOfLife)
   {
      this.endOfLife = endOfLife;
   }

   Connection createProxyConnection(final LeakTask leakTask, final long now)
   {
      return ProxyFactory.getProxyConnection(this, connection, openStatements, leakTask, now);
   }
   // ***********************************************************************
   //                      ConnectionState methods
   // ***********************************************************************

   public void resetConnectionState(final ConnectionState connectionState, final int dirtyBits) throws SQLException
   {
      stateMediator.resetConnectionState(connection, connectionState, dirtyBits);
   }

   public String getPoolName()
   {
      return hikariPool.config.getPoolName();
   }

   public Connection getConnection()
   {
      return connection;
   }

   public long getLastAccess()
   {
      return lastAccess;
   }

   public boolean isEvicted()
   {
      return evict;
   }

   public void evict()
   {
      this.evict = true;
   }

   public FastList<Statement> getStatementsList()
   {
      return openStatements;
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
   public String toString()
   {
      return connection
         + ", created " + formatDateTime(creationTime)
         + ", last release " + ClockSource.INSTANCE.elapsedMillis(lastAccess) + "ms ago, "
         + stateToString();
   }

   void close()
   {
      if (endOfLife != null && !endOfLife.isDone() && !endOfLife.cancel(false)) {
         LOGGER.warn("{} - maxLifeTime expiration task cancellation unexpectedly returned false for connection {}", getPoolName(), connection);
      }

      endOfLife = null;
      connection = null;
   }

   private static synchronized String formatDateTime(final long timestamp)
   {
      return DATE_FORMAT.format(new Date(timestamp));
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
