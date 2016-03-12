package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Matthew Tambara (matthew.tambara@liferay.com)
 */
public class ConcurrentCloseConnectionTest
{
   @Test
   public void testConcurrentClose() throws Exception
   {
	  HikariConfig config = new HikariConfig();
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

	  try (HikariDataSource ds = new HikariDataSource(config);
	      final Connection connection = ds.getConnection()) {

		  ExecutorService executorService = Executors.newFixedThreadPool(10);

		  List<Future> futures = new ArrayList<>();

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

		  for (Future future : futures) {
			  future.get();
		  }
	  }
   }
}
