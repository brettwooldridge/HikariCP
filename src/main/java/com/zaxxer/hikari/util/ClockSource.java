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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

/**
 * A resolution-independent provider of current time-stamps and elapsed time
 * calculations.
 *
 * @author Brett Wooldridge
 */
public interface ClockSource
{
   ClockSource INSTANCE = Factory.create();

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
    * Convert an opaque time-stamp returned by currentTime() into
    * nanoseconds.
    *
    * @param time an opaque time-stamp returned by an instance of this class
    * @return the time-stamp in nanoseconds
    */
   long toNanos(long time);

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
    * Get a String representation of the elapsed time in appropriate magnitude terminology.
    *
    * @param startTime an opaque time-stamp
    * @param endTime an opaque time-stamp
    * @return a string representation of the elapsed time interval
    */
   String elapsedDisplayString(long startTime, long endTime);

   TimeUnit[] TIMEUNITS_DESCENDING = {DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS};

   String[] TIMEUNIT_DISPLAY_VALUES = {"ns", "μs", "ms", "s", "m", "h", "d"};

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

   final class MillisecondClockSource extends NanosecondClockSource
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
         return MILLISECONDS.toNanos(System.currentTimeMillis() - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos(final long startTime, final long endTime)
      {
         return MILLISECONDS.toNanos(endTime - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long toMillis(final long time)
      {
         return time;
      }

      /** {@inheritDoc} */
      @Override
      public long toNanos(final long time)
      {
         return MILLISECONDS.toNanos(time);
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
         return MILLISECONDS;
      }
   }

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
      public long toMillis(final long time)
      {
         return NANOSECONDS.toMillis(time);
      }

      /** {@inheritDoc} */
      @Override
      public long toNanos(final long time)
      {
         return time;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis(final long startTime)
      {
         return NANOSECONDS.toMillis(System.nanoTime() - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis(final long startTime, final long endTime)
      {
         return NANOSECONDS.toMillis(endTime - startTime);
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
         return time + MILLISECONDS.toNanos(millis);
      }

      /** {@inheritDoc} */
      @Override
      public TimeUnit getSourceTimeUnit()
      {
         return NANOSECONDS;
      }

      /** {@inheritDoc} */
      @Override
      public String elapsedDisplayString(long startTime, long endTime)
      {
         long elapsedNanos = elapsedNanos(startTime, endTime);

         StringBuilder sb = new StringBuilder(elapsedNanos < 0 ? "-" : "");
         elapsedNanos = Math.abs(elapsedNanos);

         for (TimeUnit unit : TIMEUNITS_DESCENDING) {
            long converted = unit.convert(elapsedNanos, NANOSECONDS);
            if (converted > 0) {
               sb.append(converted).append(TIMEUNIT_DISPLAY_VALUES[unit.ordinal()]);
               elapsedNanos -= NANOSECONDS.convert(converted, unit);
            }
         }

         return sb.toString();
      }
   }
}
