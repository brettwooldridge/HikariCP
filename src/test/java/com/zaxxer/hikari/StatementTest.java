package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assert;

public class StatementTest
{
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

        ResultSet resultSet = statement.executeQuery("SELECT * from foo");
        Assert.assertNotNull(resultSet);

        connection.close();

        Assert.assertTrue(statement.isClosed());
        Assert.assertTrue(resultSet.isClosed());
    }
}
