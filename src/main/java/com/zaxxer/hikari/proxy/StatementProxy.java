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
import com.zaxxer.hikari.javassist.HikariOverride;

/**
 * @author Brett Wooldridge
 */
public abstract class StatementProxy implements IHikariStatementProxy, Statement
{
    protected static ProxyFactory PROXY_FACTORY;

    @HikariInject protected IHikariConnectionProxy _connection;

    @HikariInject protected boolean _isClosed;

    protected final Statement delegate;

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
    public void _setConnectionProxy(IHikariConnectionProxy connection)
    {
        this._connection = connection;
    }

    @HikariInject
    public SQLException _checkException(SQLException e)
    {
        return _connection._checkException(e);
    }

    @HikariInject
    public void _releaseResultSet(IHikariResultSetProxy resultSet)
    {
    }

    // **********************************************************************
    //                 Overridden java.sql.Statement Methods
    // **********************************************************************

    @HikariOverride
    public void close() throws SQLException
    {
        if (_isClosed)
        {
            return;
        }

        _isClosed = true;
        _connection._unregisterStatement(this);
        try
        {
            __close();
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public ResultSet executeQuery(String sql) throws SQLException
    {
        try
        {
            ResultSet rs = __executeQuery(sql);
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

    @HikariOverride
    public ResultSet getResultSet() throws SQLException
    {
        try
        {
            ResultSet rs = __getResultSet();
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

    @HikariOverride
    public ResultSet getGeneratedKeys() throws SQLException
    {
        try
        {
            ResultSet rs = __getGeneratedKeys();
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

    public Connection getConnection() throws SQLException
    {
        return (Connection) _connection;
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
        if (resultSet != null)
        {
            resultSet = PROXY_FACTORY.getProxyResultSet(this, resultSet);
        }
        return resultSet;
    }

    public ResultSet __getGeneratedKeys() throws SQLException
    {
        ResultSet generatedKeys = delegate.getGeneratedKeys();
        if (generatedKeys != null)
        {
            generatedKeys = PROXY_FACTORY.getProxyResultSet(this, generatedKeys);
        }
        return generatedKeys;
    }

    public ResultSet __getResultSet() throws SQLException
    {
        ResultSet resultSet = delegate.getResultSet();
        if (resultSet != null)
        {
            resultSet = PROXY_FACTORY.getProxyResultSet(this, resultSet);
        }
        return resultSet;
    }
}