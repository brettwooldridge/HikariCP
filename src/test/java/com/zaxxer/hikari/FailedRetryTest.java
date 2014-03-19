package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.mocks.StubDataSource;

public class FailedRetryTest
{
    @Test
    public void testConnectionRetries() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(0);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(2800);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);

        StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
        stubDataSource.setThrowException(new SQLException("Connection refused"));

        long start = System.currentTimeMillis();
        try
        {
            Connection connection = ds.getConnection();
            connection.close();
            Assert.fail("Should not have been able to get a connection.");
        }
        catch (SQLException e)
        {
            long elapsed = System.currentTimeMillis() - start;
            System.err.printf("Elapsed time for connection attempt %dms\n", elapsed);
            Assert.assertTrue("Didn't wait long enough for timeout", (elapsed > config.getConnectionTimeout()));
        }
    }

    @Test
    public void testConnectionRetries2() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(0);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(2800);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);

        final long timePerTry = config.getConnectionTimeout() / (config.getAcquireRetries() + 1);

        final StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
        stubDataSource.setThrowException(new SQLException("Connection refused"));

        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            public void run()
            {
                stubDataSource.setThrowException(null);
                System.err.println("Turned off exception throwing.");
            }
        }, (timePerTry * 2) + 100, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        try
        {
            Connection connection = ds.getConnection();
            System.err.println("Got a connection!");
            // connection.close();
            Assert.fail("Should not have been able to get a connection.");
        }
        catch (SQLException e)
        {
            long elapsed = System.currentTimeMillis() - start;
            System.err.printf("Elapsed time for connection attempt %dms\n", elapsed);
            Assert.assertTrue("Didn't wait long enough for timeout", (elapsed > timePerTry * 3) && (elapsed < timePerTry * 4));
        }
    }
}
