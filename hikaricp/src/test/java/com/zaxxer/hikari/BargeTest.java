/*
 * Copyright (C) 2014 Brett Wooldridge
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

package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;

/**
 * THIS IS NOT ACTUALLY A UNIT TEST AND SHOULD NOT BE ANNOTATED WITH
 * JUNIT ANNOTATIONS.  THIS IS A TEST TO MEASURE "QUEUE BARGING"
 * BEHAVIOR.
 *
 * @author Brett Wooldridge
 */
public class BargeTest
{
   public static void main(String[] args) {
      try {
         new BargeTest().bargeTest();
         new BargeTest().bargeTest2();
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   private HikariDataSource ds;

   public void bargeTest() throws InterruptedException {

      ds = new HikariDataSource();
      ds.setMaximumPoolSize(8);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final FastWorker[] workers = new FastWorker[32];
      for (int i = 0; i < workers.length; i++) {
         FastWorker w = new FastWorker();
         w.setDaemon(true);
         workers[i] = w;
         w.start();
      }

      Thread.sleep(1000);
      for (final FastWorker w : workers) {
         w.iterations = 0;
      }

      Thread.sleep(8000);

      ds.close();

      final MetricRegistry registry = new MetricRegistry();
      final Histogram histogram = registry.histogram("foo");

      for (final FastWorker w : workers) {
         histogram.update(w.iterations);
      }
      
      final Logger logger = LoggerFactory.getLogger(getClass());
      final Snapshot snapshot = histogram.getSnapshot();
      logger.info("Min: {}, Max: {}. Median: {}", snapshot.getMin(), snapshot.getMax(), (int) snapshot.getMedian());
      logger.info("75%: {}, 95%: {}, 99%: {}", (int) snapshot.get75thPercentile(), (int)snapshot.get95thPercentile(), (int)snapshot.get99thPercentile());
   }

   public void bargeTest2() throws InterruptedException {
      ds = new HikariDataSource();
      ds.setMaximumPoolSize(1);
      ds.setConnectionTestQuery("VALUES 1");
      ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final FastWorker fastWorker = new FastWorker();
      fastWorker.setDaemon(true);
      fastWorker.start();

      final PacedWorker pacedWorker = new PacedWorker();
      pacedWorker.setDaemon(true);
      pacedWorker.start();

      Thread.sleep(1000);

      pacedWorker.iterations = 0;
      
      Thread.sleep(5000);

      int snapshot = pacedWorker.iterations;

      ds.close();

      final Logger logger = LoggerFactory.getLogger(getClass());
      logger.info("Expect paced worker connection acquisitions to be ~50.  Actual: {}", snapshot);

   }

   class FastWorker extends Thread {
      volatile int iterations;

      public void run() {
         while (true) {
            try (Connection c = ds.getConnection()) {
               iterations++;
               Statement statement = c.createStatement();
               statement.execute("SOMETHING");
            }
            catch (SQLException e) {
               break;
            }
         }
      };
   }

   class PacedWorker extends Thread {
      volatile int iterations;

      public void run() {
         while (true) {
            try (Connection c = ds.getConnection()) {

               // wait 100ms, but do so without sleeping ... just spin one core
               for (final long start = System.currentTimeMillis(); System.currentTimeMillis() - start < 100;);

               iterations++;
               Statement statement = c.createStatement();
               statement.execute("SOMETHING");
            }
            catch (SQLException e) {
               break;
            }
         }
      };
   }
}
