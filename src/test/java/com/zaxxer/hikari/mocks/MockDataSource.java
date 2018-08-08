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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author Brett Wooldridge
 */
public class MockDataSource implements DataSource
{
    @Override
    public Connection getConnection() throws SQLException
    {
        return createMockConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    public static Connection createMockConnection() throws SQLException {
        // Setup mock connection
        final Connection mockConnection = mock(Connection.class);

        // Autocommit is always true by default
        when(mockConnection.getAutoCommit()).thenReturn(true);

        // Handle Connection.createStatement()
        Statement statement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(statement);
        when(mockConnection.createStatement(anyInt(), anyInt())).thenReturn(statement);
        when(mockConnection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(statement);
        when(mockConnection.isValid(anyInt())).thenReturn(true);

        // Handle Connection.prepareStatement()
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), any(int[].class))).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), any(String[].class))).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(mockPreparedStatement);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                return null;
            }
        }).doNothing().when(mockPreparedStatement).setInt(anyInt(), anyInt());

        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.getString(anyInt())).thenReturn("aString");
        when(mockResultSet.next()).thenReturn(true);

        // Handle Connection.prepareCall()
        CallableStatement mockCallableStatement = mock(CallableStatement.class);
        when(mockConnection.prepareCall(anyString())).thenReturn(mockCallableStatement);
        when(mockConnection.prepareCall(anyString(), anyInt(), anyInt())).thenReturn(mockCallableStatement);
        when(mockConnection.prepareCall(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(mockCallableStatement);

        // Handle Connection.close()
//        doAnswer(new Answer<Void>() {
//            public Void answer(InvocationOnMock invocation) throws Throwable {
//                return null;
//            }
//        }).doThrow(new SQLException("Connection is already closed")).when(mockConnection).close();

        // Handle Connection.commit()
//        doAnswer(new Answer<Void>() {
//            public Void answer(InvocationOnMock invocation) throws Throwable {
//                return null;
//            }
//        }).doThrow(new SQLException("Transaction already committed")).when(mockConnection).commit();

        // Handle Connection.rollback()
//        doAnswer(new Answer<Void>() {
//            public Void answer(InvocationOnMock invocation) throws Throwable {
//                return null;
//            }
//        }).doThrow(new SQLException("Transaction already rolledback")).when(mockConnection).rollback();

        return mockConnection;
    }
}
