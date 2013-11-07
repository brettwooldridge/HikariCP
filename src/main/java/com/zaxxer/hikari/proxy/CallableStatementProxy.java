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

import java.sql.CallableStatement;

/**
 *
 * @author Brett Wooldridge
 */
public abstract class CallableStatementProxy extends PreparedStatementProxy implements IHikariStatementProxy, CallableStatement
{
    protected CallableStatementProxy(ConnectionProxy connection, CallableStatement statement)
    {
        super(connection, statement);
    }

    // **********************************************************************
    //               Overridden java.sql.CallableStatement Methods
    // **********************************************************************

    // ***********************************************************************
    // These methods contain code we do not want injected into the actual
    // java.sql.Connection implementation class.  These methods are only
    // used when instrumentation is not available and "conventional" Javassist
    // delegating proxies are used.
    // ***********************************************************************

}