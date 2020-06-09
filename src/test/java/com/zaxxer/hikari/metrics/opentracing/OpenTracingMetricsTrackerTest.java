package com.zaxxer.hikari.metrics.opentracing;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.junit.Assert.*;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.LogEntry;
import io.opentracing.mock.MockTracer;
import java.sql.Connection;
import org.junit.Test;

public class OpenTracingMetricsTrackerTest {

   @Test
   public void testHandlesNoSpanPresent() throws Exception
   {
      MockTracer mockTracer = new MockTracer();
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new OpenTracingMetricsTrackerFactory(mockTracer));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);

      try (HikariDataSource dataSource = new HikariDataSource(config)) {
         try (Connection ignored = dataSource.getConnection()) {

         }
      }

      assertTrue(mockTracer.finishedSpans().isEmpty());
   }

   @Test
   public void testLogsEventsOnSpan() throws Exception
   {
      MockTracer mockTracer = new MockTracer();
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new OpenTracingMetricsTrackerFactory(mockTracer));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);

      MockSpan span = mockTracer.buildSpan("test_span").start();

      try (Scope ignoredScope = mockTracer.activateSpan(span)) {
         try (HikariDataSource dataSource = new HikariDataSource(config)) {
            try (Connection ignoredConnection = dataSource.getConnection()) {

            }
         }
      }

      span.finish();

      assertLogEvent("hikaricp.testLogsEventsOnSpan.connection-requested", span.logEntries().get(0));
      assertLogEvent("hikaricp.testLogsEventsOnSpan.connection-acquired", span.logEntries().get(1));
      assertLogEvent("hikaricp.testLogsEventsOnSpan.connection-returned", span.logEntries().get(2));
   }

   @Test
   public void testLogConnectionTimeout() throws Exception
   {
      MockTracer mockTracer = new MockTracer();
      HikariConfig config = newHikariConfig();
      config.setMetricsTrackerFactory(new OpenTracingMetricsTrackerFactory(mockTracer));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(250);

      MockSpan span = mockTracer.buildSpan("test_span").start();

      try (HikariDataSource dataSource = new HikariDataSource(config)) {
         try (Connection ignoredConnection1 = dataSource.getConnection()) {
            try (Scope ignoredScope = mockTracer.activateSpan(span)) {
               try (Connection ignoredConnection2 = dataSource.getConnection()) {

               } catch (Exception expected) {

               }
            }
         }
      }

      span.finish();

      assertLogEvent("hikaricp.testLogConnectionTimeout.connection-requested", span.logEntries().get(0));
      assertLogEvent("hikaricp.testLogConnectionTimeout.connection-timeout", span.logEntries().get(1));
   }

   private void assertLogEvent(String expected, LogEntry logEntry)
   {
      assertEquals(expected, logEntry.fields().get("event"));
   }
}
