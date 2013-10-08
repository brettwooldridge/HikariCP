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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.LazyLoader;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * This class generates JDBC proxy classes using CGLIB bytecode generated
 * implementations. This factory's proxies are more efficient than JdbcJavaProxyFactory
 * but less efficient than JdbcJavassistProxyFactory.
 *
 * @author Brett Wooldridge
 */
public class CglibProxyFactory implements ProxyFactory {

    private Class<Connection> proxyConnectionClass;
    private Class<Statement> proxyStatementClass;
    private Class<CallableStatement> proxyCallableStatementClass;
    private Class<PreparedStatement> proxyPreparedStatementClass;
    private Class<ResultSet> proxyResultSetClass;

    CglibProxyFactory() {
        proxyConnectionClass = createProxyConnectionClass();
        proxyStatementClass = createProxyStatementClass();
        proxyCallableStatementClass = createProxyCallableStatementClass();
        proxyPreparedStatementClass = createProxyPreparedStatementClass();
        proxyResultSetClass = createProxyResultSetClass();
    }

    /** {@inheritDoc} */
    public Connection getProxyConnection(HikariPool parentPool, Connection connection) {
        ConnectionProxy methodInterceptor = new ConnectionProxy(parentPool, connection);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(connection);

        try {
            Connection connectionCglibProxy = proxyConnectionClass.newInstance();
            ((Factory) connectionCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return connectionCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public Statement getProxyStatement(ConnectionProxy ConnectionProxy, Statement statement) {
        StatementProxy methodInterceptor = new StatementProxy(ConnectionProxy, statement);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(statement);

        try {
            Statement statementCglibProxy = proxyStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public CallableStatement getProxyCallableStatement(ConnectionProxy ConnectionProxy, CallableStatement statement) {
        CallableStatementProxy methodInterceptor = new CallableStatementProxy(ConnectionProxy, statement);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(statement);

        try {
            CallableStatement statementCglibProxy = proxyCallableStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement getProxyPreparedStatement(ConnectionProxy ConnectionProxy, PreparedStatement statement) {
        PreparedStatementProxy methodInterceptor = new PreparedStatementProxy(ConnectionProxy, statement);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(statement);

        try {
            PreparedStatement statementCglibProxy = proxyPreparedStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public ResultSet getProxyResultSet(Statement statement, ResultSet resultSet) {
        ResultSetProxy methodInterceptor = new ResultSetProxy(statement, resultSet);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(resultSet);

        try {
            ResultSet resultSetCglibProxy = proxyResultSetClass.newInstance();
            ((Factory) resultSetCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return resultSetCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------
    //  Generate CGLIB Proxy Classes
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Class<Connection> createProxyConnectionClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
        interfaces.add(IHikariConnectionProxy.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new ConnectionProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<PreparedStatement> createProxyPreparedStatementClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new PreparedStatementProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<Statement> createProxyStatementClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new StatementProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<CallableStatement> createProxyCallableStatementClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(CallableStatement.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new CallableStatementProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<ResultSet> createProxyResultSetClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(ResultSet.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new ResultSetProxy()));
        return enhancer.createClass();
    }

    // ---------------------------------------------------------------
    //  CGLIB Classes
    // ---------------------------------------------------------------
    
    static class FastDispatcher implements LazyLoader {
        private Object delegate;

        public FastDispatcher(Object delegate) {
            this.delegate = delegate;
        }

        public Object loadObject() throws Exception {
            return delegate;
        }
    }

    static class Interceptor implements MethodInterceptor {
        private HikariProxyBase<?> interceptor;

        public Interceptor(HikariProxyBase<?> interceptor) {
            this.interceptor = interceptor;
        }
        
        public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy fastProxy) throws Throwable {
            interceptor.proxy = enhanced;
            return interceptor.invoke(interceptor, method, args);
        }
    }

    static class InterceptorFilter implements CallbackFilter {
        private Map<String, Method> methodMap;

        public InterceptorFilter(HikariProxyBase<?> proxyClass) {
            methodMap = proxyClass.getMethodMap();
        }

        public int accept(Method method) {
            if (methodMap.containsKey(HikariProxyBase.getMethodKey(method))) {
                // Use the Interceptor
                return 1;
            }

            // Use the FastDispatcher
            return 0;
        }
    }
}