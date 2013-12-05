![](https://raw.github.com/wiki/brettwooldridge/HikariCP/Hikari.png)&nbsp;HikariCP <sup><sup>It's Faster.</sup></sup>&nbsp;[![Build Status](https://travis-ci.org/brettwooldridge/HikariCP.png?branch=master)](https://travis-ci.org/brettwooldridge/HikariCP)
==========

There is nothing [faster](https://github.com/brettwooldridge/HikariCP/wiki/Benchmarks).<sup>1</sup>  There is 
nothing more [correct](https://github.com/brettwooldridge/HikariCP/wiki/Correctness).  HikariCP is a "zero-overhead"
production-quality connection pool.  Coming in at roughly 50Kb, the library is extremely light.

Using a stub-JDBC implementation to isolate and measure the overhead of HikariCP, 60+ Million JDBC operations
were performed in ***8ms*** on a commodity PC.<sup>2</sup>  The next fastest connection pool (BoneCP) was ***5298ms***.

| Pool     |  Med (ms) |  Avg (ms) |  Max (ms) |
| -------- | ---------:| ---------:| ---------:|
| BoneCP   | 5298      | 3249      | 6929      |
| HikariCP | 8         | 7         | 13        |

<sub><sup>1</sup>We contend HikariCP is near the theoretical maximum on current JVM technology.</sub><br/>
<sub><sup>2</sup>400 threads, 50 connection pool. Measurements in *nanoseconds* and converted to *milliseconds*.
See benchmarks [here](https://github.com/brettwooldridge/HikariCP/wiki/Benchmarks).  See how we do it [here](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).</sub><br/>

Or look at this:
![](http://github.com/brettwooldridge/HikariCP/wiki/50Connection_MixedBench.png)

----------------------------------------------------
### Maven Repository ###

HikariCP comes with two jars: ``HikariCP-1.x.x.jar`` and ``HikariCP-agent-1.x.x.jar``.  The "core" jar contains
everything you need to run.  If you wish to use *instrumentation mode* to go a little faster, you'll also need
the agent jar.  See below for details.

##### Required #####

    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>1.1.9</version>
        <scope>compile</scope>
    </dependency>

##### Optional (Instrumentation<sup>1</sup>) #####

    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP-agent</artifactId>
        <version>1.1.9</version>
        <scope>compile</scope>
    </dependency>

<sub><sup>1</sup>*Instrumentation* mode is still considered experimental, if you use it and encounter issues, please report</sub>
<sub>&nbsp;them here and disable it by removing the agent jar or setting the ``useInstrumentation`` property to ``false``.</sub>

----------------------------------------------------
#### Driver Support ####
HikariCP has two modes of operation: **Delegation** and **Instrumentation**.  *Instrumentation* is approximately 20-40% more
performant, but both are exceedingly fast.

##### Delegation #####
Delegation mode is supported for *all* JDBC drivers.  This is included in the core HikariCP jar.

##### Instrumentation #####
Instrumentation mode is supported for specific JDBC drivers.  For instrumentation, you will need the "agent" jar
in addition to the core jar.  If your favorite database is not supported, drop us a note in the
[Google group](https://groups.google.com/d/forum/hikari-cp) and we'll try to add support for it.  Below is a table
of drivers that are supported and their status:

| Driver            | Version<sup>1</sup>      |  Status   |  DataSource<sup>2</sup>  | 
| ----------------- | --------------:| --------- | ------------ |
| Derby             | 10.10.1.1      | Tested    | org.apache.derby.jdbc.ClientDataSource40      |
| jTDS              | 1.3.1          | Untested  | net.sourceforge.jtds.jdbcx.JtdsDataSource     |
| HSQLDB            | 2.3.1          | Tested    | org.hsqldb.jdbc.JDBCDataSource                |
| MariaDB           | 1.1.5          | Tested    | org.mariadb.jdbc.MySQLDataSource              |
| MySQL Connector/J | 5.1.56         | Tested    | com.mysql.jdbc.jdbc2.optional.MysqlDataSource |
| Oracle            | 12.1.0.1       | Untested  | oracle.jdbc.pool.OracleDataSource             |
| PostgreSQL        | 9.2-1003.jdbc4 | Tested    | org.postgresql.ds.PGSimpleDataSource          |

<sub><sup>1</sup>Older/newer driver versions for a given database will *probably* work, because class names are rarely
changed.  But if it does not work, you will known quickly because HikariCP will likely fail to start.  In this case, you
can simply force *delegation mode* (see properties below).</sub><br/>
<sub><sup>2</sup>The *DataSource* is specified because it is by the specified DataSource name that HikariCP looks up the instrumentation
information in the internal codex.</sub>

------------------------------

#### Configuration (knobs, baby!) ####
The following are the various properties that can be configured in the pool, their behavior,
and their defaults.  **HikariCP uses milliseconds for *all* time values, be careful.**

Rather than coming out of the box with almost nothing configured, HikariCP comes with *sane*
defaults that let a great many deployments run without any additional tweaking (except for
the DataSource and DataSource properties).

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

``connectionInitSql``<br/>
This property sets a SQL statement that will be executed after every new connection creation
before adding it to the pool. If this SQL is not valid or throws an exception, it will be
treated as a connection failure and the standard retry logic will be followed.  *Default: none*

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

``useInstrumentation``<br/>
This property controls whether HikariCP will attempt to use bytecode instrumentation to boost
performance.  Instrumentation is enabled by default, but whether it is used or not is based on
whether the DataSource that is specified is recognized as supported.  Otherwise, delegation 
mode will be used.  If you experience a failure due to instrumentation, you can manually disable
instrumentation with this property.  *Default: true*

##### DataSource Properties #####
DataSource properies can be set on the ``HikariConfig`` object through the use of the ``addDataSourcePropery``
method, like so:

    config.addDataSourceProperty("url", "jdbc:hsqldb:mem:test");
    config.addDataSourceProperty("user", "SA");
    config.addDataSourceProperty("password", "");

See the [Initialization](#initialization) section below for further examples.


##### ***Missing Knobs*** #####

HikariCP has plenty of "knobs" to turn as you can see above, but comparatively less than some other pools.
This is a design philosophy.  The HikariCP design asthetic is Minimalism.

We're not going to (overly) question the design decisions of other pools, but we will say
that some other pools seem to implement a lot of "gimmicks" that proportedly improve
performance.  HikariCP achieves high-performance even in pools beyond realistic deployment
sizes.

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

### Initialization ###

    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(100);
    config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource);
    config.addDataSourceProperty("url", "jdbc:mysql://localhost/database");
    config.addDataSourceProperty("user", "bart");
    config.addDataSourceProperty("password", "51mp50n");
    
    HikariDataSource ds = new HikariDataSource(config);

or property file based:

    HikariConfig config = new HikariConfig("some/path/hikari.properties");
    HikariDataSource ds = new HikariDataSource(config);

Example property file:

    acquireIncrement=3
    acquireRetryDelay=1000
    connectionTestQuery=SELECT 1
    dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
    dataSource.username=test
    dataSource.password=test
    dataSource.databaseName=mydb
    dataSource.serverName=localhost

----------------------------------------------------

#### JMX Management ####
The following properties are also configurable in real-time as the pool is running via a JMX
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

#### Support ####
Google discussion group [HikariCP here](https://groups.google.com/d/forum/hikari-cp)

#### Requirements ####
 * Oracle Java 7 and above<sup>1</sup>
 * Javassist 3.18.1+ library
 * slf4j library

<sup>1</sup>It might work with other JVM, but defintely won't work with Java 6 and below because of the use of
classes that are only available in Java 7.

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/63472d76ad0d494e3c4d8fc4a13ea4ce "githalytics.com")](http://githalytics.com/brettwooldridge/HikariCP)
