/*
 * Copyright (C) 2015 Brett Wooldridge
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

import java.util.concurrent.TimeUnit;

/**
 * A System.nanoTime()-based provider of current time-stamps and elapsed time
 * calculations.
 *
 * @author Brett Wooldridge
 */
class NanosecondClockSource implements ClockSource
{
   /** {@inheritDoc} */
   @Override
   public long currentTime()
   {
      return System.nanoTime();
   }

   /** {@inheritDoc} */
   @Override
   public long elapsedTimeMs(final long startTime)
   {
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
   }

   /** {@inheritDoc} */
   @Override
   public final long toMillis(final long time)
   {
      return TimeUnit.NANOSECONDS.toMillis(time);
   }
}
