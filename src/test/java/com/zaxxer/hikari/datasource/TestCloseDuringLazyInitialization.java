package com.zaxxer.hikari.datasource;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;

public class TestCloseDuringLazyInitialization {

    @Test
    public void testCloseDuringLazyInitialization() throws Exception {
        HikariDataSource ds = new HikariDataSource();
        ds.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<Void> getConnection = () -> {
            try {
                ds.getConnection();
            } catch (SQLException e) {
                // might be closed already
            }
            return null;
        };

        Callable<Void> close = () -> {
            ds.close();
            return null;
        };

        List<Future<Void>> futures = executorService.invokeAll(Arrays.asList(getConnection, close));
        executorService.shutdown();

        for (Future<?> future : futures) {
            future.get();
        }

        assertEquals("HikariDataSource was not closed", true, ds.isClosed());
        assertEquals("HikariPool was not shutdown", false, ds.isRunning());
    }

}
