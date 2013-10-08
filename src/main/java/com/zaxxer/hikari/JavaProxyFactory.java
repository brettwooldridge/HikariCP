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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

/**
 * This class generates JDBC proxy classes using stardard java.lang.reflect.Proxy
 * implementations. 
 *
 * @author Brett Wooldridge
 */
public class JavaProxyFactory implements ProxyFactory {

    private JProxyFactory<Connection> proxyConnectionFactory;
    private JProxyFactory<Statement> proxyStatementFactory;
    private JProxyFactory<CallableStatement> proxyCallableStatementFactory;
    private JProxyFactory<PreparedStatement> proxyPreparedStatementFactory;
    private JProxyFactory<ResultSet> proxyResultSetFactory;

    JavaProxyFactory() {
        proxyConnectionFactory = createProxyConnectionFactory();
        proxyStatementFactory = createProxyStatementFactory();
        proxyCallableStatementFactory = createProxyCallableStatementFactory();
        proxyPreparedStatementFactory = createProxyPreparedStatementFactory();
        proxyResultSetFactory = createProxyResultSetFactory();
    }

    /** {@inheritDoc} */
    public Connection getProxyConnection(HikariPool parentPool, Connection connection) {
        try {
            ConnectionProxy connectionProxy = new ConnectionProxy(parentPool, connection);
            return proxyConnectionFactory.getConstructor().newInstance(connectionProxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public Statement getProxyStatement(ConnectionProxy connection, Statement statement) {
        try {
            StatementProxy jdbcStatementProxy = new StatementProxy(connection, statement);
            return proxyStatementFactory.getConstructor().newInstance(jdbcStatementProxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public CallableStatement getProxyCallableStatement(ConnectionProxy connection, CallableStatement statement) {
        try {
            CallableStatementProxy jdbcStatementProxy = new CallableStatementProxy(connection, statement);
            return proxyCallableStatementFactory.getConstructor().newInstance(jdbcStatementProxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement getProxyPreparedStatement(ConnectionProxy connection, PreparedStatement statement) {
        try {
            PreparedStatementProxy jdbcStatementProxy = new PreparedStatementProxy(connection, statement);
            return proxyPreparedStatementFactory.getConstructor().newInstance(jdbcStatementProxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public ResultSet getProxyResultSet(Statement statement, ResultSet resultSet) {
        try {
            ResultSetProxy jdbcResultSetProxy = new ResultSetProxy(statement, resultSet);
            return proxyResultSetFactory.getConstructor().newInstance(jdbcResultSetProxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------
    //  Generate high-efficiency Java Proxy Classes
    // ---------------------------------------------------------------

    private JProxyFactory<Connection> createProxyConnectionFactory() {

        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
        interfaces.add(IHikariConnectionProxy.class);

        return new JProxyFactory<Connection>(interfaces.toArray(new Class<?>[0]));
    }

    private JProxyFactory<Statement> createProxyStatementFactory() {

        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

        return new JProxyFactory<Statement>(interfaces.toArray(new Class<?>[0]));
    }

    private JProxyFactory<PreparedStatement> createProxyPreparedStatementFactory() {

        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

        return new JProxyFactory<PreparedStatement>(interfaces.toArray(new Class<?>[0]));
    }

    private JProxyFactory<ResultSet> createProxyResultSetFactory() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(ResultSet.class);

        return new JProxyFactory<ResultSet>(interfaces.toArray(new Class<?>[0]));
    }

    private JProxyFactory<CallableStatement> createProxyCallableStatementFactory() {

        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(CallableStatement.class);

        return new JProxyFactory<CallableStatement>(interfaces.toArray(new Class<?>[0]));
    }

    public static class JProxyFactory<T> {
        private final Class<?>[] interfaces;
        private Reference<Constructor<T>> ctorRef;

        public JProxyFactory(Class<?>[] interfaces) {
            this.interfaces = interfaces;
        }

        public T newInstance(InvocationHandler handler) {
            if (handler == null)
                throw new NullPointerException();

            try {
                return getConstructor().newInstance(new Object[] { handler });
            } catch (Exception e) {
                throw new InternalError(e.toString());
            }
        }

        @SuppressWarnings("unchecked")
        private synchronized Constructor<T> getConstructor() {
            Constructor<T> ctor = ctorRef == null ? null : ctorRef.get();

            if (ctor == null) {
                try {
                    ctor = (Constructor<T>) Proxy.getProxyClass(getClass().getClassLoader(), interfaces)
                            .getConstructor(new Class[] { InvocationHandler.class });
                } catch (NoSuchMethodException e) {
                    throw new InternalError(e.toString());
                }

                ctorRef = new SoftReference<Constructor<T>>(ctor);
            }

            return ctor;
        }
    }
}