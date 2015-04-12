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
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.LeakTask;
import com.zaxxer.hikari.pool.PoolBagEntry;
import com.zaxxer.hikari.util.FastList;

/**
 * This is the proxy class for java.sql.Connection.
 *
 * @author Brett Wooldridge
 */
public abstract class ConnectionProxy implements IHikariConnectionProxy
{
   private static final Logger LOGGER;
   private static final Set<String> SQL_ERRORS;

   protected Connection delegate;

   private final LeakTask leakTask;
   private final HikariPool parentPool;
   private final PoolBagEntry bagEntry;
   private final FastList<Statement> openStatements;
   
   private long lastAccess;
   private boolean isCommitStateDirty;
   private boolean isConnectionStateDirty;
   private boolean isAutoCommitDirty;
   private boolean isCatalogDirty;
   private boolean isReadOnlyDirty;
   private boolean isTransactionIsolationDirty;

   // static initializer
   static {
      LOGGER = LoggerFactory.getLogger(ConnectionProxy.class);

      SQL_ERRORS = new HashSet<String>();
      SQL_ERRORS.add("57P01"); // ADMIN SHUTDOWN
      SQL_ERRORS.add("57P02"); // CRASH SHUTDOWN
      SQL_ERRORS.add("57P03"); // CANNOT CONNECT NOW
      SQL_ERRORS.add("01002"); // SQL92 disconnect error
      SQL_ERRORS.add("JZ0C0"); // Sybase disconnect error
      SQL_ERRORS.add("JZ0C1"); // Sybase disconnect error
   }

   protected ConnectionProxy(final HikariPool pool, final PoolBagEntry bagEntry, final LeakTask leakTask) {
      this.parentPool = pool;
      this.bagEntry = bagEntry;
      this.delegate = bagEntry.connection;
      this.leakTask = leakTask;
      this.lastAccess = bagEntry.lastAccess;

      this.openStatements = new FastList<Statement>(Statement.class, 16);
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return new StringBuilder(64)
         .append(this.getClass().getSimpleName()).append('@').append(System.identityHashCode(this))
         .append(" wrapping ")
         .append(delegate).toString();
   }

   // ***********************************************************************
   //                      IHikariConnectionProxy methods
   // ***********************************************************************

   /** {@inheritDoc} */
   @Override
   public final PoolBagEntry getPoolBagEntry()
   {
      return bagEntry;
   }

   /** {@inheritDoc} */
   @Override
   public final SQLException checkException(final SQLException sqle)
   {
      String sqlState = sqle.getSQLState();
      if (sqlState != null) {
         boolean isForceClose = sqlState.startsWith("08") | SQL_ERRORS.contains(sqlState);
         if (isForceClose) {
            bagEntry.evicted = true;
            LOGGER.warn("Connection {} ({}) marked as broken because of SQLSTATE({}), ErrorCode({}).", delegate.toString(),
                                      parentPool.toString(), sqlState, sqle.getErrorCode(), sqle);
         }
         else {
            SQLException nse = sqle.getNextException();
            if (nse != null && nse != sqle) {
               checkException(nse);
            }
         }
      }
      return sqle;
   }

   /** {@inheritDoc} */
   @Override
   public final void untrackStatement(final Statement statement)
   {
      openStatements.remove(statement);
   }

   /** {@inheritDoc} */
   @Override
   public final void markCommitStateDirty()
   {
      isCommitStateDirty = true;
      lastAccess = System.currentTimeMillis();
   }

   // ***********************************************************************
   //                        Internal methods
   // ***********************************************************************

   private final <T extends Statement> T trackStatement(final T statement)
   {
      lastAccess = System.currentTimeMillis();
      openStatements.add(statement);

      return statement;
   }

   private final void resetConnectionState() throws SQLException
   {
      if (isReadOnlyDirty) {
         delegate.setReadOnly(parentPool.isReadOnly);
      }

      if (isAutoCommitDirty) {
         delegate.setAutoCommit(parentPool.isAutoCommit);
      }

      if (isTransactionIsolationDirty) {
         delegate.setTransactionIsolation(parentPool.transactionIsolation);
      }

      if (isCatalogDirty && parentPool.catalog != null) {
         delegate.setCatalog(parentPool.catalog);
      }
   }

