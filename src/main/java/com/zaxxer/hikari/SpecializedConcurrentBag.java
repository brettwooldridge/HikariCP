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
package com.zaxxer.hikari;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

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
public class SpecializedConcurrentBag<T>
{
    static final int NOT_IN_USE = 0;
    static final int IN_USE = 1;
    static final int REMOVED = -1;

    private ConcurrentHashMap<T, AtomicStampedReference<T>> map;
    private Synchronizer synchronizer;

    private ThreadLocal<LinkedList<AtomicStampedReference<T>>> threadList = new ThreadLocal<LinkedList<AtomicStampedReference<T>>>()
    {
        protected LinkedList<AtomicStampedReference<T>> initialValue()
        {
            return new LinkedList<>();
        }
    };

    public SpecializedConcurrentBag()
    {
        map = new ConcurrentHashMap<>();
        synchronizer = new Synchronizer();
    }

    public T poll(long timeout, TimeUnit timeUnit) throws InterruptedException
    {
        // Try the thread-local list first
        LinkedList<AtomicStampedReference<T>> list = threadList.get();
        while (!list.isEmpty())
        {
            AtomicStampedReference<T> stampedReference = list.removeLast();
            final T reference = stampedReference.getReference();
            if (stampedReference.compareAndSet(reference, reference, NOT_IN_USE, IN_USE))
            {
                return reference;
            }
        }

        timeout = timeUnit.toNanos(timeout);
        do {
            final long start = System.nanoTime();
            for (AtomicStampedReference<T> stampedReference : map.values())
            {
                final T reference = stampedReference.getReference();
                if (stampedReference.compareAndSet(reference, reference, NOT_IN_USE, IN_USE))
                {
                    return reference;
                }
            }

            synchronizer.tryAcquireSharedNanos(1, timeout);

            timeout -= (System.nanoTime() - start);
        } while (timeout > 0);

        return null;
    }

    public boolean offer(T value)
    {
        LinkedList<AtomicStampedReference<T>> list = threadList.get();
        AtomicStampedReference<T> stampedReference = map.get(value);
        if (stampedReference == null)
        {
            stampedReference = new AtomicStampedReference<T>(value, NOT_IN_USE);
            map.put(value, stampedReference);
            list.addLast(stampedReference);
        }
        else
        {
            final T reference = stampedReference.getReference();
            if (stampedReference.compareAndSet(reference, reference, IN_USE, NOT_IN_USE))
            {
                list.addLast(stampedReference);
            }
        }

        synchronizer.releaseShared(1);

        return true;
    }

    public void remove(T value)
    {
        AtomicStampedReference<T> stampedReference = map.get(value);
        if (stampedReference != null)
        {
            stampedReference.set(stampedReference.getReference(), REMOVED);
            map.remove(value);
        }
    }

    public List<T> values(int state)
    {
        ArrayList<T> list = new ArrayList<>(map.size());
        for (AtomicStampedReference<T> stampedReference : map.values())
        {
            if (stampedReference.getStamp() == state)
            {
                list.add(stampedReference.getReference());
            }
        }

        return list;
    }

    T checkout(T value)
    {
        AtomicStampedReference<T> stampedReference = map.get(value);
        if (stampedReference != null && stampedReference.compareAndSet(stampedReference.getReference(), stampedReference.getReference(), NOT_IN_USE, IN_USE))
        {
            return value;
        }

        return null;
    }

    void checkin(T value)
    {
        AtomicStampedReference<T> stampedReference = map.get(value);
        if (stampedReference != null)
        {
            final T reference = stampedReference.getReference();
            stampedReference.compareAndSet(reference, reference, IN_USE, NOT_IN_USE);
            synchronizer.releaseShared(1);
        }
    }

    private static class Synchronizer extends AbstractQueuedLongSynchronizer
    {
        private static final long serialVersionUID = 104753538004341218L;

        private static ThreadLocal<Long> startTimeStamp = new ThreadLocal<Long>() {
            protected Long initialValue()
            {
                return System.nanoTime();
            }
        };

        @Override
        protected long tryAcquireShared(long arg)
        {
            Long waitStart = startTimeStamp.get();

            // fairness
            if (hasQueuedPredecessors())
            {
                return -1;
            }

            if (getState() > waitStart)
            {
                startTimeStamp.remove();
                return 1;
            }

            return -1;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean tryReleaseShared(long arg)
        {
            setState(System.nanoTime());
            
            return true;
        }
    }
}
