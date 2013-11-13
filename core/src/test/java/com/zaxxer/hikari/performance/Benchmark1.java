package com.zaxxer.hikari.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import snaq.db.DBPoolDataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
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
public class Benchmark1
{
    private static int THREADS = Integer.getInteger("threads", 100);
    private static int POOL_MAX = Integer.getInteger("poolMax", 100);

    private DataSource ds;

    public static void main(String... args)
    {
        if (args.length < 3)
        {
            System.err.println("Usage: <poolname> <threads> <poolsize>");
            System.err.println("  <poolname>  'hikari' or 'bone'");
            System.exit(0);
        }

        THREADS = Integer.parseInt(args[1]);
        POOL_MAX = Integer.parseInt(args[2]);
        
        Benchmark1 benchmarks = new Benchmark1();
        if (args[0].equals("hikari"))
        {
            benchmarks.ds = benchmarks.setupHikari();
            System.out.printf("Benchmarking HikariCP - %d threads, %d connections", THREADS, POOL_MAX);
        }
        else if (args[0].equals("bone"))
        {
            benchmarks.ds = benchmarks.setupBone();
            System.out.printf("Benchmarking BoneCP - %d threads, %d connections", THREADS, POOL_MAX);
        }
        else if (args[0].equals("dbpool"))
        {
            benchmarks.ds = benchmarks.setupDbPool();
            System.out.printf("Benchmarking DbPool - %d threads, %d connections", THREADS, POOL_MAX);
        }
        else if (args[0].equals("c3p0"))
        {
            benchmarks.ds = benchmarks.setupC3P0();
            System.out.printf("Benchmarking C3P0 - %d threads, %d connections", THREADS, POOL_MAX);
        }
        else
        {
            System.err.println("Start with one of: hikari, bone");
            System.exit(0);
        }

        System.out.println("\nMixedBench");
        System.out.println(" Warming up JIT");
        benchmarks.startMixedBench(100, 10000);
        System.out.println(" MixedBench Final Timing Runs");
        benchmarks.startMixedBench(THREADS, 1000);
        benchmarks.startMixedBench(THREADS, 1000);
        benchmarks.startMixedBench(THREADS, 1000);
        benchmarks.startMixedBench(THREADS, 1000);

        System.out.println("\nBoneBench");
        System.out.println(" Warming up JIT");
        benchmarks.startSillyBench(THREADS);
        System.out.println(" BoneBench Final Timing Run");
        benchmarks.startSillyBench(THREADS);
        benchmarks.startSillyBench(THREADS);
        benchmarks.startSillyBench(THREADS);
        benchmarks.startSillyBench(THREADS);
    }

    private long startMixedBench(int threads, int iter)
    {
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        Measurable[] runners = new Measurable[threads];
        for (int i = 0; i < threads; i++)
        {
            runners[i] = new MixedRunner(barrier, latch, iter);
        }

        return runAndMeasure(runners, latch, "ms");
    }

    private long startSillyBench(int threads)
    {
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        Measurable[] runners = new Measurable[threads];
        for (int i = 0; i < threads; i++)
        {
            runners[i] = new SillyRunner(barrier, latch);
        }

        return runAndMeasure(runners, latch, "ns");
    }

    private long runAndMeasure(Measurable[] runners, CountDownLatch latch, String timeUnit)
    {
        for (int i = 0; i < runners.length; i++)
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
        long[] track = new long[runners.length];
        long max = 0, avg = 0, med = 0;
        long absoluteStart = Long.MAX_VALUE, absoluteFinish = Long.MIN_VALUE;
        for (Measurable runner : runners)
        {
            long elapsed = runner.getElapsed();
            absoluteStart = Math.min(absoluteStart, runner.getStart());
            absoluteFinish = Math.max(absoluteFinish, runner.getFinish());
            track[i++] = elapsed;
            max = Math.max(max, elapsed);
            avg = (avg + elapsed) / 2;
        }

        Arrays.sort(track);
        med = track[runners.length / 2];
        System.out.printf("  max=%d%4$s, avg=%d%4$s, med=%d%4$s\n", max, avg, med, timeUnit);

        return absoluteFinish - absoluteStart;
    }

    private DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setAcquireIncrement(5);
        config.setMinimumPoolSize(POOL_MAX / 2);
        config.setMaximumPoolSize(POOL_MAX);
        config.setConnectionTimeout(8000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setDataSourceClassName("com.zaxxer.hikari.performance.StubDataSource");
        config.setUseInstrumentation(true);

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
        config.setMinConnectionsPerPartition(POOL_MAX / 2);
        config.setMaxConnectionsPerPartition(POOL_MAX);
        config.setConnectionTimeoutInMs(8000);
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

    private DataSource setupDbPool()
    {
        DBPoolDataSource ds = new DBPoolDataSource();
        ds.setDriverClassName("com.zaxxer.hikari.performance.StubDriver");
        ds.setUrl("jdbc:stub");
        ds.setMinPool(POOL_MAX / 2);
        ds.setMaxPool(POOL_MAX);
        ds.setMaxSize(POOL_MAX);
        ds.setIdleTimeout(180);
        ds.setLoginTimeout(8);
        ds.setValidationQuery("VALUES 1");
        
        return ds;
    }

    private DataSource setupC3P0()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass( "com.zaxxer.hikari.performance.StubDriver" );            
            cpds.setJdbcUrl( "jdbc:stub" );
            cpds.setMinPoolSize(POOL_MAX / 2);
            cpds.setMaxPoolSize(POOL_MAX);
            cpds.setCheckoutTimeout(8000);
            cpds.setLoginTimeout(8);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setPreferredTestQuery("VALUES 1");
    
            return cpds;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private class MixedRunner implements Measurable
    {
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private long start;
        private long finish;
        private int counter;
        private final int iter;

        public MixedRunner(CyclicBarrier barrier, CountDownLatch latch, int iter)
        {
            this.barrier = barrier;
            this.latch = latch;
            this.iter = iter;
        }

        public void run()
        {
            try
            {
                barrier.await();

                start = System.nanoTime();
                for (int i = 0; i < iter; i++)
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

        public long getStart()
        {
            return start;
        }

        public long getFinish()
        {
            return finish;
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

        public long getStart()
        {
            return start;
        }

        public long getFinish()
        {
            return finish;
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
        long getStart();
        
        long getFinish();

        long getElapsed();

        int getCounter();
    }
}
