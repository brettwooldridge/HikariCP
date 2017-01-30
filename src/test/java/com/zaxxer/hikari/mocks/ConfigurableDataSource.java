package com.zaxxer.hikari.mocks;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSourceConfigurable;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author iobestar
 */
public class ConfigurableDataSource implements DataSource, HikariDataSourceConfigurable {

    public static volatile AtomicInteger configureInvocationCount = new AtomicInteger(0);

    @Override
    public void configure(HikariConfig config) {
        configureInvocationCount.incrementAndGet();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return MockDataSource.createMockConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
