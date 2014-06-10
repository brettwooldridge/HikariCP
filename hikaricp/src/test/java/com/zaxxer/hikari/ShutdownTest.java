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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.PoolUtilities;

/**
 * @author Brett Wooldridge
 */
public class ShutdownTest
{
    @Before
    public void beforeTest()
    {
        StubConnection.count.set(0);
    }

    @After
    public void afterTest()
    {
        StubConnection.slowCreate = false;
    }

    @Test
    public void testShutdown1() throws SQLException
    {
        Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

        StubConnection.slowCreate = true;

        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(10);
        config.setInitializationFailFast(true);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        final HikariDataSource ds = new HikariDataSource(config);
        HikariPool pool = TestElf.getPool(ds);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++)
        {
            threads[i] = new Thread() {
                public void run() {
                    try
                    {
                        if (ds.getConnection() != null)
                        {
                            PoolUtilities.quietlySleep(TimeUnit.SECONDS.toMillis(1));
                        }
                    }
                    catch (SQLException e)
                    {
                    }
                }
            };
            threads[i].setDaemon(true);
            threads[i].start();
        }

        PoolUtilities.quietlySleep(300);

        Assert.assertTrue("Totals connection count not as expected, ", pool.getTotalConnections() > 0);
        
        ds.shutdown();

        Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
        Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
        Assert.assertSame("Total connection count not as expected", 0, pool.getTotalConnections());
    }

    @Test
    public void testShutdown2() throws SQLException
    {
        Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

        StubConnection.slowCreate = true;

        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(10);
        config.setInitializationFailFast(false);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        HikariPool pool = TestElf.getPool(ds);

        PoolUtilities.quietlySleep(300);

        Assert.assertTrue("Totals connection count not as expected, ", pool.getTotalConnections() > 0);
        
        ds.shutdown();

        Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
        Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
        Assert.assertSame("Total connection count not as expected", 0, pool.getTotalConnections());
    }

    @Test
    public void testShutdown3() throws SQLException
    {
        Assert.assertSame("StubConnection count not as expected", 0, StubConnection.count.get());

        StubConnection.slowCreate = true;

        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(5);
        config.setInitializationFailFast(true);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);
        HikariPool pool = TestElf.getPool(ds);

        PoolUtilities.quietlySleep(300);

        Assert.assertTrue("Totals connection count not as expected, ", pool.getTotalConnections() == 5);
        
        ds.shutdown();

        Assert.assertSame("Active connection count not as expected, ", 0, pool.getActiveConnections());
        Assert.assertSame("Idle connection count not as expected, ", 0, pool.getIdleConnections());
        Assert.assertSame("Total connection count not as expected", 0, pool.getTotalConnections());
    }
    
    @Test
    public void testShutdown4() throws SQLException
    {
        StubConnection.slowCreate = true;
        
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(10);
        config.setInitializationFailFast(false);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

        HikariDataSource ds = new HikariDataSource(config);

        PoolUtilities.quietlySleep(300);

        ds.shutdown();

        long start = System.currentTimeMillis();
        while (PoolUtilities.elapsedTimeMs(start) < TimeUnit.SECONDS.toMillis(5) && threadCount() > 0)
        {
            PoolUtilities.quietlySleep(250);
        }

        Assert.assertSame("Thread was leaked", 0, threadCount());
    }

	private int threadCount()
	{
	    Thread[] threads = new Thread[Thread.activeCount() * 2];
	    Thread.enumerate(threads);

	    int count = 0;
	    for (Thread thread : threads)
	    {
	        count += (thread != null && thread.getName().startsWith("Hikari")) ? 1 : 0;
	    }

	    return count;
	}
}
