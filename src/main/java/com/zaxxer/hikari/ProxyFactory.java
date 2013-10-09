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

import org.slf4j.LoggerFactory;

/**
 *
 */
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
                String proxyFactoryType = System.getProperty("hikariProxyGeneratorType", "auto");
                if ("auto".equals(proxyFactoryType))
                {
                    try
                    {
                        return tryJavassist();
                    }
                    catch (ClassNotFoundException cnfe)
                    {
                        try
                        {
                            return tryCglib();
                        }
                        catch (ClassNotFoundException cnfe2)
                        {
                            return tryJava();
                        }
                    }
                }
                else if ("javassist".equals(proxyFactoryType))
                {
                    return tryJavassist();
                }
                else if ("cglib".equals(proxyFactoryType))
                {
                    return tryCglib();
                }
                else if ("java".equals(proxyFactoryType))
                {
                    return tryJava();
                }
                else
                {
                    LoggerFactory.getLogger(ProxyFactory.class).warn("Unknown proxyFactoryType '{}', falling back to java.lang.Proxy", proxyFactoryType);
                    return tryJava();
                }
            }
            catch (Exception ex)
            {
                LoggerFactory.getLogger(ProxyFactory.class).error("Error initializing ProxyFactory", ex);
                throw new RuntimeException("Error initializing ProxyFactory", ex);
            }
        }

        private static ProxyFactory tryJavassist() throws ClassNotFoundException, Exception
        {
            LoggerFactory.getLogger(ProxyFactory.class).info("Using javassist proxy factory.");
            ClassLoader classLoader = Initializer.class.getClassLoader();
            classLoader.loadClass("javassist.CtClass");
            Class<?> proxyFactoryClass = classLoader.loadClass("com.zaxxer.hikari.JavassistProxyFactoryFactory");
            Object factoryFactory = proxyFactoryClass.newInstance();
            Method getter = factoryFactory.getClass().getMethod("getProxyFactory");
            return (ProxyFactory) getter.invoke(factoryFactory);
        }

        private static ProxyFactory tryCglib() throws ClassNotFoundException, Exception
        {
            LoggerFactory.getLogger(ProxyFactory.class).info("Using CGLIB proxy factory.");
            ClassLoaderUtils.loadClass("net.sf.cglib.proxy.Enhancer");
            Class<?> proxyFactoryClass = ClassLoaderUtils.loadClass("com.zaxxer.hikari.CglibProxyFactory");
            return (ProxyFactory) proxyFactoryClass.newInstance();
        }

        private static ProxyFactory tryJava() throws ClassNotFoundException, Exception
        {
            LoggerFactory.getLogger(ProxyFactory.class).info("Using java.lang.Proxy proxy factory.");
            Class<?> proxyFactoryClass = ClassLoaderUtils.loadClass("com.zaxxer.hikari.JavaProxyFactory");
            return (ProxyFactory) proxyFactoryClass.newInstance();
        }
    }
}
