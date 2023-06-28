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

package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import static com.zaxxer.hikari.pool.TestElf.newHikariDataSource;

/**
 * Test HikariCP metrics integration.
 *
 * @author Brett Wooldridge
 */
public class TestMetrics
{
   @Test(expected = IllegalArgumentException.class)
   public void testFakeMetricRegistryThrowsIllegalArgumentException()
   {
      try (HikariDataSource ds = newHikariDataSource()) {
         ds.setMaximumPoolSize(1);
         ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

         FakeMetricRegistry metricRegistry = new FakeMetricRegistry();

         ds.setMetricRegistry(metricRegistry);
      }
   }

   private static class FakeMetricRegistry {}
}
