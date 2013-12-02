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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

/**
 *
 * @author Brett Wooldridge
 */
public final class PropertyBeanSetter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyBeanSetter.class);

    public static void setTargetFromProperties(Object target, Properties properties)
    {
        if (target == null || properties == null)
        {
            return;
        }

        for (Object propKey : properties.keySet())
        {
            String propName = propKey.toString();
            String propValue = properties.get(propKey).toString();

            if (target instanceof HikariConfig && propName.startsWith("dataSource."))
            {
                HikariConfig config = (HikariConfig) target;
                config.addDataSourceProperty(propName.substring("dataSource.".length()), propValue);
            }
            else
            {
                setProperty(target, propName, propValue);
            }
        }
    }

    private static void setProperty(Object target, String propName, String propValue)
    {
        String capitalized = "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
        try
        {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(propName, target.getClass(), null, capitalized);
            Method writeMethod = propertyDescriptor.getWriteMethod();
            Class<?> paramClass = writeMethod.getParameterTypes()[0];
            if (paramClass == int.class)
            {
                writeMethod.invoke(target, Integer.parseInt(propValue));
            }
            else if (paramClass == long.class)
            {
                writeMethod.invoke(target, Long.parseLong(propValue));
            }
            else if (paramClass == boolean.class)
            {
                writeMethod.invoke(target, Boolean.parseBoolean(propValue));
            }
            else if (paramClass == String.class)
            {
                writeMethod.invoke(target, propValue);
            }
        }
        catch (IntrospectionException e)
        {
            LOGGER.error("Property {} is does not exist on target class {}", propName, target.getClass());
            throw new RuntimeException(e);
        }
        catch (Exception e)
        {
            LOGGER.error("Exception setting property {} on target class {}", propName, target.getClass(), e);
            throw new RuntimeException(e);
        }
    }
}
