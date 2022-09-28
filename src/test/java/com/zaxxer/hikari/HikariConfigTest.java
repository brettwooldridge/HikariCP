/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.*;

public class HikariConfigTest {
   private TestAppender testAppender = new TestAppender();

   @Before
   public void setup() {
      getLoggerConfig().addAppender(testAppender, Level.ALL, null);
   }

   @After
   public void tearDown() {
      getLoggerConfig().removeAppender(testAppender.getName());
   }

   private static LoggerConfig getLoggerConfig() {
      LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
      Configuration configuration = loggerContext.getConfiguration();
      return configuration.getRootLogger();
   }

   @Test
   public void testJdbcUrlLogging() {
      List<String> urls = Arrays.asList(
         "jdbc:postgresql://host/d_dlq?user=USER&password=SECRET",
         "jdbc:postgresql://host/d_dlq?user=USER&truststorePassword=SECRET",
         "jdbc:postgresql://host/d_dlq?a=b&password=SECRET&user=USER",
         "jdbc:postgresql://host/d_dlq?a=b&sslpassword=SECRET&user=USER",
         "jdbc:postgresql://host/d_dlq?truststorePassword=SECRET;user=USER&password=SECRET#extra",
         "jdbc:postgresql://host/d_dlq?a=b&sslpassword=SECRET&password=SECRET&user=USER",
         "jdbc:postgresql://host/d_dlq?sslpassword=SECRET&password=SECRET&trustPassword=SECRET&user=USER",
         "jdbc:postgresql://host/d_dlq?password=SECRET#user=USER;extra"
      );

      for (String url : urls) {
         testJdbcUrl(url);
      }
   }

   private void testJdbcUrl(String jdbcUrl) {
      HikariConfig config = newHikariConfig();
      config.setJdbcUrl(jdbcUrl);
      config.validate();

      assertTrue(testAppender.getLog().contains("jdbc:postgresql://host/d_dlq"));
      assertTrue(testAppender.getLog().contains("user=USER"));
      assertFalse("Log should not contain password", testAppender.getLog().contains("SECRET"));
   }


   private static class TestAppender extends AbstractAppender {

      private String log;

      TestAppender() {
         super("TestAppender", (Filter)null, (Layout)null, true, Property.EMPTY_ARRAY);
      }

      @Override
      public void append(LogEvent event) {
         log += event.getMessage().getFormattedMessage() + "\n";
      }

      String getLog() {
         return log;
      }
   }
}
