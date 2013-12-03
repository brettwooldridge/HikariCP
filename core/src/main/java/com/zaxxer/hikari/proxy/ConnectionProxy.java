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

import com.zaxxer.hikari.HikariPool;
import com.zaxxer.hikari.javassist.HikariInject;
import com.zaxxer.hikari.javassist.HikariOverride;

/**
 * This is the proxy class for java.sql.Connection.  It is used in two ways:
 * 
 *  1) If instrumentation is not used, Javassist will generate a new class
 *     that extends this class and delegates all method calls to the 'delegate'
 *     member (which points to the real Connection).
 *
 *  2) If instrumentation IS used, Javassist will be used to inject all of
 *     the &amp;HikariInject and &amp;HikariOverride annotated fields and methods
 *     of this class into the  actual Connection implementation provided by the
 *     JDBC driver.  In order to avoid name conflicts when injecting code into
 *     a driver class some of the fields and methods are prefixed with _ or __.
 *     
 *     Methods prefixed with __, like __createStatement() are especially
 *     important because when we inject our own createStatement() into the
 *     target implementation, the original method is renamed to __createStatement()
 *     so that the call operates the same whether delegation or instrumentation
 *     is used.
 *
 * @author Brett Wooldridge
 */
public abstract class ConnectionProxy implements IHikariConnectionProxy, Connection
{
    private static ProxyFactory PROXY_FACTORY;

    @HikariInject protected static final Set<String> SQL_ERRORS;

    @HikariInject protected volatile boolean _isClosed;

    @HikariInject protected ArrayList<Statement> _openStatements;
    @HikariInject protected HikariPool _parentPool;
    
    @HikariInject protected volatile boolean _forceClose;
    @HikariInject protected long _creationTime;
    @HikariInject protected long _lastAccess;

    @HikariInject protected StackTraceElement[] _stackTrace;
    @HikariInject protected TimerTask _leakTask;

    protected final Connection delegate;
    
    // static initializer
    static
    {
        SQL_ERRORS = new HashSet<String>();
        SQL_ERRORS.add("57P01");  // ADMIN SHUTDOWN
        SQL_ERRORS.add("57P02");  // CRASH SHUTDOWN
        SQL_ERRORS.add("57P03");  // CANNOT CONNECT NOW
        SQL_ERRORS.add("57P02");  // CRASH SHUTDOWN
        SQL_ERRORS.add("01002");  // SQL92 disconnect error

        // This is important when injecting in instrumentation mode.  Do not change
        // this name without also fixing the HikariClassTransformer.
        __static();
    }

    protected ConnectionProxy(Connection connection)
    {
        this.delegate = connection;

        // This is important when injecting in instrumentation mode.  Do not change
        // this name without also fixing the HikariClassTransformer.
        __init();
    }

    @HikariInject 
    public void _unregisterStatement(Object statement)
    {
        // If the connection is not closed.  If it is closed, it means this is being
        // called back as a result of the close() method below in which case we
        // will clear the openStatements collection en mass.
        if (!_isClosed)
        {
            _openStatements.remove(statement);
        }
    }

    @HikariInject 
    public long _getCreationTime()
    {
        return _creationTime;
    }

    @HikariInject 
    public long _getLastAccess()
    {
        return _lastAccess;
    }

    @HikariInject 
    public void _markLastAccess()
    {
        this._lastAccess = System.currentTimeMillis();
    }

    @HikariInject
    public void _setParentPool(HikariPool parentPool)
    {
        this._parentPool = parentPool;
    }

    @HikariInject 
    public void _unclose()
    {
        _isClosed = false;
    }

    @HikariInject 
    public void _captureStack(long leakDetectionThreshold, Timer scheduler)
    {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        _stackTrace = new StackTraceElement[trace.length - 4];
        System.arraycopy(trace, 4, _stackTrace, 0, _stackTrace.length);

        _leakTask = new LeakTask(_stackTrace, leakDetectionThreshold);
        scheduler.schedule(_leakTask, leakDetectionThreshold);
    }

    @HikariInject 
    public boolean _isBrokenConnection()
    {
        return _forceClose;
    }

    @HikariInject 
    public SQLException _checkException(SQLException sqle)
    {
        String sqlState = sqle.getSQLState();
        _forceClose |= (sqlState != null && (sqlState.startsWith("08") || SQL_ERRORS.contains(sqlState)));

         return sqle;
    }

    @HikariInject
    protected void __init()
    {
        if (_openStatements == null)
        {
            _openStatements = new ArrayList<Statement>(64);
            _creationTime = _lastAccess = System.currentTimeMillis();
        }
    }

