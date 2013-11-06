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

package com.zaxxer.hikari.performance;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 *
 * @author Brett Wooldridge
 */
public class StubResultSet implements ResultSet
{
    private int counter;
    private boolean closed;

    /** {@inheritDoc} */
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        
        return null;
    }

    /** {@inheritDoc} */
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean next() throws SQLException
    {
        return (counter < 10);
    }

    /** {@inheritDoc} */
    public void close() throws SQLException
    {
        closed = true;
    }

    /** {@inheritDoc} */
    public boolean wasNull() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public String getString(int columnIndex) throws SQLException
    {
        return "aString";
    }

    /** {@inheritDoc} */
    public boolean getBoolean(int columnIndex) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public byte getByte(int columnIndex) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public short getShort(int columnIndex) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public int getInt(int columnIndex) throws SQLException
    {
        return ++counter;
    }

    /** {@inheritDoc} */
    public long getLong(int columnIndex) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public float getFloat(int columnIndex) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public double getDouble(int columnIndex) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public byte[] getBytes(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Date getDate(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Time getTime(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Timestamp getTimestamp(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public InputStream getAsciiStream(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public InputStream getUnicodeStream(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public InputStream getBinaryStream(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public String getString(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean getBoolean(String columnLabel) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public byte getByte(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public short getShort(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public int getInt(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public long getLong(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public float getFloat(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public double getDouble(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public byte[] getBytes(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Date getDate(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Time getTime(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Timestamp getTimestamp(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public InputStream getAsciiStream(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public InputStream getUnicodeStream(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public InputStream getBinaryStream(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public SQLWarning getWarnings() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void clearWarnings() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public String getCursorName() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public ResultSetMetaData getMetaData() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Object getObject(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Object getObject(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public int findColumn(String columnLabel) throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public Reader getCharacterStream(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Reader getCharacterStream(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isBeforeFirst() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean isAfterLast() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean isFirst() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean isLast() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public void beforeFirst() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void afterLast() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public boolean first() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean last() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public int getRow() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public boolean absolute(int row) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean relative(int rows) throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean previous() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public void setFetchDirection(int direction) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public int getFetchDirection() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public void setFetchSize(int rows) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public int getFetchSize() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public int getType() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public int getConcurrency() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public boolean rowUpdated() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean rowInserted() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public boolean rowDeleted() throws SQLException
    {
        return false;
    }

    /** {@inheritDoc} */
    public void updateNull(int columnIndex) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateByte(int columnIndex, byte x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateShort(int columnIndex, short x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateInt(int columnIndex, int x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateLong(int columnIndex, long x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateFloat(int columnIndex, float x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateDouble(int columnIndex, double x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateString(int columnIndex, String x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBytes(int columnIndex, byte[] x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateDate(int columnIndex, Date x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateTime(int columnIndex, Time x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateObject(int columnIndex, Object x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNull(String columnLabel) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBoolean(String columnLabel, boolean x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateByte(String columnLabel, byte x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateShort(String columnLabel, short x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateInt(String columnLabel, int x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateLong(String columnLabel, long x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateFloat(String columnLabel, float x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateDouble(String columnLabel, double x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateString(String columnLabel, String x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBytes(String columnLabel, byte[] x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateDate(String columnLabel, Date x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateTime(String columnLabel, Time x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateObject(String columnLabel, Object x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void insertRow() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateRow() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void deleteRow() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void refreshRow() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void cancelRowUpdates() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void moveToInsertRow() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void moveToCurrentRow() throws SQLException
    {
    }

    /** {@inheritDoc} */
    public Statement getStatement() throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Ref getRef(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Blob getBlob(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Clob getClob(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Array getArray(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Ref getRef(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Blob getBlob(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Clob getClob(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Array getArray(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Date getDate(int columnIndex, Calendar cal) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Date getDate(String columnLabel, Calendar cal) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Time getTime(int columnIndex, Calendar cal) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Time getTime(String columnLabel, Calendar cal) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public URL getURL(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public URL getURL(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void updateRef(int columnIndex, Ref x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateRef(String columnLabel, Ref x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBlob(int columnIndex, Blob x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBlob(String columnLabel, Blob x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateClob(int columnIndex, Clob x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateClob(String columnLabel, Clob x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateArray(int columnIndex, Array x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateArray(String columnLabel, Array x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public RowId getRowId(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public RowId getRowId(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void updateRowId(int columnIndex, RowId x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateRowId(String columnLabel, RowId x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public int getHoldability() throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc} */
    public boolean isClosed() throws SQLException
    {
        return closed;
    }

    /** {@inheritDoc} */
    public void updateNString(int columnIndex, String nString) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNString(String columnLabel, String nString) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public NClob getNClob(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public NClob getNClob(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public SQLXML getSQLXML(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public SQLXML getSQLXML(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public String getNString(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public String getNString(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Reader getNCharacterStream(int columnIndex) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public Reader getNCharacterStream(String columnLabel) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateClob(int columnIndex, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateClob(String columnLabel, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNClob(int columnIndex, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public void updateNClob(String columnLabel, Reader reader) throws SQLException
    {
    }

    /** {@inheritDoc} */
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException
    {
        return null;
    }

    /** {@inheritDoc} */
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException
    {
        return null;
    }

}
