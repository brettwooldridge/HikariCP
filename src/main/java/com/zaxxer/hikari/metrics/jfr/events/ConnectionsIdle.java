/*
 * Copyright (C) 2025 Brett Wooldridge
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

package com.zaxxer.hikari.metrics.jfr.events;

import jdk.jfr.*;

@StackTrace(false)
@Period("1 s")
@Category({"HikariCP"})
public class ConnectionsIdle extends Event {

   @Label("Pool name")
   @Name("pool")
   public String poolName;

   @Label("Connections idle")
   @Name("hikaricp_connections_idle")
   public int idleConnections;

}
