package com.zaxxer.hikari.metrics.micrometer;

import com.zaxxer.hikari.metrics.PoolStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MicrometerMetricsTrackerTest {

   private MeterRegistry mockMeterRegistry = new SimpleMeterRegistry();

   private MicrometerMetricsTracker testee;

   @Before
   public void setup(){
      testee = new MicrometerMetricsTracker("mypool", new PoolStats(1000L) {
         @Override
         protected void update() {
            // nothing
         }
      }, mockMeterRegistry);
   }

   @Test
   public void close() throws Exception {
      Assert.assertNotNull(mockMeterRegistry.find("Wait"));
      Assert.assertNotNull(mockMeterRegistry.find("Usage"));
      Assert.assertNotNull(mockMeterRegistry.find("ConnectionCreation"));
      Assert.assertNotNull(mockMeterRegistry.find("ConnectionTimeoutRate"));
      Assert.assertNotNull(mockMeterRegistry.find("TotalConnections"));
      Assert.assertNotNull(mockMeterRegistry.find("IdleConnections"));
      Assert.assertNotNull(mockMeterRegistry.find("ActiveConnections"));
      Assert.assertNotNull(mockMeterRegistry.find("PendingConnections"));

      testee.close();
   }
}
