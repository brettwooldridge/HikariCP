package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.mocks.StubDataSource;

public class TestConnectionTimeoutRetry
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
            Assert.assertTrue("Didn't wait long enough for timeout", (elapsed > config.getConnectionTimeout()));
        }
        finally
        {
            ds.shutdown();
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

        final StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
        stubDataSource.setThrowException(new SQLException("Connection refused"));

        final long timePerTry = config.getConnectionTimeout() / (config.getAcquireRetries() + 1);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(new Runnable() {
            public void run()
            {
                stubDataSource.setThrowException(null);
            }
        }, (timePerTry * 2) + 100, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        try
        {
            Connection connection = ds.getConnection();
            connection.close();

            long elapsed = System.currentTimeMillis() - start;
            Assert.assertTrue("Waited too long to get a connection.", (elapsed >= timePerTry * 3) && (elapsed < config.getConnectionTimeout()));
        }
        catch (SQLException e)
        {
            Assert.fail("Should not have timed out.");
        }
        finally
        {
            scheduler.shutdownNow();
            ds.shutdown();
        }
    }

    @Test
    public void testConnectionRetries3() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(0);
        config.setMaximumPoolSize(2);
        config.setConnectionTimeout(2800);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);

        final Connection connection1 = ds.getConnection();
        final Connection connection2 = ds.getConnection();
        Assert.assertNotNull(connection1);
        Assert.assertNotNull(connection2);

        final long timePerTry = config.getConnectionTimeout() / (config.getAcquireRetries() + 1);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.schedule(new Runnable() {
            public void run()
            {
                try
                {
                    connection1.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace(System.err);
                }
            }
        }, timePerTry  + 100, TimeUnit.MILLISECONDS);

        scheduler.schedule(new Runnable() {
            public void run()
            {
                try
                {
                    connection2.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace(System.err);
                }
            }
        }, (timePerTry * 2) + 100, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        try
        {
            Connection connection3 = ds.getConnection();
            connection3.close();

            long elapsed = System.currentTimeMillis() - start;
            Assert.assertTrue("Waited too long to get a connection.", (elapsed >= timePerTry) && (elapsed < timePerTry * 2));
        }
        catch (SQLException e)
        {
            Assert.fail("Should not have timed out.");
        }
        finally
        {
            scheduler.shutdownNow();
            ds.shutdown();
        }
    }

    @Test
    public void testConnectionRetries4() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(0);
        config.setMaximumPoolSize(1);
        config.setAcquireRetries(0);
        config.setConnectionTimeout(1000);
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
            Assert.assertTrue("Didn't wait long enough for timeout", (elapsed > config.getConnectionTimeout()));
        }
        finally
        {
            ds.shutdown();
        }
    }


    @Test
    public void testConnectionRetries5() throws SQLException
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(0);
        config.setMaximumPoolSize(2);
        config.setAcquireRetries(0);
        config.setConnectionTimeout(1000);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);

        final Connection connection1 = ds.getConnection();

        long start = System.currentTimeMillis();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.schedule(new Runnable() {
            public void run()
            {
                try
                {
                    connection1.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace(System.err);
                }
            }
        }, 250, TimeUnit.MILLISECONDS);

        StubDataSource stubDataSource = ds.unwrap(StubDataSource.class);
        stubDataSource.setThrowException(new SQLException("Connection refused"));

        try
        {
            Connection connection2 = ds.getConnection();
            connection2.close();

            long elapsed = System.currentTimeMillis() - start;
            Assert.assertTrue("Waited too long to get a connection.", (elapsed >= 250) && (elapsed < config.getConnectionTimeout()));
        }
        catch (SQLException e)
        {
            Assert.fail("Should not have timed out.");
        }
        finally
        {
            scheduler.shutdownNow();
            ds.shutdown();
        }
    }
}
