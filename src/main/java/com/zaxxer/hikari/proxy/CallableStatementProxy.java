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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Brett Wooldridge
 */
public class CallableStatementProxy extends HikariProxyBase<CallableStatement>
{
    private final ConnectionProxy connection;

    protected CallableStatementProxy(ConnectionProxy connection, CallableStatement statement)
    {
        super(statement);
        this.connection = connection;
    }

    protected SQLException checkException(SQLException e)
    {
        return connection.checkException(e);
    }

    // **********************************************************************
    //               Overridden java.sql.CallableStatement Methods
    //                       other methods are injected
    // **********************************************************************


    public void close() throws SQLException
    {
        if (delegate == null)
        {
            return;
        }

        connection.unregisterStatement(delegate);
        delegate.close();
    }

    public ResultSet executeQuery() throws SQLException
    {
        return ProxyFactory.INSTANCE.getProxyResultSet((CallableStatement) this, delegate.executeQuery());
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        return ProxyFactory.INSTANCE.getProxyResultSet((CallableStatement) this, delegate.executeQuery(sql));
    }

    public ResultSet getGeneratedKeys() throws SQLException
    {
        return ProxyFactory.INSTANCE.getProxyResultSet((CallableStatement) this, delegate.getGeneratedKeys());
    }

    /* java.sql.Wrapper implementation */

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        if (iface.isAssignableFrom(delegate.getClass()))
        {
            return (T) delegate;
        }
        if (isWrapperFor(iface))
        {
            return unwrap(delegate, iface);
        }
        throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
    }
}