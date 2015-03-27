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

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

/**
 * Helper class to register our MBeans.
 *
 * @author Brett Wooldridge
 */
public final class HikariMBeanElf
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HikariMBeanElf.class);

   static
   {
      // Stupid hack for coverage, otherwise it shows the private constructor as dead code.
      new HikariMBeanElf();
   }

   private HikariMBeanElf()
   {
      // utility class
   }

   /**
    * Register MBeans for HikariConfig and HikariPool.
    *
    * @param configuration a HikariConfig instance
    * @param pool a HikariPool instance
    */
   public static void registerMBeans(final HikariConfig configuration, final HikariPool pool)
   {
      if (!configuration.isRegisterMbeans()) {
         return;
      }

      try {
         final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

         final ObjectName poolConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + configuration.getPoolName() + ")");
         final ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + configuration.getPoolName() + ")");
         if (!mBeanServer.isRegistered(poolConfigName)) {
            mBeanServer.registerMBean(configuration, poolConfigName);
            mBeanServer.registerMBean(pool, poolName);
         }
         else {
            LOGGER.error("You cannot use the same pool name for separate pool instances.");
         }
      }
      catch (Exception e) {
         LOGGER.warn("Unable to register management beans.", e);
      }
   }

   /**
    * Unregister MBeans for HikariConfig and HikariPool.
    *
    * @param configuration a HikariConfig instance
    * @param pool a HikariPool instance
    */
   public static void unregisterMBeans(final HikariConfig configuration, final HikariPool pool)
   {
      try {
         final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

         final ObjectName poolConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + configuration.getPoolName() + ")");
         final ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + configuration.getPoolName() + ")");
         if (mBeanServer.isRegistered(poolConfigName)) {
            mBeanServer.unregisterMBean(poolConfigName);
            mBeanServer.unregisterMBean(poolName);
         }
      }
      catch (Exception e) {
         LOGGER.warn("Unable to unregister management beans.", e);
      }
   }
}
