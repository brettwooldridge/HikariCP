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

import java.sql.Connection;

import com.zaxxer.hikari.util.ConcurrentBag.BagEntry;

/**
 * Entry used in the ConcurrentBag to track Connection instances.
 *
 * @author Brett Wooldridge
 */
public final class PoolBagEntry extends BagEntry
{
   public final Connection connection;
   public final long expirationTime;
   long lastOpenTime;
   long lastAccess;

   PoolBagEntry(final Connection connection, long maxLifetime) {
      this.connection = connection;
      expirationTime = (maxLifetime > 0 ? System.currentTimeMillis() + maxLifetime : Long.MAX_VALUE);
      lastAccess = expirationTime;
   }
}

