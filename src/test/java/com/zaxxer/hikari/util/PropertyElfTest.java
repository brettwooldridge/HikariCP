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
      try {
         PropertyElf.setTargetFromProperties(testObject, properties);
         fail("Could never come here");
      }
      catch (RuntimeException e) {
         assertEquals("argument type mismatch", e.getCause().getMessage());
      }
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
}
