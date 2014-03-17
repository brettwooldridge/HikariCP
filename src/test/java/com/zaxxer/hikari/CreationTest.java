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
        config.setMaximumPoolSize(1);
        config.setAcquireIncrement(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        try
        {
            Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());
    
            Connection connection = ds.getConnection();
            Assert.assertNotNull(connection);
    
            Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 0, ds.pool.getIdleConnections());
    
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM device WHERE device_id=?");
            Assert.assertNotNull(statement);
    
            statement.setInt(1, 0);
    
            ResultSet resultSet = statement.executeQuery();
            Assert.assertNotNull(resultSet);
    
            Assert.assertFalse(resultSet.next());
    
            resultSet.close();
            statement.close();
            connection.close();
    
            Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());
        }
        finally
        {
            ds.shutdown();
        }
    }

    @Test
    public void testMaxLifetime() throws Exception
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        System.setProperty("com.zaxxer.hikari.housekeeping.period", "100");

        HikariDataSource ds = new HikariDataSource(config);

        try
        {
            System.clearProperty("com.zaxxer.hikari.housekeeping.period");
    
            ds.setMaxLifetime(700);
    
            Assert.assertSame("Total connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());
    
            Connection connection = ds.getConnection();
            Assert.assertNotNull(connection);
    
            Assert.assertSame("Second total connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Second idle connections not as expected", 0, ds.pool.getIdleConnections());
            connection.close();
    
            Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());
    
            Connection connection2 = ds.getConnection();
            Assert.assertSame("Expected the same connection", connection, connection2);
            connection2.close();
            
            Thread.sleep(2000);
    
            connection2 = ds.getConnection();
            Assert.assertNotSame("Expected a different connection", connection, connection2);
    
            connection2.close();
    
            Assert.assertSame("Post total connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Post idle connections not as expected", 1, ds.pool.getIdleConnections());
        }
        finally
        {
            ds.shutdown();
        }
    }

    @Test
    public void testDoubleClose() throws Exception
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(1);
        config.setAcquireIncrement(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        try
        {
            Connection connection = ds.getConnection();
            connection.close();
            connection.close();
        }
        finally
        {
            ds.shutdown();
        }
    }

    @Test
    public void testBackfill() throws Exception
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(4);
        config.setConnectionTimeout(500);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        try
        {
            Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());
    
            // This will take the pool down to zero
            Connection connection = ds.getConnection();
            Assert.assertNotNull(connection);
    
            Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 0, ds.pool.getIdleConnections());
    
            PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
            Assert.assertNotNull(statement);
    
            ResultSet resultSet = statement.executeQuery();
            Assert.assertNotNull(resultSet);
    
            try
            {
                statement.getMaxFieldSize();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertSame(SQLException.class, e.getClass());
            }
    
            // The connection will be ejected from the pool here
            connection.close();
    
            Assert.assertSame("Totals connections not as expected", 0, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 0, ds.pool.getIdleConnections());

            // This will create a new connection, and cause a backfill
            connection = ds.getConnection();

            // Wait for scheduled backfill to execute
            Thread.sleep(600);

            connection.close();

            Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
            Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());
        }
        finally
        {
            ds.shutdown();
        }
    }

    @Test
    public void testIsolation() throws Exception
    {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
        config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
        config.validate();
        
        int transactionIsolation = config.getTransactionIsolation();
        Assert.assertSame(Connection.TRANSACTION_REPEATABLE_READ, transactionIsolation);
    }
}
