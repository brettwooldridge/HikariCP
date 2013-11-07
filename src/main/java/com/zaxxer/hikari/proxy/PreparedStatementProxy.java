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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zaxxer.hikari.javassist.HikariOverride;

/**
 *
 * @author Brett Wooldridge
 */
public abstract class PreparedStatementProxy extends StatementProxy implements IHikariStatementProxy, PreparedStatement
{
    protected PreparedStatementProxy(ConnectionProxy connection, PreparedStatement statement)
    {
        super(connection, statement);
    }

    // **********************************************************************
    //              Overridden java.sql.PreparedStatement Methods
    // **********************************************************************
    
    @HikariOverride
    public ResultSet executeQuery() throws SQLException
    {
    	try
    	{
	        ResultSet rs = __executeQuery();
    		if (rs != null)
    		{
    		    ((IHikariResultSetProxy) rs)._setProxyStatement(this);
    		}

	        return rs;
    	}
    	catch (SQLException e)
    	{
    		throw _checkException(e);
    	}
    }

    // ***********************************************************************
    // These methods contain code we do not want injected into the actual
    // java.sql.Connection implementation class.  These methods are only
    // used when instrumentation is not available and "conventional" Javassist
    // delegating proxies are used.
    // ***********************************************************************

    public ResultSet __executeQuery() throws SQLException
    {
        ResultSet resultSet = ((PreparedStatement) delegate).executeQuery();
        if (resultSet != null)
        {
            resultSet = PROXY_FACTORY.getProxyResultSet(this, resultSet);
        }
        return resultSet;
    }
}
