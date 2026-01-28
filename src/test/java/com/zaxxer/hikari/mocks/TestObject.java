package com.zaxxer.hikari.mocks;

@SuppressWarnings("unused")
public class TestObject
{
   private TestObject testObject;
   private String string;
   private short shortRaw;
   private Short shortObj;
   private int intRaw;
   private Integer intObj;
   private long longRaw;
   private Long longObj;
   private boolean boolRaw;
   private Boolean boolObj;
   private char[] charArray;
   private String[] stringArray;
   private int[] intArray;

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

   public Short getShortObj() {
      return shortObj;
   }

   public void setShortObj(Short shortObj) {
      this.shortObj = shortObj;
   }

   public int getIntRaw() {
      return intRaw;
   }

   public void setIntRaw(int intRaw) {
      this.intRaw = intRaw;
   }

   public Integer getIntObj() {
      return intObj;
   }

   public void setIntObj(Integer intObj) {
      this.intObj = intObj;
   }

   public long getLongRaw() {
      return longRaw;
   }

   public void setLongRaw(long longRaw) {
      this.longRaw = longRaw;
   }

   public Long getLongObj() {
      return longObj;
   }

   public void setLongObj(Long longObj) {
      this.longObj = longObj;
   }

   public boolean getBoolRaw() {
      return boolRaw;
   }

   public void setBoolRaw(boolean boolRaw) {
      this.boolRaw = boolRaw;
   }

   public Boolean getBoolObj() {
      return boolObj;
   }

   public void setBoolObj(Boolean boolObj) {
      this.boolObj = boolObj;
   }

   public void setCharArray(char[] charArray)
   {
      this.charArray = charArray;
   }

   public char[] getCharArray()
   {
      return charArray;
   }

   public void setStringArray(String[] stringArray)
   {
      this.stringArray = stringArray;
   }

   public String[] getStringArray()
   {
      return stringArray;
   }

   public void setIntArray(int[] intArray)
   {
      this.intArray = intArray;
   }

   public int[] getIntArray()
   {
      return intArray;
   }
}
