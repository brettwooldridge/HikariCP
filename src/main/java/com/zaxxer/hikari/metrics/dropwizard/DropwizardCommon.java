/*
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

package com.zaxxer.hikari.metrics.dropwizard;

final class DropwizardCommon {
   private DropwizardCommon()
   {
   }

   static final String METRIC_CATEGORY = "pool";
   static final String METRIC_NAME_WAIT = "Wait";
   static final String METRIC_NAME_USAGE = "Usage";
   static final String METRIC_NAME_CONNECT = "ConnectionCreation";
   static final String METRIC_NAME_TIMEOUT_RATE = "ConnectionTimeoutRate";
   static final String METRIC_NAME_TOTAL_CONNECTIONS = "TotalConnections";
   static final String METRIC_NAME_IDLE_CONNECTIONS = "IdleConnections";
   static final String METRIC_NAME_ACTIVE_CONNECTIONS = "ActiveConnections";
   static final String METRIC_NAME_PENDING_CONNECTIONS = "PendingConnections";
   static final String METRIC_NAME_MAX_CONNECTIONS = "MaxConnections";
   static final String METRIC_NAME_MIN_CONNECTIONS = "MinConnections";
}
