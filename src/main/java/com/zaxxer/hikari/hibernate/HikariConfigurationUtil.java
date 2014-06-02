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

package com.zaxxer.hikari.hibernate;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;

import com.zaxxer.hikari.HikariConfig;

/**
 * Utility class to map Hibernate properties to HikariCP configuration properties.
 *
 * @author Brett Wooldridge, Luca Burgazzoli
 */
public class HikariConfigurationUtil
{
   public static final String CONFIG_PREFIX = "hibernate.hikari.";
   public static final String CONFIG_PREFIX_DATASOURCE = "hibernate.hikari.dataSource.";

   /**
    * Create/load a HikariConfig from Hibernate properties.
    *
    * @param props a map of Hibernate properties
    * @return a HikariConfig
    */
   @SuppressWarnings("rawtypes")
   public static HikariConfig loadConfiguration(Map props)
   {
      Properties hikariProps = new Properties();
      copyProperty(AvailableSettings.ISOLATION, props, "transactionIsolation", hikariProps);
      copyProperty(AvailableSettings.AUTOCOMMIT, props, "autoCommit", hikariProps);
      copyProperty(AvailableSettings.DRIVER, props, "driverClassName", hikariProps);
      copyProperty(AvailableSettings.URL, props, "jdbcUrl", hikariProps);
      copyProperty(AvailableSettings.USER, props, "username", hikariProps);
      copyProperty(AvailableSettings.PASS, props, "password", hikariProps);

      for (Object keyo : props.keySet()) {
         String key = (String) keyo;
         if (key.startsWith(CONFIG_PREFIX)) {
            hikariProps.setProperty(key.substring(CONFIG_PREFIX.length()), (String) props.get(key));
         }
      }

      return new HikariConfig(hikariProps);
   }

   @SuppressWarnings("rawtypes")
   private static void copyProperty(String srcKey, Map src, String dstKey, Properties dst)
   {
      if (src.containsKey(srcKey)) {
         dst.setProperty(dstKey, (String) src.get(srcKey));
      }
   }
}