package com.zaxxer.hikari.pool;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.util.PropertyElf;

public class TestPropertySetter
{
   @Test
   public void testProperty1() throws Exception
   {
      Properties propfile1 = new Properties();
      propfile1.load(TestPropertySetter.class.getResourceAsStream("/propfile1.properties"));
      HikariConfig config = new HikariConfig(propfile1);
      config.validate();

      Assert.assertEquals(5, config.getMinimumIdle());
      Assert.assertEquals("SELECT 1", config.getConnectionTestQuery());
   }

   @Test
   public void testProperty2() throws Exception
   {
      Properties propfile2 = new Properties();
      propfile2.load(TestPropertySetter.class.getResourceAsStream("/propfile2.properties"));
      HikariConfig config = new HikariConfig(propfile2);
      config.validate();

      Class<?> clazz = this.getClass().getClassLoader().loadClass(config.getDataSourceClassName());
      DataSource dataSource = (DataSource) clazz.newInstance();
      PropertyElf.setTargetFromProperties(dataSource, config.getDataSourceProperties());
   }

   @Test
   public void testObjectProperty() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
      config.addDataSourceProperty("logWriter", writer);

      Class<?> clazz = this.getClass().getClassLoader().loadClass(config.getDataSourceClassName());
      DataSource dataSource = (DataSource) clazz.newInstance();
      PropertyElf.setTargetFromProperties(dataSource, config.getDataSourceProperties());

      Assert.assertSame(PrintWriter.class, dataSource.getLogWriter().getClass());
   }

   @Test
   public void testPropertyUpperCase() throws Exception
   {
      Properties propfile3 = new Properties();
      propfile3.load(TestPropertySetter.class.getResourceAsStream("/propfile3.properties"));
      HikariConfig config = new HikariConfig(propfile3);
      config.validate();

      Class<?> clazz = this.getClass().getClassLoader().loadClass(config.getDataSourceClassName());
      DataSource dataSource = (DataSource) clazz.newInstance();
      PropertyElf.setTargetFromProperties(dataSource, config.getDataSourceProperties());
   }

   @Test
   public void testGetPropertyNames() throws Exception
   {
      Set<String> propertyNames = PropertyElf.getPropertyNames(HikariConfig.class);
      Assert.assertTrue(propertyNames.contains("dataSourceClassName"));
   }

   @Test
   public void testSetNonExistantPropertyName() throws Exception
   {
      try {
         Properties props = new Properties();
         props.put("what", "happened");
         PropertyElf.setTargetFromProperties(new HikariConfig(), props);
         Assert.fail();
      }
      catch (RuntimeException e) {
      }
   }
}
