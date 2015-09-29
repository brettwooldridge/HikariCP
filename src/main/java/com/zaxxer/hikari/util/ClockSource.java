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
   long elapsedMillis(long startTime);

   /**
    * Get the difference in milliseconds between two opaque time-stamps returned
    * by currentTime().
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @param endTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and endTime in milliseconds
    */
   long elapsedMillis(long startTime, long endTime);

   /**
    * Convert an opaque time-stamp returned by currentTime() into an
    * elapsed time in milliseconds, based on the current instant in time.
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and now in milliseconds
    */
   long elapsedNanos(long startTime);

   /**
    * Get the difference in nanoseconds between two opaque time-stamps returned
    * by currentTime().
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @param endTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and endTime in nanoseconds
    */
   long elapsedNanos(long startTime, long endTime);

   /**
    * Return the specified opaque time-stamp plus the specified number of milliseconds.
    *
    * @param time an opaque time-stamp 
    * @param millis milliseconds to add
    * @return a new opaque time-stamp
    */
   long plusMillis(long time, long millis);

   /**
    * Get the TimeUnit the ClockSource is denominated in.
    * @return
    */
   TimeUnit getSourceTimeUnit();

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

   final class MillisecondClockSource implements ClockSource
   {
      /** {@inheritDoc} */
      @Override
      public long currentTime()
      {
         return System.currentTimeMillis();
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis(final long startTime)
      {
         return System.currentTimeMillis() - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis(final long startTime, final long endTime)
      {
         return endTime - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos(final long startTime)
      {
         return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis() - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos(final long startTime, final long endTime)
      {
         return TimeUnit.MILLISECONDS.toNanos(endTime - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long toMillis(final long time)
      {
         return time;
      }

      /** {@inheritDoc} */
      @Override
      public long plusMillis(final long time, final long millis)
      {
         return time + millis;
      }

      /** {@inheritDoc} */
      @Override
      public TimeUnit getSourceTimeUnit()
      {
         return TimeUnit.MILLISECONDS;
      }
   }

   final class NanosecondClockSource implements ClockSource
   {
      /** {@inheritDoc} */
      @Override
      public long currentTime()
      {
         return System.nanoTime();
      }

      /** {@inheritDoc} */
      @Override
      public final long toMillis(final long time)
      {
         return TimeUnit.NANOSECONDS.toMillis(time);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis(final long startTime)
      {
         return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis(final long startTime, final long endTime)
      {
         return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos(final long startTime)
      {
         return System.nanoTime() - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos(final long startTime, final long endTime)
      {
         return endTime - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long plusMillis(final long time, final long millis)
      {
         return time + TimeUnit.MILLISECONDS.toNanos(millis);
      }

      /** {@inheritDoc} */
      @Override
      public TimeUnit getSourceTimeUnit()
      {
         return TimeUnit.NANOSECONDS;
      }
   }
}
