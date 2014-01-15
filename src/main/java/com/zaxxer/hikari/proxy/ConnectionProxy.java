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

package com.zaxxer.hikari.proxy;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.zaxxer.hikari.HikariPool;
import com.zaxxer.hikari.util.FastStatementList;

/**
 * This is the proxy class for java.sql.Connection.
 *
 * @author Brett Wooldridge
 */
public abstract class ConnectionProxy implements IHikariConnectionProxy
{
    private static final Set<String> SQL_ERRORS;

    protected final Connection delegate;

    private final FastStatementList openStatements;
    private final HikariPool parentPool;

    private boolean isClosed;
    private boolean forceClose;
    private boolean isTransactionIsolationDirty;
    
    private final long creationTime;
    private volatile long lastAccess;

    private StackTraceElement[] leakTrace;
    private TimerTask leakTask;

    // static initializer
    static
    {
        SQL_ERRORS = new HashSet<String>();
        SQL_ERRORS.add("57P01");  // ADMIN SHUTDOWN
        SQL_ERRORS.add("57P02");  // CRASH SHUTDOWN
        SQL_ERRORS.add("57P03");  // CANNOT CONNECT NOW
        SQL_ERRORS.add("57P02");  // CRASH SHUTDOWN
        SQL_ERRORS.add("01002");  // SQL92 disconnect error
    }

    protected ConnectionProxy(HikariPool pool, Connection connection)
    {
        this.parentPool = pool;
        this.delegate = connection;

        creationTime = lastAccess = System.currentTimeMillis();
        openStatements = new FastStatementList();
        isTransactionIsolationDirty = true;
    }
    
    public final void unregisterStatement(Object statement)
    {
        // If the connection is not closed.  If it is closed, it means this is being
        // called back as a result of the close() method below in which case we
        // will clear the openStatements collection en mass.
        if (!isClosed)
        {
            openStatements.remove(statement);
        }
    }
    
    public final long getCreationTime()
    {
        return creationTime;
    }

    public final long getLastAccess()
    {
        return lastAccess;
    }

    public final void unclose()
    {
        isClosed = false;
    }

    public final void realClose() throws SQLException
    {
        delegate.close();
    }

    public final void captureStack(long leakDetectionThreshold, Timer scheduler)
    {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        leakTrace = new StackTraceElement[trace.length - 4];
        System.arraycopy(trace, 4, leakTrace, 0, leakTrace.length);

        leakTask = new LeakTask(leakTrace, leakDetectionThreshold);
        scheduler.schedule(leakTask, leakDetectionThreshold);
    }

    public final boolean isTransactionIsolationDirty()
    {
        return isTransactionIsolationDirty;
    }

    public void resetTransactionIsolationDirty()
    {
        isTransactionIsolationDirty = false;
    }

    public final boolean isBrokenConnection()
    {
        return forceClose;
    }

    public final void checkException(SQLException sqle)
    {
        String sqlState = sqle.getSQLState();
        if (sqlState != null)
        {
            forceClose |= sqlState.startsWith("08") | SQL_ERRORS.contains(sqlState);
        }
    }

    protected final void checkClosed() throws SQLException
    {
        if (isClosed)
        {
            throw new SQLException("Connection is closed");
        }
    }

    private final <T extends Statement> T trackStatement(T statement)
    {
        openStatements.add(statement);

        return statement;
    }

    // **********************************************************************
    //                   "Overridden" java.sql.Connection Methods
    // **********************************************************************

    /** {@inheritDoc} */
    public void close() throws SQLException
    {
        if (!isClosed)
        {
            isClosed = true;

            if (leakTask != null)
            {
                leakTask.cancel();
                leakTask = null;
            }

            try
            {
                // Faster than an iterator most times
                final int length = openStatements.size();
                for (int i = 0; i < length; i++)
                {
                    try
                    {
                        openStatements.get(i).close();
                    }
                    catch (SQLException e)
                    {
                        checkException(e);
                    }
                }

                if (!delegate.getAutoCommit())
                {
                    delegate.rollback();
                }
            }
            catch (SQLException e)
            {
                checkException(e);
                throw e;
            }
            finally
            {
                openStatements.clear();
                lastAccess = System.currentTimeMillis();
                parentPool.releaseConnection(this);
            }
        }
    }

    /** {@inheritDoc} */
    public boolean isClosed() throws SQLException
    {
        return isClosed;
    }

    /** {@inheritDoc} */
    public Statement createStatement() throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__createStatement());
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__createStatement(resultSetType, resultSetConcurrency));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareCall(sql));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareCall(sql, resultSetType, resultSetConcurrency));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareStatement(sql));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareStatement(sql, autoGeneratedKeys));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareStatement(sql, resultSetType, resultSetConcurrency));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareStatement(sql, columnIndexes));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        checkClosed();
        try
        {
            return trackStatement(__prepareStatement(sql, columnNames));
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public boolean isValid(int timeout) throws SQLException
    {
        if (isClosed)
        {
            return false;
        }

        try
        {
            return delegate.isValid(timeout);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public void setTransactionIsolation(int level) throws SQLException
    {
        checkClosed();
        try
        {
            delegate.setTransactionIsolation(level);
            isTransactionIsolationDirty = true;
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    // **********************************************************************
    //                          Private Methods
    // **********************************************************************

    private final Statement __createStatement() throws SQLException
    {
        return ProxyFactory.getProxyStatement(this, delegate.createStatement());
    }

    private final Statement __createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return ProxyFactory.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency));
    }

    private final Statement __createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return ProxyFactory.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    private final CallableStatement __prepareCall(String sql) throws SQLException
    {
        return ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql));
    }

    private final CallableStatement __prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency));
    }

    private final CallableStatement __prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    private final PreparedStatement __prepareStatement(String sql) throws SQLException
    {
        return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql));
    }

    private final PreparedStatement __prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, autoGeneratedKeys));
    }

    private final PreparedStatement __prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    private final PreparedStatement __prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    private final PreparedStatement __prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnIndexes));
    }

    private final PreparedStatement __prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
    }
}
