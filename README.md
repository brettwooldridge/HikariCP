HikariCP <sub><sub>Ultimate JDBC Connection Pool<sup><sup>&nbsp;&nbsp;[We came, we saw, we kicked its ass](http://youtu.be/-xMGRA_FePw)</sup></sup></sub></sub>
========
[![Build Status](https://travis-ci.org/brettwooldridge/HikariCP.png?branch=master)](https://travis-ci.org/brettwooldridge/HikariCP)

#### TL;DR ####

There is nothing faster.  There is nothing more reliable.  There is nothing more correct.  HikariCP is an
essentially zero-overhead Production-ready connection pool.

Using a stub-JDBC implementation to isolate and measure the overhead of HikariCP, 60+ Million JDBC operations
were performed in 12ms on a commodity PC.  460x faster that the next fastest connection pool.

#### Performance ####
Let's look at some performance numbers.  HikariCP was only compared to BoneCP because, really, DBCP and C3P0 
are old and slow.  We would have run the BoneCP benchmarks but their [methodolgy is flawed]
(https://github.com/brettwooldridge/HikariCP/wiki/Benchmarking) so we wrote our own.

##### MixedBench #####
This is the so called "Mixed" benchmark, and it executes a representative array of JDBC
operations in a realistic mix.  We think *median* is the number to pay attention to, rather
than average (which can get skewed).  *Median* meaning 50% of the iterations were slower, 50% were faster.
500 threads were started, and the underlying connection pool contained 200 connections.  Measurements taken
in *nanoseconds* and converted to *milliseconds*.

| Pool     |  Med (ms) |  Avg (ms) |  Max (ms) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 5533      | 3756      | 8189      |
| HikariCP | 12        | 11        | 32        |

A breakdown of the mix operations is:

| Operation                            | Invocations |
| ------------------------------------ | -----------:|
| DataSource.getConnection()           | 1000        |
| PreparedStatement.prepareStatement() | 200,000     |
| PreparedStatement.setInt()           | 30,000,000  |
| PreparedStatement.addBatch()         | 10,000,000  |
| PreparedStatement.executeBatch()     | 100,000     |
| PreparedStatement.executeQuery()     | 100,000     |
| PreparedStatement.close()            | 200,000     |
| ResultSet.next()                     | 10,000,000  |
| ResultSet.getInt()                   | 10,000,000  |
| ResultSet.close()                    | 100,000     |
| Connection.close()                   | 1000        |

The JVM JIT was "warmed up" with a single run through, then 4 runs were made from which the run
with the lowest median time was chosen.

The benchmark was run using a stub (nop) implementation of an underlying DataSource, Connection,
PreparedStatement, and ResultSet, so the driver was taken completely out of the equation so
that the performance and overhead of the pools themselves could be measured.  Care was taken to
ensure that the JIT does not eliminate or "optimize away" the stub code.

The test was performed on an Intel Core i7 (3770) 3.4GHz iMac, MacOS X 10.8, 32GB RAM.  The
JVM benchmark was run with: ``-server -XX:+UseParallelGC -Xms256m -Xss256k -Dthreads=500 -DpoolMax=200``.
The benchmark is available in the ``src/test/java`` folder in the package ``com.zaxxer.hikari.performance``
in a main class called ``Benchmark``.

##### In Summary #####
500 threads ran 60,702,000 JDBC operations each, HikariCP did this in a median of *12ms* per thread.

------------------------------

#### (In)correctness ####
Sometimes "correctness" is objective, and sometimes it is subjective.  One example of
objective *incorrectness* in BoneCP is ``ResultSet`` handling.  Every connection pool needs to
wrap the underlying ``Connection``, ``Statement``, ``CallableStatement``, and
``PreparedStatement``, and ``ResultSet`` classes.  However, BoneCP does not wrap ResultSet.

``ResultSet`` *must* be wrapped, because ``ResultSet.getStatement()`` *must* return the
**wrapped** ``Statement`` that generated it, not the **underlying** ``Statement``.
Hibernate 4.3 for one [relies on this semantic](http://jira.codehaus.org/browse/BTM-126).

If BoneCP were to wrap ResultSet, which comprises 20,100,000 of the 60,702,000 operations in
MixedBench, its performance numbers would be poorer.  Take note that HikariCP *does* properly wrap
ResultSet and still achives the numbers above.

One example of *subjective* incorrectness is that BoneCP does not test a ``Connection`` immediately
before dispatching it from the pool.  In our opinion, this one "flaw" (or "feature") alone renders BoneCP
unsuitable for Production use.  The number one responsibility of a connection pool is to **not** give 
out possibly bad connections.  Of course there are no guarantees, and the connection could drop in the
few tens of microseconds between the test and its use in your code, but it is much more reliable than
testing once a minute or only when a SQLException has already occurred.

BoneCP may claim that testing a connection on dispatch from the pool negatively impacts performance.
However, not doing so negatively impacts reliability.  Addtionatlly, HikariCP supports the JDBC4 
``Connection.isValid()`` API, which for many drivers provides a fast non-query based aliveness test.
Regardless, it will always test a connection just microseconds before handing it to you.  Add to that
the fact that the ratio of getConnection() calls to other wrapped JDBC calls is extremely small you
you'll find that at an application level there is very little performance impact.

A particularly silly "benchmark" on the BoneCP site starts 500 threads each performing 100 ds.getConnection() /
connection.close() calls with 0ms delay between.  Who does that? The typical "mix" is dozens or hundreds of
JDBC operations between obtaining the connection and closing it (hence the "MixBench") above.  But ok, we can
run this "benchmark" too; times in *Microseconds* and measure the per-thread times across all 500 threads.

| Pool     |  Med (μs) |  Avg (μs) |  Max (μs) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 19467     | 8762      | 30851     |
| HikariCP | 74        | 62        | 112       |

------------------------------------------

#### Knobs ####
Where are all the knobs?  HikariCP has plenty of "knobs" as you can see in the configuration 
section below, but comparatively less than some other pools.  This is a design philosophy.
Configuring a connection pool, even for a large production environment, is not rocket science.

The HikariCP design semantic is minimalist.  You probably need to configure the idle timeout
for connections in the pool, but do you really need to configure how often the pool is swept
to retire them?  You might *think* you do, but if you do you're probably doing something
wrong.

We're not going to (overly) question the design decisions of other pools, but we will say
that some other pools seem to implement a lot of "gimmicks" that proportedly improve
performance.  HikariCP achieves high-performance even in pools beyond realistic deployment
sizes.  Either these "gimmicks" are a case of premature optimization, poor design, or lack
of understanding of how to leaverage what the JVM JIT can do for you to full effect.

##### ***Missing Knobs*** #####
In keeping with the *simple is better* or *less is more* design philosophy, some knobs and 
features are intentionally left out.  Here are two, and the rationale.

**Statement Cache**<br/>
Most major database JDBC drivers already have a PreparedStatement cache that can be
configured (Oracle, MySQL, PostgreSQL, Derby, etc).  A statement cache in the pool would add
unneeded weight and no additional functionality.

JDBC drivers have a special relationship with the remote database in that they are directly
connected and can share internal state that is synchronized with the backend in a way that
an external cache cannot.

It is simply unnecessary with modern database drivers to implement this at the pool level.

**Log Statement Text / Slow Query Logging**<br/>
Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL, and others.  We
consider this a "development-time" feature.  For those few databases that do not support it,
[jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/) is a good option.  It also provides
some nice additional stuff like timing, logging slow queries only, and PreparedStatement bound
parameter logging.   Great stuff during development, and even pre-Production.

Trust us, you don't want this feature -- even disabled -- in a production connection pool.  If
we can figure out how to do it without impacting performance we *might* implement it, but *we
consider even checking a additional boolean as inducing too much overhead into your queries and
results.*

----------------------------------------------------

#### Configuration (Knobs, baby!) ####
The following is the various properties that can be configured in the pool, their behavior,
and their defaults.  **HikariCP uses milliseconds for *all* time values, be careful.**

Rather than coming out of the box with almost nothing configured, HikariCP comes with *sane*
defaults that let a great many deployments run without any additional tweaking (except for
the DataSource and connection URL).

``acquireIncrement``<br/>
This property controls the maximum number of connections that are acquired at one time, with
the exception of pool initialization. *Default: 5*

``acquireRetries``<br/>
This is a per-connection attempt retry count used during new connection creation (acquisition).
If a connection creation attempt fails there will be a wait of ``acquireRetryDelay``
milliseconds followed by another attempt, up to the number of retries configured by this
property. *Default: 3*

``acquireRetryDelay``<br/>
This property controls the number of milliseconds to delay between attempts to acquire a
connection to the database.  If ``acquireRetries`` is 0, this property has no effect.
*Default: 750*

``connectionTestQuery``<br/>
This is for "legacy" databases that do not support the JDBC4 Connection.isValid() API.  This
is the query that will be executed just before a connection is given to you from the pool to
validate that the connection to the database is still alive.  It is database dependent and
should be a query that takes very little processing by the database (eg. "VALUES 1").  **See
the ``jdbc4ConnectionTest`` property for a more efficent alive test.**  One of either this
property or ``jdbc4ConnectionTest`` must be specified.  *Default: none*

``connectionTimeout``<br/>
This property controls the maximum number of milliseconds that a client (that's you) will wait
for a connection from the pool.  If this time is exceeded without a connection becoming
available, an SQLException will be thrown.  *Default: 5000*

``connectionUrl``<br/>
The is the JDBC connection URL string specific to your database. *Default: none*

``dataSourceClassName``<br/>
This is the name of the ``DataSource`` class provided by the JDBC driver.  Consult the
documentation for your specific JDBC driver to get this class name.  Note XA data sources
are not supported.  XA requires a real transaction manager like [bitronix](https://github.com/bitronix/btm).  *Default: none*

``idleTimeout``<br/>
This property controls the maximum amount of time (in milliseconds) that a connection is
allowed to sit idle in the pool.  Whether a connection is retired as idle or not is subject
to a maximum variation of +30 seconds, and average variation of +15 seconds.  A connection
will never be retired as idle *before* this timeout.  A value of 0 means that idle connections
are never removed from the pool.  *Default: 600000 (10 minutes)*

``jdbc4ConnectionTest``<br/>
This property is a boolean value that determines whether the JDBC4 Connection.isValid() method
is used to check that a connection is still alive.  This value is mutually exlusive with the
``connectionTestQuery`` property, and this method of testing connection validity should be
preferred if supported by the JDBC driver.  *Default: true*

``leakDetectionThreshold``<br/>
This property controls the amount of time that a connection can be out of the pool before a
message is logged indicating a possible connection leak.  A value of 0 means leak detection
is disabled.  While the default is 0, and other connection pool implementations state that
leak detection is "not for production" as it imposes a high overhead, at least in the case
of HikariCP the imposed overhead is only 5μs (*microseconds*) split between getConnection()
and close().  Maybe other pools are doing it wrong, but feel free to use leak detection under
HikariCP in production environments if you wish.  *Default: 0*

``maxLifetime``<br/>
This property controls the maximum lifetime of a connection in the pool.  When a connection
reaches this timeout, even if recently used, it will be retired from the pool.  An in-use
connection will never be retired, only when it is idle will it be removed.  We strongly
recommend setting this value, and using something reasonable like 30 minutes or 1 hour.  A
value of 0 indicates no maximum lifetime (infinite lifetime), subject of course to the
``idleTimeout`` setting.  *Default: 1800000 (30 minutes)*

``maximumPoolSize``<br/>
This property controls the maximum size that the pool is allowed to reach, including both
idle and in-use connections.  Basically this value will determine the maximum number of
actual connections to the database backend.  A reasonable value for this is best determined
by your execution environment.  When the pool reaches this size, and no idle connections are
available, calls to getConnection() will block for up to ``connectionTimeout`` milliseconds
before timing out.  *Default: 60*

``minimumPoolSize``<br/>
This property controls the minimum number of connections that HikariCP tries to maintain in
the pool, including both idle and in-use connections.  If the connections dip below this
value, HikariCP will make a best effort to restore them quickly and efficiently.  A reasonable
value for this is best determined by your execution environment.  *Default: 10*

``poolName``<br/>
This property represents a user-defined name for the connection pool and appears mainly
in a JMX management console to identify pools and pool configurations.  *Default: auto-generated*

----------------------------------------------------

#### JMX Management ####
The following properties are configurable in real-time as the pool is running via a JMX
management console such as JConsole:

 * ``acquireIncrement``
 * ``acquireRetries``
 * ``acquireRetryDelay``
 * ``connectionTimeout``
 * ``idleTimeout``
 * ``leakDetectionThreshold``
 * ``maxLifetime``
 * ``minimumPoolSize``
 * ``maximumPoolSize``

#### Requirements ####
 * Java 6 and above
 * Javassist library
 * slf4j library
