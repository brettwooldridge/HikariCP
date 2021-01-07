<img width="340" height="240" align="left" src="https://github.com/brettwooldridge/HikariCP/wiki/welcome-to-the-jungle.jpg">
Microbenchmarks are great at measuring performance "in the small"; for example, measuring the performance of individual methods. But good results do not necessarily translate into macro-scale performance.  Real world access patterns and demand loads often run into deeper, systemic, architectural design issues that cannot be discerned at the micro level.<br><br>

HikariCP has over 1 million users, so from time to time we are approached with challenges encountered "in the wild".  Recently, one such challenge led to a deeper investigation: ***Spike Demand***.

### The Challenge
The user has an environment where connection creation is expensive, on the order of 150ms; and yet queries typically execute in ~2ms.  Long connection setup times can be the result of various factors, alone or in combination: DNS resolution times, encrypted connections with strong encryption (2048/4096 bit), external authentication, database server load, etc.

*Generally speaking, for the best performance in response to spike demands, HikariCP recommends a fixed-size pool.*

Unfortunately, the user's application is also in an environment where many other applications are connected to the same database, and therefore dynamically-sized pools are desirable -- where idle applications are allowed to give up some of their connections.  The user is running the application with HikariCP configured as ``minimumIdle=5``.

In this environment, the application has periods of quiet, as well as sudden spikes of requests, and periods of sustained activity.  The combination of high connection setup times, a dynamically-sized pool requirement, and spike demands is just about the worst case scenario for a connection pool.

The questions ultimately were these:

> * If the pool is sitting idle with 5 connections, and is suddenly hit with 50 requests, what should happen?
> * Given that a each new connection is going to take 150ms to establish, and given that each request can ultimately be satisfied in ~2ms, shouldn't even a single one of the idle connections be able to handle all the of the requests in ~100ms anyway?
> * So, why is the pool size growing [so much]?

We thought these were interesting questions, and HikariCP was indeed creating more connections than we expected...

### 3, 2, 1 ... Go!
In order to explore these questions, we built a simulation and started measuring.  The simulation harness [code is here](https://github.com/brettwooldridge/HikariCP-benchmark/blob/master/src/test/java/com/zaxxer/hikari/benchmark/SpikeLoadTest.java).

The constraints are simple:
 * Connection establishment takes 150ms.
 * Query execution takes 2ms.
 * The maximum pool size is 50.
 * The minimum idle connections is 5.

And the simulation is fairly simple:
 * Everything is quiet, and then ... Boom! ... 50 threads, at once, wanting a connection and to execute a query.
 * Take measurements every 250μs (microseconds).

### Results
After running HikariCP through the simulation, tweaking the code (ultimately a one-line change), and satisfying ourselves that the behavior is as we would wish, we ran a few other pools through the simulation.

The code was run as follows:
```
bash$ ./spiketest.sh 150 <pool> 50
```
Where ``150`` is the connection establishment time, ``<pool>`` is one of [*hikari*, *dbcp2*, *vibur*, *tomcat*, *c3p0*], and ``50`` is the number of threads/requests.  Note that *c3p0* was dropped from the analysis here, as its run time was ~120x that of HikariCP.

#### HikariCP (v2.6.0) <sub><sup><a href="https://github.com/brettwooldridge/HikariCP/wiki/Spike-Hikari-data.txt">raw data</a></sup></sub>

--------------------
[![](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Hikari.png)](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Hikari.png)

#### Apache DBCP (v2.1.1) <sub><sup><a href="https://github.com/brettwooldridge/HikariCP/wiki/Spike-DBCP2-data.txt">raw data</a></sup></sub>

--------------------
[![](https://github.com/brettwooldridge/HikariCP/wiki/Spike-DBCP2.png)](https://github.com/brettwooldridge/HikariCP/wiki/Spike-DBCP2.png)

#### Apache Tomcat (v8.0.24) <sub><sup><a href="https://github.com/brettwooldridge/HikariCP/wiki/Spike-Tomcat-data.txt">raw data</a></sup></sub>

--------------------
[![](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Tomcat.png)](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Tomcat.png)

#### Vibur DBCP (v16.1) <sub><sup><a href="https://github.com/brettwooldridge/HikariCP/wiki/Spike-Vibur-data.txt">raw data</a></sup></sub>

--------------------
[![](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Vibur.png)](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Vibur.png)

<sup>* Note that the times provided in the raw data is the number of microseconds (μs) since the start of the test. For graphing purposes, raw data for each pool was trimmed such that the first entry has 0 requests enqueued, and the last entry has all connections completed. </sup>

--------------------
### Apache DBCP vs HikariCP
:point_right: In case you missed the *time-scale* in the graphs above, here is a properly scaled comparable.

Apache DBCP on *top*, HikariCP on the *bottom*.

[![](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Compare.png)](https://github.com/brettwooldridge/HikariCP/wiki/Spike-Compare.png)

### Commentary
We'll start by saying that we are not going to comment on the implementation specifics of the other pools, but you may be able to draw inferences by our comments regarding HikariCP.

Looking at the HikariCP graph, we couldn't have wished for a better profile; it's about as close to perfect efficiency as we could expect.  It is interesting, though not surprising, that the other pool profiles are so similar to each other. Even though arrived at via different implementations, they are the result of a *conventional* or *obvious* approach to pool design.

HikariCP's profile in this case, and the reason for the difference observed between other pools, is the result of our Prime Directive:

💡 **User threads should only ever block on the** ***pool itself***.<sup>1</sup><br>
<img width="32px" src="https://github.com/brettwooldridge/HikariCP/wiki/space60x1.gif"><sub><sup>1</sup>&nbsp;to the greatest extent possible.</sub>

Consider this hypothetical scenario:
```
There is a pool with five connections in-use, and zero idle (available) connections. Then, a new thread
comes in requesting a connection.
```
"How does the prime directive apply in this case?"  We'll answer with a question of our own:

> If the thread is directed to create a new connection, and that connection takes 150ms to establish, what happens if one of the five in-use connections is returned to the pool?

---------------------
Both Apache DBCP2 and Vibur ended the run with 45 connections, Apache Tomcat (inexplicably) with 40 connections, while HikariCP ended the run with 5 (technically six, see below).  This has major and measurable effects for real world deployments.  That is 35-40 additional connections that are not available to other applications, and 35-40 additional threads and associated memory structures in the database.

We know what you are thinking, *"What if the load had been sustained?"*&nbsp;&nbsp;The answer is: HikariCP also would have ramped up.

In point of fact, as soon as the pool hit zero available connections, right around 800μs into the run, HikariCP began requesting connections to be added to the pool asynchronously.  If the metrics had continued to be collected past the end of the spike -- out beyond 150ms -- you would observe that an additional connection is indeed added to the pool.  But *only one*, because HikariCP employs *elision logic*; at that point HikariCP would also realize that there is actually no more pending demand, and the remaining connection acquisitions would be elided.

### Epilog
This scenario represents only *one* of many access patterns. HikariCP will continue to research *and innovate* when presented with challenging problems encountered in real world deployments.  As always, thank you for your patronage.
