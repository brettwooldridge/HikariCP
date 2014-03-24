package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionStateTest
{
    @Test
    public void testAutoCommit() throws SQLException
    {
        HikariDataSource ds = new HikariDataSource();
        ds.setAutoCommit(true);
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(1);
        ds.setConnectionTestQuery("VALUES 1");
        ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        try
        {
            Connection connection = ds.getConnection();
            connection.setAutoCommit(false);
            connection.close();

            Connection connection2 = ds.getConnection();
            Assert.assertSame(connection, connection2);
            Assert.assertTrue(connection2.getAutoCommit());
            connection2.close();
        }
        finally
        {
            ds.shutdown();
        }
    }

    @Test
    public void testTransactionIsolation() throws SQLException
    {
        HikariDataSource ds = new HikariDataSource();
        ds.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(1);
        ds.setConnectionTestQuery("VALUES 1");
        ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        try
        {
            Connection connection = ds.getConnection();
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            connection.close();

            Connection connection2 = ds.getConnection();
            Assert.assertSame(connection, connection2);
            Assert.assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection2.getTransactionIsolation());
            connection2.close();
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

    @Test
    public void testCatalog() throws SQLException
    {
        HikariDataSource ds = new HikariDataSource();
        ds.setCatalog("test");
        ds.setMinimumIdle(1);
        ds.setMaximumPoolSize(1);
        ds.setConnectionTestQuery("VALUES 1");
        ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        try
        {
            Connection connection = ds.getConnection();
            connection.setCatalog("other");
            connection.close();

            Connection connection2 = ds.getConnection();
            Assert.assertSame(connection, connection2);
            Assert.assertEquals("test", connection2.getCatalog());
            connection2.close();
        }
        finally
        {
            ds.shutdown();
        }
    }
}
