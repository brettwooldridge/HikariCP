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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariPool;

/**
 *
 * @author Brett Wooldridge
 */
public class ConnectionProxy extends HikariProxyBase implements IHikariConnectionProxy
{
    private static final ProxyFactory PROXY_FACTORY;

    private static final Set<String> POSTGRESQL_ERRORS;
    private static final Set<String> SPECIAL_ERRORS;

    private final ArrayList<Statement> openStatements;
    private final AtomicBoolean isClosed;

    private final HikariPool parentPool;
    
    protected final Connection delegate;

    private volatile boolean forceClose;

    private final long creationTime;
    private long lastAccess;

    private StackTraceElement[] stackTrace;

    private TimerTask leakTask;

    // static initializer
    static
    {
        POSTGRESQL_ERRORS = new HashSet<String>();
        POSTGRESQL_ERRORS.add("57P01");  // ADMIN SHUTDOWN
        POSTGRESQL_ERRORS.add("57P02");  // CRASH SHUTDOWN
        POSTGRESQL_ERRORS.add("57P03");  // CANNOT CONNECT NOW
        POSTGRESQL_ERRORS.add("57P02");  // CRASH SHUTDOWN

        SPECIAL_ERRORS = new HashSet<String>();
        SPECIAL_ERRORS.add("01002");  // SQL92 disconnect error

        PROXY_FACTORY = JavassistProxyFactoryFactory.getProxyFactory();
    }

    // Instance initializer
    {
        openStatements = new ArrayList<Statement>(64);
        isClosed = new AtomicBoolean();
        creationTime = lastAccess = System.currentTimeMillis();
    }

    protected ConnectionProxy(HikariPool parentPool, Connection connection)
    {
        this.parentPool = parentPool;
        this.delegate = connection;
    }

    void unregisterStatement(Object statement)
    {
        // If the connection is not closed.  If it is closed, it means this is being
        // called back as a result of the close() method below in which case we
        // will clear the openStatements collection en mass.
        if (!isClosed.get())
        {
            openStatements.remove(statement);
        }
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public long getLastAccess()
    {
        return lastAccess;
    }

    public void setLastAccess(long timestamp)
    {
        this.lastAccess = timestamp;
    }

    public void unclose()
    {
        isClosed.set(false);
    }

    public Connection getDelegate()
    {
        return delegate;
    }

    public void captureStack(long leakDetectionThreshold, Timer scheduler)
    {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        stackTrace = new StackTraceElement[trace.length - 4];
        System.arraycopy(trace, 4, stackTrace, 0, stackTrace.length);

        final long leakTime = System.currentTimeMillis() + leakDetectionThreshold;
        leakTask = new TimerTask()
        {
            public void run()
            {
                if (System.currentTimeMillis() > leakTime)
                {
                    Exception e = new Exception();
                    e.setStackTrace(stackTrace);
                    LoggerFactory.getLogger(ConnectionProxy.this.getClass()).warn("Connection leak detection triggered, stack trace follows", e);
                    stackTrace = null;
                }
            }
        };

        scheduler.schedule(leakTask, leakDetectionThreshold);
    }

    public boolean isBrokenConnection()
    {
        return forceClose;
    }
    
    protected SQLException checkException(SQLException sqle)
    {
        String sqlState = sqle.getSQLState();
        if (sqlState == null)
        {
            return sqle;
        }

        sqlState = sqlState.toUpperCase();
        if (sqlState.startsWith("08"))
        {
            forceClose = true;
        }
        else if (POSTGRESQL_ERRORS.contains(sqlState.toUpperCase()) || SPECIAL_ERRORS.contains(sqlState))
        {
            forceClose = true;
        }

        return sqle;
    }

    // **********************************************************************
    //                   Overridden java.sql.Connection Methods
    //                        other methods are injected
    // **********************************************************************

    /* (non-Javadoc)
     * @see java.sql.Connection#close()
     */
    public void close() throws SQLException
    {
        if (isClosed.compareAndSet(false, true))
        {
            if (leakTask != null)
            {
                leakTask.cancel();
                leakTask = null;
            }

            if (delegate.getAutoCommit())
            {
                delegate.commit();
            }

            try
            {
                for (Statement statement : openStatements)
                {
                    statement.close();
                }
            }
            catch (SQLException e)
            {
                throw checkException(e);
            }
            finally
            {
                openStatements.clear();
                parentPool.releaseConnection(this);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#isClosed()
     */
    public boolean isClosed() throws SQLException
    {
        return isClosed.get();
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#createStatement()
     */
    public Statement createStatement() throws SQLException
    {
        try
        {
            Statement statementProxy = PROXY_FACTORY.getProxyStatement(this, delegate.createStatement());
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#createStatement(int, int)
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        try
        {
            Statement statementProxy = PROXY_FACTORY.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        try
        {
            Statement statementProxy = PROXY_FACTORY.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        try
        {
            CallableStatement statementProxy = PROXY_FACTORY.getProxyCallableStatement(this, delegate.prepareCall(sql));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        try
        {
            CallableStatement statementProxy = PROXY_FACTORY.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        try
        {
            CallableStatement statementProxy = PROXY_FACTORY.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        try
        {
            PreparedStatement statementProxy = PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        try
        {
            PreparedStatement statementProxy = PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, autoGeneratedKeys));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        try
        {
            PreparedStatement statementProxy = PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        try
        {
            PreparedStatement statementProxy = PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        try
        {
            PreparedStatement statementProxy = PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnIndexes));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        try
        {
            PreparedStatement statementProxy = PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
            openStatements.add(statementProxy);

            return statementProxy;
        }
        catch (SQLException e)
        {
            throw checkException(e);
        }
    }
}
