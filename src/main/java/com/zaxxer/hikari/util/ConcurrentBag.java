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

/**
 * This is a specialized concurrent bag that achieves superior performance
 * to LinkedBlockingQueue and LinkedTransferQueue for the purposes of a
 * connection pool.  It uses ThreadLocal storage when possible to avoid
 * locks, but resorts to scanning a common collection if there are no
 * available connections in the ThreadLocal list.  Idle connections in
 * ThreadLocal lists can be "stolen" when the poll()ing thread has none
 * of its own.  It is a "lock-less" implementation using a specialized
 * AbstractQueuedLongSynchronizer to manage cross-thread signaling.
 *
 * Note that objects that are "borrowed" from the bag are not actually
 * removed from any collection, so garbage collection will not occur
 * even if the reference is abandoned.  Thus care must be taken to
 * "requite" borrowed objects otherwise a memory leak will result.  Only
 * the "remove" method can completely remove an object from the bag.
 *
 * @author Brett Wooldridge
 *
 * @param <T> the templated type to store in the bag
 * @param <IBagManagable>
 */
public class ConcurrentBag<T extends com.zaxxer.hikari.util.ConcurrentBag.IBagManagable>
{
	public static final int STATE_NOT_IN_USE = 0;
	public static final int STATE_IN_USE = 1;
	private static final int STATE_REMOVED = -1;
	private static final int STATE_RESERVED = -2;

	/**
	 * This interface must be implemented by classes wishing to be managed by
	 * ConcurrentBag.  All implementations must be atomic with respect to state.
	 * The suggested implementation is via AtomicInteger using the methods
	 * <code>get()</code> and <code>compareAndSet()</code>.
	 */
	public interface IBagManagable
	{
	    int getState();

	    boolean compareAndSetState(int expectedState, int newState);
	}

    private ThreadLocal<LinkedList<T>> threadList;
    private CopyOnWriteArraySet<T> sharedList;
    private Synchronizer synchronizer;

    /**
     * Constructor.
     */
    public ConcurrentBag()
    {
        this.sharedList = new CopyOnWriteArraySet<T>();
        this.synchronizer = new Synchronizer();
        this.threadList = new ThreadLocal<LinkedList<T>>();
    }

    /**
     * The method will borrow an IBagManagable from the bag, blocking for the
     * specified timeout if none are available.
     * 
     * @param timeout how long to wait before giving up, in units of unit
     * @param timeUnit a <code>TimeUnit</code> determining how to interpret the timeout parameter
     * @return a borrowed instance from the bag or null if a timeout occurs
     * @throws InterruptedException if interrupted while waiting
     */
    public T borrow(long timeout, TimeUnit timeUnit) throws InterruptedException
    {
        // Try the thread-local list first
        LinkedList<T> list = threadList.get();
        if (list == null)
        {
            list = new LinkedList<T>();
            threadList.set(list);
        }

        while (!list.isEmpty())
        {
            final T reference = list.removeFirst();
            if (reference.compareAndSetState(STATE_NOT_IN_USE, STATE_IN_USE))
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
                if (reference.compareAndSetState(STATE_NOT_IN_USE, STATE_IN_USE))
                {
                    return reference;
                }
            }

            synchronizer.tryAcquireSharedNanos(startScan, timeout);

            timeout -= (System.nanoTime() - startScan);
        } while (timeout > 0);

        return null;
    }

    /**
     * This method will return a borrowed object to the bag.  Objects
     * that are borrowed from the bag but never "requited" will result
     * in a memory leak.
     *
     * @param value the value to return to the bag
     * @throws NullPointerException if value is null
     * @throws IllegalStateException if the requited value was not borrowed from the bag
     */
    public void requite(T value)
    {
    	if (value == null)
    	{
    		throw new NullPointerException("Cannot return a null value to the bag");
    	}

        if (value.compareAndSetState(STATE_IN_USE, STATE_NOT_IN_USE))
        {
        	final long returnTime = System.nanoTime();
            threadList.get().addLast(value);
            synchronizer.releaseShared(returnTime);
        }
        else
        {
            throw new IllegalStateException("Value was returned to the bag that was not borrowed");
        }
    }

    /**
     * Add a new object to the bag for others to borrow.
     *
     * @param value an object to add to the bag
     */
    public void add(T value)
    {
        sharedList.add(value);
        synchronizer.releaseShared(1);
    }

    /**
     * Remove a value from the bag.  This method should only be called
     * with objects obtained by borrow() or reserve().
     * @param value the value to remove
     * @throws IllegalStateException if an attempt is made to remove an object
     *         from the bag that was not borrowed or reserved first
     */
    public void remove(T value)
    {
        if (value.compareAndSetState(STATE_IN_USE, STATE_REMOVED) || value.compareAndSetState(STATE_RESERVED, STATE_REMOVED))
        {
        	sharedList.remove(value);
        }
        else
        {
        	throw new IllegalStateException("Attempt to remove an object from the bag that was not borrowed or reserved");
        }
    }

    /**
     * This method provides a "snaphot" in time of the IBagManagable
     * items in the bag in the specified state.  It does not "lock"
     * or reserve items in any way.
     *
     * @param state one of STATE_NOT_IN_USE or STATE_IN_USE
     * @return a possibly empty list of objects having the state specified
     */
    public List<T> values(int state)
    {
        ArrayList<T> list = new ArrayList<T>(sharedList.size());
        if (state == STATE_IN_USE || state == STATE_NOT_IN_USE)
        {
	        for (T reference : sharedList)
	        {
	            if (reference.getState() == state)
	            {
	                list.add(reference);
	            }
	        }
        }
        return list;
    }

    public boolean reserve(T value)
    {
        return value.compareAndSetState(STATE_NOT_IN_USE, STATE_RESERVED);
    }

    public void unreserve(T value)
    {
        final long checkInTime = System.nanoTime();
        if (!value.compareAndSetState(STATE_RESERVED, STATE_NOT_IN_USE))
        {
        	throw new IllegalStateException("Attempt to relinquish an object to the bag that was not reserved");
        }

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
