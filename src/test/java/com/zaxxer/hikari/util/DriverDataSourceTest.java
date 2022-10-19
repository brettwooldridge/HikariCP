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

package com.zaxxer.hikari.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class DriverDataSourceTest {

   @Test
   public void testJdbcUrlLogging() {
      List<String> urls = Arrays.asList(
         "jdbc:invalid://host/d_dlq?user=USER&password=SECRET",
         "jdbc:invalid://host/d_dlq?user=USER&truststorePassword=SECRET",
         "jdbc:invalid://host/d_dlq?a=b&password=SECRET&user=USER",
         "jdbc:invalid://host/d_dlq?a=b&sslpassword=SECRET&user=USER",
         "jdbc:invalid://host/d_dlq?a=b&sslpassword=SECRET&password=SECRET&user=USER",
         "jdbc:invalid://host/d_dlq?truststorePassword=SECRET;user=USER&password=SECRET#extra",
         "jdbc:invalid://host/d_dlq?sslpassword=SECRET&password=SECRET&trustPassword=SECRET&user=USER",
         "jdbc:invalid://host/d_dlq?password=SECRET#user=USER;extra"
      );

      for (String url : urls) {
         testExceptionMessage(url);
      }
   }

   private void testExceptionMessage(String jdbcUrl) {
      try {
         new DriverDataSource(jdbcUrl, null, new Properties(), null, null);
         fail();
      } catch (RuntimeException e) {
         String msg = e.getMessage();
         assertTrue(msg.contains("jdbc:invalid://host/d_dlq"));
         assertTrue(msg.contains("user=USER"));
         assertFalse("Exception message should not contain password", msg.contains("SECRET"));
      }

   }
}
