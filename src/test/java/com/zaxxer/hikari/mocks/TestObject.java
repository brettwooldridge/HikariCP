package com.zaxxer.hikari.mocks;

public class TestObject
{
   private TestObject testObject;
   private String string;
   private short shortRaw;

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

   public short getShortRaw() {
      return shortRaw;
   }

   public void setShortRaw(short shortRaw) {
      this.shortRaw = shortRaw;
   }
}
