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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author Brett Wooldridge
 */
final class ClosedConnection
{
   public static final Connection CLOSED_CONNECTION = getClosedConnection();

   private static Connection getClosedConnection()
   {
      InvocationHandler handler = new InvocationHandler() {
         
         @Override
         public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
         {
            final String methodName = method.getName();
            if ("abort".equals(methodName)) { 
               return Void.TYPE;
            }
            else if ("isValid".equals(methodName)) {
               return Boolean.FALSE;
            }
            else if ("toString".equals(methodName)) {
               return this.getClass().getCanonicalName();
            }

            throw new SQLException("Connection is closed");
         }
      };

      return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                           new Class[] { Connection.class },
                                           handler);
   }
}
