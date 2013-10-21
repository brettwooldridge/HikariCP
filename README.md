![](https://raw.github.com/wiki/brettwooldridge/HikariCP/Hikari.png)&nbsp;HikariCP <sup><sup>Ultimate JDBC Connection Pool</sup></sup>&nbsp;[![Build Status](https://travis-ci.org/brettwooldridge/HikariCP.png?branch=master)](https://travis-ci.org/brettwooldridge/HikariCP)
==========

There is nothing [faster](https://github.com/brettwooldridge/HikariCP/wiki/How-we-do-it).<sup>1</sup>  There is 
nothing more [correct](https://github.com/brettwooldridge/HikariCP/wiki/Correctness).  HikariCP is a "zero-overhead"
production-quality connection pool.

Using a stub-JDBC implementation to isolate and measure the overhead of HikariCP, 60+ Million JDBC operations
were performed in ***8ms*** on a commodity PC.  The next fastest connection pool (BoneCP) was ***5049ms***.<sup>2</sup>

| Pool     |  Med (ms) |  Avg (ms) |  Max (ms) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 5049      | 3249      | 6929      |
| HikariCP | 8         | 7         | 13        |

<sub><sup>1</sup>We contend HikariCP is near the theoretical maximum possible on current JVM technology.</sub><br/>
<sub><sup>2</sup>400 threads, 50 connection pool. Measurements taken in *nanoseconds* and converted to *milliseconds*.
See benchmarks [here]([faster](https://github.com/brettwooldridge/HikariCP/wiki/How-we-do-it)</sub>

------------------------------

#### Configuration (Knobs, baby!) ####
The following are the various properties that can be configured in the pool, their behavior,
and their defaults.  **HikariCP uses milliseconds for *all* time values, be careful.**

Rather than coming out of the box with almost nothing configured, HikariCP comes with *sane*
defaults that let a great many deployments run without any additional tweaking (except for
the DataSource, connection URL, and driver properties).

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

##### DataSource Properties #####
DataSource properies can be set on the ``HikariConfig`` object through the use of the ``addDataSourcePropery``
method, like so:

    config.addDataSourceProperty("url", "jdbc:hsqldb:mem:test");
    config.addDataSourceProperty("user", "SA");
    config.addDataSourceProperty("password", "");

##### ***Missing Knobs*** #####

HikariCP has plenty of "knobs" to turn as you can see above, but comparatively less than some other pools.
This is a design philosophy.  The HikariCP design asthetic is Minimalism.

We're not going to (overly) question the design decisions of other pools, but we will say
that some other pools seem to implement a lot of "gimmicks" that proportedly improve
performance.  HikariCP achieves high-performance even in pools beyond realistic deployment
sizes.  Either these "gimmicks" are a case of premature optimization, poor design, or lack
of understanding of how to leaverage what the JVM JIT can do to full effect.

In keeping with the *simple is better* or *less is more* design philosophy, some knobs and 
features are intentionally left out.  Here are two, and the rationale.

**Statement Cache**<br/>
Most major database JDBC drivers already have a Statement cache that can be configured (Oracle, 
MySQL, PostgreSQL, Derby, etc).  A statement cache in the pool would add unneeded weight and no
additional functionality.  It is simply unnecessary with modern database drivers to implement a
cache at the pool level.

**Log Statement Text / Slow Query Logging**<br/>
Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL, and others.  We
consider this a "development-time" feature.  For those few databases that do not support it,
[jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/) is a good option.  It is easy to
wrap HikariCP arould *jdbcdslog*.  It also provides some nice additional stuff like timing,
logging slow queries only, and PreparedStatement bound parameter logging.   Great stuff during
development and pre-Production.

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
 * Oracle Java 6 and above
 * Javassist 3.18.1+ library
 * slf4j library
