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

package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface whose implementers can perform one-time customization of a
 * Connection before it is added to the pool.  Note the implemention
 * of the <code>customize()</code> method must be multithread-safe as
 * it may be called by multiple threads at one time.
 *
 * @author Brett Wooldridge
 */
public interface IConnectionCustomizer
{
   /**
    * The Connection object that is passed into this method is the "raw"
    * Connection instance provided by the JDBC driver, not a wrapped
    * HikariCP connection.
    *
    * @param connection a native JDBC driver Connection instance to customize
    * @throws SQLException should be thrown if an error condition is encountered during customization
    */
   void customize(Connection connection) throws SQLException;
}
