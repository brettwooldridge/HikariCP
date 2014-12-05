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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Brett Wooldridge
 */
public abstract class AbstractBagEntry implements IConcurrentBagEntry
{
   public final AtomicInteger state = new AtomicInteger();

   @Override
   public String toString()
   {
      switch (state.get()) {
      case STATE_IN_USE:
         return "IN_USE";
      case STATE_NOT_IN_USE:
         return "NOT_IN_USE";
      case STATE_REMOVED:
         return "REMOVED";
      case STATE_RESERVED:
         return "RESERVED";
      default:
         return "Invalid";
      }
   }
}
