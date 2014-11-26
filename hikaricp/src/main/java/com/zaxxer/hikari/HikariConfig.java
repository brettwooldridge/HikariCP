/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.zaxxer.hikari.metrics.CodaHaleShim;
import com.zaxxer.hikari.proxy.JavassistProxyFactory;
import com.zaxxer.hikari.util.PropertyBeanSetter;

public class HikariConfig extends AbstractHikariConfig
{
   static {
      JavassistProxyFactory.initialize();
      CodaHaleShim.initialize();
   }

   /**
    * Default constructor
    */
   public HikariConfig()
   {
      super();
   }

   /**
    * Construct a HikariConfig from the specified properties object.
    *
    * @param properties the name of the property file
    */
   public HikariConfig(Properties properties)
   {
      super(properties);
   }

   /**
    * Construct a HikariConfig from the specified property file name.  <code>propertyFileName</code>
    * will first be treated as a path in the file-system, and if that fails the 
    * ClassLoader.getResourceAsStream(propertyFileName) will be tried.
    *
    * @param propertyFileName the name of the property file
    */
   public HikariConfig(String propertyFileName)
   {
      super(propertyFileName);
   }

   protected void loadProperties(String propertyFileName)
   {
      final File propFile = new File(propertyFileName);
      try (final InputStream is = propFile.isFile() ? new FileInputStream(propFile) : this.getClass().getResourceAsStream(propertyFileName)) {
         if (is != null) {
            Properties props = new Properties();
            props.load(is);
            PropertyBeanSetter.setTargetFromProperties(this, props);
         }
         else {
            throw new IllegalArgumentException("Property file " + propertyFileName + " was not found.");
         }
      }
      catch (IOException io) {
         throw new RuntimeException("Error loading properties file", io);
      }
   }
}
