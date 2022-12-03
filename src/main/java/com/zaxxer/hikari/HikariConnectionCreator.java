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
package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Users can implement this interface to override the default connection creation
 * procedure.
 * <p>
 * Implementors typically determine how the username and password are chosen for
 * the new connection. For example, the default implementation uses the username
 * and password from {@link HikariConfig#getUsername()} and
 * {@link HikariConfig#getPassword()} and passes them directly to
 * {@link DataSource#getConnection(String, String)} to create the connection.
 */
public interface HikariConnectionCreator
{
   /**
    *This method is called when a new connection needs to be added to the pool. One
    * of {@link DataSource#getConnection(String, String)} or
    * {@link DataSource#getConnection()} are expected to be called by implementors.
    *
    * @param dataSource The {@link DataSource} to use for getting a connection.
    * @param config The current {@link HikariConfig}
    * @return A newly created {@link Connection}
    * @throws SQLException if a database access error occurs
    */
   Connection createConnection(DataSource dataSource, HikariConfig config) throws SQLException;
}
