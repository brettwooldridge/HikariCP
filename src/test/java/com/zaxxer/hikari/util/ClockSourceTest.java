/*
 * Copyright (C) 2016 Brett Wooldridge
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

import org.junit.Assert;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * @author Brett Wooldridge
 */
public class ClockSourceTest
{
   @Test
   public void testClockSourceDisplay()
   {
      ClockSource msSource = new ClockSource.MillisecondClockSource();

      final long sTime = DAYS.toMillis(3) + HOURS.toMillis(9) + MINUTES.toMillis(24) + SECONDS.toMillis(18) + MILLISECONDS.toMillis(572);

      final long eTime = DAYS.toMillis(4) + HOURS.toMillis(9) + MINUTES.toMillis(55) + SECONDS.toMillis(23) + MILLISECONDS.toMillis(777);
      String ds1 = msSource.elapsedDisplayString0(sTime, eTime);
      Assert.assertEquals("1d31m5s205ms", ds1);

      final long eTime2 = DAYS.toMillis(3) + HOURS.toMillis(8) + MINUTES.toMillis(24) + SECONDS.toMillis(23) + MILLISECONDS.toMillis(777);
      String ds2 = msSource.elapsedDisplayString0(sTime, eTime2);
      Assert.assertEquals("-59m54s795ms", ds2);


      ClockSource nsSource = new ClockSource.NanosecondClockSource();

      final long sTime2 = DAYS.toNanos(3) + HOURS.toNanos(9) + MINUTES.toNanos(24) + SECONDS.toNanos(18) + MILLISECONDS.toNanos(572) + MICROSECONDS.toNanos(324) + NANOSECONDS.toNanos(823);

      final long eTime3 = DAYS.toNanos(4) + HOURS.toNanos(19) + MINUTES.toNanos(55) + SECONDS.toNanos(23) + MILLISECONDS.toNanos(777) + MICROSECONDS.toNanos(0) + NANOSECONDS.toNanos(982);
      String ds3 = nsSource.elapsedDisplayString0(sTime2, eTime3);
      Assert.assertEquals("1d10h31m5s204ms676µs159ns", ds3);
   }
}
