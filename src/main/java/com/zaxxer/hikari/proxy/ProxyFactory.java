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
import java.sql.ResultSet;
import java.sql.Statement;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.LeakTask;
import com.zaxxer.hikari.pool.PoolBagEntry;

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
    *
    * @param pool the {@link HikariPool} that will own this proxy
    * @param bagEntry the PoolBagEntry entry for this proxy
    * @param leakTask a leak detetection task
    * @return a proxy that wraps the specified {@link Connection}
    */
   public static IHikariConnectionProxy getProxyConnection(final HikariPool pool, final PoolBagEntry bagEntry, final LeakTask leakTask)
   {
      // Body is injected by JavassistProxyFactory
      return null;
   }

   static Statement getProxyStatement(final ConnectionProxy connection, final Statement statement)
   {
      // Body is injected by JavassistProxyFactory
      return null;
   }

   static CallableStatement getProxyCallableStatement(final ConnectionProxy connection, final CallableStatement statement)
   {
      // Body is injected by JavassistProxyFactory
      return null;
   }

   static PreparedStatement getProxyPreparedStatement(final ConnectionProxy connection, final PreparedStatement statement)
   {
      // Body is injected by JavassistProxyFactory
      return null;
   }

   static ResultSet getProxyResultSet(final ConnectionProxy connection, final ResultSet resultSet)
   {
      // Body is injected by JavassistProxyFactory
      return null;
   }
}
