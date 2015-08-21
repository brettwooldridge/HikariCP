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
 * The javax.management MBean for a Hikari pool configuration.
 *
 * @author Brett Wooldridge
 */
public interface HikariConfigMXBean
{
   /**
    * Get the maximum number of milliseconds that a client will wait for a connection from the pool. If this 
    * time is exceeded without a connection becoming available, a SQLException will be thrown from
    * {@link javax.sql.DataSource#getConnection()}.
    *
    * @return the connection timeout in milliseconds
    */
   long getConnectionTimeout();

   /**
    * Set the maximum number of milliseconds that a client will wait for a connection from the pool. If this
    * time is exceeded without a connection becoming available, a SQLException will be thrown from
    * {@link javax.sql.DataSource#getConnection()}.
    *
    * @param connectionTimeoutMs the connection timeout in milliseconds
    */
   void setConnectionTimeout(long connectionTimeoutMs);

   /**
    * Get the maximum number of milliseconds that the pool will wait for a connection to be validated as
    * alive.
    *
    * @return the validation timeout in milliseconds
    */
   long getValidationTimeout();

   /**
    * Sets the maximum number of milliseconds that the pool will wait for a connection to be validated as
    * alive.
    *
    * @param validationTimeoutMs the validation timeout in milliseconds
    */
   void setValidationTimeout(long validationTimeoutMs);

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
   int getMinimumIdle();

   /**
    * The property controls the minimum number of idle connections that HikariCP tries to maintain in the pool,
    * including both idle and in-use connections. If the idle connections dip below this value, HikariCP will
    * make a best effort to restore them quickly and efficiently.
    *
    * @param minIdle the minimum number of idle connections in the pool to maintain
    */
   void setMinimumIdle(int minIdle);

   /**
    * The property controls the maximum number of connections that HikariCP will keep in the pool,
    * including both idle and in-use connections. 
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
    * Set the password used for authentication. Changing this at runtime will apply to new connections only.
    * Altering this at runtime only works for DataSource-based connections, not Driver-class or JDBC URL-based
    * connections.
    *
    * @param password the database password
    */
   void setPassword(String password);

   /**
    * Set the username used for authentication. Changing this at runtime will apply to new connections only.
    * Altering this at runtime only works for DataSource-based connections, not Driver-class or JDBC URL-based
    * connections.
    *
    * @param username the database username
    */
   void setUsername(String username);

  
   /**
    * The name of the connection pool.
    *
    * @return the name of the connection pool
    */
   String getPoolName();
}