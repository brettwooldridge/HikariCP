/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

/**
 *
 */
package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.util.ClockSource.currentTime;
import static com.zaxxer.hikari.util.ClockSource.elapsedMillis;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.MockDataSource;

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
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(1500);
      config.setDataSource(new CustomMockDataSource());

      long start = currentTime();
      try (HikariDataSource ds = new HikariDataSource(config);
            Connection connection = ds.getConnection()) {

            connection.close();

            // Hikari only checks for validity for connections with lastAccess > 1000 ms so we sleep for 1001 ms to force
            // Hikari to do a connection validation which will fail and will trigger the connection to be closed
            quietlySleep(1100L);

            shouldFail = true;

            // on physical connection close we sleep 2 seconds
            try (Connection connection2 = ds.getConnection()) {
               assertTrue("Waited longer than timeout", (elapsedMillis(start) < config.getConnectionTimeout()));
            }
      } catch (SQLException e) {
         assertTrue("getConnection failed because close connection took longer than timeout", (elapsedMillis(start) < config.getConnectionTimeout()));
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
                  SECONDS.sleep(2);
               }
               return null;
            }
         }).when(mockConnection).close();
         return mockConnection;
      }
   }

}
