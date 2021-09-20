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

package com.zaxxer.hikari.pool;

import java.sql.*;

import com.zaxxer.hikari.util.FastList;

/**
 * A factory class that produces proxies around instances of the standard
 * JDBC interfaces.
 *
 * @author Brett Wooldridge
 */
public final class ProxyFactory
{
   private ProxyFactory()
   {
      // unconstructable
   }

   /**
    * Create a proxy for the specified {@link Connection} instance.
    * @param poolEntry the PoolEntry holding pool state
    * @param connection the raw database Connection
    * @param openStatements a reusable list to track open Statement instances
    * @param leakTask the ProxyLeakTask for this connection
    * @param isReadOnly the default readOnly state of the connection
    * @param isAutoCommit the default autoCommit state of the connection
    * @return a proxy that wraps the specified {@link Connection}
    */
   static ProxyConnection getProxyConnection(final PoolEntry poolEntry, final Connection connection, final FastList<Statement> openStatements, final ProxyLeakTask leakTask, final boolean isReadOnly, final boolean isAutoCommit)
   {
      // Body is replaced (injected) by JavassistProxyFactory
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static Statement getProxyStatement(final ProxyConnection connection, final Statement statement)
   {
      // Body is replaced (injected) by JavassistProxyFactory
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static CallableStatement getProxyCallableStatement(final ProxyConnection connection, final CallableStatement statement)
   {
      // Body is replaced (injected) by JavassistProxyFactory
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static PreparedStatement getProxyPreparedStatement(final ProxyConnection connection, final PreparedStatement statement)
   {
      // Body is replaced (injected) by JavassistProxyFactory
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static ResultSet getProxyResultSet(final ProxyConnection connection, final ProxyStatement statement, final ResultSet resultSet)
   {
      // Body is replaced (injected) by JavassistProxyFactory
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static DatabaseMetaData getProxyDatabaseMetaData(final ProxyConnection connection, final DatabaseMetaData metaData)
   {
      // Body is replaced (injected) by JavassistProxyFactory
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }
}
