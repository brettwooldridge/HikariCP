package com.zaxxer.hikari.proxy;

import java.sql.SQLException;

public interface IHikariStatementProxy
{
    void close() throws SQLException;

    void _setConnectionProxy(IHikariConnectionProxy connectionProxy);

    SQLException _checkException(SQLException e);
}
