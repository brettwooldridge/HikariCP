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

package com.zaxxer.hikari.pool;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;

/**
 * This is the proxy class for java.sql.PreparedStatement.
 *
 * @author Brett Wooldridge
 */
public abstract class ProxyPreparedStatement extends ProxyStatement implements PreparedStatement
{
   private final String sql ;
   
   protected ProxyPreparedStatement(ProxyConnection connection, PreparedStatement statement, HikariConfig config, String sql)
   {
      super(connection, statement, config);
      this.sql = sql;
   }

   // **********************************************************************
   //              Overridden java.sql.PreparedStatement Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public boolean execute() throws SQLException
   {
	  final long start = source.currentTime();
	  try {
	      connection.markCommitStateDirty();
	      return ((PreparedStatement) delegate).execute();
	  } finally {
		  log (source.elapsedMillis(start), sql);
	  }
   }

   /** {@inheritDoc} */
   @Override
   public ResultSet executeQuery() throws SQLException
   {
	  final long start = source.currentTime();
	  try {
		  connection.markCommitStateDirty();
	      ResultSet resultSet = ((PreparedStatement) delegate).executeQuery();
	      return ProxyFactory.getProxyResultSet(connection, this, resultSet); 
	  } finally {
		  log (source.elapsedMillis(start), sql);
	  }
   }

   /** {@inheritDoc} */
   @Override
   public int executeUpdate() throws SQLException
   {
	  final long start = source.currentTime();
	  try {
	      connection.markCommitStateDirty();
	      return ((PreparedStatement) delegate).executeUpdate();
	  } finally {
		  log (source.elapsedMillis(start), sql);
	  }
   }

   /** {@inheritDoc} */
   @Override
   public long executeLargeUpdate() throws SQLException
   {
	  final long start = source.currentTime();
	  try {
	      connection.markCommitStateDirty();
	      return ((PreparedStatement) delegate).executeLargeUpdate();
	  } finally {
		  log (source.elapsedMillis(start), sql);
	  }
   }
}
