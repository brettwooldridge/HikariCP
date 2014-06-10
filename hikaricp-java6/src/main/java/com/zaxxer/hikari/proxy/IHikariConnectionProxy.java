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

package com.zaxxer.hikari.proxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;

import com.zaxxer.hikari.util.ConcurrentBag.IBagManagable;

/**
 * The interface used by the Connection proxy and through which all interaction
 * by other classes flow.
 *
 * @author Brett Wooldridge
 */
public interface IHikariConnectionProxy extends Connection, IBagManagable
{
   /**
    * Catpure the stack and start leak detection.
    *
    * @param leakThreshold the number of milliseconds before a leak is reported
    * @param houseKeepingTimer the timer to run the leak detection task with
    */
   void captureStack(long leakThreshold, Timer houseKeepingTimer);

   /**
    * Check if the provided SQLException contains a SQLSTATE that indicates
    * a disconnection from the server.
    *
    * @param sqle the SQLException to check
    */
   void checkException(SQLException sqle);

   /**
    * Get the expiration timestamp of the connection.
    *
    * @return the expiration timestamp, or Long.MAX_VALUE if there is no maximum lifetime
    */
   long getExpirationTime();

   /**
    * Get the last access timestamp of the connection.
    *
    * @return the last access timestamp
    */
   long getLastAccess();

   /**
    * Get the timestamp of when the connection was removed from the pool for use.
    *
    * @return the timestamp the connection started to be used in the most recent request
    */
   long getLastOpenTime();

   /**
    * Return the broken state of the connection.  If checkException() detected
    * a broken connection, this method will return true, otherwise false.
    *
    * @return the broken state of the connection
    */
   boolean isBrokenConnection();

   /**
    * Actually close the underlying delegate Connection.
    *
    * @throws SQLException rethrown from the underlying delegate Connection
    */
   void realClose() throws SQLException;

   /**
    * Reset the delegate Connection back to pristine state.
    *
    * @throws SQLException thrown if there is an error resetting any of the state
    */
   void resetConnectionState() throws SQLException;

   /**
    * Make the Connection available for use again by marking it as not closed.
    * @param now the current time in milliseconds
    */
   void unclose(long now);

   /**
    * Called by Statement and its subclasses when they are closed to remove them
    * from the tracking list.
    *
    * @param statement the Statement to remove from tracking
    */
   void untrackStatement(Statement statement);
}
