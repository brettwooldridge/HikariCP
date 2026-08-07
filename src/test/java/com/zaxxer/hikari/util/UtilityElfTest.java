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
   public void shouldMaskQueryParameterPasswordInJdbcUrl()
   {
      assertEquals("jdbc:mysql://host/db?password=<masked>",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:mysql://host/db?password=secret"));
      assertEquals("jdbc:postgresql://host/d_dlq?user=USER&password=<masked>",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:postgresql://host/d_dlq?user=USER&password=SECRET"));
      assertEquals("jdbc:postgresql://host/d_dlq?a=b&sslpassword=<masked>&user=USER",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:postgresql://host/d_dlq?a=b&sslpassword=SECRET&user=USER"));
   }

   @Test
   public void shouldMaskAuthorityEmbeddedPasswordInJdbcUrl()
   {
      assertEquals("jdbc:postgresql://admin:<masked>@db.internal:5432/prod",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:postgresql://admin:s3cret@db.internal:5432/prod"));
      assertEquals("jdbc:mysql://root:<masked>@localhost:3306/app",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:mysql://root:p%40ss@localhost:3306/app"));
   }

   @Test
   public void shouldMaskBothAuthorityAndQueryParameterPasswords()
   {
      assertEquals("jdbc:postgresql://admin:<masked>@host:5432/db?password=<masked>",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:postgresql://admin:s3cret@host:5432/db?password=querySecret"));
   }

   @Test
   public void shouldNotMaskJdbcUrlWithoutPassword()
   {
      assertEquals("jdbc:postgresql://host:5432/db",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:postgresql://host:5432/db"));
      assertEquals("jdbc:postgresql://admin@host:5432/db",
         UtilityElf.maskPasswordInJdbcUrl("jdbc:postgresql://admin@host:5432/db"));
   }

   public static class ClassA {}

   public static final class ClassB extends ClassA {}

   public interface InterfaceC {}

   public final static class ClassC implements InterfaceC {}

   public final static class ClassD {}

   public final static class ClassZ {
      public ClassZ(ClassA _superClassA, InterfaceC _interfaceC, ClassD _classD) {}
   }
}