   // **********************************************************************
   //                   "Overridden" java.sql.Connection Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public final void close() throws SQLException
   {
      if (delegate != ClosedConnection.CLOSED_CONNECTION) {
         leakTask.cancel();

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
         }

         try {
            if (isCommitStateDirty && !delegate.getAutoCommit()) {
               delegate.rollback();
            }

            if (isConnectionStateDirty) {
               resetConnectionState();
            }

            delegate.clearWarnings();
         }
         catch (SQLException e) {
            // when connections are aborted, exceptions are often thrown that should not reach the application
            if (!bagEntry.aborted) {
               throw checkException(e);
            }
         }
         finally {
            delegate = ClosedConnection.CLOSED_CONNECTION;
            bagEntry.lastAccess = lastAccess;
            parentPool.releaseConnection(bagEntry);
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean isClosed() throws SQLException
   {
      return (delegate == ClosedConnection.CLOSED_CONNECTION);
   }

   /** {@inheritDoc} */
   @Override
   public Statement createStatement() throws SQLException
   {
      return ProxyFactory.getProxyStatement(this, trackStatement(delegate.createStatement()));
   }

   /** {@inheritDoc} */
   @Override
   public Statement createStatement(int resultSetType, int concurrency) throws SQLException
   {
      return ProxyFactory.getProxyStatement(this, trackStatement(delegate.createStatement(resultSetType, concurrency)));
   }

   /** {@inheritDoc} */
   @Override
   public Statement createStatement(int resultSetType, int concurrency, int holdability) throws SQLException
   {
      return ProxyFactory.getProxyStatement(this, trackStatement(delegate.createStatement(resultSetType, concurrency, holdability)));
   }

   /** {@inheritDoc} */
   @Override
   public CallableStatement prepareCall(String sql) throws SQLException
   {
      return ProxyFactory.getProxyCallableStatement(this, trackStatement(delegate.prepareCall(sql)));
   }

   /** {@inheritDoc} */
   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int concurrency) throws SQLException
   {
      return ProxyFactory.getProxyCallableStatement(this, trackStatement(delegate.prepareCall(sql, resultSetType, concurrency)));
   }

   /** {@inheritDoc} */
   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int concurrency, int holdability) throws SQLException
   {
      return ProxyFactory.getProxyCallableStatement(this, trackStatement(delegate.prepareCall(sql, resultSetType, concurrency, holdability)));
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, trackStatement(delegate.prepareStatement(sql)));
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, trackStatement(delegate.prepareStatement(sql, autoGeneratedKeys)));
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, trackStatement(delegate.prepareStatement(sql, resultSetType, concurrency)));
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency, int holdability) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, trackStatement(delegate.prepareStatement(sql, resultSetType, concurrency, holdability)));
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, trackStatement(delegate.prepareStatement(sql, columnIndexes)));
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, trackStatement(delegate.prepareStatement(sql, columnNames)));
   }

   /** {@inheritDoc} */
   @Override
   public void commit() throws SQLException
   {
      delegate.commit();
      isCommitStateDirty = false;
   }

   /** {@inheritDoc} */
   @Override
   public void rollback() throws SQLException
   {
      delegate.rollback();
      isCommitStateDirty = false;
   }

   /** {@inheritDoc} */
   @Override
   public void rollback(Savepoint savepoint) throws SQLException
   {
      delegate.rollback(savepoint);
      isCommitStateDirty = false;
   }

   /** {@inheritDoc} */
   @Override
   public void setAutoCommit(boolean autoCommit) throws SQLException
   {
      delegate.setAutoCommit(autoCommit);
      isConnectionStateDirty = true;
      isAutoCommitDirty = (autoCommit != parentPool.isAutoCommit);
   }

   /** {@inheritDoc} */
   @Override
   public void setReadOnly(boolean readOnly) throws SQLException
   {
      delegate.setReadOnly(readOnly);
      isConnectionStateDirty = true;
      isReadOnlyDirty = (readOnly != parentPool.isReadOnly);
   }

   /** {@inheritDoc} */
   @Override
   public void setTransactionIsolation(int level) throws SQLException
   {
      delegate.setTransactionIsolation(level);
      isConnectionStateDirty = true;
      isTransactionIsolationDirty = (level != parentPool.transactionIsolation);
   }

   /** {@inheritDoc} */
   @Override
   public void setCatalog(String catalog) throws SQLException
   {
      delegate.setCatalog(catalog);
      isConnectionStateDirty = true;
      isCatalogDirty = (catalog != null && !catalog.equals(parentPool.catalog)) || (catalog == null && parentPool.catalog != null);
   }

   /** {@inheritDoc} */
   @Override
   public final boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      return iface.isInstance(delegate) || (delegate instanceof Wrapper && delegate.isWrapperFor(iface));
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public final <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(delegate)) {
         return (T) delegate;
      }
      else if (delegate instanceof Wrapper) {
          return (T) delegate.unwrap(iface);
      }

      throw new SQLException("Wrapped connection is not an instance of " + iface);
   }
}
