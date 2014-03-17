package com.zaxxer.hikari.mocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class StubBaseConnection implements Connection
{
    /** {@inheritDoc} */
    @Override
    public Statement createStatement() throws SQLException
    {
        return new StubStatement(this);
    }

    /** {@inheritDoc} */
    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return new StubPreparedStatement(this);
    }
}
