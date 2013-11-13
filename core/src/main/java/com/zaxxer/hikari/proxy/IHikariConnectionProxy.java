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

package com.zaxxer.hikari.proxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Timer;

import com.zaxxer.hikari.HikariPool;

/**
 *
 * @author Brett Wooldridge
 */
public interface IHikariConnectionProxy
{
    void _unclose();

    void __close() throws SQLException;

    void _unregisterStatement(Object statement);

    SQLException _checkException(SQLException sqle);

    boolean _isBrokenConnection();

    long _getCreationTime();

    long _getLastAccess();

    void _markLastAccess();

    void _setParentPool(HikariPool parentPool);

    Connection getDelegate();

    /* Leak Detection API */
    void _captureStack(long leakThreshold, Timer houseKeepingTimer);
}
