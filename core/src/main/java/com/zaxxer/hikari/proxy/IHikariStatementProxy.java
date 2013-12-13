package com.zaxxer.hikari.proxy;

import java.sql.CallableStatement;
import java.sql.SQLException;

public interface IHikariStatementProxy extends CallableStatement
{
    void close() throws SQLException;

    void _setConnectionProxy(IHikariConnectionProxy connectionProxy);

    IHikariConnectionProxy _getConnectionProxy();

    void _releaseResultSet(IHikariResultSetProxy resultSet);

    void _checkException(SQLException e);
}
