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

package com.zaxxer.hikari.mocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class StubBaseConnection implements Connection
{
   public volatile boolean throwException;

   /** {@inheritDoc} */
   @Override
   public Statement createStatement() throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubStatement(this);
   }

   /** {@inheritDoc} */
   @Override
   public PreparedStatement prepareStatement(String sql) throws SQLException
   {
      if (throwException) {
         throw new SQLException();
      }
      return new StubPreparedStatement(this);
   }
}
