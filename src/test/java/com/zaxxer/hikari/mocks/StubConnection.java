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

package com.zaxxer.hikari.mocks;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import com.zaxxer.hikari.util.UtilityElf;

/**
 *
 * @author Brett Wooldridge
 */
public class StubConnection extends StubBaseConnection implements Connection
{
   public static final AtomicInteger count = new AtomicInteger();
   public static volatile boolean slowCreate;
   public static volatile boolean oldDriver;

   private static long foo;
   private boolean autoCommit;
   private int isolation = Connection.TRANSACTION_READ_COMMITTED;
   private String catalog;

   static {
      foo = System.currentTimeMillis();
   }

   public StubConnection() {
      count.incrementAndGet();
      if (slowCreate) {
         UtilityElf.quietlySleep(1000);
      }
   }

   /** {@inheritDoc} */
   @SuppressWarnings("unchecked")
   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }

      if (iface.isInstance(this)) {
         return (T) this;
      }

      throw new SQLException("Wrapped connection is not an instance of " + iface);
   }

   /** {@inheritDoc} */
   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public CallableStatement prepareCall(String sql) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public String nativeSQL(String sql) throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public void setAutoCommit(boolean autoCommit) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      this.autoCommit = autoCommit;
   }

   /** {@inheritDoc} */
   @Override
   public boolean getAutoCommit() throws SQLException
   {
      return autoCommit;
   }

   /** {@inheritDoc} */
   @Override
   public void commit() throws SQLException
   {

   }

   /** {@inheritDoc} */
   @Override
   public void rollback() throws SQLException
   {

   }

   /** {@inheritDoc} */
   @Override
   public void close() throws SQLException
   {

   }

   /** {@inheritDoc} */
   @Override
   public boolean isClosed() throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public DatabaseMetaData getMetaData() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public void setReadOnly(boolean readOnly) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean isReadOnly() throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public void setCatalog(String catalog) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      this.catalog = catalog;
   }

   /** {@inheritDoc} */
   @Override
   public String getCatalog() throws SQLException
   {
      return catalog;
   }

   /** {@inheritDoc} */
   @Override
   public void setTransactionIsolation(int level) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      this.isolation = level;
   }

   /** {@inheritDoc} */
   @Override
   public int getTransactionIsolation() throws SQLException
   {
      return isolation;
   }

   /** {@inheritDoc} */
   @Override
   public SQLWarning getWarnings() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public void clearWarnings() throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
   }

   /** {@inheritDoc} */
   @Override
   public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubPreparedStatement(this);
   }

   /** {@inheritDoc} */
   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, Class<?>> getTypeMap() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public void setTypeMap(Map<String, Class<?>> map) throws SQLException
   {
   }

   /** {@inheritDoc} */
   @Override
   public void setHoldability(int holdability) throws SQLException
   {
   }

   /** {@inheritDoc} */
   @Override
   public int getHoldability() throws SQLException
   {
      return (int) foo;
   }

   /** {@inheritDoc} */
   @Override
   public Savepoint setSavepoint() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Savepoint setSavepoint(String name) throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public void rollback(Savepoint savepoint) throws SQLException
   {
   }

   /** {@inheritDoc} */
   @Override
   public void releaseSavepoint(Savepoint savepoint) throws SQLException
   {
   }

   /** {@inheritDoc} */
   @Override
   public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubPreparedStatement(this);
   }

   /** {@inheritDoc} */
   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubPreparedStatement(this);
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubPreparedStatement(this);
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubPreparedStatement(this);
   }

   /** {@inheritDoc} */
   @Override
   public Clob createClob() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Blob createBlob() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public NClob createNClob() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public SQLXML createSQLXML() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public boolean isValid(int timeout) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public void setClientInfo(String name, String value) throws SQLClientInfoException
   {
   }

   /** {@inheritDoc} */
   @Override
   public void setClientInfo(Properties properties) throws SQLClientInfoException
   {
   }

   /** {@inheritDoc} */
   @Override
   public String getClientInfo(String name) throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Properties getClientInfo() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Array createArrayOf(String typeName, Object[] elements) throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Struct createStruct(String typeName, Object[] attributes) throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   public void setSchema(String schema) throws SQLException
   {
   }

   /** {@inheritDoc} */
   public String getSchema() throws SQLException
   {
      return null;
   }

   /** {@inheritDoc} */
   public void abort(Executor executor) throws SQLException
   {
      throw new SQLException("Intentional exception during abort");
   }

   /** {@inheritDoc} */
   public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
   {
   }

   /** {@inheritDoc} */
   public int getNetworkTimeout() throws SQLException
   {
      if (oldDriver) {
         throw new AbstractMethodError();
      }

      return 0;
   }

}
