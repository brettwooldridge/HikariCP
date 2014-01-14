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

package com.zaxxer.hikari;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HikariCP pooled DataSource.
 *
 * @author Brett Wooldridge
 */
public class HikariDataSource implements DataSource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariDataSource.class);

    private int loginTimeout;
    private volatile boolean isShutdown;

    // Package scope for testing
    HikariPool pool;

    /**
     * Construct a HikariDataSource with the specified configuration.
     *
     * @param configuration a HikariConfig instance
     */
    public HikariDataSource(HikariConfig configuration)
    {
        pool = new HikariPool(configuration);
    }

    /** {@inheritDoc} */
    public Connection getConnection() throws SQLException
    {
        if (!isShutdown)
        {
            return pool.getConnection();
        }

        throw new IllegalStateException("The datasource has been shutdown.");
    }

    /** {@inheritDoc} */
    public Connection getConnection(String username, String password) throws SQLException
    {
        LOGGER.warn("getConnection() with username and password is not supported");

        return getConnection();
    }

    /** {@inheritDoc} */
    public PrintWriter getLogWriter() throws SQLException
    {
        return (pool.dataSource != null ? pool.dataSource.getLogWriter() : null);
    }

    /** {@inheritDoc} */
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        if (pool.dataSource != null)
        {
            pool.dataSource.setLogWriter(out);
        }
    }

    /** {@inheritDoc} */
    public void setLoginTimeout(int seconds) throws SQLException
    {
        this.loginTimeout = seconds;
    }

    /** {@inheritDoc} */
    public int getLoginTimeout() throws SQLException
    {
        return loginTimeout;
    }

    /** {@inheritDoc} */
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException();
    }

    /** {@inheritDoc} */
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return (this.getClass().isAssignableFrom(iface));
    }

    public void shutdown()
    {
        boolean shutdown = isShutdown;
        isShutdown = true;
        if (!shutdown)
        {
            pool.shutdown();
            pool = null;
        }
    }
}
