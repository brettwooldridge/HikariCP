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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

/**
 * A class that reflectively sets bean properties on a target object.
 *
 * @author Brett Wooldridge
 */
public final class PropertyBeanSetter
{
   private static final Logger LOGGER = LoggerFactory.getLogger(PropertyBeanSetter.class);

   public static void setTargetFromProperties(Object target, Properties properties)
   {
      if (target == null || properties == null) {
         return;
      }

      Enumeration<?> propertyNames = properties.propertyNames();
      while (propertyNames.hasMoreElements()) {
         Object key = propertyNames.nextElement();
         String propName = key.toString();
         Object propValue = properties.getProperty(propName);
         if (propValue == null) {
            propValue = properties.get(key);
         }

         if (target instanceof HikariConfig && propName.startsWith("dataSource.")) {
            HikariConfig config = (HikariConfig) target;
            config.addDataSourceProperty(propName.substring("dataSource.".length()), propValue);
         }
         else {
            setProperty(target, propName, propValue);
         }
      }
   }

   /**
    * Get the bean-style property names for the specified object.
    *
    * @param targetClass the target object
    * @return a set of property names
    */
   public static Set<String> getPropertyNames(Class<?> targetClass)
   {
      HashSet<String> set = new HashSet<String>();
      try {
         BeanInfo info = Introspector.getBeanInfo(targetClass);
         for (PropertyDescriptor descr : info.getPropertyDescriptors()) {
            if (!"class".equals(descr.getName())) {
               set.add(descr.getName());
            }
         }

         return set;
      }
      catch (IntrospectionException e) {
         throw new RuntimeException(e);
      }
   }

   public static Object getProperty(String propName, Object target)
   {
      try {
         String capitalized = "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
         Method method = target.getClass().getMethod(capitalized);
         return method.invoke(target);
      }
      catch (Exception e) {
         try {
            String capitalized = "is" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
            Method method = target.getClass().getMethod(capitalized);
            return method.invoke(target);
         }
         catch (Exception e2) {
            return null;
         }
      }
   }

   public static Properties copyProperties(Properties props)
   {
      Properties copy = new Properties();
      for (Object key : props.keySet()) {
         copy.setProperty(key.toString(), props.get(key).toString());
      }
      return copy;
   }

   private static void setProperty(Object target, String propName, Object propValue)
   {
      String capitalized = "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
      PropertyDescriptor propertyDescriptor;
      try {
         propertyDescriptor = new PropertyDescriptor(propName, target.getClass(), null, capitalized);
      }
      catch (IntrospectionException e) {
         capitalized = "set" + propName.toUpperCase();
         try {
            propertyDescriptor = new PropertyDescriptor(propName, target.getClass(), null, capitalized);
         }
         catch (IntrospectionException e1) {
            LOGGER.error("Property {} does not exist on target {}", propName, target.getClass());
            throw new RuntimeException(e);
         }
      }

      try {
         Method writeMethod = propertyDescriptor.getWriteMethod();
         Class<?> paramClass = writeMethod.getParameterTypes()[0];
         if (paramClass == int.class) {
            writeMethod.invoke(target, Integer.parseInt(propValue.toString()));
         }
         else if (paramClass == long.class) {
            writeMethod.invoke(target, Long.parseLong(propValue.toString()));
         }
         else if (paramClass == boolean.class) {
            writeMethod.invoke(target, Boolean.parseBoolean(propValue.toString()));
         }
         else if (paramClass == String.class) {
            writeMethod.invoke(target, propValue.toString());
         }
         else {
            writeMethod.invoke(target, propValue);
         }
      }
      catch (Exception e) {
         LOGGER.error("Exception setting property {} on target {}", propName, target.getClass(), e);
         throw new RuntimeException(e);
      }
   }
}
