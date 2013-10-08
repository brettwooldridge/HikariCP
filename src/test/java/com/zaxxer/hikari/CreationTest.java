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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

/**
 * System property testProxy can be one of:
 *    "com.zaxxer.hikari.JavaProxyFactory"
 *    "com.zaxxer.hikari.CglibProxyFactory"
 *    "com.zaxxer.hikari.JavassistProxyFactory"
 *
 * @author Brett Wooldridge
 */
public class CreationTest
{
    @Test
    public void testCreate() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.MockDataSource");
        config.setProxyFactoryClassName(System.getProperty("testProxy", "auto"));

        HikariDataSource ds = new HikariDataSource();
        ds.setConfiguration(config);

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnectionCount());
        Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnectionCount());

        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnectionCount());
        Assert.assertSame("Idle connections not as expected", 0, ds.pool.getIdleConnectionCount());

        PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
        Assert.assertNotNull(statement);

        statement.setString(1, "thing");

        ResultSet resultSet = statement.executeQuery();
        Assert.assertNotNull(resultSet);

        String aString = resultSet.getString(1);
        Assert.assertSame("aString", aString);

        resultSet.close();
        statement.close();
        connection.close();

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnectionCount());
        Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnectionCount());
    }

    @Test
    public void testMaxLifetime() throws Exception
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaxLifetimeMs(500);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.MockDataSource");
        config.setProxyFactoryClassName(System.getProperty("testProxy", "auto"));

        HikariDataSource ds = new HikariDataSource();
        ds.setConfiguration(config);

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnectionCount());
        Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnectionCount());

        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnectionCount());
        Assert.assertSame("Idle connections not as expected", 0, ds.pool.getIdleConnectionCount());
        connection.close();

        Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnectionCount());

        Connection connection2 = ds.getConnection();
        Assert.assertSame("Expected the same connection", connection, connection2);
        connection2.close();
        
        Thread.sleep(501);

        connection2 = ds.getConnection();
        Assert.assertNotSame("Expected a different connection", connection, connection2);

        connection2.close();

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnectionCount());
        Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnectionCount());
    }   
}
