/*
 * Copyright (C) 2013, 2019 Brett Wooldridge
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

package com.zaxxer.hikari.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class UtilityElfTest
{
   @Test
   public void shouldReturnValidTransactionIsolationLevel()
   {
      //Act
      int expectedLevel = UtilityElf.getTransactionIsolation("TRANSACTION_SQL_SERVER_SNAPSHOT_ISOLATION_LEVEL");

      //Assert
      assertEquals(4096, expectedLevel);
   }

   @Test(expected = IllegalArgumentException.class)
   public void shouldThrowWhenInvalidTransactionNameGiven()
   {
      //Act
      UtilityElf.getTransactionIsolation("INVALID_TRANSACTION");
   }

   @Test
   public void shouldReturnTransationIsolationLevelFromInteger()
   {
      int expectedLevel = UtilityElf.getTransactionIsolation("4096");
      assertEquals(4096, expectedLevel);
   }

   @Test(expected = IllegalArgumentException.class)
   public void shouldThrowWhenInvalidTransactionIntegerGiven()
   {
      //Act
      UtilityElf.getTransactionIsolation("9999");
   }

   @Test
   public void shouldCreateInstanceOfClassWithConstructorThatAcceptsSuperClassAndInterfaceAndClassOfArguments() {
      //Act
      UtilityElf.createInstance("com.zaxxer.hikari.util.UtilityElfTest$ClassZ",
         Object.class,
         new ClassB(),
         new ClassC(),
         new ClassD());
   }

   @Test
   public void shouldCreateInstanceFindTheMostSuitableConstructor() {
      Integer i = 1;
      Long l = 2L;
      Float f = 3.0F;
      Double d = 4.0;
      BigInteger bi = BigInteger.valueOf(5);
      BigDecimal bd = BigDecimal.valueOf(6);

      MultipleConstructorsClass integerInstance =
         UtilityElf.createInstance("com.zaxxer.hikari.util.UtilityElfTest$MultipleConstructorsClass",
            MultipleConstructorsClass.class,
            i, f);
      MultipleConstructorsClass longInstance =
         UtilityElf.createInstance("com.zaxxer.hikari.util.UtilityElfTest$MultipleConstructorsClass",
            MultipleConstructorsClass.class,
            l, bi);
      MultipleConstructorsClass numberInstance =
         UtilityElf.createInstance("com.zaxxer.hikari.util.UtilityElfTest$MultipleConstructorsClass",
            MultipleConstructorsClass.class,
            d, bd);

      assertEquals(Integer.class, integerInstance.cls);
      assertEquals(Long.class, longInstance.cls);
      assertEquals(Number.class, numberInstance.cls);
   }

   public static class ClassA {}

   public static final class ClassB extends ClassA {}

   public interface InterfaceC {}

   public final static class ClassC implements InterfaceC {}

   public final static class ClassD {}

   @SuppressWarnings("unused")
   public final static class ClassZ {
      public ClassZ(ClassA _superClassA, InterfaceC _interfaceC, ClassD _classD) {}
   }

   @SuppressWarnings("unused")
   public final static class MultipleConstructorsClass {
      Number num1, num2;
      Class<?> cls; // to map which constructor is called

      public MultipleConstructorsClass(Number num1, Number num2) {
         cls = Number.class;
         this.num1 = num1;
         this.num2 = num2;
      }

      public MultipleConstructorsClass(Integer i, Float num2) {
         cls = Integer.class;
         this.num1 = i;
         this.num2 = num2;
      }

      public MultipleConstructorsClass(Long l, Number num2) {
         cls = Long.class;
         this.num1 = l;
         this.num2 = num2;
      }
   }
}
