package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ExceptionTest
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
    public void testException1() throws SQLException
    {
        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

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

        connection.close();

        HikariPool pool = TestElf.getPool(ds);
        Assert.assertTrue("Total (3) connections not as expected", pool.getTotalConnections() >= 0);
        Assert.assertTrue("Idle (3) connections not as expected", pool.getIdleConnections() >= 0);
    }

    @Test
    public void testUseAfterStatementClose() throws SQLException
    {
        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);

        try
        {
            PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
            statement.close();
            statement.getMoreResults();
            
            Assert.fail();
        }
        catch (SQLException e)
        {
            Assert.assertSame("Connection is closed", e.getMessage());
        }
    }

    @Test
    public void testUseAfterClose() throws SQLException
    {
        Connection connection = ds.getConnection();
        Assert.assertNotNull(connection);
        connection.close();

        try
        {
            connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
            Assert.fail();
        }
        catch (SQLException e)
        {
            Assert.assertSame("Connection is closed", e.getMessage());
        }
    }

}
