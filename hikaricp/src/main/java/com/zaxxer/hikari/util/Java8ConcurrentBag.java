/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.IConcurrentBagEntry.STATE_NOT_IN_USE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.stream.Collectors;

import com.zaxxer.hikari.pool.PoolBagEntry;

/**
 * This is a specialized concurrent bag that achieves superior performance
 * to LinkedBlockingQueue and LinkedTransferQueue for the purposes of a
 * connection pool.  It uses ThreadLocal storage when possible to avoid
 * locks, but resorts to scanning a common collection if there are no
 * available items in the ThreadLocal list.  Not-in-use items in the
 * ThreadLocal lists can be "stolen" when the borrowing thread has none
 * of its own.  It is a "lock-less" implementation using a specialized
 * AbstractQueuedLongSynchronizer to manage cross-thread signaling.
 *
 * Note that items that are "borrowed" from the bag are not actually
 * removed from any collection, so garbage collection will not occur
 * even if the reference is abandoned.  Thus care must be taken to
 * "requite" borrowed objects otherwise a memory leak will result.  Only
 * the "remove" method can completely remove an object from the bag.
 *
 * @author Brett Wooldridge
 *
 * @param <T> the templated type to store in the bag
 */
public final class Java8ConcurrentBag extends ConcurrentBag<PoolBagEntry>
{
   /**
    * Construct a ConcurrentBag with the specified listener.
    *
    * @param listener the IBagStateListener to attach to this bag
    */
   public Java8ConcurrentBag(IBagStateListener listener)
   {
      super(listener);
   }

   @Override
   protected AbstractQueuedLongSynchronizer createQueuedSynchronizer()
   {
      return new Synchronizer();
   }
   
   /**
    * This method provides a "snaphot" in time of the BagEntry
    * items in the bag in the specified state.  It does not "lock"
    * or reserve items in any way.  Call {@link #reserve(BagEntry)}
    * on items in list before performing any action on them.
    *
    * @param state one of STATE_NOT_IN_USE or STATE_IN_USE
    * @return a possibly empty list of objects having the state specified
    */
   public List<PoolBagEntry> values(final int state)
   {
      if (state == STATE_IN_USE || state == STATE_NOT_IN_USE) {
         return sharedList.stream()
                   .filter(reference -> reference.state.get() == state)
                   .collect(Collectors.toList());
      }

      return new ArrayList<PoolBagEntry>(0);
   }

   /**
    * Get a count of the number of items in the specified state at the time of this call.
    *
    * @param state the state of the items to count
    * @return a count of how many items in the bag are in the specified state
    */
   public int getCount(final int state)
   {
      return (int) sharedList.stream().filter(reference -> reference.state.get() == state).count();
   }

   /**
    * Our private synchronizer that handles notify/wait type semantics.
    */
   private static final class Synchronizer extends AbstractQueuedLongSynchronizer
   {
      private static final long serialVersionUID = 104753538004341218L;

      @Override
      protected long tryAcquireShared(long seq)
      {
         return getState() > seq && !hasQueuedPredecessors() ? 1L : -1L;
      }

      /** {@inheritDoc} */
      @Override
      protected boolean tryReleaseShared(long updateSeq)
      {
         setState(updateSeq);

         return true;
      }
   }
}
