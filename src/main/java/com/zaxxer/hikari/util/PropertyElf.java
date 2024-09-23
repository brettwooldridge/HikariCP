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

import com.zaxxer.hikari.HikariConfig;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A class that reflectively sets bean properties on a target object.
 *
 * @author Brett Wooldridge
 */
public final class PropertyElf
{
   private PropertyElf() {
      // cannot be constructed
   }

   public static void setTargetFromProperties(final Object target, final Properties properties)
   {
      if (target == null || properties == null) {
         return;
      }

      var methods = Arrays.asList(target.getClass().getMethods());
      properties.forEach((key, value) -> {
         var keyName = key.toString();
         if (target instanceof HikariConfig && keyName.startsWith("dataSource.")) {
            ((HikariConfig) target).addDataSourceProperty(keyName.substring("dataSource.".length()), value);
         }
         else {
            setProperty(target, keyName, value, methods);
         }
      });
   }

   /**
    * Get the bean-style property names for the specified object.
    *
    * @param targetClass the target object
    * @return a set of property names
    */
   public static Set<String> getPropertyNames(final Class<?> targetClass)
   {
      var set = new HashSet<String>();
      for (var method : targetClass.getMethods()) {
         var name = propertyNameFromGetterName(method.getName());
         try {
            if (method.getParameterTypes().length == 0 && name != null) {
               targetClass.getMethod("set" + capitalizedPropertyName(name), method.getReturnType()); // throws if method setter does not exist
               set.add(name);
            }
         }
         catch (Exception e) {
            // fall thru (continue)
         }
      }

      return set;
   }

   public static Object getProperty(final String propName, final Object target)
   {
      try {
         // use the english locale to avoid the infamous turkish locale bug
         var capitalized = "get" + capitalizedPropertyName(propName);
         var method = target.getClass().getMethod(capitalized);
         return method.invoke(target);
      }
      catch (Exception e) {
         try {
            var capitalized = "is" + capitalizedPropertyName(propName);
            var method = target.getClass().getMethod(capitalized);
            return method.invoke(target);
         }
         catch (Exception e2) {
            return null;
         }
      }
   }

   public static Properties copyProperties(final Properties props)
   {
      var copy = new Properties();
      props.forEach((key, value) -> copy.setProperty(key.toString(), value.toString()));
      return copy;
   }

   private static String propertyNameFromGetterName(final String methodName)
   {
      String name = null;
      if (methodName.startsWith("get") && methodName.length() > 3) {
         name = methodName.substring(3);
      }
      else if (methodName.startsWith("is") && methodName.length() > 2) {
         name = methodName.substring(2);
      }

      if (name != null) {
         return Character.toLowerCase(name.charAt(0)) + name.substring(1);
      }

      return null;
   }

   private static void setProperty(final Object target, final String propName, final Object propValue, final List<Method> methods)
   {
      final var logger = LoggerFactory.getLogger(PropertyElf.class);

      // use the english locale to avoid the infamous turkish locale bug
      var methodName = "set" + propName.substring(0, 1).toUpperCase(Locale.ENGLISH) + propName.substring(1);
      var writeMethod = methods.stream().filter(m -> m.getName().equals(methodName) && m.getParameterCount() == 1).findFirst().orElse(null);

      if (writeMethod == null) {
         var methodName2 = "set" + propName.toUpperCase(Locale.ENGLISH);
         writeMethod = methods.stream().filter(m -> m.getName().equals(methodName2) && m.getParameterCount() == 1).findFirst().orElse(null);
      }

      if (writeMethod == null) {
         logger.error("Property {} does not exist on target {}", propName, target.getClass());
         throw new RuntimeException(String.format("Property %s does not exist on target %s", propName, target.getClass()));
      }

      try {
         var paramClass = writeMethod.getParameterTypes()[0];
         if (paramClass == int.class) {
            writeMethod.invoke(target, Integer.parseInt(propValue.toString()));
         }
         else if (paramClass == long.class) {
            writeMethod.invoke(target, Long.parseLong(propValue.toString()));
         }
         else if (paramClass == short.class) {
            writeMethod.invoke(target, Short.parseShort(propValue.toString()));
         }
         else if (paramClass == double.class) {
            writeMethod.invoke(target, Double.parseDouble(propValue.toString()));
         }
         else if (paramClass == boolean.class || paramClass == Boolean.class) {
            writeMethod.invoke(target, Boolean.parseBoolean(propValue.toString()));
         }
         else if (paramClass.isArray() && char.class.isAssignableFrom(paramClass.getComponentType())) {
            writeMethod.invoke(target, propValue.toString().toCharArray());
         }
         else if (paramClass == String.class) {
            writeMethod.invoke(target, propValue.toString());
         }
         else {
            try {
               logger.debug("Try to create a new instance of \"{}\"", propValue);
               writeMethod.invoke(target, Class.forName(propValue.toString()).getDeclaredConstructor().newInstance());
            }
            catch (InstantiationException | ClassNotFoundException e) {
               logger.debug("Class \"{}\" not found or could not instantiate it (Default constructor)", propValue);
               writeMethod.invoke(target, propValue);
            }
         }
      }
      catch (Exception e) {
         logger.error("Failed to set property {} on target {}", propName, target.getClass(), e);
         throw new RuntimeException(e);
      }
   }

   private static String capitalizedPropertyName(String propertyName)
   {
      // use the english locale to avoid the infamous turkish locale bug
      return propertyName.substring(0, 1).toUpperCase(Locale.ENGLISH) + propertyName.substring(1);
   }
}
