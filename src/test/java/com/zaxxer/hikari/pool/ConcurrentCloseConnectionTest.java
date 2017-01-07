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

package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class ConcurrentCloseConnectionTest
{
   @Test
   public void testConcurrentClose() throws Exception
   {
	  HikariConfig config = newHikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

	  try (HikariDataSource ds = new HikariDataSource(config);
	      final Connection connection = ds.getConnection()) {

		  ExecutorService executorService = Executors.newFixedThreadPool(10);

		  List<Future<?>> futures = new ArrayList<>();

		  for (int i = 0; i < 500; i++) {
			  final PreparedStatement preparedStatement =
				  connection.prepareStatement("");

			  futures.add(executorService.submit(new Callable<Void>() {

				  @Override
				  public Void call() throws Exception {
					  preparedStatement.close();

					  return null;
				  }

			  }));
		  }

		  executorService.shutdown();

		  for (Future<?> future : futures) {
			  future.get();
		  }
	  }
   }
}
