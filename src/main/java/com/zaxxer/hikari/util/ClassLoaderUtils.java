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

package com.zaxxer.hikari.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Brett Wooldridge
 */
public final class ClassLoaderUtils
{
    /**
     * Get the class loader which can be used to generate proxies without leaking memory.
     * @return the class loader which can be used to generate proxies without leaking memory.
     */
    public static ClassLoader getClassLoader()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null)
        {
            return new CascadingClassLoader(cl);
        }
        return ClassLoaderUtils.class.getClassLoader();
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null)
        {
            return new CascadingClassLoader(cl).loadClass(className);
        }

        return Class.forName(className);
    }

    public static Set<Class<?>> getAllInterfaces(Class<?> clazz)
    {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        for (Class<?> intf : Arrays.asList(clazz.getInterfaces()))
        {
            if (intf.getInterfaces().length > 0)
            {
                interfaces.addAll(getAllInterfaces(intf));
            }
            interfaces.add(intf);
        }
        if (clazz.getSuperclass() != null)
        {
            interfaces.addAll(getAllInterfaces(clazz.getSuperclass()));
        }

        if (clazz.isInterface())
        {
            interfaces.add(clazz);
        }

        return interfaces;
    }

    private static class CascadingClassLoader extends ClassLoader
    {
        private ClassLoader contextLoader;

        CascadingClassLoader(ClassLoader contextLoader)
        {
            this.contextLoader = contextLoader;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            try
            {
                return contextLoader.loadClass(name);
            }
            catch (ClassNotFoundException cnfe)
            {
                return CascadingClassLoader.class.getClassLoader().loadClass(name);
            }
        }
    }

}
