/*
 * Copyright (C) 2016 Brett Wooldridge
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

package com.zaxxer.hikari.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;

import static com.zaxxer.hikari.pool.TestElf.*;
import static org.junit.Assert.assertEquals;

/**
 * Test for AddConnectionExecutor
 */
public class AddConnectionExecutorTest
{

   @Test
   public void testAddConnectionExecutorCoreSizeInBlockedInitialization()
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(getRandom(1, 5));
      config.setMaximumPoolSize(5);
      config.setConnectionTestQuery("SELECT 1");
      config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
      config.setInitializationFailTimeout(2);
      config.addDataSourceProperty("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      System.setProperty("com.zaxxer.hikari.blockUntilFilled", "true");
      HikariPool hikariPool = new HikariPool(config);
      System.setProperty("com.zaxxer.hikari.blockUntilFilled", "false");

      ThreadPoolExecutor addConnectionExecutor;
      try {
         Field field = hikariPool.getClass().getDeclaredField("addConnectionExecutor");
         field.setAccessible(true);
         addConnectionExecutor = (ThreadPoolExecutor) field.get(hikariPool);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

      assertEquals("Add connection executor's size equals minimum idle connection of hikari config",
         config.getMinimumIdle(), addConnectionExecutor.getCorePoolSize());
   }

   @Test
   public void testAddConnectionExecutorCoreSizeInUnBlockedInitialization()
   {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(getRandom(1, 5));
      config.setMaximumPoolSize(5);
      config.setConnectionTestQuery("SELECT 1");
      config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
      config.addDataSourceProperty("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      HikariPool hikariPool = new HikariPool(config);

      ThreadPoolExecutor addConnectionExecutor;
      try {
         Field field = hikariPool.getClass().getDeclaredField("addConnectionExecutor");
         field.setAccessible(true);
         addConnectionExecutor = (ThreadPoolExecutor) field.get(hikariPool);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

      assertEquals("Add connection executor's size equals minimum idle connection of hikari config",
         1, addConnectionExecutor.getCorePoolSize());
   }

   private static int getRandom(int min, int max){
      Random random = new Random();
      return random.nextInt(max) % (max - min + 1) + min;
   }
}
