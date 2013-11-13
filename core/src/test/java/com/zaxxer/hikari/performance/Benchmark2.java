/*
 * Copyright (C) 2013 Brett Wooldridge
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

package com.zaxxer.hikari.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 *
 * @author Brett Wooldridge
 */
public class Benchmark2
{
    private static final int THREADS = Integer.getInteger("threads", 100);
    private static final int POOL_MAX = Integer.getInteger("poolMax", 100);

    private DataSource ds;

    public static void main(String[] args)
    {
        if (args.length == 0)
        {
            System.err.println("Start with one of: hikari, bone");
            System.exit(0);
        }

        Benchmark2 benchmarks = new Benchmark2();
        if (args[0].equals("hikari"))
        {
            benchmarks.ds = benchmarks.setupHikari();
            System.out.printf("Benchmarking HikariCP - %d threads, %d pool\n", THREADS, POOL_MAX);
        }
        else if (args[0].equals("bone"))
        {
            benchmarks.ds = benchmarks.setupBone();
            System.out.printf("Benchmarking BoneCP - %d threads, %d pool\n", THREADS, POOL_MAX);
        }
        else
        {
            System.err.println("Start with one of: hikari, bone");
            System.exit(0);
        }
        
        System.out.println("\nMixedBench");
        System.out.println(" Warming up JIT");
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        System.out.println(" MixedBench Final Timing Runs");
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();

    }

    private DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setAcquireIncrement(5);
        config.setMinimumPoolSize(POOL_MAX / 2);
        config.setMaximumPoolSize(POOL_MAX);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setAutoCommit(true);

        config.setDataSourceClassName("org.hsqldb.jdbc.JDBCDataSource");
        config.addDataSourceProperty("url", "jdbc:hsqldb:mem:test");
        config.addDataSourceProperty("user", "SA");
        config.addDataSourceProperty("password", "");

        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }
    
    private DataSource setupBone()
    {
        try
        {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        BoneCPConfig config = new BoneCPConfig();
        config.setAcquireIncrement(5);
        config.setMinConnectionsPerPartition(POOL_MAX / 2);
        config.setMaxConnectionsPerPartition(POOL_MAX);
        config.setConnectionTimeoutInMs(30000);
        config.setIdleMaxAgeInMinutes(30);
        config.setConnectionTestStatement("SELECT 1");
        config.setCloseOpenStatements(true);
        config.setDefaultAutoCommit(true);
        config.setDisableConnectionTracking(true);

        config.setJdbcUrl("jdbc:hsqldb:mem:test");
        config.setUsername("SA");
        config.setPassword("");

        BoneCPDataSource ds = new BoneCPDataSource(config);
        return ds;
    }

    private void startMixedBench()
    {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);

        setupSchema(true);

        Measurable[] runners = new Measurable[THREADS];
        for (int i = 0; i < THREADS; i++)
        {
            runners[i] = new SillyRunner2(barrier, latch);
        }

        runAndMeasure(runners, latch, "ms");

        setupSchema(false);
    }

    private void setupSchema(boolean create)
    {
        Connection connection = null;
        try
        {
            connection = ds.getConnection();
            Statement statement = connection.createStatement();
            if (create)
            {
                statement.execute("CREATE TABLE test ( column INTEGER )");
                statement.execute("CREATE INDEX test_ndx ON test(column)");
            }
            else
            {
                statement.execute("DROP TABLE test");
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Failed to create test schema", e);
        }
        finally
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException e)
                {
                }
            }
        }
    }

    private void runAndMeasure(Measurable[] runners, CountDownLatch latch, String timeUnit)
    {
        for (int i = 0; i < THREADS; i++)
        {
            Thread t = new Thread(runners[i]);
            t.start();
        }

        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        int i = 0;
        long[] track = new long[THREADS];
        long max = 0, avg = 0, med = 0;
        for (Measurable runner : runners)
        {
            long elapsed = runner.getElapsed();
            track[i++] = elapsed;
            max = Math.max(max, elapsed);
            avg = (avg + elapsed) / 2;
        }

        Arrays.sort(track);
        med = track[THREADS / 2];
        System.out.printf("  max=%d%4$s, avg=%d%4$s, med=%d%4$s\n", max, avg, med, timeUnit);
    }

    private class SillyRunner2 implements Measurable
    {
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private long start;
        private long finish;
        private int counter;

        public SillyRunner2(CyclicBarrier barrier, CountDownLatch latch)
        {
            this.barrier = barrier;
            this.latch = latch;
        }

        public void run()
        {
            try
            {
                barrier.await();

                start = System.nanoTime();
                for (int i = 0; i < 1000; i++)
                {
                    Connection connection = ds.getConnection();
                    for (int j = 0; j < 100; j++)
                    {
                        PreparedStatement statement = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
                        statement.close();
                    }
                    connection.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                finish = System.nanoTime();
                latch.countDown();
            }
        }

        public long getElapsed()
        {
            return TimeUnit.NANOSECONDS.toMillis(finish - start);
        }

        public int getCounter()
        {
            return counter;
        }
    }

    private interface Measurable extends Runnable
    {
        long getElapsed();

        int getCounter();
    }
}
