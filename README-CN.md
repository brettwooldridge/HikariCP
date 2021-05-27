<h1><img src="https://github.com/brettwooldridge/HikariCP/wiki/Hikari.png"> HikariCP<sup><sup>&nbsp;更快.</sup></sup><sub><sub><sup>Hi·ka·ri [hi·ka·'lē] &#40;<i>来源: 日语</i>): 光; 光线.</sup></sub></sub></h1><br>

[![][Build Status img]][Build Status]
[![][Coverage Status img]][Coverage Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

快速, 简单, 可靠。 HikariCP 是一个生产可用的“零开销”JDBC连接池.  库非常轻量，大小仅有 130Kb。 参考 [我们怎么做到的](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).

&nbsp;&nbsp;&nbsp;<sup>**"简单性是可靠性的先决条件."**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- *Edsger Dijkstra*</sup>

----------------------------------------------------

_Java 8 thru 11 maven artifact:_
```xml
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>3.3.1</version>
    </dependency>
```
_Java 7 maven artifact (*maintenance mode*):_
```xml
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP-java7</artifactId>
        <version>2.4.13</version>
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
或者 [从这里下载](http://search.maven.org/#search%7Cga%7C1%7Ccom.zaxxer.hikaricp).

----------------------------------------------------

##### JMH 微型基准测试 :checkered_flag:

微型基准测试是使用 [JMH 微型基准测试框架](http://openjdk.java.net/projects/code-tools/jmh/)创建，用于隔离和衡量连接池开销的测试程序。 
你可以自行checkout [HikariCP 基准测试项目细节](https://github.com/brettwooldridge/HikariCP-benchmark) 并查看或运行基准测试.

![](https://github.com/brettwooldridge/HikariCP/wiki/HikariCP-bench-2.6.0.png)

 * 一个 *Connection Cycle* 定义为一次 ``DataSource.getConnection()``/``Connection.close()``.
 * 一个 *Statement Cycle* 定义为一次 ``Connection.prepareStatement()``, ``Statement.execute()``, ``Statement.close()``.

<sup>
<sup>1</sup> 版本: HikariCP 2.6.0, commons-dbcp2 2.1.1, Tomcat 8.0.24, Vibur 16.1, c3p0 0.9.5.2, Java 8u111 <br/>
<sup>2</sup> Intel Core i7-3770 CPU @ 3.40GHz <br/>
<sup>3</sup> 非竞争测试基准: 32 线程数/32 连接数, 竞争测试基准: 32 线程数, 16 连接数 <br/>
<sup>4</sup> 当Apache Tomcat使用 <i>StatementFinalizer</i>时 <a href="https://raw.githubusercontent.com/wiki/brettwooldridge/HikariCP/markdown/Tomcat-Statement-Failure.md">因为太多的垃圾回收时间</a>无法完成语句基准测试<br/>
<sup>5</sup> Apache DBCP<a href="https://raw.githubusercontent.com/wiki/brettwooldridge/HikariCP/markdown/Dbcp2-Statement-Failure.md">因为过多的垃圾回收时间</a>无法完成语句基准测试 
</sup>

----------------------------------------------------
#### 分析 :microscope:

#### 尖峰需求连接池池比较
<a href="https://github.com/brettwooldridge/HikariCP/blob/dev/documents/Welcome-To-The-Jungle.md"><img width="400" align="right" src="https://github.com/brettwooldridge/HikariCP/wiki/Spike-Hikari.png"></a>
HikariCP v2.6和其他连接池关于独特的尖峰需求负载的分析。

用户的环境带来了获取新连接的高成本和动态大小池的需求, 但是还需要响应请求峰值。  从 [这里](https://github.com/brettwooldridge/HikariCP/blob/dev/documents/Welcome-To-The-Jungle.md)查看关于尖峰需求处理的信息。
<br/>
<br/>
#### 你 [可能] 做错了。
<a href=""><img width="200" align="right" src="https://github.com/brettwooldridge/HikariCP/wiki/Postgres_Chart.png"></a>
AKA *"你可能对连接池大小调整不清楚"*.  观看这个来自Oracle Real-world Performance 组的视频，了解为什么连接池不需要调整的像往常一样大。  实际上，超大连接池对性能有明显的可证明的*负面*影响; 在Oracle的这个演示中，差异为50倍.  [更多信息参考](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).
<br/>
#### WIX 工程分析
<a href="https://www.wix.engineering/blog/how-does-hikaricp-compare-to-other-connection-pools"><img width="180" align="left" src="https://github.com/brettwooldridge/HikariCP/wiki/Wix-Engineering.png"></a>
我们要感谢WIX上的人们在他们的[工程博客](https://www.wix.engineering/blog/how-does-hikaricp-compare-to-other-connection-pools)上对HikariCP自发和深入的文章。如果你有时间可以看一下。
<br/>
<br/>
<br/>
#### 失败: 连接池表现很差
阅读我们有趣的 ["数据库宕机" 连接池挑战](https://github.com/brettwooldridge/HikariCP/wiki/Bad-Behavior:-Handling-Database-Down).

----------------------------------------------------
#### "模仿是最狡猾的形式" - <sub><sup>anonymous</sup></sub>
像任何产品一样，像HikariCP这样在免费市场上竞争的开源软件。我们明白。我们知道一旦公开产品优势通常会被选中。并且我们知道，时代精神可以同时独立的产生想法。但特别是开源项目中的时间表也很明确，我们希望用户了解我们领域的创新流程。看到数百小时的思想和研究结果如此容易地加入，这可能令人沮丧，也许这是自由市场所固有的，但我们并没有士气低落。 *我们很有动力去扩大差距*。

----------------------------------------------------
##### 用户推荐

[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet3.png)](https://twitter.com/jkuipers)<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet1.png)](https://twitter.com/steve_objectify)<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet2.png)](https://twitter.com/brettemeyer)<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/tweet4.png)](https://twitter.com/dgomesbr/status/527521925401419776)

------------------------------
#### 配置 (旋钮, 宝贝!)
HikariCP提供了*合理的*的默认设置，在大多数部署中都表现良好，无需额外调整。**除了下面标明为“要点”的项外，每个配置项都是可选的**。

<sup>&#128206;</sup>&nbsp;*HikariCP 使用毫秒来表示所有时间值。*

&#128680;&nbsp;HikariCP HikariCP依靠精确的计时器来提高性能和可靠性。你的服务器跟一个时间源比如：NT服务进行同步是*必需*的。 *特别是*当你的服务器运行在一个虚拟机时。  为什么? [查看这里了解更多](https://dba.stackexchange.com/a/171020). **不要依赖虚拟机管理程序来设置“同步”虚拟机的时间，应当在虚拟机内配置时间同步源。**   如果你因没有时间同步导致的问题在网上寻求帮助，你会在Twitter上被公开嘲笑。

##### 核心配置

&#128288;``dataSourceClassName``<br/>
这是JDBC驱动提供的``DataSource``类名，请参阅特定JDBC驱动程序的文档以获取此类名，
或参见[下表](https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names) 。
注意不支持XA数据源。XA需要像[bitronix](https://github.com/bitronix/btm)这样的真实事务管理器 。
请注意，如果您使用的``jdbcUrl`` 是“老套的”基于DriverManager的JDBC驱动程序配置，则不需要此属性 。 
*默认值：无*

*- 或者 -*

&#128288;``jdbcUrl``<br/>
这个属性指示HikariCP使用“基于DriverManager的”配置。
我们认为（上面）基于DataSource的配置由于各种原因（见下文）更优越，但对于许多部署而言，几乎没有显著差异。 
**将此属性与“旧”驱动程序一起使用时，您可能还需要设置该driverClassName属性，但首先尝试下不使用该属性** 。 
请注意，如果使用此属性，您仍可以使用*DataSource*属性来配置驱动程序，实际上建议使用URL本身中指定的驱动程序参数。
*默认值：无*

&#128288;``username``<br/>
这个属性设置从基础驱动程序获取*Connections*时使用的默认身份验证用户名。
请注意，对于DataSources，它通过底层DataSource调用``DataSource.getConnection(*username*, password)``以非常确定的方式工作。
但是，对于基于驱动程序的配置，每个驱动程序都不同。在基于驱动程序的情况下，HikariCP将使用``username``设置
传递给``DriverManager.getConnection(jdbcUrl, props)``的调用中 ``Properties`` 的``user``属性。
如果这不是您所需要的，请完全跳过此方法并调用比如：``addDataSourceProperty("username", ...)``。
*默认值：无*

&#128288;``password``<br/>
这个属性设置从基础驱动程序获取*Connections*时使用的默认验证密码。
请注意，对于DataSources，它通过底层DataSource调用``DataSource.getConnection(username, *password*)``以非常确定的方式工作。
但是，对于基于驱动程序的配置，每个驱动程序都不同。在基于驱动程序的情况下，HikariCP将使用``password``设置
传递给``DriverManager.getConnection(jdbcUrl, props)``的调用中 ``Properties`` 的``password``属性。
如果这不是您所需要的，请完全跳过此方法并调用比如：``addDataSourceProperty("pass", ...)``。 
*默认值：无*

##### 常用配置

&#9989;``autoCommit``<br/>
这个属性控制从池返回的连接的默认自动提交行为。它是一个布尔值。 *默认值：true*

&#8986;``connectionTimeout``<br/>
这个属性控制如果在没有连接可用的情况下，客户端（即您）等待从池中获取连接的最大毫秒数。
超过此时间还没有获得可用连接，则将抛出SQLException。
最低可接受的连接超时为250毫秒。 *默认值：30000（30秒）*

&#8986;``idleTimeout``<br/>
这个属性控制池中连接允许空闲的最长时间。 **此设置仅在``minimumIdle``定义为小于``maximumPoolSize``时才适用**。
一旦池连接数达到``minimumIdle``， 空闲连接将*不再*释放。连接是否空闲退出的最大变化为+30秒，平均变化为+15秒。
在这个超时*之前*，连接永远不会作为空闲连接释放。值为0表示空闲连接永远不会从池中释放。
允许的最小值为10000毫秒（10秒）。 *默认值：600000（10分钟）*

&#8986;``maxLifetime``<br/>
这个属性控制池中连接的最长生命周期。使用中的连接永远不会释放，只有当它关闭时才会被删除。
池中连接会被逐个释放以避免锐减。 
**我们强烈建议设置此值，并且它应比任何数据库或infrastructure imposed的连接时间限制短几秒**。 
值为0表示没有最大寿命（无限寿命），当然受限制于``idleTimeout``的配置。 &默认值：1800000（30分钟）*

&#128288;``connectionTestQuery``<br/>
**如果您的驱动程序支持JDBC4，我们强烈建议您不要设置这个属性**。
这适用于不支持JDBC4 ``Connection.isValid() API``的“遗留”驱动程序。
这是一个在从池中给出连接之前执行的查询，以验证与数据库的连接是否仍然存在。
**再次强调，先尝试运行没有配置此属性的连接池，如果您的驱动程序不符合JDBC4，HikariCP将记录错误以通知您**。 
*默认值：无*

&#128290;``minimumIdle``<br/>
这个属性控制HikariCP可以在池中维护的最小空闲连接数。
如果空闲连接低于此值并且池中的总连接数小于``maximumPoolSize``，则HikariCP将尽最大努力快速有效地添加新的连接。
但是，为了获得最高性能和对峰值需求的响应，我们建议*不要*设置此值，而是允许HikariCP充当*固定大小*的连接池。 
*默认值：与maximumPoolSize相同*

&#128290;``maximumPoolSize``<br/>
这个属性控制连接池连接数达到的最大值，包括空闲和正在使用的连接。基本上，此值将确定后端数据库的最大实际连接数。
对此的合理值最好由您的执行环境决定。当池达到此大小且没有空闲连接可用时，对getConnection（）的调用将在超时前阻塞最多``connectionTimeout`` 毫秒。
请参考[有关连接池大小](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)。 
*默认值：10*

&#128200;``metricRegistry``<br/>
这个属性仅可通过编程配置或IoC容器获得。
此属性允许您指定连接池池用来记录各种度量标准的*Codahale/Dropwizard* ``MetricRegistry``实例。
有关详细信息，请参考[Metrics](https://github.com/brettwooldridge/HikariCP/wiki/Dropwizard-Metrics) wiki页面。
*默认值：无*

&#128200;``healthCheckRegistry``<br/>
这个属性仅可通过编程配置或IoC容器获得。
此属性允许您指定连接池用于报告当前的健康信息的 *Codahale/Dropwizard* ``HealthCheckRegistry``示例。
有关详细信息，请参考[Health Checks](https://github.com/brettwooldridge/HikariCP/wiki/Dropwizard-HealthChecks) wiki页面。 
*默认值：无*

&#128288;``poolName``<br/>
这个属性表示用户定义的连接池名称，主要显示在日志记录和JMX管理控制台中，以标识池和池配置。 
*默认值：自动生成*

##### 不经常使用

&#8986;``initializationFailTimeout``<br/>
这个属性控制如果无法成功初始化连接，则池是否“快速失败”。
任何正数都被认为是尝试获取初始连接的毫秒数; 在此期间，应用程序线程将被阻塞。如果在超时前无法获取连接，将引发异常。
此超时在``connectionTimeout``*之后*应用。如果值为零（0），HikariCP将尝试获取并验证连接。如果获得连接但验证失败，将引发异常并且连接池池不会启动。
但是，如果无法获得连接，则连接池池将启动，但稍后获取连接的尝试可能会失败。
小于零的值连接池将跳过尝试初始化连接立即启动，并且将在后台尝试获取连接。
因此，稍后获得连接的尝试可能失败。 
*默认值：1*

&#10062;``isolateInternalQueries``<br/>
这个属性确定HikariCP是否在其自己的事务中隔离内部池查询，例如连接探活测试。
由于这些通常是只读查询，因此很少需要将它们封装在自己的事务中。
此属性仅在``autoCommit``禁用时适用。 
*默认值：false*

&#10062;``allowPoolSuspension``<br/>
这个属性控制是否可以通过JMX挂起和恢复连接池。这对某些故障自动转移方案很有用。
当连接池挂起时，调用 ``getConnection()``将*不会*超时并将一直保持到池恢复为止。 
*默认值：false*


&#10062;``readOnly``<br/>
这个属性控制默认情况下从池中获取的*Connections*是否处于只读模式。
请注意，某些数据库不支持只读模式的概念，而其他数据库在*Connection*设置为只读时提供查询优化。
您是否需要此属性在很大程度上取决于您的应用程序和数据库。 
*默认值：false*

&#10062;``registerMbeans``<br/>
这个属性控制是否注册JMX管理Bean（“MBean”）。 
*默认值：false*

&#128288;``catalog``<br/>
这个属性为支持目录的概念的数据库设置默认目录。如果未指定此属性，则使用JDBC驱动程序定义的缺省目录。 
*默认值：驱动程序默认*

&#128288;``connectionInitSql``<br/>
这个属性设置一个SQL语句，用于每次创建新连接之后执行，然后再将其添加到池中。如果此SQL无效或引发异常，则将其视为连接失败，并将遵循标准重试逻辑。 
*默认值：无*

&#128288;``driverClassName``<br/>
HikariCP将尝试通过驱动程序管理器仅基于``jdbcUrl``来解析驱动程序，但对于某些较老的驱动程序，还必须指定``driverClassName``。
除非您收到明显的错误消息，指出未找到驱动程序，否则请忽略此属性。 
*默认值：无*

&#128288;``transactionIsolation``<br/>
这个属性控制从连接池返回连接的默认事务隔离级别。
如果未指定此属性，则使用JDBC驱动程序定义的缺省事务隔离级别。
仅当您所有查询都适用特定隔离需求时才使用此属性。
此属性的值是``Connection``类的常量，比如``TRANSACTION_READ_COMMITTED``, ``TRANSACTION_REPEATABLE_READ``等 
*默认值：驱动程序默认*

&#8986;``validationTimeout``<br/>
这个属性控制测试连接是否存活的最长时间。该值必须小于``connectionTimeout``。可接受的最小验证超时时长为250毫秒。 
*默认值：5000*

&#8986;``leakDetectionThreshold``<br/>
这个属性控制一个连接从连接池取出的最大时长，超时则通过日志记录可能出现连接泄漏。值为0表示禁用泄漏检测。
启用泄漏检测的可接受值最小是2000（2秒）。 
*默认值：0*

&#10145;``dataSource``<br/>
这个属性仅可通过编程配置或IoC容器获得。此属性允许您直接设置由连接池包装的``DataSource``的实例，而不是让HikariCP通过反射构造。
这在一些依赖注入框架中很有用。指定此属性后，``dataSourceClassName``属性和所有特定于DataSource的属性将被忽略。 
*默认值：无*

&#128288;``schema``<br/>
这个属性设置支持模式概念数据库的*schema*。如果未指定此属性，则使用JDBC驱动程序定义的默认schema。 
*默认值：驱动程序默认*

&#10145;``threadFactory``<br/>
这个属性仅可通过编程配置或IoC容器获得。此属性允许您设置用于创建连接池使用的所有线程的``java.util.concurrent.ThreadFactory``实例。
在某些只能通过由应用程序容器提供的``ThreadFactory``创建线程的受限执行环境中会需要它。 
*默认值：无*

&#10145;``scheduledExecutor``<br/>
这个属性仅可通过编程配置或IoC容器获得。此属性允许您设置用于各种内部计划任务的``java.util.concurrent.ScheduledExecutorService``实例。
如果向HikariCP提供``ScheduledThreadPoolExecutor``实例，建议同时使用``setRemoveOnCancelPolicy(true)``。 
*默认值：无*

----------------------------------------------------

#### 丢失的配置

正如你在上面看到的那样，HikariCP有很多“旋钮”，但比其他一些连接池要少。
这是一种设计理念。HikariCP设计美学是极简主义。为了保持*简即好*或*少即多*的设计理念，有些配置项是故意遗漏的。

#### 语句缓存

许多连接池，包括Apache DBCP，Vibur，c3p0和其他连接池都提供``PreparedStatement``缓存。HikariCP没有。为什么？

在连接池层，``PreparedStatements``只能按照*每个连接*缓存。
如果您的应用程序有250个常用查询和20个连接的池，那么您要求数据库保留5000个查询执行计划 - 同样地，连接池也必须缓存这么多``PreparedStatements``及其相关的对象图。

大多数流行的数据库JDBC驱动程序已经具有可以配置的Statement缓存，包括PostgreSQL，Oracle，Derby，MySQL，DB2等等。
JDBC驱动程序处于利用数据库特定功能的独特位置，几乎所有缓存实现都能*够跨连接*共享执行计划。
这意味着，250个常用的查询会在数据库中生成250个执行计划，而不是内存中的5000个语句和相关的执行计划。
聪明的实现甚至不会在驱动程序级别的内存中保留``PreparedStatement``对象，而只是将新实例附加到现有的计划ID。

在连接池层使用语句缓存是一种[反模式](https://en.wikipedia.org/wiki/Anti-pattern)，与驱动程序提供的缓存相比，会对应用程序性能产生负面影响。

#### SQL日志记录/慢查询日志记录

与Statement缓存一样，大多数流行数据库供应商都支持通过自己的驱动程序属性进行语句记录，这包括Oracle，MySQL，Derby，MSSQL等。
有些甚至支持慢查询记录。对于那些不支持的少数数据库，有几个选项可用。
我们收到过[一份报告称p6spy运行良好](https://github.com/brettwooldridge/HikariCP/issues/57#issuecomment-354647631)，
并且还注意到[log4jdbc](https://github.com/arthurblake/log4jdbc) 和 [jdbcdslog-exp](https://code.google.com/p/jdbcdslog-exp/)的可用性。

#### 快速恢复
有关如何配置驱动程序和系统以从数据库重启和网络分区事件中正确恢复的详细信息，请阅读[快速恢复指南](https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery)。

----------------------------------------------------

### 初始化

你可以像这样使用类 ``HikariConfig`` <sup>1</sup>:
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
&nbsp;<sup><sup>1</sup> 特定于MySQL的示例，请不要一 字 不 差的复制。</sup>

或者像这样直接实例化一个 ``HikariDataSource`` :
```java
HikariDataSource ds = new HikariDataSource();
ds.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
ds.setUsername("bart");
ds.setPassword("51mp50n");
...
```
或者基于property文件:
```java
// Examines both filesystem and classpath for .properties file
HikariConfig config = new HikariConfig("/some/path/hikari.properties");
HikariDataSource ds = new HikariDataSource(config);
```
示例property文件:
```ini
dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
dataSource.user=test
dataSource.password=test
dataSource.databaseName=mydb
dataSource.portNumber=5432
dataSource.serverName=localhost
```
或者基于 ``java.util.Properties`` :
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

还有一个可用的系统属性：``hikaricp.configurationFile``，用于指定属性文件的位置。
如果您打算使用此选项，请使用默认构造函数创建``HikariConfig`` 或者 ``HikariDataSource``实例，这个属性文件将被加载。

### 性能Tips
[MySQL性能Tips](https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration)

###  常用的DataSource类名

我们建议使用``dataSourceClassName`` 而不是``jdbcUrl``，但是，我们再说一遍，*任何一种都是是可以接受的*。
&#9888;&nbsp;*注意：使用Spring Boot自动配置的用户，需要使用基于``jdbcUrl``的配置。*

&#9888;&nbsp;已知MySQL数据源在网络超时支持方面被打破。请改用``jdbcUrl``配置。

以下是常用数据库的JDBC *DataSource*类列表：

| 数据库         | 驱动       | *DataSource* 类 |
|:---------------- |:------------ |:-------------------|
| Apache Derby     | Derby        | org.apache.derby.jdbc.ClientDataSource |
| Firebird         | Jaybird      | org.firebirdsql.ds.FBSimpleDataSource |
| H2               | H2           | org.h2.jdbcx.JdbcDataSource |
| HSQLDB           | HSQLDB       | org.hsqldb.jdbc.JDBCDataSource |
| IBM DB2          | IBM JCC      | com.ibm.db2.jcc.DB2SimpleDataSource |
| IBM Informix     | IBM Informix | com.informix.jdbcx.IfxDataSource |
| MS SQL Server    | Microsoft    | com.microsoft.sqlserver.jdbc.SQLServerDataSource |
| ~~MySQL~~        | Connector/J  | ~~com.mysql.jdbc.jdbc2.optional.MysqlDataSource~~ |
| MariaDB          | MariaDB      | org.mariadb.jdbc.MariaDbDataSource |
| Oracle           | Oracle       | oracle.jdbc.pool.OracleDataSource |
| OrientDB         | OrientDB     | com.orientechnologies.orient.jdbc.OrientDataSource |
| PostgreSQL       | pgjdbc-ng    | com.impossibl.postgres.jdbc.PGDataSource |
| PostgreSQL       | PostgreSQL   | org.postgresql.ds.PGSimpleDataSource |
| SAP MaxDB        | SAP          | com.sap.dbtech.jdbc.DriverSapDB |
| SQLite           | xerial       | org.sqlite.SQLiteDataSource |
| SyBase           | jConnect     | com.sybase.jdbc4.jdbc.SybDataSource |

### Play 框架插件

注意Play 2.4现在默认使用HikariCP。Play框架出了一个新的插件： [play-hikaricp](http://edulify.github.io/play-hikaricp.edulify.com/)。
如果您正在使用优秀的Play框架，您的应用程序应该使用HikariCP。谢谢Edulify团队！

### Clojure 包装器

由[tomekw](https://github.com/tomekw)创建了一个新的Clojure包装器，可以在[这里](https://github.com/tomekw/hikari-cp)找到。

### JRuby 包装器

[tomekw](https://github.com/tomekw)已经创建一个新的JRuby包装器，可以在[这里](https://github.com/tomekw/hucpa)找到

----------------------------------------------------

### Support <sup><sup>&#128172;</sup></sup>

HikariCP谷歌讨论组在这里[这里](https://groups.google.com/d/forum/hikari-cp)，增长中的[FAQ](https://github.com/brettwooldridge/HikariCP/wiki/FAQ)。

[![](https://raw.github.com/wiki/brettwooldridge/HikariCP/twitter.png)](https://twitter.com/share?text=Interesting%20JDBC%20Connection%20Pool&hashtags=HikariCP&url=https%3A%2F%2Fgithub.com%2Fbrettwooldridge%2FHikariCP)&nbsp;[![](https://raw.github.com/wiki/brettwooldridge/HikariCP/facebook.png)](http://www.facebook.com/plugins/like.php?href=https%3A%2F%2Fgithub.com%2Fbrettwooldridge%2FHikariCP&width&layout=standard&action=recommend&show_faces=true&share=false&height=80)

### Wiki

不要忘记 [Wiki](https://github.com/brettwooldridge/HikariCP/wiki) 上的额外信息，比如:
 * [FAQ](https://github.com/brettwooldridge/HikariCP/wiki/FAQ)
 * [Hibernate 4.x Configuration](https://github.com/brettwooldridge/HikariCP/wiki/Hibernate4)
 * [MySQL Configuration Tips](https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration)
 * 等等。

----------------------------------------------------

### 要求

 &#8658; Java 8+ (Java 6/7 已处于维护模式)<br/>
 &#8658; slf4j 类库<br/>

### 赞助商
高性能项目永远不会嫌工具多！我们要感谢以下公司：

感谢 [ej-technologies](https://www.ej-technologies.com) 出色的一体化分析器 [JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html).

YourKit通过其功能齐全的Java Profiler支持开源项目。点击下面的YourKit logo以了解更多信息。<br/>
[![](https://github.com/brettwooldridge/HikariCP/wiki/yklogo.png)](http://www.yourkit.com/java/profiler/index.jsp)<br/>


### Contributions

请从``dev``分支执行更改并提交拉取请求，而不是``master``分支。
请将编辑器设置为使用空格而不是制表符，并遵循您正在编辑的代码的明显样式。
如果您希望尝试新功能，``dev``分支总是比``master``分支更新 。

[Build Status]:https://travis-ci.org/brettwooldridge/HikariCP
[Build Status img]:https://travis-ci.org/brettwooldridge/HikariCP.svg?branch=dev

[Coverage Status]:https://codecov.io/gh/brettwooldridge/HikariCP
[Coverage Status img]:https://codecov.io/gh/brettwooldridge/HikariCP/branch/dev/graph/badge.svg

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/com.zaxxer/HikariCP
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/com.zaxxer/HikariCP/badge.svg

[Javadocs]:http://javadoc.io/doc/com.zaxxer/HikariCP
[Javadocs img]:http://javadoc.io/badge/com.zaxxer/HikariCP.svg
