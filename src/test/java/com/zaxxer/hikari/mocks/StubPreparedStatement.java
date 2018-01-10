/*
 * Copyright (C) 2013 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.mocks;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 *
 * @author Brett Wooldridge
 */
@SuppressWarnings("RedundantThrows")
public class StubPreparedStatement extends StubStatement implements PreparedStatement
{
    StubPreparedStatement(Connection connection)
    {
        super(connection);
    }

    /** {@inheritDoc} */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException
    {
        return new StubResultSet();
    }

    /** {@inheritDoc} */
    @Override
    public int executeUpdate(String sql) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxFieldSize() throws SQLException
    {
        throw new SQLException("Simulated disconnection error", "08999");
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxFieldSize(int max) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxRows() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxRows(int max) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public int getQueryTimeout() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setQueryTimeout(int seconds) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void clearWarnings() throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setCursorName(String name) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute(String sql) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public ResultSet getResultSet() throws SQLException
    {
        return new StubResultSet();
    }

    /** {@inheritDoc} */
    @Override
    public int getUpdateCount() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getMoreResults() throws SQLException
    {
        if (isClosed())
        {
            throw new SQLException("Connection is closed");
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setFetchDirection(int direction) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public int getFetchDirection() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setFetchSize(int rows) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public int getFetchSize() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getResultSetConcurrency() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getResultSetType() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void addBatch(String sql) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void clearBatch() throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public int[] executeBatch() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getMoreResults(int current) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException
    {
        return new StubResultSet();
    }

    /** {@inheritDoc} */
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int getResultSetHoldability() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setPoolable(boolean poolable) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPoolable() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void closeOnCompletion() throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCloseOnCompletion() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
       if (iface.isInstance(this)) {
          return (T) this;
       }

       throw new SQLException("Wrapped connection is not an instance of " + iface);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public ResultSet executeQuery() throws SQLException
    {
        return new StubResultSet();
    }

    /** {@inheritDoc} */
    @Override
    public int executeUpdate() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setShort(int parameterIndex, short x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setInt(int parameterIndex, int x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setLong(int parameterIndex, long x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setString(int parameterIndex, String x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("deprecation")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void clearParameters() throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void addBatch() throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNString(int parameterIndex, String value) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException
    {
    }

    /** {@inheritDoc} */
    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException
    {
    }

}
