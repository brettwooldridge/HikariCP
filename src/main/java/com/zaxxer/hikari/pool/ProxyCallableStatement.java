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

package com.zaxxer.hikari.pool;

import java.sql.CallableStatement;

/**
 * This is the proxy class for java.sql.CallableStatement.
 *
 * @author Brett Wooldridge
 */
public abstract class ProxyCallableStatement extends ProxyPreparedStatement implements CallableStatement
{
   protected ProxyCallableStatement(ProxyConnection connection, CallableStatement statement)
   {
      super(connection, statement);
   }

   // **********************************************************************
   //               Overridden java.sql.CallableStatement Methods
   // **********************************************************************

}