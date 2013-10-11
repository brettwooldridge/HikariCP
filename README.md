HikariCP <sub><sub>Ultimate JDBC Connection Pool<sup><sup>&nbsp;&nbsp;[We came, we saw, we kicked its ass](http://youtu.be/-xMGRA_FePw)</sup></sup></sub></sub>
========

There is nothing faster.  There is nothing more reliable.  There is nothing more correct.

Are you using DBCP, C3P0 or BoneCP?  *Stop.*

#### TL;DR ####
Let's look at some performance numbers.  HikariCP was only compared to BoneCP because, really,
DBCP and C3P0 are old and slow and I don't know why anyone would use them.

##### MixedBench #####
This is the so called "Mixed" benchmark, and it executes a representative array of JDBC
operations in a realistic mix.  We think *median* is the number to pay attention to, rather
than average (which can get skewed).  *Median* meaning 50% of the iterations were slower, %50 were faster.  200 threads were started, and the underlying connection pool contained 100
connections.

| Pool     |  Med (ms) |  Avg (ms) |  Max (ms) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 2155      | 1541      | 3265      |
| HikariCP | 230       | 139       | 526       |

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

The test was performed on an Intel Core i7 (3770 Ivy Bridge) 3.4GHz iMac with 32GB of RAM.  The
JVM benchmark was run with: ``-server -XX:+UseParallelGC -Xss256k``.

##### In Summary #####
200 threads ran 60,702,000 JDBC operations each, HikariCP did this in a median of *230ms* per thread.

------------------------------

#### (In)correctness ####
Sometimes "correctness" is objective, and sometimes it is subjective.  One example of
objective *incorrectness* in BoneCP is ``ResultSet`` handling.  Every connection pool needs to
wrap the underlying ``Connection``, ``Statement``, ``CallableStatement``, and
``PreparedStatement``, and ``ResultSet`` classes.  However, BoneCP does not wrap ResultSet.

``ResultSet`` *must* be wrapped, because ``ResultSet.getStatement()`` *must* return the
**wrapped** ``Statement`` that generated it, not the **underlying** ``Statement``.
Hibernate 4.3 for one relies on this semantic.

If BoneCP were to wrap ResultSet, which comprises 20,100,000 of the 60,702,000 operations in
MixedBench, its performance numbers would be far poorer.  Also take note that HikariCP *does*
properly wrap ResultSet and still achives the numbers above.

One example of *subjective* incorrectness -- being my personal opinion -- is that
BoneCP does not test a ``Connection`` immediately before dispatching it from the pool.  It is
through this mechanism that it achives some of it's speed.  In my opinion, this one "flaw"
(or "feature") renders BoneCP insuitable for Production use.  The number one responsibility of
a connection pool is to **not** give out possibly bad connections.  If you have ever run a
load-balancer in front of read-slaves, or have ever needed to bounce the DB while the
application was running, you certainly didn't do it with BoneCP.

Over on the BoneCP site, you can find a comparison of BoneCP vs. DBCP and C3P0.  DBCP and C3P0,
as poor as they are, at least are performing aliveness tests before dispatching connections.
So, it's not really a fair comparison.  HikariCP supports the JDBC4 ``Connection.isValid()``
API, which for many drivers provides a fast non-query based aliveness test.

A particularly silly "benchmark" on the BoneCP site starts 500 threads each performing 100
DataSource.getConnection() / connection.close() calls with 0ms delay between.  Who does that?
The typical "mix" is dozens or hundreds of JDBC operations between obtaining the connection and
closing it (hence the "MixBench") above.  But ok, we can run this "benchmark" too (times in
Microseconds):

| Pool     |  Med (μs) |  Avg (μs) |  Max (μs) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 19467     | 8762      | 30851     |
| HikariCP | 76        | 65        | 112       |

The times are per-thread reflecting 100 getConnection()/close() operations with no wait between.

------------------------------------------

#### Knobs ####
Where are all the knobs?  HikariCP has plenty of "knobs" as you can see in the configuration section below, but comparatively less than some other pools.  This is a design philosophy.
Configuring a connection pool, even for a large production environment, is not rocket science.
The HikariCP design semantic is minimalist.  You probably need to configure the idle timeout
for connections in the pool, but do you really need to configure how often the pool is swept
to retire them?  You might *think* you do, but if you do you're probably doing something
wrong.

##### ***Missing Knobs*** #####
In keeping with the *simple is better* or *less is more* design philosophy, some knobs and features are intentionally left out.  Here are some, and the reasons.

**Statement Cache**<br/>
Many (most?) major database JDBC drivers already have a PreparedStatement cache that can be
configured (Oracle, MySQL, PostgreSQL, Derby, etc).  A statement cache in the pool would add
unneeded weight, no additional functionality, and possibly incorrect behavior...

JDBC drivers have a special relationship with the remote database in that they are directly
connected and can share internal state that is synchronized with the backend.  **It is
inherently unsafe to cache PreparedStatements outside of the driver.**  Why?  Again, drivers
have the advantage of deep knowledge of the the database for which they are designed.

Take for example DDL.  This is from a real world application we encountered (using BoneCP btw).
Data was inserted into a table lets call X1.  Every night, programatically X1 was renamed to X2, and a new X1 was created with identical structure.  Basically, the application was
"rolling" tables over daily (while running).  In spite of the structure of X1 being identical
after rolling, the database considered a PreparedStatement compiled against the old table to be
invalid (probably there was some kind of UUID contained within).  When the statement pool
returned one of these statements, the application blew up.  Turning off the cache in the
connection pool, and enabling it in the driver fixed the issue.  How?  It is only speculation,
but it seems likely that driver in this case checks with the DB to ensure the statement is
still valid and if not recompiles it transparently.

Regardless of correctness or not (I'm sure it varies by DB vendor), it is unnecessary with
modern database drivers to implement this at the pool level.

**Log Statement Text**<br/>
Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL.  We consider this
a "development-time" feature.  For those few databases that do not support it,
[log4jdbc](https://code.google.com/p/log4jdbc/) is a good option.  Actually *log4jdbc* provides some nice additional stuff like various timings, and PreparedStatement bound parameter logging.

HikariCP is focused (primarily) on performance in a Production environment, so it is doubtful
that we will ever implement this kind of feature given inherent driver support and alternative
solutions.

----------------------------------------------------

#### Configuration (knobs baby!) ####
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
allowed to sit idle in the pool.
