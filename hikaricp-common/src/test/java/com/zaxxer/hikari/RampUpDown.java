package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

public class RampUpDown
{
    @Test
    public void rampUpDownTest() throws SQLException, InterruptedException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(60);
        config.setInitializationFailFast(true);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "250");

        HikariDataSource ds = new HikariDataSource(config);
        ds.setIdleTimeout(1000);

        Assert.assertSame("Totals connections not as expected", 5, TestElf.getPool(ds).getTotalConnections());

        Connection[] connections = new Connection[ds.getMaximumPoolSize()];
        for (int i = 0; i < connections.length; i++)
        {
            connections[i] = ds.getConnection();
        }

        Assert.assertSame("Totals connections not as expected", 60, TestElf.getPool(ds).getTotalConnections());

        for (Connection connection : connections)
        {
            connection.close();
        }

        Thread.sleep(2500);

        Assert.assertSame("Totals connections not as expected", 5, TestElf.getPool(ds).getTotalConnections());

        ds.close();
    }
}
