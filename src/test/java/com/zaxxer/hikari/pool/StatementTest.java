package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class StatementTest
{
    private HikariDataSource ds;

    @Before
    public void setup()
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(2);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        ds = new HikariDataSource(config);
    }

    @After
    public void teardown()
    {
        ds.close();
    }

    @Test
    public void testStatementClose() throws SQLException
    {
        ds.getConnection().close();

        HikariPool pool = TestElf.getPool(ds);
        Assert.assertTrue("Total connections not as expected", pool.getTotalConnections() >= 1);
        Assert.assertTrue("Idle connections not as expected", pool.getIdleConnections() >= 1);

        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

        Assert.assertTrue("Total connections not as expected", pool.getTotalConnections() >= 1);
        Assert.assertTrue("Idle connections not as expected", pool.getIdleConnections() >= 0);

        Statement statement = connection.createStatement();
        Assert.assertNotNull(statement);

        connection.close();

        Assert.assertTrue(statement.isClosed());
    }

    @Test
    public void testAutoStatementClose() throws SQLException
    {
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
        Connection connection = ds.getConnection();

        Statement statement1 = connection.createStatement();

        statement1.close();
        statement1.close();

        connection.close();
    }

    @Test
    public void testOutOfOrderStatementClose() throws SQLException
    {
        Connection connection = ds.getConnection();

        Statement statement1 = connection.createStatement();
        Statement statement2 = connection.createStatement();

        statement1.close();
        statement2.close();

        connection.close();
    }    
}
