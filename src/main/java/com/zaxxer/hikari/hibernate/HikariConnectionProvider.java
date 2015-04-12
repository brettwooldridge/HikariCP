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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Version;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Connection provider for Hibernate 4.3.
 *
 * @author Brett Wooldridge, Luca Burgazzoli
 */
public class HikariConnectionProvider implements ConnectionProvider, Configurable, Stoppable
{
   private static final long serialVersionUID = -9131625057941275711L;

   private static final Logger LOGGER = LoggerFactory.getLogger(HikariConnectionProvider.class);

   /**
    * HikariCP configuration.
    */
   private HikariConfig hcfg;

   /**
    * HikariCP data source.
    */
   private HikariDataSource hds;

   // *************************************************************************
   //
   // *************************************************************************

   /**
    * c-tor
    */
   public HikariConnectionProvider()
   {
      this.hcfg = null;
      this.hds = null;
      if (Version.getVersionString().substring(0, 5).compareTo("4.3.6") >= 1) {
         LOGGER.warn("com.zaxxer.hikari.hibernate.HikariConnectionProvider has been deprecated for versions of "
                     + "Hibernate 4.3.6 and newer.  Please switch to org.hibernate.hikaricp.internal.HikariCPConnectionProvider.");
      }
   }

   // *************************************************************************
   // Configurable
   // *************************************************************************

   @SuppressWarnings("rawtypes")
   @Override
   public void configure(Map props) throws HibernateException
   {
      try {
         LOGGER.debug("Configuring HikariCP");

         this.hcfg = HikariConfigurationUtil.loadConfiguration(props);
         this.hds = new HikariDataSource(this.hcfg);

      }
      catch (Exception e) {
         throw new HibernateException(e);
      }

      LOGGER.debug("HikariCP Configured");
   }

   // *************************************************************************
   // ConnectionProvider
   // *************************************************************************

   @Override
   public Connection getConnection() throws SQLException
   {
      Connection conn = null;
      if (this.hds != null) {
         conn = this.hds.getConnection();
      }

      return conn;
   }

   @Override
   public void closeConnection(Connection conn) throws SQLException
   {
      conn.close();
   }

   @Override
   public boolean supportsAggressiveRelease()
   {
      return false;
   }

   @Override
   @SuppressWarnings("rawtypes")
   public boolean isUnwrappableAs(Class unwrapType)
   {
      return ConnectionProvider.class.equals(unwrapType) || HikariConnectionProvider.class.isAssignableFrom(unwrapType);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T unwrap(Class<T> unwrapType)
   {
       if ( ConnectionProvider.class.equals( unwrapType ) ||
               HikariConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
           return (T) this;
       }
       else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
           return (T) this.hds;
       }
       else {
           throw new UnknownUnwrapTypeException( unwrapType );
       }
   }

   // *************************************************************************
   // Stoppable
   // *************************************************************************

   @Override
   public void stop()
   {
      this.hds.close();
   }
}
