## Ludicrous speed, GO! ##

It might be said that I love high-performance software.  While that might be true, I think what I really love is the profile/think/tweak cycle.  Figuring out where a bottleneck is and trying to eliminate it.  Hitting a performance wall that makes you revisit assumptions or re-architect a component.

But more important to HikariCP than performance is *reliability*.  HikariCP has best-of-breed resilience in the face of network disruption, and each release has brought with it improved reliability under load.  However, the costs of these reliability gains have been loses in performance.  HikariCP 2.3.8 is roughly 10-15% slower than release 2.0.1.
