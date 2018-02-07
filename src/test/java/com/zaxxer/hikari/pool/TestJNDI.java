/*
 * Copyright (C) 2014 Brett Wooldridge
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariJNDIFactory;
import com.zaxxer.hikari.mocks.StubDataSource;
import org.junit.Test;
import org.osjava.sj.jndi.AbstractContext;

import javax.naming.*;
import java.sql.Connection;

import static com.zaxxer.hikari.pool.TestElf.getUnsealedConfig;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.*;

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
      ref.add(new BogusRef("maxLifetime", "30000"));
      ref.add(new BogusRef("maximumPoolSize", "10"));
      ref.add(new BogusRef("dataSource.loginTimeout", "10"));
      Context nameCtx = new BogusContext();

      try (HikariDataSource ds = (HikariDataSource) jndi.getObjectInstance(ref, null, nameCtx, null)) {
         assertNotNull(ds);
         assertEquals("foo", getUnsealedConfig(ds).getUsername());
      }
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
      ref.add(new BogusRef("maxLifetime", "30000"));
      ref.add(new BogusRef("maximumPoolSize", "10"));
      ref.add(new BogusRef("dataSource.loginTimeout", "10"));
      Context nameCtx = new BogusContext2();

      try (HikariDataSource ds = (HikariDataSource) jndi.getObjectInstance(ref, null, nameCtx, null)) {
         assertNotNull(ds);
         assertEquals("foo", getUnsealedConfig(ds).getUsername());
      }
   }

   @Test
   public void testJndiLookup3() throws Exception
   {
      HikariJNDIFactory jndi = new HikariJNDIFactory();

      Reference ref = new Reference("javax.sql.DataSource");
      ref.add(new BogusRef("dataSourceJNDI", "java:comp/env/HikariDS"));
      try {
         jndi.getObjectInstance(ref, null, null, null);
         fail();
      }
      catch (RuntimeException e) {
         assertTrue(e.getMessage().contains("JNDI context does not found"));
      }
   }

   @Test
   public void testJndiLookup4() throws Exception
   {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.memory.MemoryContextFactory");
      System.setProperty("org.osjava.sj.jndi.shared", "true");
      InitialContext ic = new InitialContext();

      StubDataSource ds = new StubDataSource();

      Context subcontext = ic.createSubcontext("java:/comp/env/jdbc");
      subcontext.bind("java:/comp/env/jdbc/myDS", ds);

      HikariConfig config = newHikariConfig();
      config.setDataSourceJNDI("java:/comp/env/jdbc/myDS");

      try (HikariDataSource hds = new HikariDataSource(config);
           Connection conn = hds.getConnection()) {
         assertNotNull(conn);
      }
   }

   @SuppressWarnings("unchecked")
   private class BogusContext extends AbstractContext
   {
      @Override
      public Context createSubcontext(Name name)
      {
         return null;
      }

      @Override
      public Object lookup(String name)
      {
         final HikariDataSource ds = new HikariDataSource();
         ds.setPoolName("TestJNDI");
         return ds;
      }
   }

   @SuppressWarnings("unchecked")
   private class BogusContext2 extends AbstractContext
   {
      @Override
      public Context createSubcontext(Name name)
      {
         return null;
      }

      @Override
      public Object lookup(String name)
      {
         return new StubDataSource();
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
