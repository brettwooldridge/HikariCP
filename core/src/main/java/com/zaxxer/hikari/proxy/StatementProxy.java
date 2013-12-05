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
 * This is the proxy class for java.sql.Statement.  It is used in two ways:
 * 
 *  1) If instrumentation is not used, Javassist will generate a new class
 *     that extends this class and delegates all method calls to the 'delegate'
 *     member (which points to the real Statement).
 *
 *  2) If instrumentation IS used, Javassist will be used to inject all of
 *     the &amp;HikariInject and &amp;HikariOverride annotated fields and methods
 *     of this class into the actual Statement implementation provided by the
 *     JDBC driver.  In order to avoid name conflicts when injecting code into
 *     a driver class some of the fields and methods are prefixed with _ or __.
 *     
 *     Methods prefixed with __, like __executeQuery() are especially
 *     important because when we inject our own executeQuery() into the
 *     target implementation, the original method is renamed to __executeQuery()
 *     so that the call operates the same whether delegation or instrumentation
 *     is used.
 *
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
        // This is important when injecting in instrumentation mode.  Do not change
        // this name without also fixing the HikariClassTransformer.
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
    public IHikariConnectionProxy _getConnectionProxy()
    {
        return _connection;
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

    @HikariInject
    protected <T extends ResultSet> T _trackResultSet(T resultSet)
    {
        if (resultSet != null)
        {
            ((IHikariResultSetProxy) resultSet)._setProxyStatement(this);
        }
        return resultSet;
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
            return _trackResultSet(__executeQuery(sql));
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
            return _trackResultSet(__getResultSet());
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
            return _trackResultSet(__getGeneratedKeys());
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
        return wrapResultSet(delegate.executeQuery(sql));
    }

    public ResultSet __getGeneratedKeys() throws SQLException
    {
        return wrapResultSet(delegate.getGeneratedKeys());
    }

    public ResultSet __getResultSet() throws SQLException
    {
        return wrapResultSet(delegate.getResultSet());
    }

    protected ResultSet wrapResultSet(ResultSet resultSet)
    {
        if (resultSet != null)
        {
            resultSet = PROXY_FACTORY.getProxyResultSet(this, resultSet);
        }

        return resultSet;        
    }
}