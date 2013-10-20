package com.zaxxer.hikari.proxy;

import java.sql.SQLException;

public interface IHikariStatementProxy
{
    void close() throws SQLException;

    void setConnectionProxy(IHikariConnectionProxy connectionProxy);

    SQLException checkException(SQLException e);
}
