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
   static ClockSource CLOCK = Factory.create();

   /**
    * Get the current time-stamp (resolution is opaque).
    *
    * @return the current time-stamp
    */
   static long currentTime() {
      return CLOCK.currentTime0();
   }

   long currentTime0();

   /**
    * Convert an opaque time-stamp returned by currentTime() into
    * milliseconds.
    *
    * @param time an opaque time-stamp returned by an instance of this class
    * @return the time-stamp in milliseconds
    */
   static long toMillis(long time) {
      return CLOCK.toMillis0(time);
   }

   long toMillis0(long time);

   /**
    * Convert an opaque time-stamp returned by currentTime() into
    * nanoseconds.
    *
    * @param time an opaque time-stamp returned by an instance of this class
    * @return the time-stamp in nanoseconds
    */
   static long toNanos(long time) {
      return CLOCK.toNanos0(time);
   }

   long toNanos0(long time);

   /**
    * Convert an opaque time-stamp returned by currentTime() into an
    * elapsed time in milliseconds, based on the current instant in time.
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and now in milliseconds
    */
   static long elapsedMillis(long startTime) {
      return CLOCK.elapsedMillis0(startTime);
   }

   long elapsedMillis0(long startTime);

   /**
    * Get the difference in milliseconds between two opaque time-stamps returned
    * by currentTime().
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @param endTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and endTime in milliseconds
    */
   static long elapsedMillis(long startTime, long endTime) {
      return CLOCK.elapsedMillis0(startTime, endTime);
   }

   long elapsedMillis0(long startTime, long endTime);

   /**
    * Convert an opaque time-stamp returned by currentTime() into an
    * elapsed time in milliseconds, based on the current instant in time.
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and now in milliseconds
    */
   static long elapsedNanos(long startTime) {
      return CLOCK.elapsedNanos0(startTime);
   }

   long elapsedNanos0(long startTime);

   /**
    * Get the difference in nanoseconds between two opaque time-stamps returned
    * by currentTime().
    *
    * @param startTime an opaque time-stamp returned by an instance of this class
    * @param endTime an opaque time-stamp returned by an instance of this class
    * @return the elapsed time between startTime and endTime in nanoseconds
    */
   static long elapsedNanos(long startTime, long endTime) {
      return CLOCK.elapsedNanos0(startTime, endTime);
   }

   long elapsedNanos0(long startTime, long endTime);

   /**
    * Return the specified opaque time-stamp plus the specified number of milliseconds.
    *
    * @param time an opaque time-stamp
    * @param millis milliseconds to add
    * @return a new opaque time-stamp
    */
   static long plusMillis(long time, long millis) {
      return CLOCK.plusMillis0(time, millis);
   }

   long plusMillis0(long time, long millis);

   /**
    * Get the TimeUnit the ClockSource is denominated in.
    * @return
    */
   static TimeUnit getSourceTimeUnit() {
      return CLOCK.getSourceTimeUnit0();
   }

   TimeUnit getSourceTimeUnit0();

   /**
    * Get a String representation of the elapsed time in appropriate magnitude terminology.
    *
    * @param startTime an opaque time-stamp
    * @param endTime an opaque time-stamp
    * @return a string representation of the elapsed time interval
    */
   static String elapsedDisplayString(long startTime, long endTime) {
      return CLOCK.elapsedDisplayString0(startTime, endTime);
   }

   default String elapsedDisplayString0(long startTime, long endTime) {
      long elapsedNanos = elapsedNanos0(startTime, endTime);

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

   TimeUnit[] TIMEUNITS_DESCENDING = {DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS};

   String[] TIMEUNIT_DISPLAY_VALUES = {"ns", "µs", "ms", "s", "m", "h", "d"};

   /**
    * Factory class used to create a platform-specific ClockSource.
    */
   class Factory
   {
      private static ClockSource create() {
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
      public long currentTime0() {
         return System.currentTimeMillis();
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis0(final long startTime) {
         return System.currentTimeMillis() - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis0(final long startTime, final long endTime) {
         return endTime - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos0(final long startTime) {
         return MILLISECONDS.toNanos(System.currentTimeMillis() - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos0(final long startTime, final long endTime) {
         return MILLISECONDS.toNanos(endTime - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long toMillis0(final long time) {
         return time;
      }

      /** {@inheritDoc} */
      @Override
      public long toNanos0(final long time) {
         return MILLISECONDS.toNanos(time);
      }

      /** {@inheritDoc} */
      @Override
      public long plusMillis0(final long time, final long millis) {
         return time + millis;
      }

      /** {@inheritDoc} */
      @Override
      public TimeUnit getSourceTimeUnit0() {
         return MILLISECONDS;
      }
   }

   class NanosecondClockSource implements ClockSource
   {
      /** {@inheritDoc} */
      @Override
      public long currentTime0() {
         return System.nanoTime();
      }

      /** {@inheritDoc} */
      @Override
      public long toMillis0(final long time) {
         return NANOSECONDS.toMillis(time);
      }

      /** {@inheritDoc} */
      @Override
      public long toNanos0(final long time) {
         return time;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis0(final long startTime) {
         return NANOSECONDS.toMillis(System.nanoTime() - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedMillis0(final long startTime, final long endTime) {
         return NANOSECONDS.toMillis(endTime - startTime);
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos0(final long startTime) {
         return System.nanoTime() - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long elapsedNanos0(final long startTime, final long endTime) {
         return endTime - startTime;
      }

      /** {@inheritDoc} */
      @Override
      public long plusMillis0(final long time, final long millis) {
         return time + MILLISECONDS.toNanos(millis);
      }

      /** {@inheritDoc} */
      @Override
      public TimeUnit getSourceTimeUnit0() {
         return NANOSECONDS;
      }
   }
}
