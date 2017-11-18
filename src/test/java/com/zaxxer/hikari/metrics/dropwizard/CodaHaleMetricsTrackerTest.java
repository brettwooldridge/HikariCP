package com.zaxxer.hikari.metrics.dropwizard;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;

@RunWith(MockitoJUnitRunner.class)
public class CodaHaleMetricsTrackerTest {

   @Mock
   public MetricRegistry mockMetricRegistry;

   private CodaHaleMetricsTracker testee;

   @Before
   public void setup(){
      testee = new CodaHaleMetricsTracker("mypool", null, mockMetricRegistry);
   }

   @Test
   public void close() throws Exception {
      testee.close();

      verify(mockMetricRegistry).remove("mypool.pool.Wait");
      verify(mockMetricRegistry).remove("mypool.pool.Usage");
      verify(mockMetricRegistry).remove("mypool.pool.ConnectionCreation");
      verify(mockMetricRegistry).remove("mypool.pool.ConnectionTimeoutRate");
      verify(mockMetricRegistry).remove("mypool.pool.TotalConnections");
      verify(mockMetricRegistry).remove("mypool.pool.IdleConnections");
      verify(mockMetricRegistry).remove("mypool.pool.ActiveConnections");
      verify(mockMetricRegistry).remove("mypool.pool.PendingConnections");
   }
}
