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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.javassist.HikariInject;

/**
 * @author Brett Wooldridge
 */
public abstract class ResultSetProxy implements IHikariResultSetProxy, ResultSet
{
    @HikariInject protected IHikariStatementProxy _statement;

    protected final ResultSet delegate;

    protected ResultSetProxy(IHikariStatementProxy statement, ResultSet resultSet)
    {
        this._statement = statement;
        this.delegate = resultSet;
    }

    @HikariInject
    public SQLException _checkException(SQLException e)
    {
        return _statement._checkException(e);
    }
    
    public void _setProxyStatement(IHikariStatementProxy statement)
    {
        this._statement = statement;
    }
    
    // **********************************************************************
    //                 Overridden java.sql.ResultSet Methods
    //                      other methods are injected
    // **********************************************************************

    public Statement getStatement() throws SQLException
    {
        return (Statement) _statement;
    }
}