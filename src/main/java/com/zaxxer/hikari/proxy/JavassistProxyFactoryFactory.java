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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;

import com.zaxxer.hikari.util.ClassLoaderUtils;

/**
 *
 * @author Brett Wooldridge
 */
public final class JavassistProxyFactoryFactory
{
    private static final ProxyFactory proxyFactory;

    private ClassPool classPool;

    static
    {
    	ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
        	Thread.currentThread().setContextClassLoader(JavassistProxyFactoryFactory.class.getClassLoader());
        	
        	JavassistProxyFactoryFactory proxyFactoryFactory = new JavassistProxyFactoryFactory();

        	proxyFactory = proxyFactoryFactory.generateProxyFactory();
        }
        catch (Exception e)
        {
        	throw new RuntimeException(e);
        }
        finally
        {
        	Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private JavassistProxyFactoryFactory()
    {
        classPool = new ClassPool();
        classPool.importPackage("java.sql");
        classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));

        try
        {
            String methodBody = "{ _checkClosed(); try { return ((cast) delegate).method($$); } catch (SQLException e) { throw _checkException(e); } }";
            generateProxyClass(Connection.class, ConnectionProxy.class, methodBody);
            methodBody = "{ try { return ((cast) delegate).method($$); } catch (SQLException e) { throw _checkException(e); } }";
            generateProxyClass(Statement.class, StatementProxy.class, methodBody);
            generateProxyClass(CallableStatement.class, CallableStatementProxy.class, methodBody);
            generateProxyClass(PreparedStatement.class, PreparedStatementProxy.class, methodBody);
            generateProxyClass(ResultSet.class, ResultSetProxy.class, methodBody);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static ProxyFactory getProxyFactory()
    {
        return proxyFactory;
    }

    private ProxyFactory generateProxyFactory() throws Exception
    {
        String packageName = ProxyFactory.class.getPackage().getName();
        CtClass targetCt = classPool.makeClass(packageName + ".JavassistProxyFactory");
        CtClass superCt = classPool.getCtClass(ProxyFactory.class.getName());
        targetCt.setSuperclass(superCt);
        targetCt.setModifiers(Modifier.FINAL);

        for (CtMethod intfMethod : superCt.getDeclaredMethods())
        {
            CtMethod method = CtNewMethod.copy(intfMethod, targetCt, null);

            StringBuilder call = new StringBuilder("{");
            if ("getProxyConnection".equals(method.getName()))
            {
                call.append("return new ").append(packageName).append(".ConnectionJavassistProxy($$);");
            }
            if ("getProxyStatement".equals(method.getName()))
            {
                call.append("return $2 != null ? new ").append(packageName).append(".StatementJavassistProxy($$) : null;");
            }
            if ("getProxyPreparedStatement".equals(method.getName()))
            {
                call.append("return $2 != null ? new ").append(packageName).append(".PreparedStatementJavassistProxy($$) : null;");
            }
            if ("getProxyResultSet".equals(method.getName()))
            {
                call.append("return $2 != null ? new ").append(packageName).append(".ResultSetJavassistProxy($$) : null;");
            }
            if ("getProxyCallableStatement".equals(method.getName()))
            {
                call.append("return $2 != null ? new ").append(packageName).append(".CallableStatementJavassistProxy($$) : null;");
            }
            call.append('}');
            method.setBody(call.toString());
            targetCt.addMethod(method);
        }

        Class<?> clazz = targetCt.toClass(classPool.getClassLoader(), null);
        return (ProxyFactory) clazz.newInstance();
    }

    /**
     *  Generate Javassist Proxy Classes
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> generateProxyClass(Class<T> primaryInterface, Class<?> superClass, String methodBody) throws Exception
    {
        // Make a new class that extends one of the JavaProxy classes (ie. superClass); use the name to XxxJavassistProxy instead of XxxProxy
        String superClassName = superClass.getName();
        CtClass superClassCt = classPool.getCtClass(superClassName);
        CtClass targetCt = classPool.makeClass(superClassName.replace("Proxy", "JavassistProxy"), superClassCt);
        targetCt.setModifiers(Modifier.FINAL);

        // Make a set of method signatures we inherit implementation for, so we don't generate delegates for these
        Set<String> superSigs = new HashSet<String>();
        for (CtMethod method : superClassCt.getMethods())
        {
            if ((method.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT)
            {
                superSigs.add(method.getName() + method.getSignature());
            }
        }

        methodBody = methodBody.replace("cast", primaryInterface.getName());

        Set<String> methods = new HashSet<String>();
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(primaryInterface);
        for (Class<?> intf : interfaces)
        {
            CtClass intfCt = classPool.getCtClass(intf.getName());
            targetCt.addInterface(intfCt);
            for (CtMethod intfMethod : intfCt.getDeclaredMethods())
            {
                if (superSigs.contains(intfMethod.getName() + intfMethod.getSignature()))
                {
                    // don't generate delegates for methods we override
                    continue;
                }

                // Ignore already added methods that come from other interfaces
                if (methods.contains(intfMethod.getName() + intfMethod.getSignature()))
                {
                    continue;
                }

                CtMethod method = CtNewMethod.copy(intfMethod, targetCt, null);
                methods.add(intfMethod.getName() + intfMethod.getSignature());

                // Generate a method that simply invokes the same method on the delegate
                String modifiedBody = methodBody.replace("method", method.getName());
                if (method.getReturnType() == CtClass.voidType)
                {
                    modifiedBody = modifiedBody.replace("return", "");
                }

                method.setBody(modifiedBody);
                targetCt.addMethod(method);
            }
        }

        return targetCt.toClass(classPool.getClassLoader(), null);
    }
}
