/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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

package com.zaxxer.hikari.metrics;

/**
 * This class only supports realtime, not historical metrics.
 *
 * @author Brett Wooldridge
 */
public class MetricsTracker implements AutoCloseable
{
   public MetricsTracker()
   {
   }

   public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos)
   {
   }

   public void recordConnectionUsageMillis(final long elapsedBorrowedMillis)
   {
   }

   public void recordConnectionTimeout()
   {
   }

   @Override
   public void close()
   {
   }
}
