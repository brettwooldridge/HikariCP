package com.zaxxer.hikari.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import javax.sql.DataSource;

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
        }

        benchmarks.start();
    }

    private DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumPoolSize(1);
        config.setMaximumPoolSize(100);
        config.setConnectionTimeoutMs(5000);
        config.setConnectionTestQuery("VALUES 1");
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.MockDataSource");
        config.setProxyFactoryClassName(System.getProperty("testProxy", "auto"));
        config.setProxyFactoryClassName(System.getProperty("testProxy", "com.zaxxer.hikari.CglibProxyFactory"));

        HikariDataSource ds = new HikariDataSource();
        ds.setConfiguration(config);
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
        }
        long avg = min + ((max - min) / 2);
        System.out.printf("min=%d, max=%d, avg=%d\n", min, max, avg);
    }

    private class Runner implements Runnable
    {
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private long start;
        private long finish;

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
                for (int i = 0; i < 10; i++)
                {
                    Connection connection = ds.getConnection();
                    for (int j = 0; j < 20; j++)
                    {
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM test WHERE foo=?");
                        for (int k = 0; k < 30; k++)
                        {
                            statement.setInt(1, i);
                            ResultSet resultSet = statement.executeQuery();
                            resultSet.next();
                            resultSet.close();
                        }
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
    }
}
