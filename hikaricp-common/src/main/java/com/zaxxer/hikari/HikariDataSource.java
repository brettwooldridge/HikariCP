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

import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.HashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.util.DriverDataSource;

/**
 * The HikariCP pooled DataSource.
 *
 * @author Brett Wooldridge
 */
public class HikariDataSource extends HikariConfig implements DataSource, Closeable
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HikariDataSource.class);

   // We use a concrete HashMap rather than Map to avoid an invokeinterface callsite
   private final HashMap<MultiPoolKey, HikariPool> multiPool;

   private volatile boolean isShutdown;

   private final HikariPool fastPathPool;
   private volatile HikariPool pool;

   /**
    * Default constructor.  Setters be used to configure the pool.  Using
    * this constructor vs. {@link #HikariDataSource(HikariConfig)} will
    * result in {@link #getConnection()} performance that is slightly lower
    * due to lazy initialization checks.
    */
   public HikariDataSource()
   {
      super();
      fastPathPool = null;
      multiPool = new HashMap<MultiPoolKey, HikariPool>();
   }

   /**
    * Construct a HikariDataSource with the specified configuration.
    *
    * @param configuration a HikariConfig instance
    */
   public HikariDataSource(HikariConfig configuration)
   {
      configuration.validate();
      configuration.copyState(this);
      multiPool = new HashMap<MultiPoolKey, HikariPool>();

      LOGGER.info("HikariCP pool {} is starting.", configuration.getPoolName());
      pool = fastPathPool = new HikariPool(this);
      multiPool.put(new MultiPoolKey(getUsername(), getPassword()), pool);
   }

   /** {@inheritDoc} */
   @Override
   public Connection getConnection() throws SQLException
   {
      if (isShutdown) {
         throw new SQLException("Pool has been shutdown");
      }

      if (fastPathPool != null) {
         return fastPathPool.getConnection();
      }

      // See http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
      HikariPool result = pool;
      if (result == null) {
         synchronized (this) {
            result = pool;
            if (result == null) {
               validate();
               LOGGER.info("HikariCP pool {} is starting.", getPoolName());
               pool = result = new HikariPool(this);
               multiPool.put(new MultiPoolKey(getUsername(), getPassword()), pool);
            }
         }
      }

      return result.getConnection();
   }

   /** {@inheritDoc} */
   @Override
   public Connection getConnection(String username, String password) throws SQLException
   {
      if (isShutdown) {
         throw new SQLException("Pool has been shutdown");
      }

      final MultiPoolKey key = new MultiPoolKey(username, password);

      HikariPool hikariPool;
      synchronized (multiPool) {
         hikariPool = multiPool.get(key);
         if (hikariPool == null) {
            hikariPool = new HikariPool(this, username, password);
            multiPool.put(key, hikariPool);
         }
      }

      return hikariPool.getConnection();
   }

   /** {@inheritDoc} */
   @Override
   public PrintWriter getLogWriter() throws SQLException
   {
      return (pool.getDataSource() != null ? pool.getDataSource().getLogWriter() : null);
   }

   /** {@inheritDoc} */
   @Override
   public void setLogWriter(PrintWriter out) throws SQLException
   {
      if (pool.getDataSource() != null) {
         pool.getDataSource().setLogWriter(out);
      }
   }

   /** {@inheritDoc} */
   @Override
   public void setLoginTimeout(int seconds) throws SQLException
   {
      for (HikariPool hikariPool : multiPool.values()) {
         hikariPool.getDataSource().setLoginTimeout(seconds);
      }
   }

   /** {@inheritDoc} */
   @Override
   public int getLoginTimeout() throws SQLException
   {
      HikariPool hikariPool = multiPool.values().iterator().next();
      if (hikariPool != null) {
         return hikariPool.getDataSource().getLoginTimeout();
      }

      return 0;
   }

   /** {@inheritDoc} */
   public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
   {
      throw new SQLFeatureNotSupportedException();
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(this)) {
         return (T) this;
      }
      else if (pool != null) {
         if (iface.isInstance(pool.getDataSource())) {
            return (T) pool.getDataSource();
         }
         else if (pool.getDataSource() instanceof Wrapper) {
            return (T) pool.getDataSource().unwrap(iface);
         }
      }

      throw new SQLException("Wrapped DataSource is not an instance of " + iface);
   }

   /** {@inheritDoc} */
   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      if (iface.isInstance(this)) {
         return true;
      }
      else if (pool != null) {
         if (iface.isInstance(pool.getDataSource())) {
            return true;
         }
         else if (pool.getDataSource() instanceof Wrapper) {
            return pool.getDataSource().isWrapperFor(iface);
         }
      }

      return false;
   }

   /**
    * Evict a connection from the pool.  Use caution using this method, if you
    * evict the same connection more than one time, the internal pool accounting
    * will become invalid and the pool may stop functioning.
    *
    * @param connection the connection to evict from the pool
    */
   public void evictConnection(Connection connection)
   {
      if (!isShutdown && pool != null && connection instanceof IHikariConnectionProxy) {
         pool.evictConnection((IHikariConnectionProxy) connection);
      }
   }

   /**
    * <code>close()</code> and <code>shutdown()</code> are synonymous.
    */
   public void close()
   {
      shutdown();
   }

   /**
    * Shutdown the DataSource and its associated pool.
    */
   public void shutdown()
   {
      if (isShutdown) {
         return;
      }

      isShutdown = true;

      if (fastPathPool != null) {
         shutdownHelper(fastPathPool);
      }

      for (HikariPool hikariPool : multiPool.values()) {
         shutdownHelper(hikariPool);
      }
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return String.format("HikariDataSource (%s)", pool);
   }

   private void shutdownHelper(HikariPool hPool)
   {
      try {
         hPool.shutdown();
      }
      catch (InterruptedException e) {
         LoggerFactory.getLogger(getClass()).warn("Interrupted during shutdown", e);
      }

      if (hPool.getDataSource() instanceof DriverDataSource) {
         ((DriverDataSource) hPool.getDataSource()).shutdown();
      }
   }

   private static class MultiPoolKey
   {
      private String username;
      private String password;

      MultiPoolKey(String username, String password)
      {
         this.username = username;
         this.password = password;
      }

      @Override
      public int hashCode()
      {
         return (password == null ? 0 : password.hashCode());
      }

      @Override
      public boolean equals(Object obj)
      {
         MultiPoolKey otherKey = ((MultiPoolKey) obj);
         if (username != null && !username.equals(otherKey.username)) {
            return false;
         }
         else if (username == null && otherKey.username != null) {
            return false;
         }
         else if (password != null && !password.equals(otherKey.password)) {
            return false;
         }
         else if (password == null && otherKey.password != null) {
            return false;
         }

         return true;
      }
   }
}
