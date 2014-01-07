package com.zaxxer.hikari.proxy;

import java.sql.SQLException;
import java.sql.Statement;

public interface IHikariStatementProxy extends Statement
{
    void close() throws SQLException;

    void _setConnectionProxy(IHikariConnectionProxy connectionProxy);

    IHikariConnectionProxy _getConnectionProxy();

    void _releaseResultSet(IHikariResultSetProxy resultSet);

    void _checkException(SQLException e);
}
