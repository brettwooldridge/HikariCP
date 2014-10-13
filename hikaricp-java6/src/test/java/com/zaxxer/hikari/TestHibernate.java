package com.zaxxer.hikari;

import java.io.FileInputStream;
import java.sql.Connection;
import java.util.Properties;

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
      props.load(new FileInputStream("src/test/resources/hibernate.properties"));

      provider.configure(props);
      Connection connection = provider.getConnection();
      provider.closeConnection(connection);

      Assert.assertNotNull(provider.unwrap(HikariConnectionProvider.class));
      Assert.assertFalse(provider.supportsAggressiveRelease());
      provider.stop();
   }
}
