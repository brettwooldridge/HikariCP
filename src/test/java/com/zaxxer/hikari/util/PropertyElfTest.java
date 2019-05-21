package com.zaxxer.hikari.util;

import org.junit.Assert;
import org.junit.Test;
import com.zaxxer.hikari.mocks.TestObject;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class PropertyElfTest
{
   @Test
   public void setTargetFromProperties() throws Exception
   {
      Properties properties = new Properties();
      properties.setProperty("string", "aString");
      properties.setProperty("testObject", "com.zaxxer.hikari.mocks.TestObject");
      TestObject testObject = new TestObject();
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertEquals("aString", testObject.getString());
      assertEquals(com.zaxxer.hikari.mocks.TestObject.class, testObject.getTestObject().getClass());
      assertNotSame(testObject, testObject.getTestObject());
   }

   @Test
   public void setTargetFromPropertiesNotAClass() throws Exception
   {
      Properties properties = new Properties();
      properties.setProperty("string", "aString");
      properties.setProperty("testObject", "it is not a class");
      TestObject testObject = new TestObject();
      try {
         PropertyElf.setTargetFromProperties(testObject, properties);
         fail("Could never come here");
      }
      catch (RuntimeException e) {
         assertEquals("argument type mismatch", e.getCause().getMessage());
      }
   }

   @Test
   public void test() throws Exception
   {
      TestObject testObject = new TestObject();
      testObject.setString("test");
      Set<String> set = PropertyElf.getPropertyNames(testObject.getClass());
      Field[] fields = testObject.getClass().getDeclaredFields();
      assertEquals(1,set.stream().filter(value->value.equals(fields[0].getName())).count());
      assertEquals(1,set.stream().filter(value->value.equals(fields[1].getName())).count());

      assertEquals("test",PropertyElf.getProperty("string",testObject));

      Properties properties = new Properties();
      properties.setProperty("string", "aString");
      properties.setProperty("testObject", "it is not a class");
      Properties properties1 = PropertyElf.copyProperties(properties);
      assertTrue(properties1.equals(properties));
   }
}
