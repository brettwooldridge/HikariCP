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

package com.zaxxer.hikari;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.HashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.proxy.ConnectionProxy;

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
         Field field = ConnectionProxy.class.getDeclaredField("isCommitStateDirty");
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
      SimpleLogger simpleLogger = (SimpleLogger) LoggerFactory.getLogger(clazz);
      try {
         Field field = clazz.getClassLoader().loadClass("org.slf4j.impl.SimpleLogger").getDeclaredField("TARGET_STREAM");
         field.setAccessible(true);
         field.set(simpleLogger, stream);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static void setSlf4jLogLevel(Class<?> clazz, int logLevel)
   {
      SimpleLogger simpleLogger = (SimpleLogger) LoggerFactory.getLogger(clazz);
      try {
         Field field = clazz.getClassLoader().loadClass("org.slf4j.impl.SimpleLogger").getDeclaredField("currentLogLevel");
         field.setAccessible(true);
         field.setInt(simpleLogger, logLevel);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}
