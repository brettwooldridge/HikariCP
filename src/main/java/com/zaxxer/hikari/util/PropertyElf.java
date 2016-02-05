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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

/**
 * A class that reflectively sets bean properties on a target object.
 *
 * @author Brett Wooldridge
 */
public final class PropertyElf
{
   private static final Logger LOGGER = LoggerFactory.getLogger(PropertyElf.class);

   private static final Pattern GETTER_PATTERN = Pattern.compile("(get|is)[A-Z].+");

   public static void setTargetFromProperties(final Object target, final Properties properties)
   {
      if (target == null || properties == null) {
         return;
      }

      List<Method> methods = Arrays.asList(target.getClass().getMethods());
      Enumeration<?> propertyNames = properties.propertyNames();
      while (propertyNames.hasMoreElements()) {
         Object key = propertyNames.nextElement();
         String propName = key.toString();
         Object propValue = properties.getProperty(propName);
         if (propValue == null) {
            propValue = properties.get(key);
         }

         if (target instanceof HikariConfig && propName.startsWith("dataSource.")) {
            ((HikariConfig) target).addDataSourceProperty(propName.substring("dataSource.".length()), propValue);
         }
         else {
            setProperty(target, propName, propValue, methods);
         }
      }
   }

   /**
    * Get the bean-style property names for the specified object.
    *
    * @param targetClass the target object
    * @return a set of property names
    */
   public static Set<String> getPropertyNames(final Class<?> targetClass)
   {
      HashSet<String> set = new HashSet<>();
      Matcher matcher = GETTER_PATTERN.matcher("");
      for (Method method : targetClass.getMethods()) {
         String name = method.getName();
         if (method.getParameterTypes().length == 0 && matcher.reset(name).matches()) {
            name = name.replaceFirst("(get|is)", "");
            try {
               if (targetClass.getMethod("set" + name, method.getReturnType()) != null) {
                  name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                  set.add(name);
               }
            }
            catch (Exception e) {
               continue;
            }
         }
      }

      return set;
   }

   public static Object getProperty(final String propName, final Object target)
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

   public static Properties copyProperties(final Properties props)
   {
      Properties copy = new Properties();
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
         copy.setProperty(entry.getKey().toString(), entry.getValue().toString());
      }
      return copy;
   }

   private static void setProperty(final Object target, final String propName, final Object propValue, final List<Method> methods)
   {
      Method writeMethod = null;
      String methodName = "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);

      for (Method method : methods) {
         if (method.getName().equals(methodName) && method.getParameterTypes().length == 1) {
            writeMethod = method;
            break;
         }
      }

      if (writeMethod == null) {
         methodName = "set" + propName.toUpperCase();
         for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1) {
               writeMethod = method;
               break;
            }
         }
      }

      if (writeMethod == null) {
         LOGGER.error("Property {} does not exist on target {}", propName, target.getClass());
         throw new RuntimeException(String.format("Property %s does not exist on target %s", propName, target.getClass()));
      }

      try {
         Class<?> paramClass = writeMethod.getParameterTypes()[0];
         if (paramClass == int.class) {
            writeMethod.invoke(target, Integer.parseInt(propValue.toString()));
         }
         else if (paramClass == long.class) {
            writeMethod.invoke(target, Long.parseLong(propValue.toString()));
         }
         else if (paramClass == boolean.class || paramClass == Boolean.class) {
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
         LOGGER.error("Failed to set property {} on target {}", propName, target.getClass(), e);
         throw new RuntimeException(e);
      }
   }
}
