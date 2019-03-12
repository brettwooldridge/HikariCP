package com.zaxxer.hikari.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class UtilityElfTest {

   @Test
   public void shouldReturnValidTransactionIsolationLevel() {
      //Arrange

      //Act
      int expectedLevel = UtilityElf.getTransactionIsolation("TRANSACTION_SQL_SERVER_SNAPSHOT_ISOLATION_LEVEL");

      //Assert
      assertEquals(expectedLevel, 4096);
   }


   @Test(expected = IllegalArgumentException.class)
   public void shouldThrowWhenInvalidTransactionNameGiven() {
      //Arrange

      //Act
      UtilityElf.getTransactionIsolation("INVALID_TRANSACTION");
   }
}
