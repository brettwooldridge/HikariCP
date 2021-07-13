/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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

import com.zaxxer.hikari.util.PropertyElf;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import java.util.Hashtable;
import java.util.Properties;

/**
 * A JNDI factory that produces HikariDataSource instances.
 *
 * @author Brett Wooldridge
 */
public class HikariJNDIFactory implements ObjectFactory
{
   @Override
   synchronized public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception
   {
      // We only know how to deal with <code>javax.naming.Reference</code> that specify a class name of "javax.sql.DataSource"
      if (obj instanceof Reference && "javax.sql.DataSource".equals(((Reference) obj).getClassName())) {
         var ref = (Reference) obj;
         var hikariPropSet = PropertyElf.getPropertyNames(HikariConfig.class);

         var properties = new Properties();
         var enumeration = ref.getAll();
         while (enumeration.hasMoreElements()) {
            var element = enumeration.nextElement();
            var type = element.getType();
            if (type.startsWith("dataSource.") || hikariPropSet.contains(type)) {
               properties.setProperty(type, element.getContent().toString());
            }
         }
         return createDataSource(properties, nameCtx);
      }
      return null;
   }

   private DataSource createDataSource(final Properties properties, final Context context) throws NamingException
   {
      var jndiName = properties.getProperty("dataSourceJNDI");
      if (jndiName != null) {
         return lookupJndiDataSource(properties, context, jndiName);
      }

      return new HikariDataSource(new HikariConfig(properties));
   }

   private DataSource lookupJndiDataSource(final Properties properties, final Context context, final String jndiName) throws NamingException
   {
      if (context == null) {
         throw new RuntimeException("JNDI context does not found for dataSourceJNDI : " + jndiName);
      }

      var jndiDS = (DataSource) context.lookup(jndiName);
      if (jndiDS == null) {
         final var ic = new InitialContext();
         jndiDS = (DataSource) ic.lookup(jndiName);
         ic.close();
      }

      if (jndiDS != null) {
         var config = new HikariConfig(properties);
         config.setDataSource(jndiDS);
         return new HikariDataSource(config);
      }

      return null;
   }
}
