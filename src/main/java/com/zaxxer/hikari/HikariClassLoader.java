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

import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Loader;
import javassist.NotFoundException;

public class HikariClassLoader extends Loader
{
    private String dataSourceClassName;
    private ClassPool classPool;

    static
    {
        ClassPool.releaseUnmodifiedClassFile = true;
        ClassPool.doPruning = true;
    }

    HikariClassLoader(String dataSourceClassName)
    {
        this.dataSourceClassName = dataSourceClassName;
        ClassPool parentPool = ClassPool.getDefault();
        this.classPool = new ClassPool(parentPool);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException
    {
        CtClass ctClass;
        CtClass[] interfaces;
        try
        {
            ctClass = classPool.get(className);
            interfaces = ctClass.getInterfaces();
        }
        catch (NotFoundException e)
        {
            throw new ClassNotFoundException("HikariClassLoader could not load class " + className, e);
        }

        for (CtClass interf : interfaces)
        {
            if ("java.sql.Connection".equals(interf.getName()))
            {
                return enhanceConnection(ctClass);
            }
        }

        ctClass.detach();

        return super.findClass(className);
    }

    private Class<?> enhanceConnection(CtClass connectionClass)
    {
        try
        {
            String connectionClassName = connectionClass.getName();
            // Rename the original class
            connectionClass.setName(connectionClassName + "Hikari");
            connectionClass.toClass();

            CtClass throwaway = classPool.get("com.zaxxer.hikari.ThrowawayConnection");
            CtClass proxy = classPool.get("com.zaxxer.hikari.ConnectionProxy");
            CtMethod[] methods = proxy.getMethods();
            proxy.setName(connectionClassName);
            proxy.setSuperclass(connectionClass);

            // General method to fix "super" delegation
            for (CtMethod method : methods)
            {
                CtMethod realMethod;
                CtMethod throwawayMethod;
                try
                {
                    throwawayMethod = throwaway.getMethod(method.getName(), method.getSignature());
                    realMethod = connectionClass.getMethod(method.getName(), method.getSignature());
                }
                catch (NotFoundException nfe)
                {
                    continue;
                }

                CodeConverter converter = new CodeConverter();
                converter.redirectMethodCall(throwawayMethod, realMethod);
                method.instrument(converter);
            }

            // Special case for realClose() handling
            CtMethod method = proxy.getMethod("realClose", "()V");
            CtMethod throwawayMethod = throwaway.getMethod("close", method.getSignature());
            CtMethod realMethod = connectionClass.getMethod("close", method.getSignature());
            CodeConverter converter = new CodeConverter();
            converter.redirectMethodCall(throwawayMethod, realMethod);
            method.instrument(converter);
            
            proxy.debugWriteFile("/tmp");
            return proxy.toClass();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
