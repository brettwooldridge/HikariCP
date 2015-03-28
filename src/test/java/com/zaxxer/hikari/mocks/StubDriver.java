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

package com.zaxxer.hikari.mocks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 * @author Brett Wooldridge
 */
public class StubDriver implements Driver
{
    private static final Driver driver;

    static
    {
        driver = new StubDriver();
        try
        {
            DriverManager.registerDriver(driver);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Connection connect(String url, Properties info) throws SQLException
    {
        return new StubConnection();
    }

    /** {@inheritDoc} */
    @Override
    public boolean acceptsURL(String url) throws SQLException
    {
        return "jdbc:stub".equals(url);
    }

    /** {@inheritDoc} */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getMajorVersion()
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getMinorVersion()
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean jdbcCompliant()
    {
        return true;
    }

    /** {@inheritDoc} */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }
}
