/*
 * Copyright (C) 2013 Brett Wooldridge
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

import static com.zaxxer.hikari.util.IBagManagable.NOT_IN_USE;
import static com.zaxxer.hikari.util.IBagManagable.IN_USE;
import static com.zaxxer.hikari.util.IBagManagable.REMOVED;

/**
 * This is a specialized concurrent bag that achieves superior performance
 * to LinkedBlockingQueue and LinkedTransferQueue for the purposes of a
 * connection pool.  It uses ThreadLocal storage when possible to avoid
 * locks, but resorts to scanning a common collection if there are no
 * available connections in the ThreadLocal list.  It is a "lock-less"
 * implementation using a specialized AbstractQueuedLongSynchronizer to
 * manage cross-thread signaling.
 *
 * @author Brett Wooldridge
 *
 * @param <T> the templated type to store in the bag
 */
public class SpecializedConcurrentBag<T extends IBagManagable>
{
    private ThreadLocal<LinkedList<T>> threadList;
    private CopyOnWriteArraySet<T> sharedList;
    private Synchronizer synchronizer;

    /**
     * Constructor.
     *
     * @param initialCapacity initial bag capacity
     */
    public SpecializedConcurrentBag(int initialCapacity)
    {
        this.sharedList = new CopyOnWriteArraySet<>();
        this.synchronizer = new Synchronizer();
        this.threadList = new ThreadLocal<LinkedList<T>>() {
            protected LinkedList<T> initialValue()
            {
                return new LinkedList<>();
            }
        };
    }

    public T poll(long timeout, TimeUnit timeUnit) throws InterruptedException
    {
        // Try the thread-local list first
        final LinkedList<T> list = threadList.get();
        while (!list.isEmpty())
        {
            final T reference = list.removeFirst();
            if (reference.compareAndSetState(NOT_IN_USE, IN_USE))
            {
                return reference;
            }
        }

        // Otherwise, scan the shared list ... for maximum of timeout
        timeout = timeUnit.toNanos(timeout);
        do {
            final long startScan = System.nanoTime();
            for (T reference : sharedList)
            {
                if (reference.compareAndSetState(NOT_IN_USE, IN_USE))
                {
                    return reference;
                }
            }

            synchronizer.tryAcquireSharedNanos(startScan, timeout);

            timeout -= (System.nanoTime() - startScan);
        } while (timeout > 0);

        return null;
    }

    public void add(T value)
    {
        sharedList.add(value);
        synchronizer.releaseShared(1);
    }

    public boolean offer(T value)
    {
        final long offerTime = System.nanoTime();
        if (value.compareAndSetState(IN_USE, NOT_IN_USE))
        {
            threadList.get().addLast(value);
        }
        else
        {
            return false;
        }

        synchronizer.releaseShared(offerTime);

        return true;
    }

    public void remove(T value)
    {
        value.setState(REMOVED);
        sharedList.remove(value);
    }

    public List<T> values(int state)
    {
        ArrayList<T> list = new ArrayList<>(sharedList.size());
        for (T reference : sharedList)
        {
            if (reference.getState() == state)
            {
                list.add(reference);
            }
        }

        return list;
    }

    public T checkout(T value)
    {
        if (value.compareAndSetState(NOT_IN_USE, IN_USE))
        {
            return value;
        }

        return null;
    }

    public void checkin(T value)
    {
        final long checkInTime = System.nanoTime();
        value.compareAndSetState(IN_USE, NOT_IN_USE);
        synchronizer.releaseShared(checkInTime);
    }

    /**
     * Our private synchronizer that handles notify/wait type semantics.
     */
    private static class Synchronizer extends AbstractQueuedLongSynchronizer
    {
        private static final long serialVersionUID = 104753538004341218L;

        @Override
        protected long tryAcquireShared(long startScanTime)
        {
            // fairness
            if (hasQueuedPredecessors())
            {
                return -1;
            }

            return getState() > startScanTime ? 1 : -1;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean tryReleaseShared(long updateTime)
        {
            setState(updateTime);
            
            return true;
        }
    }
}
