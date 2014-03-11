## It's Faster. ##
There is nothing [faster](https://github.com/brettwooldridge/HikariCP/wiki/Benchmarks).  There is
nothing more [correct](https://github.com/brettwooldridge/HikariCP/wiki/Correctness).  HikariCP is a "zero-overhead"
production-quality connection pool.

Using a stub-JDBC implementation to isolate and measure the overhead of HikariCP, comparative benchmarks were
performed on a commodity PC.  The next fastest connection pool was BoneCP.

<a href="http://github.com/brettwooldridge/HikariCP/wiki/50Connection_MixedBench.png"><img src="http://github.com/brettwooldridge/HikariCP/wiki/50Connection_MixedBench.png" width="680"/></a>

And how does it achieve such incredible performance?  If you're really technically
minded, read [here](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).  Otherwise, just drop it
in and let your code run like its pants are on fire.

### Maven Respository ###

    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>1.3.3</version>
        <scope>compile</scope>
    </dependency>

### Initialization ###

    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(100);
    config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
    config.addDataSourceProperty("url", "jdbc:mysql://localhost/database");
    config.addDataSourceProperty("user", "bart");
    config.addDataSourceProperty("password", "51mp50n");

    DataSource ds = new HikariDataSource(config);

or property file-based:

    HikariConfig config = new HikariConfig("some/path/hikari.properties");
    DataSource ds = new HikariDataSource(config);

Example property file:

    acquireIncrement=3
    acquireRetryDelay=1000
    connectionTestQuery=SELECT 1
    dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
    dataSource.username=test
    dataSource.password=test
    dataSource.databaseName=mydb
    dataSource.serverName=localhost


You can find information about the [configuration properties here](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby).

### Support ###
Google discussion group [HikariCP here](https://groups.google.com/d/forum/hikari-cp).

### Requirements ###
* Java 7 and above
* Javassist 3.18.1+ library
* slf4j library
