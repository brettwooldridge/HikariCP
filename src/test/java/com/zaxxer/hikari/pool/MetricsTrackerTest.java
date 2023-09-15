package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.mocks.StubDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;
import java.util.concurrent.TimeUnit;

import static com.zaxxer.hikari.pool.TestElf.newHikariDataSource;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author wvuong@chariotsolutions.com on 2/16/17.
 */
public class MetricsTrackerTest
{
   @Test
   public void connectionTimeoutIsRecorded() throws Exception
   {
      assertThrows(SQLTransientConnectionException.class, () -> {
         int timeoutMillis = 1000;
         int timeToCreateNewConnectionMillis = timeoutMillis * 2;

         StubDataSource stubDataSource = new StubDataSource();
         stubDataSource.setConnectionAcquisitionTime(timeToCreateNewConnectionMillis);

         StubMetricsTracker metricsTracker = new StubMetricsTracker();

         try (HikariDataSource ds = newHikariDataSource()) {
            ds.setMinimumIdle(0);
            ds.setMaximumPoolSize(1);
            ds.setConnectionTimeout(timeoutMillis);
            ds.setDataSource(stubDataSource);
            ds.setMetricsTrackerFactory((poolName, poolStats) -> metricsTracker);

            try (Connection c = ds.getConnection()) {
               fail("Connection shouldn't have been successfully created due to configured connection timeout");

            } finally {
               // assert that connection timeout was measured
               assertThat(metricsTracker.connectionTimeoutRecorded, is(true));
               // assert that measured time to acquire connection should be roughly equal or greater than the configured connection timeout time
               assertTrue(metricsTracker.connectionAcquiredNanos >= TimeUnit.NANOSECONDS.convert(timeoutMillis, TimeUnit.MILLISECONDS));

               metricsTracker.close();
            }
         }
      });
   }

   @SuppressWarnings("unused")
   private static class StubMetricsTracker implements IMetricsTracker
   {
      private Long connectionCreatedMillis;
      private Long connectionAcquiredNanos;
      private Long connectionBorrowedMillis;
      private boolean connectionTimeoutRecorded;

      @Override
      public void recordConnectionCreatedMillis(long connectionCreatedMillis)
      {
         this.connectionCreatedMillis = connectionCreatedMillis;
      }

      @Override
      public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos)
      {
         this.connectionAcquiredNanos = elapsedAcquiredNanos;
      }

      @Override
      public void recordConnectionUsageMillis(long elapsedBorrowedMillis)
      {
         this.connectionBorrowedMillis = elapsedBorrowedMillis;
      }

      @Override
      public void recordConnectionTimeout()
      {
         this.connectionTimeoutRecorded = true;
      }
   }
}
