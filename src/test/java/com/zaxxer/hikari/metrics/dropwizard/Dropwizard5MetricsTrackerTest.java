package com.zaxxer.hikari.metrics.dropwizard;

import com.zaxxer.hikari.mocks.StubPoolStats;
import io.dropwizard.metrics5.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class Dropwizard5MetricsTrackerTest
{
   @Mock
   public MetricRegistry mockMetricRegistry;

   private Dropwizard5MetricsTracker testee;

   @Before
   public void setup()
   {
      testee = new Dropwizard5MetricsTracker("mypool", new StubPoolStats(0), mockMetricRegistry);
   }

   @Test
   public void close()
   {
      testee.close();

      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.Wait"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.Usage"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.ConnectionCreation"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.ConnectionTimeoutRate"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.TotalConnections"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.IdleConnections"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.ActiveConnections"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.PendingConnections"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.MaxConnections"));
      verify(mockMetricRegistry).remove(MetricRegistry.name("mypool.pool.MinConnections"));
   }
}
