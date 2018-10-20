package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static com.zaxxer.hikari.pool.TestElf.newHikariDataSource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Tests for {@link HikariDataSource#isRunning()}.
 */
public class TestIsRunning
{
    @Test
    public void testRunningNormally()
    {
        try (HikariDataSource ds = new HikariDataSource(basicConfig()))
        {
            assertTrue(ds.isRunning());
        }
    }


    @Test
    public void testNoPool()
    {
        try (HikariDataSource ds = newHikariDataSource())
        {
            assertNull("Pool should not be initialized.", getPool(ds));
            assertFalse(ds.isRunning());
        }
    }


    @Test
    public void testSuspendAndResume()
    {
        try (HikariDataSource ds = new HikariDataSource(basicConfig()))
        {
            ds.getHikariPoolMXBean().suspendPool();
            assertFalse(ds.isRunning());

            ds.getHikariPoolMXBean().resumePool();
            assertTrue(ds.isRunning());
        }
    }


    @Test
    public void testShutdown()
    {
        try (HikariDataSource ds = new HikariDataSource(basicConfig()))
        {
            ds.close();
            assertFalse(ds.isRunning());
        }
    }


    private HikariConfig basicConfig()
    {
        HikariConfig config = newHikariConfig();
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("VALUES 1");
        config.setConnectionInitSql("SELECT 1");
        config.setReadOnly(true);
        config.setConnectionTimeout(2500);
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(30));
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
        config.setAllowPoolSuspension(true);

        return config;
    }
}
