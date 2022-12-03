/*
 * Copyright (C) 2022 Brett Wooldridge
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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConnectionCreator;

public final class DefaultHikariConnectionCreator implements HikariConnectionCreator
{
   @Override
   public Connection createConnection(DataSource dataSource, HikariConfig config) throws SQLException
   {
      var username = config.getUsername();
      var password = config.getPassword();

      return (username == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
   }
}
