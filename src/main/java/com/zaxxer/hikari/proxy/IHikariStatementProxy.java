package com.zaxxer.hikari.proxy;

import java.sql.SQLException;

public interface IHikariStatementProxy
{
    void close() throws SQLException;

    void _setConnectionProxy(IHikariConnectionProxy connectionProxy);

    IHikariConnectionProxy _getConnectionProxy();

    void _releaseResultSet(IHikariResultSetProxy resultSet);

    SQLException _checkException(SQLException e);
}
