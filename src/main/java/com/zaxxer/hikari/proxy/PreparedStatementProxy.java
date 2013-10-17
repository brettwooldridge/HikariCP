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

/**
 *
 * @author Brett Wooldridge
 */
public class PreparedStatementProxy extends HikariProxyBase
{
    private static final ProxyFactory PROXY_FACTORY;

    private final ConnectionProxy connection;

    protected final PreparedStatement delegate;

    static
    {
        PROXY_FACTORY = JavassistProxyFactoryFactory.getProxyFactory();
    }

    protected PreparedStatementProxy(ConnectionProxy connection, PreparedStatement statement)
    {
        this.connection = connection;
        this.delegate = statement;
    }

    protected SQLException checkException(SQLException e)
    {
        return connection.checkException(e);
    }

    // **********************************************************************
    //              Overridden java.sql.PreparedStatement Methods
    //                      other methods are injected
    // **********************************************************************

    public void close() throws SQLException
    {
        if (delegate == null)
        {
            return;
        }

        connection.unregisterStatement(this);
        delegate.close();
    }

    public ResultSet getResultSet() throws SQLException
    {
        ResultSet resultSet = delegate.getResultSet();
        if (resultSet == null)
        {
            return null;
        }
        return PROXY_FACTORY.getProxyResultSet((PreparedStatement) this, resultSet);
    }

    public ResultSet executeQuery() throws SQLException
    {
        ResultSet resultSet = delegate.executeQuery();
        if (resultSet == null)
        {
            return null;
        }
        return PROXY_FACTORY.getProxyResultSet((PreparedStatement) this, resultSet);
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        ResultSet resultSet = delegate.executeQuery(sql);
        if (resultSet == null)
        {
            return null;
        }
        return PROXY_FACTORY.getProxyResultSet((PreparedStatement) this, resultSet);
    }

    public ResultSet getGeneratedKeys() throws SQLException
    {
        ResultSet generatedKeys = delegate.getGeneratedKeys();
        if (generatedKeys == null)
        {
            return null;
        }
        return PROXY_FACTORY.getProxyResultSet((PreparedStatement) this, generatedKeys);
    }

    /* java.sql.Wrapper implementation */

    // TODO: fix wrapper
//    public boolean isWrapperFor(Class<?> iface) throws SQLException
//    {
//        return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
//    }
//
//    @SuppressWarnings("unchecked")
//    public <T> T unwrap(Class<T> iface) throws SQLException
//    {
//        if (iface.isAssignableFrom(delegate.getClass()))
//        {
//            return (T) delegate;
//        }
//        if (isWrapperFor(iface))
//        {
//            return unwrap(delegate, iface);
//        }
//        throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
//    }
}