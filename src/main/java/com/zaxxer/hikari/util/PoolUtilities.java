package com.zaxxer.hikari.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class PoolUtilities
{
    public static void quietlyCloseConnection(Connection connection)
    {
        try
        {
            connection.close();
        }
        catch (SQLException e)
        {
            return;
        }
    }

    /**
     * Execute the user-specified init SQL.
     *
     * @param connection the connection to initialize
     * @throws SQLException throws if the init SQL execution fails
     */
    public static void executeSqlAutoCommit(Connection connection, String sql) throws SQLException
    {
        if (sql != null)
        {
            connection.setAutoCommit(true);
            Statement statement = connection.createStatement();
            try
            {
                statement.execute(sql);
            }
            finally
            {
                statement.close();
            }
        }
    }

    public static void quietlySleep(int millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static ThreadPoolExecutor createThreadPoolExecutor(final int queueSize, final String threadName)
    {
        ThreadFactory threadFactory = new ThreadFactory() {
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r, threadName);
                t.setDaemon(true);
                return t;
            }
        };

        int processors = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueSize);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(processors, processors, 10, TimeUnit.SECONDS, queue, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
