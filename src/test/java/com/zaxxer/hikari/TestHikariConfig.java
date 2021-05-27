/*
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

import java.util.function.Supplier;

public class TestHikariConfig {

   @Test
   public void testPasswordAsString()
   {
      // default password NULL
      HikariConfig config = new HikariConfig();
      Assert.assertNull(config.getPassword());
      Assert.assertNull(config.getPasswordSupplier());

      // set password NULL
      config = new HikariConfig();
      config.setPassword(null);
      Assert.assertNull(config.getPassword());
      Assert.assertNull(config.getPasswordSupplier());

      // set password value
      config = new HikariConfig();
      config.setPassword("password");
      Assert.assertEquals("password", config.getPassword());
      Assert.assertNull(config.getPasswordSupplier());
   }

   @Test
   public void testPasswordAsSupplier()
   {
      HikariConfig config = new HikariConfig();
      Supplier<String> supplier = () -> "password";
      config.setPasswordSupplier(supplier);
      Assert.assertEquals("password", config.getPassword());
      Assert.assertEquals(supplier, config.getPasswordSupplier());
   }

}
