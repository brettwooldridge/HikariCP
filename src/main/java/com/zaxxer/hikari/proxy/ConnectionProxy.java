/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariPool;
import com.zaxxer.hikari.util.FastStatementList;

/**
 * This is the proxy class for java.sql.Connection.
 *
 * @author Brett Wooldridge
 */
public abstract class ConnectionProxy implements IHikariConnectionProxy
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxy.class);

    private static final Set<String> SQL_ERRORS;

    protected final Connection delegate;

    private final long creationTime;
    private final FastStatementList openStatements;
    private final HikariPool parentPool;
    private final int defaultIsolationLevel;
    private final boolean defaultAutoCommit;
    private final String defaultCatalog;
    private final AtomicInteger state;

    private boolean isClosed;
    private boolean forceClose;
    private boolean isTransactionIsolationDirty;
    private boolean isAutoCommitDirty;
    private boolean isCatalogDirty;
    private volatile long lastAccess;

    private StackTraceElement[] leakTrace;
    private TimerTask leakTask;

    private final int hashCode;


    // static initializer
    static
    {
        SQL_ERRORS = new HashSet<String>();
        SQL_ERRORS.add("57P01");  // ADMIN SHUTDOWN
        SQL_ERRORS.add("57P02");  // CRASH SHUTDOWN
        SQL_ERRORS.add("57P03");  // CANNOT CONNECT NOW
        SQL_ERRORS.add("01002");  // SQL92 disconnect error
        SQL_ERRORS.add("JZ0C0");  // Sybase disconnect error
        SQL_ERRORS.add("JZ0C1");  // Sybase disconnect error
    }

    protected ConnectionProxy(HikariPool pool, Connection connection, int defaultIsolationLevel, boolean defaultAutoCommit, String defaultCatalog)
    {
        this.parentPool = pool;
        this.delegate = connection;
        this.defaultIsolationLevel = defaultIsolationLevel;
        this.defaultAutoCommit = defaultAutoCommit;
        this.defaultCatalog = defaultCatalog;
        this.state = new AtomicInteger();

        this.creationTime = lastAccess = System.currentTimeMillis();
        this.openStatements = new FastStatementList();
        this.hashCode = System.identityHashCode(this);

        isCatalogDirty = true;
        isAutoCommitDirty = true;
        isTransactionIsolationDirty = true;
    }

    public final void untrackStatement(Object statement)
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
            if (forceClose)
            {
                LOGGER.warn("Connection {} marked as broken because of SQLSTATE({}), ErrorCode({}): {}", delegate.toString(), sqlState, sqle.getErrorCode(), sqle.getNextException());
            }
            else if (sqle.getNextException() instanceof SQLException)
            {
                checkException(sqle.getNextException());
            }
        }
    }

    @Override
    public final boolean equals(Object other)
    {
        return this == other;
    }

    @Override
    public final int hashCode()
    {
        return hashCode;
    }

    protected final void checkClosed() throws SQLException
    {
        if (isClosed)
        {
            throw new SQLException("Connection is closed");
        }
    }

    private <T extends Statement> T trackStatement(T statement)
    {
        openStatements.add(statement);

        return statement;
    }

    public final void resetConnectionState() throws SQLException
    {
        if (!delegate.getAutoCommit())
        {
            delegate.rollback();
        }

        if (isAutoCommitDirty)
        {
            delegate.setAutoCommit(defaultAutoCommit);
            isAutoCommitDirty = false;
        }

        if (isTransactionIsolationDirty)
        {
            delegate.setTransactionIsolation(defaultIsolationLevel);
            isTransactionIsolationDirty = false;
        }

        if (isCatalogDirty && defaultCatalog != null)
        {
            delegate.setCatalog(defaultCatalog);
            isCatalogDirty = false;
        }
        
        delegate.clearWarnings();
    }

    // **********************************************************************
    //                       IBagManagable Methods
    // **********************************************************************

    /** {@inheritDoc} */
    @Override
    public final int getState()
    {
        return state.get();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean compareAndSetState(int expectedState, int newState)
    {
        return state.compareAndSet(expectedState, newState);
    }

    // **********************************************************************
    //                   "Overridden" java.sql.Connection Methods
    // **********************************************************************

    /** {@inheritDoc} */
    public final void close() throws SQLException
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
                final int size = openStatements.size();
                for (int i = 0; i < size; i++)
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
                if (size > 0)
                {
                    openStatements.clear();
                }

                resetConnectionState();
            }
            catch (SQLException e)
            {
                checkException(e);
                throw e;
            }
            finally
            {
                lastAccess = System.currentTimeMillis();
                parentPool.releaseConnection(this);
            }
        }
    }

    /** {@inheritDoc} */
    public final boolean isClosed() throws SQLException
    {
        return isClosed;
    }

    /** {@inheritDoc} */
    public final Statement createStatement() throws SQLException
    {
        checkClosed();
        try
        {
            Statement proxyStatement = ProxyFactory.getProxyStatement(this, delegate.createStatement());
            return trackStatement(proxyStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        checkClosed();
        try
        {
            Statement proxyStatement = ProxyFactory.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency));
            return trackStatement(proxyStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        try
        {
            Statement proxyStatement = ProxyFactory.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
            return trackStatement(proxyStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final CallableStatement prepareCall(String sql) throws SQLException
    {
        checkClosed();
        try
        {
            CallableStatement proxyCallableStatement = ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql));
            return trackStatement(proxyCallableStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        checkClosed();
        try
        {
            CallableStatement proxyCallableStatement = ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency));
            return trackStatement(proxyCallableStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        try
        {
            CallableStatement proxyCallableStatement = ProxyFactory.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
            return trackStatement(proxyCallableStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final PreparedStatement prepareStatement(String sql) throws SQLException
    {
        checkClosed();
        try
        {
            PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql));
            return trackStatement(proxyPreparedStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        checkClosed();
        try
        {
            PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, autoGeneratedKeys));
            return trackStatement(proxyPreparedStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        checkClosed();
        try
        {
            PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
            return trackStatement(proxyPreparedStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        try
        {
            PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
            return trackStatement(proxyPreparedStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        checkClosed();
        try
        {
            PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnIndexes));
            return trackStatement(proxyPreparedStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        checkClosed();
        try
        {
            PreparedStatement proxyPreparedStatement = ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
            return trackStatement(proxyPreparedStatement);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final boolean isValid(int timeout) throws SQLException
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
    public final void setAutoCommit(boolean autoCommit) throws SQLException
    {
        checkClosed();
        try
        {
            delegate.setAutoCommit(autoCommit);
            isAutoCommitDirty = (autoCommit != defaultAutoCommit);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final void setTransactionIsolation(int level) throws SQLException
    {
        checkClosed();
        try
        {
            delegate.setTransactionIsolation(level);
            isTransactionIsolationDirty = (level != defaultIsolationLevel);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }
    }

    @Override
    public final void setCatalog(String catalog) throws SQLException
    {
        checkClosed();
        try
        {
            delegate.setCatalog(catalog);
            isCatalogDirty = !catalog.equals(defaultCatalog);
        }
        catch (SQLException e)
        {
            checkException(e);
            throw e;
        }        
    }

    /** {@inheritDoc} */
    public final boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return iface.isInstance(delegate);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public final <T> T unwrap(Class<T> iface) throws SQLException
    {
        if (iface.isInstance(delegate))
        {
            return (T) delegate;
        }

        throw new SQLException("Wrapped connection is not an instance of " + iface);
    }
}
