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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brett Wooldridge
 */
public abstract class HikariProxyBase<T> implements InvocationHandler
{
    private final static Map<Method, String> methodKeyMap = new ConcurrentHashMap<Method, String>();

    protected Object proxy;

    final protected T delegate;

    protected HikariProxyBase(T delegate)
    {
        this.delegate = delegate;
    }

    protected abstract Map<String, Method> getMethodMap();

    @SuppressWarnings("unchecked")
    protected T getProxy()
    {
        return (T) proxy;
    }

    abstract SQLException checkException(SQLException e);

    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (Proxy.isProxyClass(proxy.getClass()))
        {
            this.proxy = (T) proxy;
        }

        try
        {
            Method ourMethod = getMethodMap().get(getMethodKey(method));
            if (ourMethod != null)
            {
                return ourMethod.invoke(this, args);
            }

            return method.invoke(delegate, args);
        }
        catch (InvocationTargetException ite)
        {
            throw ite.getTargetException();
        }
    }

    protected static Map<String, Method> createMethodMap(Class<?> clazz)
    {
        HashMap<String, Method> selfMethodMap = new HashMap<String, Method>();
        for (Method method : clazz.getDeclaredMethods())
        {
            if ((method.getModifiers() & Method.PUBLIC) == Method.PUBLIC)
            {
                selfMethodMap.put(getMethodKey(method), method);
            }
        }
        return selfMethodMap;
    }

    protected static String getMethodKey(Method method)
    {
        String key = methodKeyMap.get(method);
        if (key != null)
        {
            return key;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType().getName()).append(method.getName());
        for (Class<?> type : method.getParameterTypes())
        {
            sb.append(type.getName());
        }
        key = sb.toString();
        methodKeyMap.put(method, key);
        return key;
    }

    protected static boolean isWrapperFor(Object obj, Class<?> param)
    {
        try
        {
            Method isWrapperForMethod = obj.getClass().getMethod("isWrapperFor", Class.class);
            return (Boolean) isWrapperForMethod.invoke(obj, param);
        }
        catch (NoSuchMethodException ex)
        {
            throw new UnsupportedOperationException("isWrapperFor is not supported", ex);
        }
        catch (IllegalAccessException ex)
        {
            throw new UnsupportedOperationException("isWrapperFor is not supported", ex);
        }
        catch (InvocationTargetException ex)
        {
            throw new UnsupportedOperationException("isWrapperFor is not supported", ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T unwrap(Object obj, Class<T> param)
    {
        try
        {
            Method unwrapMethod = obj.getClass().getMethod("unwrap", Class.class);
            return (T) unwrapMethod.invoke(obj, param);
        }
        catch (NoSuchMethodException ex)
        {
            throw new UnsupportedOperationException("unwrap is not supported", ex);
        }
        catch (IllegalAccessException ex)
        {
            throw new UnsupportedOperationException("unwrap is not supported", ex);
        }
        catch (InvocationTargetException ex)
        {
            throw new UnsupportedOperationException("unwrap is not supported", ex);
        }
    }
}
