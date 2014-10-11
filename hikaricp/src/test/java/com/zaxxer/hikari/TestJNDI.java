package com.zaxxer.hikari;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;

import org.junit.Assert;
import org.junit.Test;
import org.osjava.sj.jndi.AbstractContext;

public class TestJNDI
{
   @Test
   public void testJndiLookup1() throws Exception
   {
      HikariJNDIFactory jndi = new HikariJNDIFactory();
      Reference ref = new Reference("javax.sql.DataSource");
      ref.add(new BogusRef("driverClassName", "com.zaxxer.hikari.mocks.StubDriver"));
      ref.add(new BogusRef("jdbcUrl", "jdbc:stub"));
      ref.add(new BogusRef("username", "foo"));
      ref.add(new BogusRef("password", "foo"));
      ref.add(new BogusRef("minimumIdle", "0"));
      ref.add(new BogusRef("maxLifetime", "20000"));
      ref.add(new BogusRef("maximumPoolSize", "10"));
      ref.add(new BogusRef("dataSource.loginTimeout", "10"));
      Context nameCtx = new BogusContext();

      HikariDataSource ds = (HikariDataSource) jndi.getObjectInstance(ref, null, nameCtx, null);
      Assert.assertNotNull(ds);
      Assert.assertEquals("foo", ds.getUsername());
   }

   @Test
   public void testJndiLookup2() throws Exception
   {
      HikariJNDIFactory jndi = new HikariJNDIFactory();
      Reference ref = new Reference("javax.sql.DataSource");
      ref.add(new BogusRef("dataSourceJNDI", "java:comp/env/HikariDS"));
      ref.add(new BogusRef("driverClassName", "com.zaxxer.hikari.mocks.StubDriver"));
      ref.add(new BogusRef("jdbcUrl", "jdbc:stub"));
      ref.add(new BogusRef("username", "foo"));
      ref.add(new BogusRef("password", "foo"));
      ref.add(new BogusRef("minimumIdle", "0"));
      ref.add(new BogusRef("maxLifetime", "20000"));
      ref.add(new BogusRef("maximumPoolSize", "10"));
      ref.add(new BogusRef("dataSource.loginTimeout", "10"));
      Context nameCtx = new BogusContext();

      HikariDataSource ds = (HikariDataSource) jndi.getObjectInstance(ref, null, nameCtx, null);
      Assert.assertNotNull(ds);
      Assert.assertEquals("foo", ds.getUsername());
   }

   private class BogusContext extends AbstractContext
   {
      @Override
      public Context createSubcontext(Name name) throws NamingException
      {
         return null;
      }

      @Override
      public Object lookup(String name) throws NamingException
      {
         return new HikariDataSource();
      }
   }

   private class BogusRef extends RefAddr
   {
      private static final long serialVersionUID = 1L;

      private String content;
      BogusRef(String type, String content)
      {
         super(type);
         this.content = content;
      }

      @Override
      public Object getContent()
      {
         return content;
      }
   }
}
