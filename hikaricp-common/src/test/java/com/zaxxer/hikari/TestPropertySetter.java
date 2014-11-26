package com.zaxxer.hikari;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.util.PropertyBeanSetter;

public class TestPropertySetter
{
   @Test
   public void testProperty1()
   {
      File file = new File("/propfile1.properties");
      HikariConfig config = new HikariConfig(file.getPath());
      config.validate();

      Assert.assertEquals(5, config.getMinimumIdle());
      Assert.assertEquals("SELECT 1", config.getConnectionTestQuery());
   }

   @Test
   public void testProperty2() throws Exception
   {
      File file = new File("/propfile2.properties");
      HikariConfig config = new HikariConfig(file.getPath());
      config.validate();

      Class<?> clazz = this.getClass().getClassLoader().loadClass(config.getDataSourceClassName());
      DataSource dataSource = (DataSource) clazz.newInstance();
      PropertyBeanSetter.setTargetFromProperties(dataSource, config.getDataSourceProperties());
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
      PropertyBeanSetter.setTargetFromProperties(dataSource, config.getDataSourceProperties());

      Assert.assertSame(PrintWriter.class, dataSource.getLogWriter().getClass());
   }

   @Test
   public void testPropertyUpperCase() throws Exception
   {
      File file = new File("/propfile3.properties");
      HikariConfig config = new HikariConfig(file.getPath());
      config.validate();

      Class<?> clazz = this.getClass().getClassLoader().loadClass(config.getDataSourceClassName());
      DataSource dataSource = (DataSource) clazz.newInstance();
      PropertyBeanSetter.setTargetFromProperties(dataSource, config.getDataSourceProperties());
   }

   @Test
   public void testGetPropertyNames() throws Exception
   {
      Set<String> propertyNames = PropertyBeanSetter.getPropertyNames(HikariConfig.class);
      Assert.assertTrue(propertyNames.contains("dataSourceClassName"));
   }

   @Test
   public void testSetNonExistantPropertyName() throws Exception
   {
      try {
         Properties props = new Properties();
         props.put("what", "happened");
         PropertyBeanSetter.setTargetFromProperties(new HikariConfig(), props);
         Assert.fail();
      }
      catch (RuntimeException e) {
      }
   }
}
