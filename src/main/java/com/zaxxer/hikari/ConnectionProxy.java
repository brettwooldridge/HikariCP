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

package com.zaxxer.hikari;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Brett Wooldridge
 */
public class ConnectionProxy extends HikariProxyBase<Connection> implements IHikariConnectionProxy
{
    private final static Map<String, Method> selfMethodMap = createMethodMap(ConnectionProxy.class);

    private Set<Statement> openStatements;
    private AtomicBoolean isClosed;

    private HikariPool parentPool;

    private long lastAccess;
    private long creationTime;

    private StackTraceElement[] stackTrace;

    // Instance initializer
    {
        openStatements = Collections.newSetFromMap(new ConcurrentHashMap<Statement, Boolean>());
        isClosed = new AtomicBoolean();
        creationTime = System.currentTimeMillis();
    }

    /**
     * Default constructor.
     */
    protected ConnectionProxy()
    {
    }

    protected ConnectionProxy(HikariPool parentPool, Connection connection)
    {
        initialize(parentPool, connection);
    }

    void initialize(HikariPool parentPool, Connection connection)
    {
        this.parentPool = parentPool;
        this.proxy = this;
        this.delegate = connection;
    }

    void unregisterStatement(Object statement)
    {
        openStatements.remove(statement);
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

        isClosed.set(false);
        openStatements.clear();
    }

    public Connection getDelegate()
    {
        return delegate;
    }

    public void captureStack()
    {
        this.stackTrace = Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] getStackTrace()
    {
        return stackTrace;
    }

    @Override
    protected Map<String, Method> getMethodMap()
    {
        return selfMethodMap;
    }

    // **********************************************************************
    //                   Overridden java.sql.Connection Methods
    // **********************************************************************

    /* (non-Javadoc)
     * @see java.sql.Connection#close()
     */
    public void close() throws SQLException
    {
        if (isClosed.compareAndSet(false, true))
        {
            for (Object statement : openStatements)
            {
                ((Statement) statement).close();
            }

            parentPool.releaseConnection((IHikariConnectionProxy) proxy);
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
        Statement statement = delegate.createStatement();
        Statement statementProxy = ProxyFactory.INSTANCE.getProxyStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#createStatement(int, int)
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency);
        Statement statementProxy = ProxyFactory.INSTANCE.getProxyStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        Statement statementProxy = ProxyFactory.INSTANCE.getProxyStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        CallableStatement statement = delegate.prepareCall(sql);
        CallableStatement statementProxy = ProxyFactory.INSTANCE.getProxyCallableStatement(this, statement);
        openStatements.add(statementProxy);
        
        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        CallableStatement statement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        CallableStatement statementProxy = ProxyFactory.INSTANCE.getProxyCallableStatement(this, statement);
        openStatements.add(statementProxy);
        
        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        CallableStatement statement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        CallableStatement statementProxy = ProxyFactory.INSTANCE.getProxyCallableStatement(this, statement);
        openStatements.add(statementProxy);
        
        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        PreparedStatement statement = delegate.prepareStatement(sql);
        PreparedStatement statementProxy = ProxyFactory.INSTANCE.getProxyPreparedStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        PreparedStatement statement = delegate.prepareStatement(sql, autoGeneratedKeys);
        PreparedStatement statementProxy = ProxyFactory.INSTANCE.getProxyPreparedStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }
    
    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        PreparedStatement statement = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        PreparedStatement statementProxy = ProxyFactory.INSTANCE.getProxyPreparedStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        PreparedStatement statement = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        PreparedStatement statementProxy = ProxyFactory.INSTANCE.getProxyPreparedStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        PreparedStatement statement = delegate.prepareStatement(sql, columnIndexes);
        PreparedStatement statementProxy = ProxyFactory.INSTANCE.getProxyPreparedStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        PreparedStatement statement = delegate.prepareStatement(sql, columnNames);
        PreparedStatement statementProxy = ProxyFactory.INSTANCE.getProxyPreparedStatement(this, statement);
        openStatements.add(statementProxy);

        return statementProxy;
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#rollback()
     */
    public void rollback() throws SQLException
    {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    public void rollback(Savepoint savepoint) throws SQLException
    {
        // TODO Auto-generated method stub
    }
}
