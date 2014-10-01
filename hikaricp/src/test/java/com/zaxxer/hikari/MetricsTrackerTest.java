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

import org.junit.Test;

import static org.mockito.Mockito.*;

import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.metrics.IMetricsTracker;

public class MetricsTrackerTest {
   @Test
   public void trackOneConnection() throws Exception
   {
      IMetricsTracker.MetricsContext mockContext = mock(IMetricsTracker.MetricsContext.class);
      IMetricsTracker mockTracker = mock(IMetricsTracker.class);
      when(mockTracker.recordConnectionRequest(anyLong())).thenReturn(mockContext);

      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setMetricsTracker(mockTracker);

      HikariDataSource ds = new HikariDataSource(config);
      try {
         Connection conn = ds.getConnection();
         conn.close();
      }
      finally {
         ds.close();
      }

      verify(mockTracker).recordConnectionRequest(anyLong());
      verify(mockContext).stop();
      verify(mockTracker).recordConnectionUsage(anyLong());
   }
}

