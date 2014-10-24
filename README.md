![](https://raw.github.com/wiki/brettwooldridge/HikariCP/Hikari.png)&nbsp;HikariCP <sup><sup>It's Faster.</sup></sup>&nbsp;[![Build Status](https://travis-ci.org/brettwooldridge/HikariCP.png?branch=master)](https://travis-ci.org/brettwooldridge/HikariCP)&nbsp;[![Coverage Status](https://coveralls.io/repos/brettwooldridge/HikariCP/badge.png?branch=dev)](https://coveralls.io/r/brettwooldridge/HikariCP?branch=dev)<img src='https://raw.github.com/wiki/brettwooldridge/HikariCP/space60x1.gif'><sup><sup>[![](https://raw.github.com/wiki/brettwooldridge/HikariCP/twitter.png)](https://twitter.com/share?text=Interesting%20JDBC%20Connection%20Pool&hashtags=HikariCP&url=https%3A%2F%2Fgithub.com%2Fbrettwooldridge%2FHikariCP)</sup></sup><br><sub><sub><sup>Hi·ka·ri [hi·ka·'lē] &#40;*Origin: Japanese*): light; ray.</sup></sub></sub>
==========

Fast, simple, reliable.  HikariCP is a "zero-overhead" production ready JDBC connection pool.  Coming in at roughly 70Kb, the library is very light.  Read about [how we do it here](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).

&nbsp;&nbsp;&nbsp;<sup>**"Simplicity is prerequisite for reliability."**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- *Edsger Djikstra*</sup>

----------------------------------------------------
_Java 8 maven artifact:_
```xml
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>2.1.0</version>
        <scope>compile</scope>
    </dependency>
```
_Java 6 and Java 7 maven artifact:_
```xml
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP-java6</artifactId>
        <version>2.1.0</version>
        <scope>compile</scope>
    </dependency>
```

----------------------------------------------------
***JMH Benchmarks***<br/>
Microbenchmarks were created to isolate and measure the overhead of pools using the [JMH microbenchmark framework](http://openjdk.java.net/projects/code-tools/jmh/) developed by the Oracle JVM performance team. You can checkout the [HikariCP benchmark project for details](https://github.com/brettwooldridge/HikariCP-benchmark) and review/run the benchmarks yourself.

![](https://github.com/brettwooldridge/HikariCP/wiki/Benchmarks.png)

 * One *Connection Cycle* is defined as single ``DataSource.getConnection()``/``Connection.close()``.
   * In *Unconstrained* benchmark, connections > threads.
   * In *Constrained* benchmark, threads > connections (2:1).
 * One *Statement Cycle* is defined as single ``Connection.prepareStatement()``, ``Statement.execute()``, ``Statement.close()``.

<sup>
<sup>1</sup> Versions: HikariCP 2.1.0, BoneCP 0.8.0, Tomcat 8.0.9, Vibur 1.2.0, C3P0 0.9.5-pre8, Java 8u20 <br/>
<sup>2</sup> Java options: -server -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xmx512m <br/>
</sup>

----------------------------------------------------
***User Testimonials***<br/>

[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet3.png)](https://twitter.com/jkuipers)

[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet1.png)](https://twitter.com/steve_objectify)

[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet2.png)](https://twitter.com/brettemeyer)

The guys over at [Edulify](https://edulify.com) were experiencing connection leaks and other issues using BoneCP in their Play Framework application.  They created a [HikariCP plugin for Play Framework](http://edulify.github.io/play-hikaricp.edulify.com/) to give HikariCP a try.

In their own words, *"HikariCP is supposed to be the fastest connection pool in Java land. But we did not start to use it because of speed, but because of its reliability.  Here is a cool graph that shows connections opened to PostgreSQL.  As you can see, the pool is way more stable. Also it is keeping its size at the minimum since we deploy it."*

![](https://github.com/brettwooldridge/HikariCP/wiki/HikariVsBone.png)

----------------------------------------------------

#### Failure: Pools behaving badly
Read our interesting ["Database down" pool challenge](https://github.com/brettwooldridge/HikariCP/wiki/Bad-Behavior:-Handling-Database-Down).

#### You're [probably] doing it wrong.
AKA ["What you probably didn't know about connection pool sizing"](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).  Read on to find out.

------------------------------

#### Configuration (knobs, baby!)
HikariCP comes with *sane* defaults that perform well in most deployments without additional tweaking.

<sup>:paperclip:</sup>&nbsp;*HikariCP uses milliseconds for all time values.*

:white_check_mark:``autoCommit``<br/>
This property controls the default auto-commit behavior of connections returned from the pool.
It is a boolean value.  *Default: true*

:negative_squared_cross_mark:``readOnly``<br/>
This property controls whether *Connections* obtained from the pool are in read-only mode by
default.  Note some databases do not support the concept of read-only mode, while others provide
query optimizations when the *Connection* is set to read-only.  Whether you need this property
or not will depend largely on your application and database.  *Default: false*

:abc:``transactionIsolation``<br/>
This property controls the default transaction isolation level of connections returned from
the pool.  If this property is not specified, the default transaction isolation level defined
by the JDBC driver is used.  Typically, the JDBC driver default transaction isolation level
should be used.  Only use this property if you have specific isolation requirements that are
common for all queries, otherwise simply set the isolation level manually when creating or
preparing statements.  The value of this property is the constant name from the ``Connection``
class such as ``TRANSACTION_READ_COMMITTED``, ``TRANSACTION_REPEATABLE_READ``, etc.  *Default: driver default*

:abc:``catalog``<br/>
This property sets the default *catalog* for databases that support the concept of catalogs.
If this property is not specified, the default catalog defined by the JDBC driver is used.
*Default: driver default*

:watch:``connectionTimeout``<br/>
This property controls the maximum number of milliseconds that a client (that's you) will wait
for a connection from the pool.  If this time is exceeded without a connection becoming
available, a SQLException will be thrown.  100ms is the minimum value.  *Default: 30000 (30 seconds)*

:watch:``idleTimeout``<br/>
This property controls the maximum amount of time (in milliseconds) that a connection is
allowed to sit idle in the pool.  Whether a connection is retired as idle or not is subject
to a maximum variation of +30 seconds, and average variation of +15 seconds.  A connection
will never be retired as idle *before* this timeout.  A value of 0 means that idle connections
are never removed from the pool.  *Default: 600000 (10 minutes)*

:watch:``maxLifetime``<br/>
This property controls the maximum lifetime of a connection in the pool.  When a connection
reaches this timeout, even if recently used, it will be retired from the pool.  An in-use
connection will never be retired, only when it is idle will it be removed.  We strongly
recommend setting this value, and using something reasonable like 30 minutes or 1 hour.  A
value of 0 indicates no maximum lifetime (infinite lifetime), subject of course to the
``idleTimeout`` setting.  *Default: 1800000 (30 minutes)*

:watch:``leakDetectionThreshold``<br/>
This property controls the amount of time that a connection can be out of the pool before a
message is logged indicating a possible connection leak.  A value of 0 means leak detection
is disabled.  Lowest acceptable value for enabling leak detection is 10000 (10 secs). *Default: 0*

:negative_squared_cross_mark:``initializationFailFast``<br/>
This property controls whether the pool will "fail fast" if the pool cannot be seeded with
initial connections successfully.  If connections cannot be created at pool startup time,
a ``RuntimeException`` will be thrown from the ``HikariDataSource`` constructor.  This
property has no effect if ``minimumIdle`` is 0.  *Default: false*

:white_check_mark:``jdbc4ConnectionTest``<br/>
This property is a boolean value that determines whether the JDBC4 Connection.isValid() method
is used to check that a connection is still alive.  This value is mutually exclusive with the
``connectionTestQuery`` property, and this method of testing connection validity should be
preferred if supported by the JDBC driver.  *Default: true*

:abc:``connectionTestQuery``<br/>
This is for "legacy" databases that do not support the JDBC4 Connection.isValid() API.  This
is the query that will be executed just before a connection is given to you from the pool to
validate that the connection to the database is still alive.  **If your drvier supports JDBC4
we strongly recommend not setting this property.**  See the ``jdbc4ConnectionTest`` property
for a more efficent alive test.  One of either this property or ``jdbc4ConnectionTest`` must
be specified.  *Default: none*

:abc:``connectionInitSql``<br/>
This property sets a SQL statement that will be executed after every new connection creation
before adding it to the pool. If this SQL is not valid or throws an exception, it will be
treated as a connection failure and the standard retry logic will be followed.  *Default: none*

:abc:``dataSourceClassName``<br/>
This is the name of the ``DataSource`` class provided by the JDBC driver.  Consult the
documentation for your specific JDBC driver to get this class name, or see the [table](https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names) below.
Note XA data sources are not supported.  XA requires a real transaction manager like
[bitronix](https://github.com/bitronix/btm). Note that you do not need this property if you are using
``driverClassName`` to wrap an old-school DriverManager-based JDBC driver.  The HikariCP team
considers ``dataSourceClassName`` to be a superior method of creating connections compared to
``driverClassName``. *Default: none*

:abc:``driverClassName``<br/>
This property allows HikariCP to wrap an old-school JDBC driver as a ``javax.sql.DataSource``.
It is unnecessary when using the ``dataSourceClassName`` property, which is the preferred way
of creating connections in HikariCP.  DataSources are provided by all but the oldest JDBC drivers.
If ``driverClassName`` is used, then the ``jdbcUrl`` property must also be set. *Default: none*

:abc:``jdbcUrl``<br/>
This property is only used when the ``driverClassName`` property is used to wrap an old-school
JDBC driver as a ``javax.sql.DataSource``.  While JBDC URLs are popular, HikariCP does not
recommend using them.  The *DataSource* implementation provided by your driver provides bean
properties for all the driver parameters that used to be specified in the JDBC URL. Before using
the ``jdbcUrl`` and ``driverClassName`` because that's the way you've always done it, consider
using the more modern and maintainable ``dataSourceClassName`` approach instead.  Note that if
this property is used, you may still use *DataSource* properties to configure your driver and
is in fact recommended.  *Default: none*

:hash:``minimumIdle``<br/>
This property controls the minimum number of *idle connections* that HikariCP tries to maintain
in the pool.  If the idle connections dip below this value, HikariCP will make a best effort to
add additional connections quickly and efficiently.  However, for maximum performance and
responsiveness to spike demands, we recommend *not* setting this value and instead allowing
HikariCP to act as a *fixed size* connection pool.  *Default: same as maximumPoolSize*

:hash:``maximumPoolSize``<br/>
This property controls the maximum size that the pool is allowed to reach, including both
idle and in-use connections.  Basically this value will determine the maximum number of
actual connections to the database backend.  A reasonable value for this is best determined
by your execution environment.  When the pool reaches this size, and no idle connections are
available, calls to getConnection() will block for up to ``connectionTimeout`` milliseconds
before timing out.  *Default: 10*

:abc:``username``<br/>
This property sets the default authentication username used when obtaining *Connections* from
the underlying driver.  Note that for DataSources this works in a very deterministic fashion by
calling ``DataSource.getConnection(*username*, password)`` on the underlying DataSource.  However,
for Driver-based configurations, every driver is different.  In the case of Driver-based, HikariCP
will use this ``username`` property to set a ``user`` property in the ``Properties`` passed to the
driver's ``DriverManager.getConnection(jdbcUrl, props)`` call.  If this is not what you need,
skip this method entirely and call ``addDataSourceProperty("username", ...)``, for example.
*Default: none*

:abc:``password``<br/>
This property sets the default authentication password used when obtaining *Connections* from
the underlying driver. Note that for DataSources this works in a very deterministic fashion by
calling ``DataSource.getConnection(username, *password*)`` on the underlying DataSource.  However,
for Driver-based configurations, every driver is different.  In the case of Driver-based, HikariCP
will use this ``password`` property to set a ``password`` property in the ``Properties`` passed to the
driver's ``DriverManager.getConnection(jdbcUrl, props)`` call.  If this is not what you need,
skip this method entirely and call ``addDataSourceProperty("pass", ...)``, for example.
*Default: none*

:abc:``poolName``<br/>
This property represents a user-defined name for the connection pool and appears mainly
in logging and JMX management consoles to identify pools and pool configurations.  *Default: auto-generated*

:negative_squared_cross_mark:``registerMbeans``<br/>
This property controls whether or not JMX Management Beans ("MBeans") are registered or not.
*Default: false*

##### Infrequently used

:arrow_right:``dataSource``<br/>
This property is only available via programmatic configuration.  This property allows you
to directly set the instance of the ``DataSource`` to be wrapped by the pool, rather than
having HikariCP construct it via reflection.  This can be useful in some dependency injection
frameworks. When this property is specified, the ``dataSourceClassName`` property and all
DataSource-specific properties will be ignored.
*Default: none*

:abc: ``connectionCustomizerClassName``<br/>
This property allows you to specify an implementation of the ``IConnectionCustomizer`` interface.  The
``customize(Connection)`` method will be invoked on each new connection *before* it is added to the
pool.

:negative_squared_cross_mark:``isolateInternalQueries``<br/>
This property determines whether HikariCP isolates internal pool queries, such as the
connection alive test, in their own transaction.  Since these are typically read-only
queries, it is rarely necessary to encapsulate them in their own transaction.  This
property only applies if ``autoCommit`` is disabled.  *Default: false*


***Missing Knobs***<br/>
HikariCP has plenty of "knobs" to turn as you can see above, but comparatively less than some other pools.
This is a design philosophy.  The HikariCP design asthetic is Minimalism.  In keeping with the
*simple is better* or *less is more* design philosophy, some knobs are intentionally left out.  Here are two,
and the rationale.

**Statement Cache**<br/>
Most major database JDBC drivers already have a Statement cache that can be configured
([MySQL](https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration), PostgreSQL, Derby, etc).
A statement cache in the pool would add unneeded weight and no additional functionality.  It is simply
unnecessary with modern database drivers to implement a cache at the pool level.

**Log Statement Text / Slow Query Logging**<br/>
Like Statement caching, most major database vendors support statement logging through
properties of their own driver.  This includes Oracle, MySQL, Derby, MSSQL, and others.  Some
even support slow query logging. We consider this a "development-time" feature.  For those few
databases that do not support it, [jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/) is
a good option.  Great stuff during development and pre-Production.

----------------------------------------------------

### Initialization

You can use the ``HikariConfig`` class like so:
```java
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(100);
config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
config.addDataSourceProperty("serverName", "localhost");
config.addDataSourceProperty("port", "3306");
config.addDataSourceProperty("databaseName", "mydb");
config.addDataSourceProperty("user", "bart");
config.addDataSourceProperty("password", "51mp50n");

HikariDataSource ds = new HikariDataSource(config);
```
or directly instantiate a ``HikariDataSource`` like so:
```java
HikariDataSource ds = new HikariDataSource();
ds.setMaximumPoolSize(100);
ds.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
ds.addDataSourceProperty("serverName", "localhost");
ds.addDataSourceProperty("port", "3306");
ds.addDataSourceProperty("databaseName", "mydb");
ds.addDataSourceProperty("user", "bart");
ds.addDataSourceProperty("password", "51mp50n");
```
or property file based:
```java
HikariConfig config = new HikariConfig("some/path/hikari.properties");
HikariDataSource ds = new HikariDataSource(config);
```
Example property file:
```ini
connectionTestQuery=SELECT 1
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.user=test
dataSource.password=test
dataSource.databaseName=mydb
dataSource.serverName=localhost
```
or ``java.util.Properties`` based:
```java
Properties props = new Properties();
props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
props.setProperty("dataSource.user", "test");
props.setProperty("dataSource.password", "test");
props.setProperty("dataSource.databaseName", "mydb");
props.setProperty("dataSource.logWriter", new PrintWriter(System.out));

HikariConfig config = new HikariConfig(props);
HikariDataSource ds = new HikariDataSource(config);
```

There is also a System property available, ``hikaricp.configurationFile``, that can be used to specify the
location of a properties file.  If you intend to use this option, construct a ``HikariConfig`` or ``HikariDataSource``
instance using the default constructor and the properties file will be loaded.

#### HikariConfig vs. HikariDataSource
The advantage of configuring via ``HikariConfig`` over ``HikariDataSource`` is that when using the ``HikariConfig`` we know at ``DataSource`` construction-time what the configuration is, so the pool can be initialized at that point.  However, when using ``HikariDataSource`` alone, we don't know that you are *done* configuring the DataSource until ``getConnection()`` is called.  In that case, ``getConnection()`` must perform an additional check to see if the pool has been initialized yet or not.  The cost (albeit small) of this check is incurred on every invocation of ``getConnection()`` in that case.

### Popular DataSource Class Names
We recommended using ``dataSourceClassName`` instead of ``driverClassName``/``jdbcUrl``, *but both are acceptable*.  *Note: Spring Boot auto-configuration users, you need to use ``driverClassName``/``jdbcUrl``-based configuration.*

Here is a list of JDBC *DataSource* classes for popular databases:

| Database         | Driver       | *DataSource* class |
|:---------------- |:------------ |:-------------------|
| Apache Derby     | Derby        | org.apache.derby.jdbc.ClientDataSource |
| Firebird         | Jaybird      | org.firebirdsql.pool.FBSimpleDataSource |
| IBM DB2          | DB2          | com.ibm.db2.jcc.DB2SimpleDataSource |
| H2               | H2           | org.h2.jdbcx.JdbcDataSource |
| HSQLDB           | HSQLDB       | org.hsqldb.jdbc.JDBCDataSource |
| MariaDB & MySQL  | MariaDB      | org.mariadb.jdbc.MySQLDataSource |
| MySQL            | Connector/J  | com.mysql.jdbc.jdbc2.optional.MysqlDataSource |
| MS SQL Server    | Microsoft    | com.microsoft.sqlserver.jdbc.SQLServerDataSource |
| Oracle           | Oracle       | oracle.jdbc.pool.OracleDataSource |
| PostgreSQL       | pgjdbc-ng    | com.impossibl.postgres.jdbc.PGDataSource |
| PostgreSQL       | PostgreSQL   | org.postgresql.ds.PGSimpleDataSource |
| SyBase           | jConnect     | com.sybase.jdbcx.SybDataSource |

### Play Framework Plugin

A new plugin has come up for the the Play framework; [play-hikaricp](http://edulify.github.io/play-hikaricp.edulify.com/).  If you're using the excellent Play framework,  your application deserves HikariCP.  Thanks Edulify Team!

### Clojure Wrapper

A new Clojure wrapper has been created by [tomekw](https://github.com/tomekw) and can be [found here](https://github.com/tomekw/hikari-cp).

----------------------------------------------------

### Support <sup><sup>:speech_balloon:</sup></sup>
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
 &#8658; Java 6 and above<br/>
 &#8658; Javassist 3.18.1+ library<br/>
 &#8658; slf4j library<br/>

### Contributions
Please perform changes and submit pull requests from the ``dev`` branch instead of ``master``.  Please set your editor to use spaces instead of tabs, and adhere to the apparent style of the code you are editing.  The ``dev`` branch is always more "current" than the ``master`` if you are looking to live life on the edge.

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/63472d76ad0d494e3c4d8fc4a13ea4ce "githalytics.com")](http://githalytics.com/brettwooldridge/HikariCP)
