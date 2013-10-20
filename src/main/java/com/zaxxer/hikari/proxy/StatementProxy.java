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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.javassist.HikariInject;

/**
 * @author Brett Wooldridge
 */
public class StatementProxy extends HikariProxyBase implements IHikariStatementProxy
{
    protected static ProxyFactory PROXY_FACTORY;

    @HikariInject protected IHikariConnectionProxy _connection;
    
    protected Statement delegate;

    static
    {
        __static();
    }

    protected StatementProxy(IHikariConnectionProxy connection, Statement statement)
    {
        this._connection = connection;
        this.delegate = statement;
    }

    @HikariInject
    public void setConnectionProxy(Connection connection)
    {
        this._connection = (IHikariConnectionProxy) connection;
    }

    @HikariInject
    public SQLException checkException(SQLException e)
    {
        return _connection.checkException(e);
    }

    // **********************************************************************
    //                 Overridden java.sql.Statement Methods
    // **********************************************************************

    @HikariInject
    public void close() throws SQLException
    {
        _connection.unregisterStatement(this);
        __close();
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        IHikariResultSetProxy resultSet = (IHikariResultSetProxy) __executeQuery(sql);
        resultSet.setProxyStatement(this);

        return (ResultSet) resultSet;
    }

    public ResultSet getGeneratedKeys() throws SQLException
    {
        IHikariResultSetProxy resultSet = (IHikariResultSetProxy) __getGeneratedKeys();
        resultSet.setProxyStatement(this);

        return (ResultSet) resultSet;
    }

    // ***********************************************************************
    // These methods contain code we do not want injected into the actual
    // java.sql.Connection implementation class.  These methods are only
    // used when instrumentation is not available and "conventional" Javassist
    // delegating proxies are used.
    // ***********************************************************************

    private static void __static()
    {
        if (PROXY_FACTORY == null)
        {
            PROXY_FACTORY = JavassistProxyFactoryFactory.getProxyFactory();
        }
    }
    
    public void __close() throws SQLException
    {
        if (delegate.isClosed())
        {
            return;
        }

        delegate.close();        
    }

    public ResultSet __executeQuery(String sql) throws SQLException
    {
        ResultSet resultSet = delegate.executeQuery(sql);
        if (resultSet == null)
        {
            return null;
        }

        return PROXY_FACTORY.getProxyResultSet(this, resultSet);
    }

    public ResultSet __getGeneratedKeys() throws SQLException
    {
        ResultSet generatedKeys = delegate.getGeneratedKeys();
        if (generatedKeys == null)
        {
            return null;
        }

        return PROXY_FACTORY.getProxyResultSet(this, generatedKeys);
    }

    // TODO: fix wrapper
}