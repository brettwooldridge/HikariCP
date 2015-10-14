/*
 * Copyright (C) 2013 Brett Wooldridge
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

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.CsvLogEventLayout;
import org.apache.logging.slf4j.Log4jLogger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Utility methods for testing.
 *
 * @author Brett Wooldridge
 */
public final class TestElf
{
   private TestElf() {
      // default constructor
   }

   public static HikariPool getPool(HikariDataSource ds)
   {
      try {
         Field field = ds.getClass().getDeclaredField("pool");
         field.setAccessible(true);
         return (HikariPool) field.get(ds);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @SuppressWarnings("unchecked")
   public static HashMap<Object, HikariPool> getMultiPool(HikariDataSource ds)
   {
      try {
         Field field = ds.getClass().getDeclaredField("multiPool");
         field.setAccessible(true);
         return (HashMap<Object, HikariPool>) field.get(ds);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static boolean getConnectionCommitDirtyState(Connection connection)
   {
      try {
         Field field = ProxyConnection.class.getDeclaredField("isCommitStateDirty");
         field.setAccessible(true);
         return field.getBoolean(connection);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static void setConfigUnitTest(boolean unitTest)
   {
      try {
         Field field = HikariConfig.class.getDeclaredField("unitTest");
         field.setAccessible(true);
         field.setBoolean(null, unitTest);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static void setSlf4jTargetStream(Class<?> clazz, PrintStream stream)
   {
      try {
         Log4jLogger log4Jlogger = (Log4jLogger) LoggerFactory.getLogger(clazz);

         Field field = clazz.getClassLoader().loadClass("org.apache.logging.slf4j.Log4jLogger").getDeclaredField("logger");
         field.setAccessible(true);

         Logger logger = (Logger) field.get(log4Jlogger);
         if (logger.getAppenders().containsKey("string")) {
            Appender appender = logger.getAppenders().get("string");
            logger.removeAppender(appender);
         }

         logger.addAppender(new StringAppender("string", stream));
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static void setSlf4jLogLevel(Class<?> clazz, Level logLevel)
   {
      try {
         Log4jLogger log4Jlogger = (Log4jLogger) LoggerFactory.getLogger(clazz);

         Field field = clazz.getClassLoader().loadClass("org.apache.logging.slf4j.Log4jLogger").getDeclaredField("logger");
         field.setAccessible(true);

         Logger logger = (Logger) field.get(log4Jlogger);
         logger.setLevel(logLevel);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static class StringAppender extends AbstractAppender
   {
      private static final long serialVersionUID = -1932433845656444920L;
      private PrintStream stream;

      StringAppender(String name, PrintStream stream)
      {
         super(name, null, CsvLogEventLayout.createDefaultLayout());
         this.stream = stream;
      }

      @Override
      public void append(LogEvent event)
      {
         stream.println(event.getMessage().getFormattedMessage());
      }
   }
}
