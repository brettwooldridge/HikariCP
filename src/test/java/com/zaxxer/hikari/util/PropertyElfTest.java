package com.zaxxer.hikari.util;

import org.junit.Test;
import com.zaxxer.hikari.mocks.TestObject;

import java.util.Properties;

import static org.junit.Assert.*;

public class PropertyElfTest
{
   @Test
   public void setTargetFromProperties() throws Exception
   {
      Properties properties = new Properties();
      properties.setProperty("string", "aString");
      properties.setProperty("testObject", "com.zaxxer.hikari.mocks.TestObject");
      properties.setProperty("shortRaw", "1");
      properties.setProperty("charArray", "aCharArray");
      TestObject testObject = new TestObject();
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertEquals("aString", testObject.getString());
      assertEquals((short) 1, testObject.getShortRaw());
      assertArrayEquals("aCharArray".toCharArray(), testObject.getCharArray());
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

      RuntimeException e =
         assertThrows(RuntimeException.class, () -> PropertyElf.setTargetFromProperties(testObject, properties));
      assertEquals("argument type mismatch", e.getCause().getMessage());
   }

   @Test
   public void setStringArray()
   {
      Properties properties = new Properties();
      TestObject testObject = new TestObject();

      properties.setProperty("stringArray", "abc,123");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new String[] {"abc", "123"}, testObject.getStringArray());

      properties.setProperty("stringArray", "abc\\,123");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new String[] {"abc,123"}, testObject.getStringArray());

      properties.setProperty("stringArray", "abc\\\\,123");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new String[] {"abc\\","123"}, testObject.getStringArray());

      properties.setProperty("stringArray", "");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new String[] {}, testObject.getStringArray());

      properties.setProperty("stringArray", "abc,12\\3");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new String[] {"abc","123"}, testObject.getStringArray());

      properties.setProperty("stringArray", "abc,123\\");
      assertThrows(RuntimeException.class, () -> PropertyElf.setTargetFromProperties(testObject, properties));
   }

   @Test
   public void setIntArray()
   {
      Properties properties = new Properties();
      TestObject testObject = new TestObject();

      properties.setProperty("intArray", "1,2,3");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new int[] {1,2,3}, testObject.getIntArray());

      properties.setProperty("intArray", "");
      PropertyElf.setTargetFromProperties(testObject, properties);
      assertArrayEquals(new int[] {}, testObject.getIntArray());
   }

   @Test
   public void shouldHandlePrimitiveTypeBoxing () throws Exception
   {
      Properties properties = new Properties();
      TestObject testObject = new TestObject();

      properties.setProperty("shortRaw", "1");
      properties.setProperty("shortObj", "1");
      properties.setProperty("intRaw", "2");
      properties.setProperty("intObj", "2");
      properties.setProperty("longRaw", "3");
      properties.setProperty("longObj", "3");
      properties.setProperty("boolRaw", "true");
      properties.setProperty("boolObj", "false");

      PropertyElf.setTargetFromProperties(testObject, properties);

      assertEquals((short) 1, testObject.getShortRaw());
      assertEquals(Short.valueOf("1"), testObject.getShortObj());
      assertEquals(2, testObject.getIntRaw());
      assertEquals(Integer.valueOf(2), testObject.getIntObj());
      assertEquals(3L, testObject.getLongRaw());
      assertEquals(Long.valueOf(3L), testObject.getLongObj());
      assertTrue(testObject.getBoolRaw()); // equivalent to assertEquals(true, testObject.getBoolRaw());
      assertEquals(Boolean.FALSE, testObject.getBoolObj());
   }
}
