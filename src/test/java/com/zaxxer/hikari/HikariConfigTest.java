package com.zaxxer.hikari;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HikariConfigTest {

   @Test
   public void getPasswordReturnsPasswordFromSupplier() {
      HikariConfig config = new HikariConfig();
      config.setPasswordSupplier(() -> "supplied-password");

      String password = config.getPassword();

      assertThat(password, is(equalTo("supplied-password")));
   }

   @Test
   public void getPasswordReturnsDefaultPassword() {
      HikariConfig config = new HikariConfig();
      config.setPassword("default-password");

      String password = config.getPassword();

      assertThat(password, is(equalTo("default-password")));
   }

   @Test(expected = IllegalStateException.class)
   public void setPasswordThrowsExceptionWhenSupplierWasProvided() {
      HikariConfig config = new HikariConfig();
      config.setPasswordSupplier(() -> "supplied-password");

      config.setPassword("default-password");

      fail("Should have thrown exception");
   }

   @Test(expected = IllegalStateException.class)
   public void setPasswordSupplierThrowsExceptionWhenDefaultPasswordWasProvided() {
      HikariConfig config = new HikariConfig();
      config.setPassword("default-password");

      config.setPasswordSupplier(() -> "supplied-password");

      fail("Should have thrown exception");
   }
}
