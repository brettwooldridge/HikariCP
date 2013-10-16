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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * @author Brett Wooldridge
 */
public abstract class HikariProxyBase<T>
{
    protected Object proxy;

    final protected T delegate;

    protected HikariProxyBase(T delegate)
    {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    protected T getProxy()
    {
        return (T) proxy;
    }

    protected abstract SQLException checkException(SQLException e);

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
