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

package com.zaxxer.hikari.proxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;

/**
 * This is the proxy class for java.sql.Statement.
 *
 * @author Brett Wooldridge
 */
public abstract class StatementProxy implements Statement
{
   protected final IHikariConnectionProxy connection;
   protected final Statement delegate;

   private boolean isClosed;

   protected StatementProxy(IHikariConnectionProxy connection, Statement statement)
   {
      this.connection = connection;
      this.delegate = statement;
   }

   protected final SQLException checkException(SQLException e)
   {
      return connection.checkException(e);
   }

   // **********************************************************************
   //                 Overridden java.sql.Statement Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public final void close() throws SQLException
   {
      if (isClosed) {
         return;
      }

      isClosed = true;
      connection.untrackStatement(this);

      try {
         delegate.close();
      }
      catch (SQLException e) {
         throw connection.checkException(e);
      }
   }

   /** {@inheritDoc} */
   @Override
   public final Connection getConnection() throws SQLException
   {
      return connection;
   }

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

      throw new SQLException("Wrapped statement is not an instance of " + iface);
   }
}