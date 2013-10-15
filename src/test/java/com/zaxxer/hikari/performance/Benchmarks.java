package com.zaxxer.hikari.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
 * This test requires the Javassist library to be present in the classpath.
 * To reproduce our results, it should be run as follows:
 * 
 *    JVM parameters: -server -XX:+UseParallelGC -Xss256k -Dthreads=200
 *
 * @author Brett Wooldridge
 */
public class Benchmarks
{
    private static final int THREADS = Integer.getInteger("threads", 100);
    private static final int POOL_MAX = Integer.getInteger("poolMax", 100);

    private DataSource ds;

    public static void main(String... args)
    {
        if (args.length == 0)
        {
            System.err.println("Start with one of: hikari, bone, c3p0, dbcp");
            System.exit(0);
        }

        Benchmarks benchmarks = new Benchmarks();
        if (args[0].equals("hikari"))
        {
            benchmarks.ds = benchmarks.setupHikari();
            System.out.println("Benchmarking HikariCP");
        }
        else if (args[0].equals("bone"))
        {
            benchmarks.ds = benchmarks.setupBone();
            System.out.println("Benchmarking BoneCP");
        }

        System.out.println("\nMixedBench");
        System.out.println(" Warming up JIT");
        benchmarks.startMixedBench();
        System.out.println(" MixedBench Final Timing Runs");
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();
        benchmarks.startMixedBench();

        System.out.println("\nBoneBench");
        System.out.println(" Warming up JIT");
        benchmarks.startSillyBench();
        System.out.println(" BoneBench Final Timing Run");
        benchmarks.startSillyBench();
        benchmarks.startSillyBench();
        benchmarks.startSillyBench();
        benchmarks.startSillyBench();
    }

    private DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setAcquireIncrement(5);
        config.setMinimumPoolSize(20);
        config.setMaximumPoolSize(POOL_MAX);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setDataSourceClassName("com.zaxxer.hikari.performance.StubDataSource");
        config.setProxyFactoryType(System.getProperty("testProxy", "javassist"));

        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }

    private DataSource setupBone()
    {
        try
        {
            Class.forName("com.zaxxer.hikari.performance.StubDriver");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        BoneCPConfig config = new BoneCPConfig();
        config.setAcquireIncrement(5);
        config.setMinConnectionsPerPartition(20);
        config.setMaxConnectionsPerPartition(POOL_MAX);
        config.setConnectionTimeoutInMs(5000);
        config.setIdleMaxAgeInMinutes(30);
        config.setConnectionTestStatement("VALUES 1");
        config.setCloseOpenStatements(true);
        config.setDisableConnectionTracking(true);
        config.setJdbcUrl("jdbc:stub");
        config.setUsername("nobody");
        config.setPassword("nopass");

        BoneCPDataSource ds = new BoneCPDataSource(config);
        return ds;
    }

    private void startMixedBench()
    {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);

        Measurable[] runners = new Measurable[THREADS];
        for (int i = 0; i < THREADS; i++)
        {
            runners[i] = new MixedRunner(barrier, latch);
        }

        runAndMeasure(runners, latch, "ms");
    }

    private void startSillyBench()
    {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);

        Measurable[] runners = new Measurable[THREADS];
        for (int i = 0; i < THREADS; i++)
        {
            runners[i] = new SillyRunner(barrier, latch);
        }

        runAndMeasure(runners, latch, "ns");
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

    private class MixedRunner implements Measurable
    {
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private long start;
        private long finish;
        private int counter;

        public MixedRunner(CyclicBarrier barrier, CountDownLatch latch)
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
                        for (int k = 0; k < 100; k++)
                        {
                            statement.setInt(1, i);
                            statement.setInt(1, j);
                            statement.setInt(1, k);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                        statement.close();

                        statement = connection.prepareStatement("SELECT * FROM test WHERE foo=?");
                        ResultSet resultSet = statement.executeQuery();
                        for (int k = 0; k < 100; k++)
                        {
                            resultSet.next();
                            counter += resultSet.getInt(1); // ensures the JIT doesn't optimize this loop away
                        }
                        resultSet.close();
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

    private class SillyRunner implements Measurable
    {
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private long start;
        private long finish;

        public SillyRunner(CyclicBarrier barrier, CountDownLatch latch)
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
                for (int i = 0; i < 100; i++)
                {
                    Connection connection = ds.getConnection();
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
            return finish - start;
        }

        public int getCounter()
        {
            return 0;
        }
    }

    private interface Measurable extends Runnable
    {
        long getElapsed();

        int getCounter();
    }
}
