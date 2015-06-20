/*
 * Copyright (C) 2015 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.util;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

/**
 * A specialized wait/notify class useful for resource tracking through the
 * use of a monotonically-increasing long sequence.
 * <p>
 * When a shared resource becomes available the {@link #signal()} method should
 * be called unconditionally.
 * <p>
 * A thread wishing to acquire a shared resource should: <br>
 * <ul>
 *   <li>Obtain the current sequence from the {@link #currentSequence()} method </li>
 *   <li>Call {@link #waitUntilSequenceExceeded(long, long)} with that sequence.  </li>
 *   <li>Upon receiving a <code>true</code> result from {@link #waitUntilSequenceExceeded(long, long)},
 *       the current sequence should again be obtained from the {@link #currentSequence()} method,
 *       and an attempt to acquire the resource should be made. </li>
 *   <li>If the shared resource cannot be acquired, the thread should again call 
 *       {@link #waitUntilSequenceExceeded(long, long)} with the previously obtained sequence. </li>
 *   <li>If <code>false</code> is received from {@link #waitUntilSequenceExceeded(long, long)}
 *       then a timeout has occurred. </li>
 * </ul>
 * <p>
 * When running on Java 8 and above, this class leverages the fact that when {@link LongAdder}
 * is monotonically increasing, and only {@link LongAdder#increment()} and {@link LongAdder#sum()}
 * are used, it can be relied on to be Sequentially Consistent.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Sequential_consistency">Java Spec</a> 
 * @author Brett Wooldridge
 */
public final class QueuedSequenceSynchronizer
{
   private final Sequence sequence;
   private final Synchronizer synchronizer;

   /**
    * Default constructor
    */
   public QueuedSequenceSynchronizer()
   {
      this.synchronizer = new Synchronizer();
      this.sequence = Sequence.Factory.create();
   }

   /**
    * Signal any waiting threads.
    */
   public void signal()
   {
      synchronizer.releaseShared(1);
   }

   /**
    * Get the current sequence.
    *
    * @return the current sequence
    */
   public long currentSequence()
   {
      return sequence.get();
   }

   /**
    * Block the current thread until the current sequence exceeds the specified threshold, or
    * until the specified timeout is reached.
    * 
    * @param sequence the threshold the sequence must reach before this thread becomes unblocked
    * @param nanosTimeout a nanosecond timeout specifying the maximum time to wait
    * @return true if the threshold was reached, false if the wait timed out
    * @throws InterruptedException if the thread is interrupted while waiting
    */
   public boolean waitUntilSequenceExceeded(long sequence, long nanosTimeout) throws InterruptedException
   {
      return synchronizer.tryAcquireSharedNanos(sequence, nanosTimeout);
   }

   /**
    * Queries whether any threads are waiting to for the sequence to reach a particular threshold.
    *
    * @return true if there may be other threads waiting for a sequence threshold to be reached
    */
   public boolean hasQueuedThreads()
   {
      return synchronizer.hasQueuedThreads();
   }

   /**
    * Returns an estimate of the number of threads waiting for a sequence threshold to be reached. The
    * value is only an estimate because the number of threads may change dynamically while this method
    * traverses internal data structures. This method is designed for use in monitoring system state,
    * not for synchronization control.
    *
    * @return the estimated number of threads waiting for a sequence threshold to be reached
    */
   public int getQueueLength()
   {
      return synchronizer.getQueueLength();
   }

   private final class Synchronizer extends AbstractQueuedLongSynchronizer
   {
      private static final long serialVersionUID = 104753538004341218L;

      /** {@inheritDoc} */
      @Override
      protected long tryAcquireShared(final long seq)
      {
         return sequence.get() - (seq + 1);
      }

      /** {@inheritDoc} */
      @Override
      protected boolean tryReleaseShared(final long unused)
      {
         sequence.increment();
         return true;
      }
   }
}
