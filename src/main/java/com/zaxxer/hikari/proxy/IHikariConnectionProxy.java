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
import java.util.Timer;

import com.zaxxer.hikari.util.ConcurrentBag.IBagManagable;

/**
 * @author Brett Wooldridge
 */
public interface IHikariConnectionProxy extends Connection, IBagManagable
{
    void unclose();

    void realClose() throws SQLException;

    void untrackStatement(Object statement);

    void checkException(SQLException sqle);

    boolean isBrokenConnection();

    long getCreationTime();

    long getLastAccess();

    void resetConnectionState() throws SQLException;

    /* Leak Detection API */
    void captureStack(long leakThreshold, Timer houseKeepingTimer);
}
