package com.zaxxer.hikari.proxy;

import java.sql.SQLException;

public interface IHikariStatementProxy
{
    void close() throws SQLException;

    void _setConnectionProxy(IHikariConnectionProxy connectionProxy);

    void _releaseResultSet(IHikariResultSetProxy resultSet);

    SQLException _checkException(SQLException e);
}
