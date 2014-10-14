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
