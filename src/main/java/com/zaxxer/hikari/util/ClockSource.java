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

/**
 * A resolution-independent provider of current time-stamps and elapsed time
 * calculations.
 * 
 * @author Brett Wooldridge
 */
public interface ClockSource
{
   final ClockSource INSTANCE = Factory.create();

   /**
    * Get the current time-stamp (resolution is opaque).
    *
    * @return the current time-stamp
    */
   long currentTime();

   /**
    * Convert an opaque time-stamp returned by currentTime() into
    * milliseconds.
    *
    * @param time an opaque time-stamp returned by an instance of this class
    * @return the time-stamp in milliseconds
    */
   long toMillis(long time);

   /**
    * Convert an opaque time-stamp returned by currentTime() into an
    * elapsed time in milliseconds, based on the current instant in time.
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and now in milliseconds
    */
   long elapsedTimeMs(long startTime);

   /**
    * Factory class used to create a platform-specific ClockSource. 
    */
   class Factory
   {
      private static ClockSource create()
      {
         String os = System.getProperty("os.name");
         if ("Mac OS X".equals(os)) {
            return new MillisecondClockSource();
         }

         return new NanosecondClockSource();
      }
   }
}
