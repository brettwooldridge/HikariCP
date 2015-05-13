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

package com.zaxxer.hikari.util;


/**
 * A System.currentTimeMillis()-based provider of current time-stamps and elapsed time
 * calculations.
 *
 * @author Brett Wooldridge
 */
class MillisecondClockSource implements ClockSource
{
   /** {@inheritDoc} */
   @Override
   public long currentTime()
   {
      return System.currentTimeMillis();
   }

   /** {@inheritDoc} */
   @Override
   public long elapsedTimeMs(final long startTime)
   {
      return System.currentTimeMillis() - startTime;
   }

   /** {@inheritDoc} */
   @Override
   public long toMillis(long time)
   {
      return time;
   }
}
