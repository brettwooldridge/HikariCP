# Ludicrous speed, GO! #

One of the things I truly enjoy as a software engineer is the benchmark/think/tweak cycle. Figuring out where a bottleneck is and trying to eliminate it. Hitting a performance wall that makes you revisit assumptions, re-architect a component, or research alternate algorithms.

But more important to HikariCP than performance, is *reliability* and *simplicity*.

HikariCP has best-of-breed resilience in the face of network disruption, and each release has brought with it improved stability and consistency under load.  However, some of these reliability gains have come at the expense of performance.  Consequently, HikariCP 2.3.8 is roughly 20% slower than HikariCP 2.0.1.

In HikariCP 2.4.0, after many releases focused on reliability, *I wanted to regain our performance.*

## Sequential Consistency ##

HikariCP utilises a specialized collection called a ``ConcurrentBag``<sup>[code](https://github.com/brettwooldridge/HikariCP/blob/HikariCP-2.3.8/hikaricp-common/src/main/java/com/zaxxer/hikari/util/ConcurrentBag.java)</sup> to hold connections in the pool. And like all pools, we need to be able to signal waiting threads when connections are returned to the pool.  In versions prior to 2.4.0, ``ConcurrentBag`` utilised an implementation of  ``AbstractQueuedLongSynchronizer``<sup>[doc](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/AbstractQueuedLongSynchronizer.html)</sup> for wait/notify semantics.

``AbstractQueuedLongSynchronizer`` provides useful features like efficient FIFO thread queueing and parking/unparking.  Subclasses generally rely on the provided ``setState()``, ``getState()``, and ``compareAndSetState()`` methods, which are merely wrappers around an ``AtomicLong``, to implement their synchronization semantic.

The performance of HikariCP's ``AbstractQueuedLongSynchronizer`` implementation was fine, very good even, but the fact that ``AtomicLong`` [performs poorly under contention](https://issues.apache.org/jira/browse/HADOOP-5318) periodically surfaced in my brain.

I kept thinking "there must be some way to take advantage of Java 8's ``LongAdder``<sup>[doc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/LongAdder.html)</sup>". It's well known that ``LongAdder`` has much higher performance under contention, that is its *raison d'être*.  I won't bore you with [all of the particulars](http://psy-lob-saw.blogspot.jp/2013/06/java-concurrent-counters-by-numbers.html).

<a href="https://minddotout.wordpress.com/2013/05/11/java-8-concurrency-longadder/"><img src="images/longadderquad1.png" style="float: left;"/></a>
However, ``LongAdder`` is not generally considered a replacement for ``AtomicLong`` when it comes to anything related to synchronization.  The JavaDoc ominously states:

> This class is usually preferable to AtomicLong when multiple threads update a common
> sum that is used for purposes such as collecting statistics, *not for fine-grained
> synchronization control.*

This is because ``LongAdder`` is not [Sequentially Consistent](https://en.wikipedia.org/wiki/Sequential_consistency).

[*Except when it is.*](http://concurrencyfreaks.blogspot.jp/2013/09/longadder-is-not-sequentially-consistent.html)

It turns out that ``LongAdder`` *is* Sequentially Consistent *if* you stick to only the ``increment()`` and ``sum()`` methods.  That is to say, the value must monotonically increase.

It now seemed possible to create new ``LongAdder``-based wait/notify mechanism that substantially outperforms the previous, as long as we adhere to the SC constraints above.

``QueuedSequenceSynchronizer``<sup>[code](https://github.com/brettwooldridge/HikariCP/blob/dev/src/main/java/com/zaxxer/hikari/util/QueuedSequenceSynchronizer.java)</sup> is a mash-up of ``LongAdder`` and ``AbstractQueuedLongSynchronizer``, taking advantage of the performance of the former and the infrastructure of the later.  On Java 7 it falls back to ``AtomicLong``<sup>1</sup>, but on Java 8 ... *it's ludicrously fast.*

<sup><sup>1 </sup>*Unless DropWizard is present, in which case we use their ``LongAdder`` Java 7 backport.*</sup>

## Do you have a nanosecond? ##

Without much fanfare, I'll cut to the chase and stick with a simple before/after on my Core i7 (3770) 3.4GHz "Ivy Bridge" iMac.
<br>

  <a href="images/Hikari-2.4-vs-2.3.png"><img src="images/Hikari-2.4-vs-2.3.png"/></a>

Put another way, roundtrip times (``getConnection()``/``close()``) are now between 150-250 nanoseconds on commodity hardware.

As usual in our benchmarks, *"Unconstrained"* means that there are more available connections than threads.  And *"Constrained"* means that the number of threads outnumber connections 2:1.

Of course, the benchmark basically creates maximum contention (~20-50k calls *per millisecond*), so in production environments we would expect L2 cache-line invalidation to be less frequent (to put it mildly).

In the case of *unconstrained* access, the ``QueuedSequenceSynchronizer`` doesn't really come into play much.  The big win comes from the fact that released connections are merely incrementing a ``LongAdder`` in v2.4.0, compared to incrementing an ``AtomicLong`` in v2.3.x.

In the case of *constrained* access, the ``QueuedSequenceSynchronizer`` sees quite a bit more action.  My concern was that the necessity of calling ``LongAdder.sum()``, which is generally much slower than ``AtomicLong.get()``, would result in worse performance than v2.3.x instead of better.

However, this fear proved unfounded.  While the *constrained* performance is roughly half of the *unconstrained* in v2.4.0, it still amazingly beats the *unconstrained* performance of v2.3.x.

## Standard comparables

Stacking HikariCP 2.4.0 up against the usual pools in the benchmark suite...

<a href="images/HikariCP-bench-2.4.0.png"><img src="images/HikariCP-bench-2.4.0.png"/></a>

* One *Connection Cycle* is defined as single ``DataSource.getConnection()``/``Connection.close()``.
  * In *Unconstrained* benchmark, connections > threads.
  * In *Constrained* benchmark, threads > connections (2:1).
* One *Statement Cycle* is defined as single ``Connection.prepareStatement()``, ``Statement.execute()``, ``Statement.close()``.

<sup>
<sup>&#42;</sup> Versions: HikariCP 2.4.0, commons-dbcp2 2.1, Tomcat 8.0.23, Vibur 3.0, c3p0 0.9.5.1, Java 8u45 <br/>
<sup>&#42;</sup> Java options: -server -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xmx1096m <br/>
</sup>

## Scratching an itch

Having scratched my performance itch, at least for a while, I'll be turning my attention back to equally important tasks such as improving metrics reporting and performance bottleneck troubleshooting ability.

Thanks for reading.
