## It's Faster. ##
There is nothing [faster](https://github.com/brettwooldridge/HikariCP/wiki/Benchmarks).  There is
nothing more [correct](https://github.com/brettwooldridge/HikariCP/wiki/Correctness).  HikariCP is a "zero-overhead"
production-quality connection pool.

Using a stub-JDBC (nop) implementation to isolate and measure the overhead of HikariCP, 60+ Million JDBC operations
were performed in *8ms* on a commodity PC.  The test below is a run with a "constrained" pool of 50 connections,
with comparison to BoneCP.

<a href="http://github.com/brettwooldridge/HikariCP/wiki/50Connection_MixedBench.png"><img src="http://github.com/brettwooldridge/HikariCP/wiki/50Connection_MixedBench.png" width="680"/></a>
The same test was run for an "unconstrained" pool, meaning that connections in the pool outnumber the number of
threads that are executing.  This should be the "best case" for a connection pool due to low contention.

<a href="http://github.com/brettwooldridge/HikariCP/wiki/Unconstrained_MixedBench.png"><img src="http://github.com/brettwooldridge/HikariCP/wiki/Unconstrained_MixedBench.png" width="680"/></a>

How does HikariCP stay flat?  And how does it achieve such incredible performance?  If you're really technically
minded, read [here](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).  Otherwise, just drop it
in and let your code run like its pants are on fire.

### Maven Respository ###
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>1.1.2</version>
        <scope>compile</scope>
    </dependency>

If you don't use programmatic initialization, you can change the *scope* to 'runtime'.

### Initialization ###
    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(100);
    config.setDataSourceClassName("com.mysql.jdbc.Driver");
    config.addDataSourceProperty("url", "jdbc:mysql://localhost/database");
    config.addDataSourceProperty("user", "bart");
    config.addDataSourceProperty("password", "51mp50n");

    HikariDataSource ds = new HikariDataSource(config);


or property file based:

    HikariConfig config = new HikariConfig("some/path/hikari.properties");
    HikariDataSource ds = new HikariDataSource(config);

You can find information about the [configuration properties here](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby).

### Support ###
Google discussion group [HikariCP here](https://groups.google.com/d/forum/hikari-cp).

### Requirements ###
* Oracle Java 7 and above
* Javassist 3.18.1+ library
* slf4j library