    @HikariInject
    protected void _checkClosed() throws SQLException
    {
        if (_isClosed)
        {
            throw new SQLException("Connection is closed");
        }
    }

    @HikariInject
    protected <T extends Statement> T _trackStatement(T statement)
    {
        IHikariStatementProxy statementProxy = (IHikariStatementProxy) statement;
        if (statementProxy._getConnectionProxy() == null)
        {
            statementProxy._setConnectionProxy(this);
            _openStatements.add(statement);
        }

        return statement;
    }

    // **********************************************************************
    //                   "Overridden" java.sql.Connection Methods
    // **********************************************************************

    @HikariOverride
    public void close() throws SQLException
    {
        if (!_isClosed)
        {
            _isClosed = true;
            if (_leakTask != null)
            {
                _leakTask.cancel();
                _leakTask = null;
            }

            try
            {
                // Faster than an iterator most times
                final int length = _openStatements.size();
                for (int i = 0; i < length; i++)
                {
                    _openStatements.get(i).close();
                }
            }
            catch (SQLException e)
            {
                throw _checkException(e);
            }
            finally
            {
                _openStatements.clear();
                _parentPool.releaseConnection(this);
            }
        }
    }

    @HikariOverride
    public boolean isClosed() throws SQLException
    {
        return _isClosed;
    }

    @HikariOverride
    public Statement createStatement() throws SQLException
    {
        _checkClosed();
        try
        {
            Statement statement = __createStatement();

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        _checkClosed();
        try
        {
            Statement statement = __createStatement(resultSetType, resultSetConcurrency);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        _checkClosed();
        try
        {
            Statement statement = __createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        _checkClosed();
        try
        {
            CallableStatement statement = __prepareCall(sql);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        _checkClosed();
        try
        {
            CallableStatement statement = __prepareCall(sql, resultSetType, resultSetConcurrency);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        _checkClosed();
        try
        {
            CallableStatement statement = __prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        _checkClosed();
        try
        {
            PreparedStatement statement = __prepareStatement(sql);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        _checkClosed();
        try
        {
            PreparedStatement statement = __prepareStatement(sql, autoGeneratedKeys);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        _checkClosed();
        try
        {
            PreparedStatement statement = __prepareStatement(sql, resultSetType, resultSetConcurrency);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        _checkClosed();
        try
        {
            PreparedStatement statement = __prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        _checkClosed();
        try
        {
            PreparedStatement statement = __prepareStatement(sql, columnIndexes);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        _checkClosed();
        try
        {
            PreparedStatement statement = __prepareStatement(sql, columnNames);

            return _trackStatement(statement);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    @HikariOverride
    public boolean isValid(int timeout) throws SQLException
    {
        if (_isClosed)
        {
            return false;
        }

        try
        {
            return __isValid(timeout);
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    // ***********************************************************************
    // These methods contain code we do not want injected into the actual
    // java.sql.Connection implementation class.  These methods are only
    // used when instrumentation is not available and "conventional" Javassist
    // delegating proxies are used.
    // ***********************************************************************

    private static void __static()
    {
        if (PROXY_FACTORY == null)
        {
            PROXY_FACTORY = JavassistProxyFactoryFactory.getProxyFactory();
        }
    }

    public final void __close() throws SQLException
    {
        delegate.close();
    }

    public final boolean __isValid(int timeout) throws SQLException
    {
        return delegate.isValid(timeout);
    }

    public final Statement __createStatement() throws SQLException
    {
        return PROXY_FACTORY.getProxyStatement(this, delegate.createStatement());
    }

    public final Statement __createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return PROXY_FACTORY.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency));
    }

    public final Statement __createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return PROXY_FACTORY.getProxyStatement(this, delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public final CallableStatement __prepareCall(String sql) throws SQLException
    {
        return PROXY_FACTORY.getProxyCallableStatement(this, delegate.prepareCall(sql));
    }

    public final CallableStatement __prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return PROXY_FACTORY.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency));
    }

    public final CallableStatement __prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return PROXY_FACTORY.getProxyCallableStatement(this, delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public final PreparedStatement __prepareStatement(String sql) throws SQLException
    {
        return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql));
    }

    public final PreparedStatement __prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, autoGeneratedKeys));
    }

    public final PreparedStatement __prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    public final PreparedStatement __prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public final PreparedStatement __prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnIndexes));
    }

    public final PreparedStatement __prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
    }
}
