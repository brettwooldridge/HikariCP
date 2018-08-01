package com.zaxxer.hikari.mocks;

public class TestObject
{
   private TestObject testObject;
   private String string;

   public void setTestObject(TestObject testObject)
   {
      this.testObject = testObject;
   }

   public void setString(String string)
   {
      this.string = string;
   }

   public TestObject getTestObject()
   {
      return testObject;
   }

   public String getString()
   {
      return string;
   }
}
