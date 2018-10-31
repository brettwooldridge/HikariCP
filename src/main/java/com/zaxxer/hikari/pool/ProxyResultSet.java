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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This is the proxy class for java.sql.ResultSet.
 *
 * @author Brett Wooldridge
 */
public abstract class ProxyResultSet implements ResultSet
{
   protected final ProxyConnection connection;
   protected final ProxyStatement statement;
   final ResultSet delegate;
   private final boolean optimizeFindColumn;
   private Map<String, Integer> columnsByName;

   protected ProxyResultSet(ProxyConnection connection, ProxyStatement statement, ResultSet resultSet, boolean optimizeFindColumn)
   {
      this.connection = connection;
      this.statement = statement;
      this.delegate = resultSet;
      this.optimizeFindColumn = optimizeFindColumn;
   }

   @SuppressWarnings("unused")
   final SQLException checkException(SQLException e)
   {
      return connection.checkException(e);
   }

   /** {@inheritDoc} */
   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + '@' + System.identityHashCode(this) + " wrapping " + delegate;
   }

   // **********************************************************************
   //                 Overridden java.sql.ResultSet Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public final Statement getStatement() throws SQLException
   {
      return statement;
   }

   /** {@inheritDoc} */
   @Override
   public void updateRow() throws SQLException
   {
      connection.markCommitStateDirty();
      delegate.updateRow();
   }

   /** {@inheritDoc} */
   @Override
   public void insertRow() throws SQLException
   {
      connection.markCommitStateDirty();
      delegate.insertRow();
   }

   /** {@inheritDoc} */
   @Override
   public void deleteRow() throws SQLException
   {
      connection.markCommitStateDirty();
      delegate.deleteRow();
   }


   private static final SQLException NO_SUCH_COLUMN_EXCEPTION = new SQLException()
   {
      private static final long serialVersionUID = 4466502690997888852L;

      @Override
      public String getMessage()
      {
         return "Column is not in the ResultSet";
      }

      @Override
      public synchronized Throwable fillInStackTrace()
      {
         return this;
      }
   };

   private static final int NO_SUCH_COLUMN = 0;

   @Override
   public int findColumn( String columnName ) throws SQLException
   {
      if ( !optimizeFindColumn )
      {
         return delegate.findColumn(columnName);
      }

      int columnIndex = findColumnIndex( columnName );
      if ( NO_SUCH_COLUMN == columnIndex )
      {
         throw NO_SUCH_COLUMN_EXCEPTION;
      }
      return columnIndex;
   }

   private int findColumnIndex( final String columnName ) throws SQLException
   {
      if ( null == columnName )
      {
         return NO_SUCH_COLUMN;
      }
      if ( null == columnsByName)
      {
         // We use getColumnName as it gives us direct access to the statement accessors in the case of the Oracle driver
         // and means we can populate the map completely on the first call, avoiding the iteration behaviour of findColumn
         ResultSetMetaData metaData = getMetaData();
         int columnCount = metaData.getColumnCount();
         columnsByName = new HashMap<>( columnCount * 2, 1.0f );
         for ( int i = 0; i < columnCount; i++ )
         {
            int columnIndex = i + 1;
            columnsByName.put( metaData.getColumnName( columnIndex ).toLowerCase( Locale.US ), columnIndex );
         }
      }
      // Avoid a contains(String) call and toLowerCase to return as quickly as we can for the most likely case
      Integer index = columnsByName.get( columnName );
      if ( null != index )
      {
         return index;
      }
      // Attempt the lowercase version, caching the representation so we avoid the toLowerCase on the next attempt
      index = columnsByName.get( columnName.toLowerCase( Locale.US ) );
      if ( null != index )
      {
         columnsByName.put( columnName, index );
         return index;
      }
      return NO_SUCH_COLUMN;
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public final <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(delegate)) {
         return (T) delegate;
      }
      else if (delegate != null) {
          return delegate.unwrap(iface);
      }

      throw new SQLException("Wrapped ResultSet is not an instance of " + iface);
   }
}
