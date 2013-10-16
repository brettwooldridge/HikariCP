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

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariPool;

public interface ProxyFactory
{
    /* Classes should use ProxyFactory.INSTANCE to access the factory */
    final ProxyFactory INSTANCE = Initializer.initialize();

    Connection getProxyConnection(HikariPool parentPool, Connection connection);

    Statement getProxyStatement(ConnectionProxy connection, Statement statement);

    CallableStatement getProxyCallableStatement(ConnectionProxy connection, CallableStatement statement);

    PreparedStatement getProxyPreparedStatement(ConnectionProxy connection, PreparedStatement statement);

    ResultSet getProxyResultSet(Statement statement, ResultSet resultSet);

    /**************************************************************************
     *
     * Initializer class used to initialize the proxy factory. 
     */
    class Initializer
    {
        private static ProxyFactory initialize()
        {
            try
            {
                ClassLoader classLoader = Initializer.class.getClassLoader();
                classLoader.loadClass("javassist.CtClass");
                Class<?> proxyFactoryClass = classLoader.loadClass("com.zaxxer.hikari.proxy.JavassistProxyFactoryFactory");
                Object factoryFactory = proxyFactoryClass.newInstance();
                Method getter = factoryFactory.getClass().getMethod("getProxyFactory");
                return (ProxyFactory) getter.invoke(factoryFactory);
            }
            catch (Exception ex)
            {
                LoggerFactory.getLogger(ProxyFactory.class).error("Error initializing ProxyFactory", ex);
                throw new RuntimeException("Error initializing ProxyFactory", ex);
            }
        }
    }
}
