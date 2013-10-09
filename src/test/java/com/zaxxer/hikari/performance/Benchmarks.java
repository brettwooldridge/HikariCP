package com.zaxxer.hikari.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Benchmarks
{
    private static final int THREADS = Integer.getInteger("threads", 100);

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
            System.out.println("Testing HikariCP");
        }
        else if (args[0].equals("bone"))
        {
            benchmarks.ds = benchmarks.setupBone();
            System.out.println("Testing BoneCP");
        }

        System.out.println("  Warming up JIT");
        benchmarks.start();
        benchmarks.start();
        System.out.println("\n  Final Timing Run");
        benchmarks.start();
    }

    private DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(20);
        config.setMaximumPoolSize(50);
        config.setConnectionTimeoutMs(5000);
        config.setJdbc4ConnectionTest(true);
        config.setDataSourceClassName("com.zaxxer.hikari.performance.StubDataSource");
        config.setProxyFactoryType(System.getProperty("testProxy", "javassist"));

        HikariDataSource ds = new HikariDataSource();
        ds.setConfiguration(config);
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
        config.setMinConnectionsPerPartition(20);
        config.setMaxConnectionsPerPartition(50);
        config.setConnectionTimeoutInMs(5000);
        config.setConnectionTestStatement("VALUES 1");
        config.setCloseOpenStatements(true);
        config.setDisableConnectionTracking(true);
        config.setJdbcUrl("jdbc:stub");
        config.setUsername("nobody");
        config.setPassword("nopass");

        BoneCPDataSource ds = new BoneCPDataSource(config);
        return ds;
    }

    private void start()
    {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);

        Runner[] runners = new Runner[THREADS];
        for (int i = 0; i < THREADS; i++)
        {
            runners[i] = new Runner(barrier, latch);
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

        long min = Integer.MAX_VALUE, max = 0;
        for (Runner runner : runners)
        {
            long elapsed = runner.getElapsed();
            min = Math.min(min, elapsed);
            max = Math.max(max, elapsed);
            if (runner.getCounter() != 505000000)
            {
                System.err.println("Incorrect counter value from runner: " + runner.getCounter());
            }
        }
        long avg = min + ((max - min) / 2);
        System.out.printf("  min=%d, max=%d, avg=%d\n", min, max, avg);
    }

    private class Runner implements Runnable
    {
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private long start;
        private long finish;
        private int counter;

        public Runner(CyclicBarrier barrier, CountDownLatch latch)
        {
            this.barrier = barrier;
            this.latch = latch;
        }

        public void run()
        {
            try
            {
                barrier.await();

                start = System.currentTimeMillis();
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
                        }
                        statement.close();

                        statement = connection.prepareStatement("SELECT * FROM test WHERE foo=?");
                        ResultSet resultSet = statement.executeQuery();
                        for (int k = 0; k < 100; k++)
                        {
                            resultSet.next();
                            counter += resultSet.getInt(1);
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
                finish = System.currentTimeMillis();
                latch.countDown();
            }
        }

        public long getElapsed()
        {
            return finish - start;
        }

        public long getCounter()
        {
            return counter;
        }
    }
}
