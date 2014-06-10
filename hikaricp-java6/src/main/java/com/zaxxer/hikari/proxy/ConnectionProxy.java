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

package com.zaxxer.hikari.proxy;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.FastList;

/**
 * This is the proxy class for java.sql.Connection.
 *
 * @author Brett Wooldridge
 */
public abstract class ConnectionProxy implements IHikariConnectionProxy
{
   private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxy.class);

   private static final Set<String> SQL_ERRORS;

   protected final Connection delegate;

   private final FastList<Statement> openStatements;
   private final HikariPool parentPool;
   private final AtomicInteger state;
   private final String defaultCatalog;
   private final long expirationTime;
   private final int defaultIsolationLevel;
   private final boolean defaultAutoCommit;
   private final boolean defaultReadOnly;

   private boolean forceClose;
   private boolean isAutoCommitDirty;
   private boolean isCatalogDirty;
   private boolean isClosed;
   private boolean isReadOnlyDirty;
   private boolean isTransactionIsolationDirty;
   private volatile long lastAccess;
   private long uncloseTime;

   private TimerTask leakTask;

   private final int hashCode;

   // static initializer
   static {
      SQL_ERRORS = new HashSet<String>();
      SQL_ERRORS.add("57P01"); // ADMIN SHUTDOWN
      SQL_ERRORS.add("57P02"); // CRASH SHUTDOWN
      SQL_ERRORS.add("57P03"); // CANNOT CONNECT NOW
      SQL_ERRORS.add("01002"); // SQL92 disconnect error
      SQL_ERRORS.add("JZ0C0"); // Sybase disconnect error
      SQL_ERRORS.add("JZ0C1"); // Sybase disconnect error
   }

   protected ConnectionProxy(HikariPool pool, Connection connection, long maxLifetime, int defaultIsolationLevel, boolean defaultAutoCommit,
         boolean defaultReadOnly, String defaultCatalog)
   {
      this.parentPool = pool;
      this.delegate = connection;
      this.defaultIsolationLevel = defaultIsolationLevel;
      this.defaultAutoCommit = defaultAutoCommit;
      this.defaultReadOnly = defaultReadOnly;
      this.defaultCatalog = defaultCatalog;
      this.state = new AtomicInteger();

      long now = System.currentTimeMillis();
      this.expirationTime = (maxLifetime > 0 ? now + maxLifetime : Long.MAX_VALUE);
      this.lastAccess = now;
      this.openStatements = new FastList<Statement>(Statement.class);
      this.hashCode = System.identityHashCode(this);

      isCatalogDirty = true;
      isReadOnlyDirty = defaultReadOnly;
      isAutoCommitDirty = true;
      isTransactionIsolationDirty = true;
   }

   /** {@inheritDoc} */
   @Override
   public final boolean equals(Object other)
   {
      return this == other;
   }

   /** {@inheritDoc} */
   @Override
   public final int hashCode()
   {
      return hashCode;
   }

   // ***********************************************************************
   //                      IHikariConnectionProxy methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public final void captureStack(long leakDetectionThreshold, Timer scheduler)
   {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      StackTraceElement[] leakTrace = new StackTraceElement[trace.length - 4];
      System.arraycopy(trace, 4, leakTrace, 0, leakTrace.length);

      leakTask = new LeakTask(leakTrace, leakDetectionThreshold);
      scheduler.schedule(leakTask, leakDetectionThreshold);
   }

   /** {@inheritDoc} */
   @Override
   public final void checkException(SQLException sqle)
   {
      String sqlState = sqle.getSQLState();
      if (sqlState != null) {
         forceClose |= sqlState.startsWith("08") | SQL_ERRORS.contains(sqlState);
         if (forceClose) {
            LOGGER.warn(String.format("Connection %s (%s) marked as broken because of SQLSTATE(%s), ErrorCode(%d).", delegate.toString(),
                                      parentPool.toString(), sqlState, sqle.getErrorCode()), sqle);
         }
         else if (sqle.getNextException() instanceof SQLException) {
            checkException(sqle.getNextException());
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public final long getExpirationTime()
   {
      return expirationTime;
   }

   /** {@inheritDoc} */
   @Override
   public final long getLastAccess()
   {
      return lastAccess;
   }

   /** {@inheritDoc} */
   @Override
   public long getLastOpenTime()
   {
      return uncloseTime;
   }

   /** {@inheritDoc} */
   @Override
   public final boolean isBrokenConnection()
   {
      return forceClose;
   }

   /** {@inheritDoc} */
   @Override
   public final void realClose() throws SQLException
   {
      delegate.close();
   }

   /** {@inheritDoc} */
   @Override
   public final void resetConnectionState() throws SQLException
   {
      if (!delegate.getAutoCommit()) {
         delegate.rollback();
      }

      if (isReadOnlyDirty) {
         delegate.setReadOnly(defaultReadOnly);
         isReadOnlyDirty = false;
      }

      if (isAutoCommitDirty) {
         delegate.setAutoCommit(defaultAutoCommit);
         isAutoCommitDirty = false;
      }

      if (isTransactionIsolationDirty) {
         delegate.setTransactionIsolation(defaultIsolationLevel);
         isTransactionIsolationDirty = false;
      }

      if (isCatalogDirty && defaultCatalog != null) {
         delegate.setCatalog(defaultCatalog);
         isCatalogDirty = false;
      }

      delegate.clearWarnings();
   }

   /** {@inheritDoc} */
   @Override
   public final void unclose(final long now)
   {
      isClosed = false;
      uncloseTime = now;
   }

   /** {@inheritDoc} */
   @Override
   public final void untrackStatement(Statement statement)
   {
      // If the connection is not closed.  If it is closed, it means this is being
      // called back as a result of the close() method below in which case we
      // will clear the openStatements collection en mass.
      if (!isClosed) {
         openStatements.remove(statement);
      }
   }

   // ***********************************************************************
   //                        Internal methods
   // ***********************************************************************

   protected final void checkClosed() throws SQLException
   {
      if (isClosed) {
         throw new SQLException("Connection is closed");
      }
   }

   private <T extends Statement> T trackStatement(T statement)
   {
      openStatements.add(statement);

      return statement;
   }

   // **********************************************************************
   //                       IBagManagable Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public final int getState()
   {
      return state.get();
   }

   /** {@inheritDoc} */
   @Override
   public final boolean compareAndSetState(int expectedState, int newState)
   {
      return state.compareAndSet(expectedState, newState);
   }

   // **********************************************************************
   //                   "Overridden" java.sql.Connection Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public final void close() throws SQLException
   {
      if (!isClosed) {
         isClosed = true;

         if (leakTask != null) {
            leakTask.cancel();
            leakTask = null;
         }

         try {
            final int size = openStatements.size();
            if (size > 0) {
               for (int i = 0; i < size; i++) {
                  try {
                     openStatements.get(i).close();
                  }
                  catch (SQLException e) {
                     checkException(e);
                  }
               }

               openStatements.clear();
            }

            resetConnectionState();
         }
         catch (SQLException e) {
            checkException(e);
            throw e;
         }
         finally {
            lastAccess = System.currentTimeMillis();
            parentPool.releaseConnection(this, forceClose);
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public final boolean isClosed() throws SQLException
   {
      return isClosed;
   }

   /** {@inheritDoc} */
   @Override
   public final Statement createStatement() throws SQLException
   {
      checkClosed();
      try {
         Statement proxyStatement = ProxyFactory.getProxyStatement(this, delegate.createStatement());
         return trackStatement(proxyStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
   {
      checkClosed();
      try {
         Statement proxyStatement = ProxyFactory.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency));
         return trackStatement(proxyStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
   {
      checkClosed();
      try {
         Statement proxyStatement = ProxyFactory.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
         return trackStatement(proxyStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final CallableStatement prepareCall(String sql) throws SQLException
   {
      checkClosed();
      try {
         CallableStatement proxyCallableStatement = ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql));
         return trackStatement(proxyCallableStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
   {
      checkClosed();
      try {
         CallableStatement proxyCallableStatement = ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency));
         return trackStatement(proxyCallableStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
   {
      checkClosed();
      try {
         CallableStatement proxyCallableStatement = ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency,
                                                                                                                      resultSetHoldability));
         return trackStatement(proxyCallableStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final PreparedStatement prepareStatement(String sql) throws SQLException
   {
      checkClosed();
      try {
         PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql));
         return trackStatement(proxyPreparedStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
   {
      checkClosed();
      try {
         PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, autoGeneratedKeys));
         return trackStatement(proxyPreparedStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
   {
      checkClosed();
      try {
         PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this,
                                                                                           delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
         return trackStatement(proxyPreparedStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
   {
      checkClosed();
      try {
         PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType,
                                                                                                                           resultSetConcurrency,
                                                                                                                           resultSetHoldability));
         return trackStatement(proxyPreparedStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
   {
      checkClosed();
      try {
         PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnIndexes));
         return trackStatement(proxyPreparedStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
   {
      checkClosed();
      try {
         PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
         return trackStatement(proxyPreparedStatement);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final boolean isValid(int timeout) throws SQLException
   {
      if (isClosed) {
         return false;
      }

      try {
         return delegate.isValid(timeout);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final void setAutoCommit(boolean autoCommit) throws SQLException
   {
      checkClosed();
      try {
         delegate.setAutoCommit(autoCommit);
         isAutoCommitDirty = (autoCommit != defaultAutoCommit);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final void setReadOnly(boolean readOnly) throws SQLException
   {
      checkClosed();
      try {
         delegate.setReadOnly(readOnly);
         isReadOnlyDirty = (readOnly != defaultReadOnly);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final void setTransactionIsolation(int level) throws SQLException
   {
      checkClosed();
      try {
         delegate.setTransactionIsolation(level);
         isTransactionIsolationDirty = (level != defaultIsolationLevel);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   @Override
   public final void setCatalog(String catalog) throws SQLException
   {
      checkClosed();
      try {
         delegate.setCatalog(catalog);
         isCatalogDirty = !catalog.equals(defaultCatalog);
      }
      catch (SQLException e) {
         checkException(e);
         throw e;
      }
   }

   /** {@inheritDoc} */
   @Override
   public final boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      return iface.isInstance(delegate);
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public final <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(delegate)) {
         return (T) delegate;
      }

      throw new SQLException("Wrapped connection is not an instance of " + iface);
   }
}
