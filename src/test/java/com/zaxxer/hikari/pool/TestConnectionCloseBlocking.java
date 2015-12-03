/**
 *
 */
package com.zaxxer.hikari.pool;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.MockDataSource;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.UtilityElf;

/**
 * Test for cases when db network connectivity goes down and close is called on existing connections. By default Hikari
 * blocks longer than getMaximumTimeout (it can hang for a lot of time depending on driver timeout settings). Closing
 * async the connections fixes this issue.
 *
 */
public class TestConnectionCloseBlocking {
   private static volatile boolean shouldFail = false;

   // @Test
   public void testConnectionCloseBlocking() throws SQLException {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(1500);
      config.setDataSource(new CustomMockDataSource());

      long start = ClockSource.INSTANCE.currentTime();
      try (HikariDataSource ds = new HikariDataSource(config)) {
         Connection connection = ds.getConnection();
         connection.close();

         // Hikari only checks for validity for connections with lastAccess > 1000 ms so we sleep for 1001 ms to force
         // Hikari to do a connection validation which will fail and will trigger the connection to be closed
         UtilityElf.quietlySleep(1100L);

         shouldFail = true;

         // on physical connection close we sleep 2 seconds
         connection = ds.getConnection();

         Assert.assertTrue("Waited longer than timeout", (ClockSource.INSTANCE.elapsedMillis(start) < config.getConnectionTimeout()));
      } catch (SQLException e) {
         Assert.assertTrue("getConnection failed because close connection took longer than timeout", (ClockSource.INSTANCE.elapsedMillis(start) < config.getConnectionTimeout()));
      }
   }

   private static class CustomMockDataSource extends MockDataSource {
      @Override
      public Connection getConnection() throws SQLException {
         Connection mockConnection = super.getConnection();
         when(mockConnection.isValid(anyInt())).thenReturn(!shouldFail);
         doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
               if (shouldFail) {
                  TimeUnit.SECONDS.sleep(2);
               }
               return null;
            }
         }).when(mockConnection).close();
         return mockConnection;
      }
   }

}
