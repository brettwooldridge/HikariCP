package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;

public class StatementTest
{
    @Test
    public void testStatementClose() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(2);
        config.setAcquireIncrement(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
        Assert.assertSame("Idle connections not as expected", 1, ds.pool.getIdleConnections());

        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

        Assert.assertSame("Totals connections not as expected", 1, ds.pool.getTotalConnections());
        Assert.assertSame("Idle connections not as expected", 0, ds.pool.getIdleConnections());

        Statement statement = connection.createStatement();
        Assert.assertNotNull(statement);

        connection.close();

        Assert.assertTrue(statement.isClosed());
    }

    @Test
    public void testAutoStatementClose() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(2);
        config.setAcquireIncrement(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

        Statement statement1 = connection.createStatement();
        Assert.assertNotNull(statement1);
        Statement statement2 = connection.createStatement();
        Assert.assertNotNull(statement2);

        connection.close();

        Assert.assertTrue(statement1.isClosed());
        Assert.assertTrue(statement2.isClosed());
    }

    @Test
    public void testDoubleStatementClose() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(2);
        config.setAcquireIncrement(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        Connection connection = ds.getConnection();

        Statement statement1 = connection.createStatement();

        statement1.close();
        statement1.close();

        connection.close();
    }

    @Test
    public void testOutOfOrderStatementClose() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(2);
        config.setAcquireIncrement(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        Connection connection = ds.getConnection();

        Statement statement1 = connection.createStatement();
        Statement statement2 = connection.createStatement();

        statement1.close();
        statement2.close();

        connection.close();
    }    
}
