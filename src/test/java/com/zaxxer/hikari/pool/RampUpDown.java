package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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

        try (HikariDataSource ds = new HikariDataSource(config)) {

           ds.setIdleTimeout(1000);
           HikariPool pool = TestElf.getPool(ds);

           Assert.assertSame("Total connections not as expected", 5, pool.getTotalConnections());

           Connection[] connections = new Connection[ds.getMaximumPoolSize()];
           for (int i = 0; i < connections.length; i++)
           {
               connections[i] = ds.getConnection();
           }

           Assert.assertSame("Total connections not as expected", 60, pool.getTotalConnections());

           for (Connection connection : connections)
           {
               connection.close();
           }

           Thread.sleep(2500);

           Assert.assertSame("Total connections not as expected", 5, pool.getTotalConnections());
        }
    }
}
