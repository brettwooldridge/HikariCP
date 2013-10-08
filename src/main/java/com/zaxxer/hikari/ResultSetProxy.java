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

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
public class ResultSetProxy extends HikariProxyBase<ResultSet>
{

    private final static Map<String, Method> selfMethodMap = createMethodMap(ResultSetProxy.class);

    private Statement statement;
    
    protected ResultSetProxy()
    {
        // Default constructor
    }

    protected ResultSetProxy(Statement statement, ResultSet resultSet)
    {
        initialize(statement, resultSet);
    }

    void initialize(Statement statement, ResultSet resultSet)
    {
        this.proxy = this;
        this.statement = statement;
        this.delegate = resultSet;
    }

    /* Overridden methods of java.sql.ResultSet */

    public Statement getStatement() throws SQLException
    {
        return statement;
    }

    /* Overridden methods of ProxyBase */

    @Override
    protected Map<String, Method> getMethodMap()
    {
        return selfMethodMap;
    }
}