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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.slf4j.LoggerFactory;

/**
 *
 */
public interface ProxyFactory
{
    /* Classes should use JdbcProxyFactory.INSTANCE to access the factory */
    final ProxyFactory INSTANCE = Initializer.initialize();

    /* Methods used to create the proxies around various JDBC classes */

    Connection getProxyConnection(HikariPool parentPool, Connection connection);

    Statement getProxyStatement(ConnectionProxy connection, Statement statement);

    CallableStatement getProxyCallableStatement(ConnectionProxy connection, CallableStatement statement);

    PreparedStatement getProxyPreparedStatement(ConnectionProxy connection, PreparedStatement statement);

    ResultSet getProxyResultSet(Statement statement, ResultSet resultSet);

    /**
     * Initializer class used to initialize the proxy factory. 
     */
    class Initializer
    {
        private static ProxyFactory initialize()
        {
            try
            {
                String jdbcProxyFactoryClass = System.getProperty("hikariProxyGeneratorClass", "auto");
                if ("auto".equals(jdbcProxyFactoryClass))
                {
                    try
                    {
                        ClassLoaderUtils.loadClass("javassist.CtClass");
                        jdbcProxyFactoryClass = "com.zaxxer.hikari.JavassistProxyFactory";
                        LoggerFactory.getLogger(ProxyFactory.class).info("Using javassist proxy factory.");
                    }
                    catch (ClassNotFoundException cnfe)
                    {
                        try
                        {
                            ClassLoaderUtils.loadClass("net.sf.cglib.proxy.Enhancer");
                            jdbcProxyFactoryClass = "com.zaxxer.hikari.CglibProxyFactory";
                            LoggerFactory.getLogger(ProxyFactory.class).info("Using CGLIB proxy factory.");
                        }
                        catch (ClassNotFoundException cnfe2)
                        {
                            jdbcProxyFactoryClass = "com.zaxxer.hikari.JavaProxyFactory";
                            LoggerFactory.getLogger(ProxyFactory.class).info("Using java.lang.Proxy proxy factory.");
                        }
                    }
                }
                Class<?> proxyFactoryClass = ClassLoaderUtils.loadClass(jdbcProxyFactoryClass);
                return (ProxyFactory) proxyFactoryClass.newInstance();
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Error initializing ProxyFactory", ex);
            }
        }
    }
}