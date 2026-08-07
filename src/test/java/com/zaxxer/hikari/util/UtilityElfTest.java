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
   public void shouldMaskPasswordQueryParameter()
   {
      //Arrange
      String url = "jdbc:mysql://host:3306/db?password=secret&user=admin";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals("jdbc:mysql://host:3306/db?password=<masked>&user=admin", masked);
   }

   @Test
   public void shouldMaskPasswordEmbeddedInUrlAuthority()
   {
      //Arrange
      String url = "jdbc:postgresql://admin:s3cret@db.internal:5432/prod";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals("jdbc:postgresql://admin:<masked>@db.internal:5432/prod", masked);
   }

   @Test
   public void shouldMaskBothAuthorityAndQueryParameterPasswords()
   {
      //Arrange
      String url = "jdbc:postgresql://user:pass@host/db?password=other";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals("jdbc:postgresql://user:<masked>@host/db?password=<masked>", masked);
   }

   @Test
   public void shouldPreservePortWhenMaskingAuthorityPassword()
   {
      //Arrange
      String url = "jdbc:mysql://user:pass@host:3306/db";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals("jdbc:mysql://user:<masked>@host:3306/db", masked);
   }

   @Test
   public void shouldNotMaskUrlWithoutAuthorityPassword()
   {
      //Arrange
      String url = "jdbc:postgresql://host:5432/db?user=admin";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals(url, masked);
   }

   @Test
   public void shouldMaskEverythingAfterFirstColonInAuthority()
   {
      //Arrange
      String url = "jdbc:postgresql://user:p:a:ss@host/db";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals("jdbc:postgresql://user:<masked>@host/db", masked);
   }

   @Test
   public void shouldNotMaskUrlWithAtSignInQueryParameter()
   {
      //Arrange
      String url = "jdbc:mysql://host:3306/db?user=admin@corp.com";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals(url, masked);
   }

   @Test
   public void shouldMaskAuthorityPasswordWithEmptyUsername()
   {
      //Arrange
      String url = "jdbc:postgresql://:secret@host:5432/db";

      //Act
      String masked = UtilityElf.maskPasswordInJdbcUrl(url);

      //Assert
      assertEquals("jdbc:postgresql://:<masked>@host:5432/db", masked);
   }

   @Test
   public void shouldNotMaskMalformedPasswordsWithReservedDelimiters()
   {
      //Arrange: RFC 3986 reserves / ? # as delimiters inside the authority, so these
      //passwords cannot be told apart from path/query content by a regex over the URL.
      String urlWithSlash = "jdbc:mysql://app:Sup3r/Secur3@dbhost/db";
      String urlWithQuestion = "jdbc:mysql://app:Secret?x@dbhost/db";
      String urlWithHash = "jdbc:mysql://app:Secret#x@dbhost/db";

      //Act
      String maskedSlash = UtilityElf.maskPasswordInJdbcUrl(urlWithSlash);
      String maskedQuestion = UtilityElf.maskPasswordInJdbcUrl(urlWithQuestion);
      String maskedHash = UtilityElf.maskPasswordInJdbcUrl(urlWithHash);

      //Assert
      assertEquals(urlWithSlash, maskedSlash);
      assertEquals(urlWithQuestion, maskedQuestion);
      assertEquals(urlWithHash, maskedHash);
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
