package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.util.Properties;

import org.hibernate.service.UnknownUnwrapTypeException;
import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.hibernate.HikariConnectionProvider;

public class TestHibernate
{
   @Test
   public void testConnectionProvider() throws Exception
   {
      HikariConnectionProvider provider = new HikariConnectionProvider();

      Properties props = new Properties();
      props.load(getClass().getResourceAsStream("/hibernate.properties"));

      provider.configure(props);
      Connection connection = provider.getConnection();
      provider.closeConnection(connection);

      Assert.assertNotNull(provider.unwrap(HikariConnectionProvider.class));
      Assert.assertFalse(provider.supportsAggressiveRelease());

      try {
         provider.unwrap(TestHibernate.class);
         Assert.fail("Expected exception");
      }
      catch (UnknownUnwrapTypeException e) {
      }

      provider.stop();
   }
}
