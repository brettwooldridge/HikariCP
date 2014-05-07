## It's Faster. ##
There is nothing [faster](https://github.com/brettwooldridge/HikariCP/wiki/Benchmarks).  There is
nothing more [correct](https://github.com/brettwooldridge/HikariCP/wiki/Correctness).  HikariCP is a "zero-overhead"
production-quality connection pool.

Using a stub-JDBC implementation to isolate and measure the overhead of HikariCP, comparative benchmarks were
performed on a commodity PC.  The next fastest connection pool was BoneCP.

<a href="http://github.com/brettwooldridge/HikariCP/wiki/Benchmarks.png"><img src="http://github.com/brettwooldridge/HikariCP/wiki/Benchmarks.png" width="680"/></a>

And how does it achieve such incredible performance?  If you're really technically
minded, read [here](https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole).  Otherwise, just drop it
in and let your code run like its pants are on fire.

### Maven Repository ###

    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>1.3.8</version>
        <scope>compile</scope>
    </dependency>

### Initialization and Configuration###

See the [main project page](https://github.com/brettwooldridge/HikariCP#initialization) for initialization examples.

You can find information about the [configuration properties here](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby).

### Support ###
Google discussion group [HikariCP here](https://groups.google.com/d/forum/hikari-cp).

### Requirements ###
* Java 6 and above
* Javassist 3.18.1+ library
* slf4j library
