<h1>![](https://github.com/brettwooldridge/HikariCP/wiki/Hikari.png) HikariCP<sup><sup>&nbsp;It's Faster.</sup></sup><sub><sub><sup>Hi·ka·ri [hi·ka·'lē] &#40;*Origin: Japanese*): light; ray.</sup></sub></sub></h1><br>
[![][Build Status img]][Build Status]
[![][Issue Stats img]][Issue Stats]
[![][Coverage Status img]][Coverage Status]
[![][Dependency Status img]][Dependency Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]

Fast, simple, reliable.  HikariCP is a "zero-overhead" production ready JDBC connection pool.  At roughly 90Kb, the library is very light.  Read about [how we do it here](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).

&nbsp;&nbsp;&nbsp;<sup>**"Simplicity is prerequisite for reliability."**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- *Edsger Dijkstra*</sup>

----------------------------------------------------

_Java 7 and Java 8 maven artifact:_
```xml
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>2.4.5</version>
    </dependency>
```
_Java 6 maven artifact (*maintenance mode*):_
```xml
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP-java6</artifactId>
        <version>2.3.13</version>
    </dependency>
```
Or [download from here](http://search.maven.org/#search%7Cga%7C1%7Ccom.zaxxer.hikaricp).

----------------------------------------------------
[![](https://github.com/brettwooldridge/HikariCP/wiki/LudicrousBlog.png)](http://brettwooldridge.github.io/HikariCP/ludicrous.html)

##### JMH Benchmarks

Microbenchmarks were created to isolate and measure the overhead of pools using the [JMH microbenchmark framework](http://openjdk.java.net/projects/code-tools/jmh/) developed by the Oracle JVM performance team. You can checkout the [HikariCP benchmark project for details](https://github.com/brettwooldridge/HikariCP-benchmark) and review/run the benchmarks yourself.

![](https://github.com/brettwooldridge/HikariCP/wiki/HikariCP-bench-2.4.0.png)

 * One *Connection Cycle* is defined as single ``DataSource.getConnection()``/``Connection.close()``.
   * In *Unconstrained* benchmark, connections > threads.
   * In *Constrained* benchmark, threads > connections (2:1).
 * One *Statement Cycle* is defined as single ``Connection.prepareStatement()``, ``Statement.execute()``, ``Statement.close()``.

<sup>
<sup>1</sup> Versions: HikariCP 2.4.0, commons-dbcp2 2.1, Tomcat 8.0.23, Vibur 3.0, c3p0 0.9.5.1, Java 8u45 <br/>
<sup>2</sup> Java options: -server -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xmx512m <br/>
</sup>

----------------------------------------------------
##### User Testimonials

[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet3.png)](https://twitter.com/jkuipers)<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet1.png)](https://twitter.com/steve_objectify)<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet2.png)](https://twitter.com/brettemeyer)<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet4.png)](https://twitter.com/dgomesbr)

In the words of the guys over at [Edulify](https://edulify.com), *"HikariCP is supposed to be the fastest connection pool in Java land. But we did not start to use it because of speed, but because of its reliability.  Here is a cool graph that shows connections opened to PostgreSQL.  As you can see, the pool is way more stable. Also it is keeping its size at the minimum since we deploy it."*

![](https://github.com/brettwooldridge/HikariCP/wiki/HikariVsBone.png)

----------------------------------------------------
#### WIX Engineering Analysis
We'd like to thank the guys over at WIX for the unsolicited and deep write-up about HikariCP on their [engineering blog](http://engineering.wix.com/2015/04/28/how-does-hikaricp-compare-to-other-connection-pools/).  Take a look if you have time.

#### Failure: Pools behaving badly
Read our interesting ["Database down" pool challenge](https://github.com/brettwooldridge/HikariCP/wiki/Bad-Behavior:-Handling-Database-Down).

#### You're [probably] doing it wrong.
AKA ["What you probably didn't know about connection pool sizing"](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).  Read on to find out.

------------------------------
#### Configuration (knobs, baby!)
HikariCP comes with *sane* defaults that perform well in most deployments without additional tweaking. **Every property is optional, except for the "essentials" marked below.**

<sup>&#128206;</sup>&nbsp;*HikariCP uses milliseconds for all time values.*

##### Essentials

&#128288;``dataSourceClassName``<br/>
This is the name of the ``DataSource`` class provided by the JDBC driver.  Consult the
documentation for your specific JDBC driver to get this class name, or see the [table](https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names) below.
Note XA data sources are not supported.  XA requires a real transaction manager like
[bitronix](https://github.com/bitronix/btm). Note that you do not need this property if you are using
``jdbcUrl`` for "old-school" DriverManager-based JDBC driver configuration.
*Default: none*

*- or -*

&#128288;``jdbcUrl``<br/>
This property directs HikariCP to use "DriverManager-based" configuration.  We feel that DataSource-based
configuration (above) is superior for a variety of reasons (see below), but for many deployments there is
little significant difference.  **When using this property with "old" drivers, you may also need to set
the  ``driverClassName`` property, but try it first without.**  Note that if this property is used, you may
still use *DataSource* properties to configure your driver and is in fact recommended over driver parameters
specified in the URL itself.
*Default: none*

***

&#128288;``username``<br/>
This property sets the default authentication username used when obtaining *Connections* from
the underlying driver.  Note that for DataSources this works in a very deterministic fashion by
calling ``DataSource.getConnection(*username*, password)`` on the underlying DataSource.  However,
for Driver-based configurations, every driver is different.  In the case of Driver-based, HikariCP
will use this ``username`` property to set a ``user`` property in the ``Properties`` passed to the
driver's ``DriverManager.getConnection(jdbcUrl, props)`` call.  If this is not what you need,
skip this method entirely and call ``addDataSourceProperty("username", ...)``, for example.
*Default: none*

&#128288;``password``<br/>
This property sets the default authentication password used when obtaining *Connections* from
the underlying driver. Note that for DataSources this works in a very deterministic fashion by
calling ``DataSource.getConnection(username, *password*)`` on the underlying DataSource.  However,
for Driver-based configurations, every driver is different.  In the case of Driver-based, HikariCP
will use this ``password`` property to set a ``password`` property in the ``Properties`` passed to the
driver's ``DriverManager.getConnection(jdbcUrl, props)`` call.  If this is not what you need,
skip this method entirely and call ``addDataSourceProperty("pass", ...)``, for example.
*Default: none*

##### Frequently used

&#9989;``autoCommit``<br/>
This property controls the default auto-commit behavior of connections returned from the pool.
It is a boolean value.
*Default: true*

&#8986;``connectionTimeout``<br/>
This property controls the maximum number of milliseconds that a client (that's you) will wait
for a connection from the pool.  If this time is exceeded without a connection becoming
available, a SQLException will be thrown.  1000ms is the minimum value.
*Default: 30000 (30 seconds)*

&#8986;``idleTimeout``<br/>
This property controls the maximum amount of time that a connection is allowed to sit idle in the
pool.  Whether a connection is retired as idle or not is subject to a maximum variation of +30
seconds, and average variation of +15 seconds.  A connection will never be retired as idle *before*
this timeout.  A value of 0 means that idle connections are never removed from the pool.
*Default: 600000 (10 minutes)*

&#8986;``maxLifetime``<br/>
This property controls the maximum lifetime of a connection in the pool.  When a connection
reaches this timeout it will be retired from the pool, subject to a maximum variation of +30
seconds.  An in-use connection will never be retired, only when it is closed will it then be
removed.  **We strongly recommend setting this value, and it should be at least 30 seconds less
than any database-level connection timeout.**  A value of 0 indicates no maximum lifetime 
(infinite lifetime), subject of course to the ``idleTimeout`` setting.
*Default: 1800000 (30 minutes)*

&#128288;``connectionTestQuery``<br/>
**If your driver supports JDBC4 we strongly recommend not setting this property.** This is for 
"legacy" databases that do not support the JDBC4 ``Connection.isValid() API``.  This is the query that
will be executed just before a connection is given to you from the pool to validate that the 
connection to the database is still alive. *Again, try running the pool without this property,
HikariCP will log an error if your driver is not JDBC4 compliant to let you know.*
*Default: none*

&#128290;``minimumIdle``<br/>
This property controls the minimum number of *idle connections* that HikariCP tries to maintain
in the pool.  If the idle connections dip below this value, HikariCP will make a best effort to
add additional connections quickly and efficiently.  However, for maximum performance and
responsiveness to spike demands, we recommend *not* setting this value and instead allowing
HikariCP to act as a *fixed size* connection pool.
*Default: same as maximumPoolSize*

&#128290;``maximumPoolSize``<br/>
This property controls the maximum size that the pool is allowed to reach, including both
idle and in-use connections.  Basically this value will determine the maximum number of
actual connections to the database backend.  A reasonable value for this is best determined
by your execution environment.  When the pool reaches this size, and no idle connections are
available, calls to getConnection() will block for up to ``connectionTimeout`` milliseconds
before timing out.
*Default: 10*

&#128200;``metricRegistry``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to specify an instance of a *Codahale/Dropwizard* ``MetricRegistry`` to be used by the
pool to record various metrics.  See the [Metrics](https://github.com/brettwooldridge/HikariCP/wiki/Codahale-Metrics)
wiki page for details.
*Default: none*

&#128200;``healthCheckRegistry``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to specify an instance of a *Codahale/Dropwizard* ``HealthCheckRegistry`` to be used by the
pool to report current health information.  See the [Health Checks](https://github.com/brettwooldridge/HikariCP/wiki/Dropwizard-HealthChecks)
wiki page for details.
*Default: none*

&#128288;``poolName``<br/>
This property represents a user-defined name for the connection pool and appears mainly
in logging and JMX management consoles to identify pools and pool configurations.
*Default: auto-generated*

##### Infrequently used

&#9989;``initializationFailFast``<br/>
This property controls whether the pool will "fail fast" if the pool cannot be seeded with
initial connections successfully.  If you want your application to start *even when* the
database is down/unavailable, set this property to ``false``.
*Default: true*

&#10062;``isolateInternalQueries``<br/>
This property determines whether HikariCP isolates internal pool queries, such as the
connection alive test, in their own transaction.  Since these are typically read-only
queries, it is rarely necessary to encapsulate them in their own transaction.  This
property only applies if ``autoCommit`` is disabled.
*Default: false*

&#10062;``allowPoolSuspension``<br/>
This property controls whether the pool can be suspended and resumed through JMX.  This is
useful for certain failover automation scenarios.  When the pool is suspended, calls to
``getConnection()`` will *not* timeout and will be held until the pool is resumed.
*Default: false*

&#10062;``readOnly``<br/>
This property controls whether *Connections* obtained from the pool are in read-only mode by
default.  Note some databases do not support the concept of read-only mode, while others provide
query optimizations when the *Connection* is set to read-only.  Whether you need this property
or not will depend largely on your application and database. 
*Default: false*

&#10062;``registerMbeans``<br/>
This property controls whether or not JMX Management Beans ("MBeans") are registered or not.
*Default: false*

&#128288;``catalog``<br/>
This property sets the default *catalog* for databases that support the concept of catalogs.
If this property is not specified, the default catalog defined by the JDBC driver is used.
*Default: driver default*

&#128288;``connectionInitSql``<br/>
This property sets a SQL statement that will be executed after every new connection creation
before adding it to the pool. If this SQL is not valid or throws an exception, it will be
treated as a connection failure and the standard retry logic will be followed.
*Default: none*

&#128288;``driverClassName``<br/>
HikariCP will attempt to resolve a driver through the DriverManager based solely on the ``jdbcUrl``,
but for some older drivers the ``driverClassName`` must also be specified.  Omit this property unless
you get an obvious error message indicating that the driver was not found.
*Default: none*

&#128288;``transactionIsolation``<br/>
This property controls the default transaction isolation level of connections returned from
the pool.  If this property is not specified, the default transaction isolation level defined
by the JDBC driver is used.  Only use this property if you have specific isolation requirements that are
common for all queries.  The value of this property is the constant name from the ``Connection``
class such as ``TRANSACTION_READ_COMMITTED``, ``TRANSACTION_REPEATABLE_READ``, etc.
*Default: driver default*

&#8986;``validationTimeout``<br/>
This property controls the maximum amount of time that a connection will be tested for aliveness.
This value must be less than the ``connectionTimeout``.  The lowest accepted validation timeout is
1000ms (1 second).
*Default: 5000*

&#8986;``leakDetectionThreshold``<br/>
This property controls the amount of time that a connection can be out of the pool before a
message is logged indicating a possible connection leak.  A value of 0 means leak detection
is disabled.  Lowest acceptable value for enabling leak detection is 2000 (2 secs).
*Default: 0*

&#10145;``dataSource``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to directly set the instance of the ``DataSource`` to be wrapped by the pool, rather than
having HikariCP construct it via reflection.  This can be useful in some dependency injection
frameworks. When this property is specified, the ``dataSourceClassName`` property and all
DataSource-specific properties will be ignored.
*Default: none*

&#10145;``threadFactory``<br/>
This property is only available via programmatic configuration or IoC container.  This property
allows you to set the instance of the ``java.util.concurrent.ThreadFactory`` that will be used
for creating all threads used by the pool. It is needed in some restricted execution environments
where threads can only be created through a ``ThreadFactory`` provided by the application container.
*Default: none*

#### Missing Knobs

HikariCP has plenty of "knobs" to turn as you can see above, but comparatively less than some other pools.
This is a design philosophy.  The HikariCP design aesthetic is Minimalism.  In keeping with the
*simple is better* or *less is more* design philosophy, some configuration axis are intentionally left out.

#### Statement Cache

Many connection pools, including Apache DBCP, Vibur, CP30 and others offer ``PreparedStatement`` caching.
HikariCP does not.  Why?

At the connection pool layer ``PreparedStatements`` can only be cached *per connection*.  If your application
has 250 commonly executed queries and a pool of 20 connections you are asking your database to hold on to
5000 query execution plans -- and similarly the pool must cache this many ``PreparedStatements`` and their
related graph of objects.

Most major database JDBC drivers already have a Statement cache that can be configured, including PostgreSQL,
Oracle, Derby, MySQL, DB2, and many others.  JDBC drivers are in a unique position to exploit database specific
features, and nearly all of the caching implementations are capable of sharing execution plans *across connections*.
This means that instead of 5000 statements in memory and associated execution plans, your 250 commonly executed
queries result in exactly 250 execution plans in the database.  Clever implementations do not even retain
``PreparedStatement`` objects in memory at the driver-level but instead merely attach new instances to existing plan IDs.

Using a statement cache at the pooling layer is an [anti-pattern](https://en.wikipedia.org/wiki/Anti-pattern),
and will negatively impact your application performance compared to driver-provided caches.

#### Log Statement Text / Slow Query Logging

Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL, and others.  Some
even support slow query logging.  For those few databases that do not support it, [log4jdbc](https://github.com/arthurblake/log4jdbc) or [jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/) are
good options.

----------------------------------------------------

### Initialization

You can use the ``HikariConfig`` class like so:
```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
config.setUsername("bart");
config.setPassword("51mp50n");
config.addDataSourceProperty("cachePrepStmts", "true");
config.addDataSourceProperty("prepStmtCacheSize", "250");
config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

HikariDataSource ds = new HikariDataSource(config);
```
or directly instantiate a ``HikariDataSource`` like so:
```java
HikariDataSource ds = new HikariDataSource();
ds.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
ds.setUsername("bart");
ds.setPassword("51mp50n");
...
```
or property file based:
```java
HikariConfig config = new HikariConfig("some/path/hikari.properties");
HikariDataSource ds = new HikariDataSource(config);
```
Example property file:
```ini
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.user=test
dataSource.password=test
dataSource.databaseName=mydb
dataSource.portNumber=5432
dataSource.serverName=localhost
```
or ``java.util.Properties`` based:
```java
Properties props = new Properties();
props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
props.setProperty("dataSource.user", "test");
props.setProperty("dataSource.password", "test");
props.setProperty("dataSource.databaseName", "mydb");
props.put("dataSource.logWriter", new PrintWriter(System.out));

HikariConfig config = new HikariConfig(props);
HikariDataSource ds = new HikariDataSource(config);
```

There is also a System property available, ``hikaricp.configurationFile``, that can be used to specify the
location of a properties file.  If you intend to use this option, construct a ``HikariConfig`` or ``HikariDataSource``
instance using the default constructor and the properties file will be loaded.

### Popular DataSource Class Names

We recommended using ``dataSourceClassName`` instead of ``jdbcUrl``, but both are acceptable.  We'll say that again, *both are acceptable*.  *Note: Spring Boot auto-configuration users, you need to use ``jdbcUrl``-based configuration.*

Here is a list of JDBC *DataSource* classes for popular databases:

| Database         | Driver       | *DataSource* class |
|:---------------- |:------------ |:-------------------|
| Apache Derby     | Derby        | org.apache.derby.jdbc.ClientDataSource |
| Firebird         | Jaybird      | org.firebirdsql.pool.FBSimpleDataSource |
| H2               | H2           | org.h2.jdbcx.JdbcDataSource |
| HSQLDB           | HSQLDB       | org.hsqldb.jdbc.JDBCDataSource |
| IBM DB2          | IBM JCC      | com.ibm.db2.jcc.DB2SimpleDataSource |
| IBM Informix     | IBM Informix | com.informix.jdbcx.IfxDataSource |
| MS SQL Server    | Microsoft    | com.microsoft.sqlserver.jdbc.SQLServerDataSource |
| MySQL            | Connector/J  | com.mysql.jdbc.jdbc2.optional.MysqlDataSource |
| MySQL/MariaDB    | MariaDB      | org.mariadb.jdbc.MySQLDataSource |
| Oracle           | Oracle       | oracle.jdbc.pool.OracleDataSource |
| OrientDB         | OrientDB     | com.orientechnologies.orient.jdbc.OrientDataSource |
| PostgreSQL       | pgjdbc-ng    | com.impossibl.postgres.jdbc.PGDataSource |
| PostgreSQL       | PostgreSQL   | org.postgresql.ds.PGSimpleDataSource |
| SAP MaxDB        | SAP          | com.sap.dbtech.jdbc.DriverSapDB |
| SQLite           | xerial       | org.sqlite.SQLiteDataSource |
| SyBase           | jConnect     | com.sybase.jdbc4.jdbc.SybDataSource |

### Play Framework Plugin

Note Play 2.4 now uses HikariCP by default.  A new plugin has come up for the the Play framework; [play-hikaricp](http://edulify.github.io/play-hikaricp.edulify.com/).  If you're using the excellent Play framework,  your application deserves HikariCP.  Thanks Edulify Team!

### Clojure Wrapper

A new Clojure wrapper has been created by [tomekw](https://github.com/tomekw) and can be [found here](https://github.com/tomekw/hikari-cp).

----------------------------------------------------

### Support <sup><sup>&#128172;</sup></sup>

Google discussion group [HikariCP here](https://groups.google.com/d/forum/hikari-cp), growing [FAQ](https://github.com/brettwooldridge/HikariCP/wiki/FAQ).

[![](https://raw.github.com/wiki/brettwooldridge/HikariCP/twitter.png)](https://twitter.com/share?text=Interesting%20JDBC%20Connection%20Pool&hashtags=HikariCP&url=https%3A%2F%2Fgithub.com%2Fbrettwooldridge%2FHikariCP)&nbsp;[![](https://raw.github.com/wiki/brettwooldridge/HikariCP/facebook.png)](http://www.facebook.com/plugins/like.php?href=https%3A%2F%2Fgithub.com%2Fbrettwooldridge%2FHikariCP&width&layout=standard&action=recommend&show_faces=true&share=false&height=80)

### Wiki

Don't forget the [Wiki](https://github.com/brettwooldridge/HikariCP/wiki) for additional information such as:
 * [FAQ](https://github.com/brettwooldridge/HikariCP/wiki/FAQ)
 * [Hibernate 4.x Configuration](https://github.com/brettwooldridge/HikariCP/wiki/Hibernate4)
 * [MySQL Configuration Tips](https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration)
 * etc.

----------------------------------------------------

### Requirements

 &#8658; Java 7 and above<br/>
 &#8658; slf4j library<br/>

### Sponsors

YourKit supports open source projects with its full-featured Java Profiler.  Click the YourKit logo below to learn more.<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/yklogo.png)](http://www.yourkit.com/java/profiler/index.jsp)<br/>

### Contributions

Please perform changes and submit pull requests from the ``dev`` branch instead of ``master``.  Please set your editor to use spaces instead of tabs, and adhere to the apparent style of the code you are editing.  The ``dev`` branch is always more "current" than the ``master`` if you are looking to live life on the edge.

[Build Status]:https://travis-ci.org/brettwooldridge/HikariCP
[Build Status img]:https://travis-ci.org/brettwooldridge/HikariCP.svg?branch=master

[Issue Stats]:http://issuestats.com/github/brettwooldridge/HikariCP
[Issue Stats img]:http://issuestats.com/github/brettwooldridge/HikariCP/badge/issue?style=flat&concise=true

[Coverage Status]:https://coveralls.io/r/brettwooldridge/HikariCP?branch=master
[Coverage Status img]:https://coveralls.io/repos/brettwooldridge/HikariCP/badge.svg?branch=master

[Dependency Status]:https://www.versioneye.com/user/projects/551ce51c3661f1bee50004e0
[Dependency Status img]:https://www.versioneye.com/user/projects/551ce51c3661f1bee50004e0/badge.svg?style=flat

[license]:LICENSE
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/com.zaxxer/HikariCP
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/com.zaxxer/HikariCP/badge.svg
