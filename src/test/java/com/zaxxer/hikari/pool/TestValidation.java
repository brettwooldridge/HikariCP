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
package com.zaxxer.hikari.pool;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;

/**
 * @author Brett Wooldridge
 */
public class TestValidation
{
   @Test
   public void validateLoadProperties()
   {
      System.setProperty("hikaricp.configurationFile", "/propfile1.properties");
      HikariConfig config = new HikariConfig();
      System.clearProperty("hikaricp.configurationFile");
      Assert.assertEquals(5, config.getMinimumIdle());
   }

   @Test
   public void validateMissingProperties()
   {
      try {
         HikariConfig config = new HikariConfig("missing");
         config.validate();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("property file"));
      }
   }

   @Test
   public void validateMissingDS()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.validate();
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("dataSource or dataSourceClassName or jdbcUrl is required."));
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
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("jdbcUrl is required with driverClassName"));
      }
   }

   @Test
   public void validateDriverAndUrl()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setDriverClassName("com.zaxxer.hikari.mocks.StubDriver");
         config.setJdbcUrl("jdbc:stub");
         config.validate();
      }
      catch (Throwable t) {
          Assert.fail(t.getMessage());
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
         Assert.assertTrue(ise.getMessage().contains("class of driverClassName "));
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
         Assert.assertTrue(ise.getMessage().contains("connectionTimeout cannot be less than 250ms"));
      }
   }

   @Test
   public void validateInvalidValidationTimeout()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setValidationTimeout(10L);
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         Assert.assertTrue(ise.getMessage().contains("validationTimeout cannot be less than 250ms"));
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
   public void validateIdleTimeoutTooSmall()
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(HikariConfig.class, ps);

      HikariConfig config = new HikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setIdleTimeout(TimeUnit.SECONDS.toMillis(5));
      config.validate();
      Assert.assertTrue(new String(baos.toByteArray()).contains("less than 10000ms"));
   }

   @Test
   public void validateIdleTimeoutExceedsLifetime()
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos, true);
      TestElf.setSlf4jTargetStream(HikariConfig.class, ps);

      HikariConfig config = new HikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaxLifetime(TimeUnit.MINUTES.toMillis(2));
      config.setIdleTimeout(TimeUnit.MINUTES.toMillis(3));
      config.validate();

      String s = new String(baos.toByteArray());
      Assert.assertTrue("idleTimeout is close to or more than maxLifetime, disabling it." + s + "*", s.contains("is close to or more than maxLifetime"));
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
   public void validateInvalidLifetime()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setConnectionTimeout(Integer.MAX_VALUE);
         config.setIdleTimeout(1000L);
         config.setMaxLifetime(-1L);
         config.setLeakDetectionThreshold(1000L);
         config.validate();
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
         // pass
      }
   }

   @Test
   public void validateInvalidLeakDetection()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setLeakDetectionThreshold(1000L);
         config.validate();
         Assert.fail();
      }
      catch (IllegalArgumentException ise) {
      // pass
      }
   }

   @Test
   public void validateZeroConnectionTimeout()
   {
      try {
         HikariConfig config = new HikariConfig();
         config.setConnectionTimeout(0);
         config.validate();
         Assert.assertEquals(Integer.MAX_VALUE, config.getConnectionTimeout());
      }
      catch (IllegalArgumentException ise) {
         // pass
      }
   }
}
