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
      assertEquals(expectedLevel, 4096);
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
      assertEquals(expectedLevel, 4096);
   }

   @Test(expected = IllegalArgumentException.class)
   public void shouldThrowWhenInvalidTransactionIntegerGiven()
   {
      //Act
      UtilityElf.getTransactionIsolation("9999");
   }
}
