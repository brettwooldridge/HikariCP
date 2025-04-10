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

package com.zaxxer.hikari.metrics.jfr;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;
import com.zaxxer.hikari.metrics.jfr.events.*;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;

import java.util.*;

public class JFREventMetricsTracker implements IMetricsTracker {

   private final String poolName;
   private final PoolStats poolStats;
   private final Map<Class<? extends Event>, Runnable> periodicEvents = new HashMap<>();

   JFREventMetricsTracker(final String poolName, final PoolStats poolStats) {
      this.poolName = Objects.requireNonNull(poolName, "Missing pool name");
      this.poolStats = Objects.requireNonNull(poolStats, "Missing pool stats");
      this.periodicEvents.put(ConnectionsActive.class, () -> {
         ConnectionsActive event = new ConnectionsActive();
         if (event.isEnabled()) {
            event.poolName = this.poolName;
            event.activeConnections = this.poolStats.getActiveConnections();
            event.commit();
         }
      });
      this.periodicEvents.put(ConnectionsIdle.class, () -> {
         ConnectionsIdle event = new ConnectionsIdle();
         if (event.isEnabled()) {
            event.poolName = this.poolName;
            event.idleConnections = this.poolStats.getIdleConnections();
            event.commit();
         }
      });
      this.periodicEvents.put(ConnectionsMax.class, () -> {
         ConnectionsMax event = new ConnectionsMax();
         if (event.isEnabled()) {
            event.poolName = this.poolName;
            event.maxConnections = this.poolStats.getMaxConnections();
            event.commit();
         }
      });
      this.periodicEvents.put(ConnectionsMin.class, () -> {
         ConnectionsMin event = new ConnectionsMin();
         if (event.isEnabled()) {
            event.poolName = this.poolName;
            event.minConnections = this.poolStats.getMinConnections();
            event.commit();
         }
      });
      this.periodicEvents.put(ConnectionsTotal.class, () -> {
         ConnectionsTotal event = new ConnectionsTotal();
         if (event.isEnabled()) {
            event.poolName = this.poolName;
            event.totalConnections = this.poolStats.getTotalConnections();
            event.commit();
         }
      });
      this.periodicEvents.put(ThreadsPending.class, () -> {
         ThreadsPending event = new ThreadsPending();
         if (event.isEnabled()) {
            event.poolName = this.poolName;
            event.threadsPending = this.poolStats.getPendingThreads();
            event.commit();
         }
      });
      registerEventClasses(periodicEvents.keySet(), ConnectionAcquired.class, ConnectionCreated.class, ConnectionTimeout.class, ConnectionUsage.class);
      addPeriodicEvents(periodicEvents);
   }

   @SafeVarargs
   private static void registerEventClasses(Set<Class<? extends Event>> events, Class<? extends Event> ... additional) {
      events.forEach(FlightRecorder::register);
      Arrays.stream(additional).forEach(FlightRecorder::register);
   }

   private static void addPeriodicEvents(Map<Class<? extends Event>, Runnable> periodicEvents) {
      periodicEvents.forEach(FlightRecorder::addPeriodicEvent);
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
      ConnectionCreated event = new ConnectionCreated();
      if (event.isEnabled()) {
         event.poolName = this.poolName;
         event.connectionCreatedMillis = connectionCreatedMillis;
         event.commit();
      }
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
      ConnectionAcquired event = new ConnectionAcquired();
      if (event.isEnabled()) {
         event.poolName = this.poolName;
         event.elapsedAcquiredNanos = elapsedAcquiredNanos;
         event.commit();
      }
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
      ConnectionUsage event = new ConnectionUsage();
      if (event.isEnabled()) {
         event.poolName = this.poolName;
         event.elapsedBorrowedMillis = elapsedBorrowedMillis;
         event.commit();
      }
   }

   @Override
   public void recordConnectionTimeout() {
      ConnectionTimeout event = new ConnectionTimeout();
      if (event.isEnabled()) {
         event.poolName = this.poolName;
         event.commit();
      }
   }

   @Override
   public void close() {
      this.periodicEvents.values().forEach(FlightRecorder::removePeriodicEvent);
   }

}
