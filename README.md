HikariCP <sub><sub>Ultimate JDBC Connection Pool<sup><sup>&nbsp;&nbsp;[We came, we saw, we kicked its ass](http://youtu.be/-xMGRA_FePw)</sup></sup></sub></sub>
========

#### TL;DR ####

There is nothing faster.  There is nothing more reliable.  There is nothing more correct.

HikariCP is an essentially zero-overhead Production-ready connection pool.  Using a stub-JDBC implementation
to isolate and measure the overhead of HikariCP, 60+ Million JDBC operations were performed in 25ms on a
commodity PC.  Almost 40x faster that the next fastest connection pool.

#### Performance ####
Let's look at some performance numbers.  HikariCP was only compared to BoneCP because, really, DBCP and C3P0 
are old and slow.

##### MixedBench #####
This is the so called "Mixed" benchmark, and it executes a representative array of JDBC
operations in a realistic mix.  We think *median* is the number to pay attention to, rather
than average (which can get skewed).  *Median* meaning 50% of the iterations were slower, %50 were faster.
200 threads were started, and the underlying connection pool contained 100 connections.

| Pool     |  Med (ms) |  Avg (ms) |  Max (ms) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 976       | 909       | 1463      |
| HikariCP | 25        | 22        | 90       |

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
with the lowest median time was chosen.  The run with the lowest *median* was also the run
with the lowest *average* -- so don't think we're skewing the results.

The benchmark was run using a stub (nop) implementation of an underlying DataSource, Connection,
PreparedStatement, and ResultSet, so the driver was taken completely out of the equation so
that the performance and overhead of the pools themselves could be measured.

The test was performed on an Intel Core i7 (2600) 3.4GHz iMac with 24GB of RAM.  The
JVM benchmark was run with: ``-server -XX:+UseParallelGC -Xss256k -Dthreads=200 -DpoolMax=100``.

##### In Summary #####
200 threads ran 60,702,000 JDBC operations each, HikariCP did this in a median of *25ms* per thread.

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
you'll find that at an application level there is very little (if any) performance difference.

A particularly silly "benchmark" on the BoneCP site starts 500 threads each performing 100
DataSource.getConnection() / connection.close() calls with 0ms delay between.  Who does that?
The typical "mix" is dozens or hundreds of JDBC operations between obtaining the connection and
closing it (hence the "MixBench") above.  But ok, we can run this "benchmark" too (times in
Microseconds):

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
performance.  HikariCP achieves high-performance even in pools beyond unrealistic deployment
sizes.  Either these "gimmicks" are a case of premature optimization or reflective of a poor
design/lack of understanding of how to leaverage the JVM JIT to full effect.

##### ***Missing Knobs*** #####
In keeping with the *simple is better* or *less is more* design philosophy, some knobs and 
features are intentionally left out.  Here are two, and the rationale.

**Statement Cache**<br/>
Most major database JDBC drivers already have a PreparedStatement cache that can be
configured (Oracle, MySQL, PostgreSQL, Derby, etc).  A statement cache in the pool would add
unneeded weight, no additional functionality, and possibly incorrect behavior...

JDBC drivers have a special relationship with the remote database in that they are directly
connected and can share internal state that is synchronized with the backend.  **It is
inherently unsafe to cache PreparedStatements outside of the driver.**  Why?  Again, drivers
have the advantage of deep knowledge of the the database for which they are designed.

Take for example DDL.  This is from a real world application we encountered.  Data was inserted
into a table lets call X1.  Every night, programatically X1 was renamed to X2, and a new X1 was
created with identical structure.  Basically, the application was "rolling" tables over daily
(while running).  In spite of the structure of X1 being identical after rolling, the database
considered a PreparedStatement compiled against the old table to be invalid.  When the statement
pool returned one of these statements, the application failed.  Turning off the cache in the
connection pool, and enabling it in the driver fixed the issue. How?  It is only speculation,
but it seems likely that driver in this case checks with the DB to ensure the statement is still
valid and if not recompiles it transparently. *Just because a connection is still valid does
not mean that prepared statements previously generated by it are valid.*

Regardless of correctness or not it is unnecessary with modern database drivers to implement this
at the pool level.

**Log Statement Text / Slow Query Logging**<br/>
Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL, and others.  We
consider this a "development-time" feature.  For those few databases that do not support it,
[jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/) is a good option.  It also provides
some nice additional stuff like timing, logging slow queries only, and PreparedStatement bound
parameter logging.

Trust us, you don't want this feature -- even disabled -- in a production connection pool.
*We consider even checking a boolean as inducing too much overhead into your queries and results.*

----------------------------------------------------

#### Configuration (Knobs, baby!) ####
The following is the various properties that can be configured in the pool, their behavior,
and their defaults.  HikariCP uses milliseconds for *all* time values, be careful.

``acquireIncrement``<br/>
This property controls the maximum number of connections that are acquired at one time, with
the exception of pool initialization. *Default: 1*

``acquireRetries``<br/>
This is a per-connection attempt retry count used during new connection creation (acquisition).
If a connection creation attempt fails there will be a wait of ``acquireRetryDelay``
milliseconds followed by another attempt, up to the number of retries configured by this
property. *Default: 0*

``acquireRetryDelay``<br/>
This property controls the number of milliseconds to delay between attempts to acquire a
connection to the database.  If ``acquireRetries`` is 0, this property has no effect.
*Default: 0*

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
available, an SQLException will be thrown.  <i>Default: Integer.MAX_VALUE</i>

``connectionUrl``<br/>
The is the JDBC connection URL string specific to your database. *Default: none*

``dataSourceClassName``<br/>
This is the name of the ``DataSource`` class provided by the JDBC driver.  Consult the
documentation for your specific JDBC driver to get this class name.  Note XA data sources
are not supported.  XA requires a real transaction manager like [bitronix](https://github.com/bitronix/btm).  *Default: none*

``idleTimeout``<br/>
This property controls the maximum amount of time (in milliseconds) that a connection is
allowed to sit idle in the pool.  Whether a connection is retired as idle or not is subject
to a maximum variation of +60 seconds, and average variation of +30 seconds.  A connection
will never be retired as idle *before* this timeout.  A value of 0 means that idle connections
are never removed from the pool.  *Default: 0*

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
``idleTimeout`` setting.  *Default: 0*

``maximumPoolSize``<br/>
The property controls the maximum size that the pool is allowed to reach, including both
idle and in-use connections.  Basically this value will determine the maximum number of
actual connections to the database backend.  A reasonable value for this is best determined
by your execution environment.  When the pool reaches this size, and no idle connections are
available, calls to getConnection() will block for up to ``connectionTimeout`` milliseconds
before timing out.  *Default: 1*

``minimumPoolSize``<br/>
The property controls the minimum number of connections that HikariCP tries to maintain in
the pool, including both idle and in-use connections.  If the connections dip below this
value, HikariCP will make a best effort to restore them quickly and efficiently.  A reasonable
value for this is best determined by your execution environment.  *Default: 0*

#### Requirements ####
 * Java 6 and above
 * Javassist library
 * slf4j library
