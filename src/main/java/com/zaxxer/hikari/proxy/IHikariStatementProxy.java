package com.zaxxer.hikari.proxy;

import java.sql.Connection;
import java.sql.SQLException;

public interface IHikariStatementProxy
{
    void close() throws SQLException;

    void setConnectionProxy(Connection connectionProxy);

    SQLException checkException(SQLException e);
}
