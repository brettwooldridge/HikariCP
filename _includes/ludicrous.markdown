# Ludicrous speed, GO! #

What I truly enjoy as a software engineer is the profile/think/tweak cycle. Figuring out where a bottleneck is and trying to eliminate it. Hitting a performance wall that makes you revisit assumptions, re-architect a component, or research alternate algorithms.

But more important to HikariCP than performance, is *reliability* and *simplicity*.

HikariCP has best-of-breed resilience in the face of network disruption, and each release has brought with it improved stability and consistency under load.  However, some of these reliability gains have come at the expense of performance.  Consequently, HikariCP 2.3.8 is roughly 20% slower than HikariCP 2.0.1.

In HikariCP 2.4.0, after many releases focused on reliability, *I wanted to regain our performance.*

## Sequential Consistency ##

HikariCP utilises a specialized collection called a ``ConcurrentBag``<sup>[code](https://github.com/brettwooldridge/HikariCP/blob/HikariCP-2.3.8/hikaricp-common/src/main/java/com/zaxxer/hikari/util/ConcurrentBag.java)</sup> to hold connections in the pool. And like all pools, we need to be able to signal waiting threads when connections are returned to the pool.  In versions prior to 2.4.0, ``ConcurrentBag`` utilised a custom implementation ``AbstractQueuedLongSynchronizer``<sup>[doc](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/AbstractQueuedLongSynchronizer.html)</sup> for wait/notify semantics.

``AbstractQueuedLongSynchronizer`` provides useful features like efficient thread queueing and parking/unparking.  Subclasses generally rely on the provided ``setState()``, ``getState()``, and ``compareAndSetState()`` methods, which are merely wrappers around an ``AtomicLong`` instance.

The performance of HikariCP's ``AbstractQueuedLongSynchronizer`` implementation was fine, but the fact that ``AtomicLong`` [performs poorly under contention](https://issues.apache.org/jira/browse/HADOOP-5318) periodically surfaced in my brain, usually as drifted off to sleep.

There must be some way to take advantage of Java 8's ``LongAdder``<sup>[doc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/LongAdder.html)</sup>. It is well known that ``LongAdder`` has much higher performance under contention, that is its *raison d'être*.  I won't bore you with [all of the particulars](http://psy-lob-saw.blogspot.jp/2013/06/java-concurrent-counters-by-numbers.html).

<a href="https://minddotout.wordpress.com/2013/05/11/java-8-concurrency-longadder/"><img src="images/longadderquad1.png" style="float: left;"/></a>
However, ``LongAdder`` is not generally considered a *replacement* for ``AtomicLong``.  The JavaDoc ominously states:

> This class is usually preferable to AtomicLong when multiple threads update a common
> sum that is used for purposes such as collecting statistics, *not for fine-grained
> synchronization control.*

This is because ``LongAdder`` is not [Sequentially Consistent](https://en.wikipedia.org/wiki/Sequential_consistency).

[*Except when it is.*](http://concurrencyfreaks.blogspot.jp/2013/09/longadder-is-not-sequentially-consistent.html)

It turns out that ``LongAdder`` is Sequentially Consistent when only the ``increment()`` and ``sum()`` methods are used.  That is to say, the value must monotonically increase.

It now seemed possible to create new ``LongAdder``-based wait/notify mechanism that substantially outperforms the previous, as long as we adhere to the SC constraints above.

``QueuedSequenceSynchronizer``<sup>[code](https://github.com/brettwooldridge/HikariCP/blob/dev/src/main/java/com/zaxxer/hikari/util/QueuedSequenceSynchronizer.java)</sup> is a mash-up of ``LongAdder`` and ``AbstractQueuedLongSynchronizer``, taking advantage of the performance of the former and the infrastructure of the later.  On Java 7 it falls back to ``AtomicLong``, but on Java 8 ... *it's ludicrously fast.*

## Pretty Pictures ##
