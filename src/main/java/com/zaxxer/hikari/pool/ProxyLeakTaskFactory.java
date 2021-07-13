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

package com.zaxxer.hikari.pool;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory for {@link ProxyLeakTask} Runnables that are scheduled in the future to report leaks.
 *
 * @author Brett Wooldridge
 * @author Andreas Brenk
 */
class ProxyLeakTaskFactory
{
   private ScheduledExecutorService executorService;
   private long leakDetectionThreshold;

   ProxyLeakTaskFactory(final long leakDetectionThreshold, final ScheduledExecutorService executorService)
   {
      this.executorService = executorService;
      this.leakDetectionThreshold = leakDetectionThreshold;
   }

   ProxyLeakTask schedule(final PoolEntry poolEntry)
   {
      return (leakDetectionThreshold == 0) ? ProxyLeakTask.NO_LEAK : scheduleNewTask(poolEntry);
   }

   void updateLeakDetectionThreshold(final long leakDetectionThreshold)
   {
      this.leakDetectionThreshold = leakDetectionThreshold;
   }

   private ProxyLeakTask scheduleNewTask(PoolEntry poolEntry) {
      var task = new ProxyLeakTask(poolEntry);
      task.schedule(executorService, leakDetectionThreshold);

      return task;
   }
}
