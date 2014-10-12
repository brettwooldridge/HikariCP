/*
 * Copyright (C) 2014 Brett Wooldridge
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
package com.zaxxer.hikari;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Brett Wooldridge
 */
public class TestValidation
{
   @Test
   public void validateInvalidCustomizer()
   {
      HikariConfig config = new HikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setConnectionCustomizerClassName("invalid");
      config.validate();

      Assert.assertNull(config.getConnectionCustomizerClassName());
   }

   @Test
   public void validateMissingProperties()
   {
      try {
         HikariConfig config = new HikariConfig("missing");
         config.validate();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("Property file"));
      }
   }

   @Test
   public void validateMissingUrl()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setDriverClassName("com.zaxxer.hikari.mocks.StubDriver");
         config.validate();
         Assert.fail();
      }
      catch (IllegalStateException ise) {
         // pass
      }
   }

   @Test
   public void validateMissingDriver()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setJdbcUrl("jdbc:stub");
         config.validate();
         Assert.fail();
      }
      catch (IllegalStateException ise) {
         // pass
      }
   }

   @Test
   public void validateBadDriver()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setDriverClassName("invalid");
         config.validate();
         Assert.fail();
      }
      catch (RuntimeException ise) {
         Assert.assertTrue(ise.getMessage().contains("driverClassName specified class"));
      }
   }

   @Test
   public void validateMissingDriverAndDS()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.validate();
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("one of either dataSource"));
      }
   }

   @Test
   public void validateInvalidConnectionTimeout()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setConnectionTimeout(10L);
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("connectionTimeout cannot be less than 100ms"));
      }
   }

   @Test
   public void validateInvalidIdleTimeout()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setIdleTimeout(-1L);
         Assert.fail("negative idle timeout accepted");
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("idleTimeout cannot be negative"));
      }
   }

   @Test
   public void validateInvalidMinIdle()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setMinimumIdle(-1);
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("minimumIdle cannot be negative"));
      }
   }

   @Test
   public void validateInvalidMaxPoolSize()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setMaximumPoolSize(0);
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("maxPoolSize cannot be less than 1"));
      }
   }

   @Test
   public void validateBothDriverAndDS()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
         config.setDriverClassName("com.zaxxer.hikari.mocks.StubDriver");
         config.setJdbcUrl("jdbc:stub");
         config.validate();
         Assert.fail();
      }
      catch (IllegalStateException ise) {
         Assert.assertTrue(ise.getMessage().contains("both driverClassName"));
      }
   }

   @Test
   public void validateMissingTest()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
         config.setJdbc4ConnectionTest(false);
         config.validate();
         Assert.fail();
      }
      catch (IllegalStateException ise) {
         Assert.assertTrue(ise.getMessage().contains("Either jdbc4ConnectionTest"));
      }
   }

   @Test
   public void validateInvalidLifetime()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setConnectionTimeout(Integer.MAX_VALUE);
         config.setIdleTimeout(1000L);
         config.setLeakDetectionThreshold(1000L);
         config.setMaxLifetime(-1L);
         config.validate();
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         // pass
      }
   }
}
