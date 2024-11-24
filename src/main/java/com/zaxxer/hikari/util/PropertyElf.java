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
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that reflectively sets bean properties on a target object.
 *
 * @author Brett Wooldridge
 */
public final class PropertyElf
{
   private static final Pattern DURATION_PATTERN = Pattern.compile("^(?<number>\\d+)(?<unit>ms|s|m|h|d)$");

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
         String value = propValue.toString();
         if (paramClass == int.class) {
            writeMethod.invoke(target, parseDuration(value).map(duration -> (int) duration.toMillis()).orElseGet(() -> Integer.parseInt(value)));
         }
         else if (paramClass == long.class) {
            writeMethod.invoke(target, parseDuration(value).map(Duration::toMillis).orElseGet(() -> Long.parseLong(value)));
         }
         else if (paramClass == short.class) {
            writeMethod.invoke(target, Short.parseShort(value));
         }
         else if (paramClass == boolean.class || paramClass == Boolean.class) {
            writeMethod.invoke(target, Boolean.parseBoolean(value));
         }
         else if (paramClass.isArray() && char.class.isAssignableFrom(paramClass.getComponentType())) {
            writeMethod.invoke(target, value.toCharArray());
         }
         else if (paramClass == String.class) {
            writeMethod.invoke(target, value);
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

   private static Optional<Duration> parseDuration(String value)
   {
      Matcher matcher = DURATION_PATTERN.matcher(value);
      if (matcher.matches()) {
         long number = Long.parseLong(matcher.group("number"));
         String unit = matcher.group("unit");
         switch (unit) {
            case "ms":
               return Optional.of(Duration.ofMillis(number));
            case "s":
               return Optional.of(Duration.ofSeconds(number));
            case "m":
               return Optional.of(Duration.ofMinutes(number));
            case "h":
               return Optional.of(Duration.ofHours(number));
            case "d":
               return Optional.of(Duration.ofDays(number));
            default:
               throw new IllegalStateException(String.format("Could not match unit, got %s (from given value %s)", unit, value));
         }
      } else {
         return Optional.empty();
      }
   }
}
