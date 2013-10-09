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

package com.zaxxer.hikari.performance;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 *
 * @author Brett Wooldridge
 */
public class StubDataSource implements DataSource
{
    /** {@inheritDoc} */
    public PrintWriter getLogWriter() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void setLogWriter(PrintWriter out) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void setLoginTimeout(int seconds) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }

    /** {@inheritDoc} */
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public Connection getConnection() throws SQLException
    {
        return new StubConnection();
    }

    /** {@inheritDoc} */
    public Connection getConnection(String username, String password) throws SQLException
    {
        return new StubConnection();
    }
}
