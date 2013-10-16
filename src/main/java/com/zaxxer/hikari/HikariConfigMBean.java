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

package com.zaxxer.hikari;

/**
 * The javax.management MBean for a Hikiri pool configuration.
 *
 * @author Brett Wooldridge
 */
public interface HikariConfigMBean
{
    /**
     * This property controls the maximum number of connections that are acquired at one time,
     * with the exception of pool initialization.
     *
     * @return the acquire increment
     */
    int getAcquireIncrement();

    /**
     * This property controls the maximum number of connections that are acquired at one time,
     * with the exception of pool initialization.
     *
     * @param acquireIncrement the acquire increment
     */
    void setAcquireIncrement(int acquireIncrement);

    /**
     * This is a per-connection attempt retry count used during new connection creation (acquisition).
     * If a connection creation attempt fails there will be a wait of {@link #getAcquireRetryDelay} milliseconds
     * followed by another attempt, up to the number of retries configured by this property.
     * 
     * @return the acquire retry count
     */
    int getAcquireRetries();

    /**
     * This is a per-connection attempt retry count used during new connection creation (acquisition).
     * If a connection creation attempt fails there will be a wait of {@link #setAcquireRetryDelay} milliseconds
     * followed by another attempt, up to the number of retries configured by this property.
     *
     * @param acquireRetries the acquire retry count
     */
    void setAcquireRetries(int acquireRetries);

    /**
     * This property controls the number of milliseconds to delay between attempts to acquire a connection
     * to the database. If acquireRetries is 0, this property has no effect.
     *
     * @return the acquire retry delay in milliseconds
     */
    long getAcquireRetryDelay();

    /**
     * This property controls the number of milliseconds to delay between attempts to acquire a connection
     * to the database. If acquireRetries is 0, this property has no effect.
     * 
     * @param acquireRetryDelayMs the acquire retry delay in milliseconds
     */
    void setAcquireRetryDelay(long acquireRetryDelayMs);

    /**
     * This is for "legacy" databases that do not support the JDBC4 {@code Connection.isValid()} API. This is the 
     * query that will be executed just before a connection is given to you from the pool to validate that
     * the connection to the database is still alive. It is database dependent and should be a query that
     * takes very little processing by the database (eg. "VALUES 1"). See the {code getJdbc4ConnectionTest()} property
     * for a more efficent alive test. One of either this property or jdbc4ConnectionTest must be specified.
     *
     * @return the connection timeout in milliseconds
     */
    long getConnectionTimeout();

    /**
     * This is for "legacy" databases that do not support the JDBC4 {code Connection.isValid()} API. This is the 
     * query that will be executed just before a connection is given to you from the pool to validate that
     * the connection to the database is still alive. It is database dependent and should be a query that
     * takes very little processing by the database (eg. "VALUES 1"). See the {@code setJdbc4ConnectionTest()} property
     * for a more efficent alive test. One of either this property or jdbc4ConnectionTest must be specified.

     * @param connectionTimeoutMs the connection timeout in milliseconds
     */
    void setConnectionTimeout(long connectionTimeoutMs);

    /**
     * This property controls the maximum amount of time (in milliseconds) that a connection is allowed to sit 
     * idle in the pool. Whether a connection is retired as idle or not is subject to a maximum variation of +30
     * seconds, and average variation of +15 seconds. A connection will never be retired as idle before this timeout.
     * A value of 0 means that idle connections are never removed from the pool.
     *
     * @return the idle timeout in milliseconds
     */
    long getIdleTimeout();

    /**
     * This property controls the maximum amount of time (in milliseconds) that a connection is allowed to sit 
     * idle in the pool. Whether a connection is retired as idle or not is subject to a maximum variation of +30
     * seconds, and average variation of +15 seconds. A connection will never be retired as idle before this timeout.
     * A value of 0 means that idle connections are never removed from the pool.
     *
     * @param idleTimeoutMs the idle timeout in milliseconds
     */
    void setIdleTimeout(long idleTimeoutMs);

    /**
     * This property controls the amount of time that a connection can be out of the pool before a message is
     * logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
     *
     * @return the connection leak detection threshold in milliseconds
     */
    long getLeakDetectionThreshold();

    /**
     * This property controls the amount of time that a connection can be out of the pool before a message is
     * logged indicating a possible connection leak. A value of 0 means leak detection is disabled.
     *
     * @param leakDetectionThresholdMs the connection leak detection threshold in milliseconds
     */
    void setLeakDetectionThreshold(long leakDetectionThresholdMs);

    /**
     * This property controls the maximum lifetime of a connection in the pool. When a connection reaches this
     * timeout, even if recently used, it will be retired from the pool. An in-use connection will never be
     * retired, only when it is idle will it be removed.
     *
     * @return the maximum connection lifetime in milliseconds
     */
    long getMaxLifetime();

    /**
     * This property controls the maximum lifetime of a connection in the pool. When a connection reaches this
     * timeout, even if recently used, it will be retired from the pool. An in-use connection will never be
     * retired, only when it is idle will it be removed.
     *
     * @param maxLifetimeMs the maximum connection lifetime in milliseconds
     */
    void setMaxLifetime(long maxLifetimeMs);

    /**
     * The property controls the maximum size that the pool is allowed to reach, including both idle and in-use
     * connections. Basically this value will determine the maximum number of actual connections to the database
     * backend.
     * <p>
     * When the pool reaches this size, and no idle connections are available, calls to getConnection() will
     * block for up to connectionTimeout milliseconds before timing out.
     *
     * @return the minimum number of connections in the pool
     */
    int getMinimumPoolSize();

    /**
     * The property controls the minimum number of connections that HikariCP tries to maintain in the pool,
     * including both idle and in-use connections. If the connections dip below this value, HikariCP will
     * make a best effort to restore them quickly and efficiently.
     *
     * @param minPoolSize the minimum number of connections in the pool
     */
    void setMinimumPoolSize(int minPoolSize);

    /**
     * The property controls the minimum number of connections that HikariCP tries to maintain in the pool,
     * including both idle and in-use connections. If the connections dip below this value, HikariCP will
     * make a best effort to restore them quickly and efficiently.
     *
     * @return the maximum number of connections in the pool
     */
    int getMaximumPoolSize();

    /**
     * The property controls the maximum size that the pool is allowed to reach, including both idle and in-use
     * connections. Basically this value will determine the maximum number of actual connections to the database
     * backend.
     * <p>
     * When the pool reaches this size, and no idle connections are available, calls to getConnection() will
     * block for up to connectionTimeout milliseconds before timing out.
     *
     * @param maxPoolSize the maximum number of connections in the pool
     */
    void setMaximumPoolSize(int maxPoolSize);

    /**
     * The name of the connection pool.
     *
     * @return the name of the connection pool
     */
    String getPoolName();
}