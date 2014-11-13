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
package com.zaxxer.hikari.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import javax.sql.DataSource;

public final class DriverDataSource implements DataSource
{
   private final String jdbcUrl;
   private final Properties driverProperties;
   private final Driver driver;

   public DriverDataSource(String jdbcUrl, Properties properties, String username, String password)
   {
      try {
         this.jdbcUrl = jdbcUrl;
         this.driverProperties = new Properties();
         this.driverProperties.putAll(properties);
         if (username != null) {
            driverProperties.put("user", driverProperties.getProperty("user", username));
         }
         if (password != null) {
            driverProperties.put("password", driverProperties.getProperty("password", password));
         }

         driver = DriverManager.getDriver(jdbcUrl);
      }
      catch (SQLException e) {
         throw new RuntimeException("Unable to get driver for JDBC URL " + jdbcUrl, e);
      }
   }

   @Override
   public Connection getConnection() throws SQLException
   {
      return DriverManager.getConnection(jdbcUrl, driverProperties);
   }

   @Override
   public Connection getConnection(String username, String password) throws SQLException
   {
      return getConnection();
   }

   @Override
   public PrintWriter getLogWriter() throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public void setLogWriter(PrintWriter logWriter) throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public void setLoginTimeout(int seconds) throws SQLException
   {
      DriverManager.setLoginTimeout(seconds);
   }

   @Override
   public int getLoginTimeout() throws SQLException
   {
      return DriverManager.getLoginTimeout();
   }

   public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
   {
      return driver.getParentLogger();
   }

   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      return false;
   }

   public void shutdown()
   {
   }
}
