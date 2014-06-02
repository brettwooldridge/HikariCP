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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

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

   /**
    * This interface is implemented by a listener to the bag.  The listener
    * will be informed of when the bag has become empty.  The usual course
    * of action by the listener in this case is to attempt to add an item
    * to the bag.
    */
   public interface IBagStateListener
   {
      void addBagItem();
   }

   private ThreadLocal<FastList<WeakReference<T>>> threadList;
   private CopyOnWriteArraySet<T> sharedList;
   private Synchronizer synchronizer;
   private IBagStateListener listener;

   /**
    * Constructor.
    */
   public ConcurrentBag()
   {
      this.sharedList = new CopyOnWriteArraySet<T>();
      this.synchronizer = new Synchronizer();
      this.threadList = new ThreadLocal<FastList<WeakReference<T>>>();
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
      FastList<WeakReference<T>> list = threadList.get();
      if (list == null) {
         list = new FastList<WeakReference<T>>(WeakReference.class);
         threadList.set(list);
      }
      else {
         for (int i = list.size() - 1; i >= 0; i--) {
            final WeakReference<T> reference = list.removeLast();
            final T element = reference.get();
            if (element != null && element.compareAndSetState(STATE_NOT_IN_USE, STATE_IN_USE)) {
               return element;
            }
         }
      }

      // Otherwise, scan the shared list ... for maximum of timeout
      timeout = timeUnit.toNanos(timeout);
      do {
         final long startScan = System.nanoTime();
         for (T reference : sharedList) {
            if (reference.compareAndSetState(STATE_NOT_IN_USE, STATE_IN_USE)) {
               return reference;
            }
         }

         if (listener != null) {
            listener.addBagItem();
         }

         synchronizer.tryAcquireSharedNanos(startScan, timeout);

         timeout -= (System.nanoTime() - startScan);
      }
      while (timeout > 0L);

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
   public void requite(final T value)
   {
      if (value == null) {
         throw new NullPointerException("Cannot return a null value to the bag");
      }

      if (value.compareAndSetState(STATE_IN_USE, STATE_NOT_IN_USE)) {
         FastList<WeakReference<T>> list = threadList.get();
         if (list == null) {
            list = new FastList<WeakReference<T>>(WeakReference.class);
            threadList.set(list);
         }

         list.add(new WeakReference<T>(value));
         synchronizer.releaseShared(System.nanoTime());
      }
      else {
         throw new IllegalStateException("Value was returned to the bag that was not borrowed: " + value);
      }
   }

   /**
    * Add a new object to the bag for others to borrow.
    *
    * @param value an object to add to the bag
    */
   public void add(final T value)
   {
      final long addTime = System.nanoTime();
      sharedList.add(value);
      synchronizer.releaseShared(addTime);
   }

   /**
    * Remove a value from the bag.  This method should only be called
    * with objects obtained by {@link #borrow(long, TimeUnit)} or {@link #reserve(IBagManagable)}.
    * @param value the value to remove
    * @throws IllegalStateException if an attempt is made to remove an object
    *         from the bag that was not borrowed or reserved first
    */
   public void remove(T value)
   {
      if (value.compareAndSetState(STATE_IN_USE, STATE_REMOVED) || value.compareAndSetState(STATE_RESERVED, STATE_REMOVED)) {
         if (!sharedList.remove(value)) {
            throw new IllegalStateException("Attempt to remove an object from the bag that does not exist");
         }
      }
      else {
         throw new IllegalStateException("Attempt to remove an object from the bag that was not borrowed or reserved");
      }
   }

   /**
    * This method provides a "snaphot" in time of the IBagManagable
    * items in the bag in the specified state.  It does not "lock"
    * or reserve items in any way.  Call {@link #reserve(IBagManagable)}
    * on items in list before performing any action on them.
    *
    * @param state one of STATE_NOT_IN_USE or STATE_IN_USE
    * @return a possibly empty list of objects having the state specified
    */
   public List<T> values(int state)
   {
      ArrayList<T> list = new ArrayList<T>(sharedList.size());
      if (state == STATE_IN_USE || state == STATE_NOT_IN_USE) {
         for (T reference : sharedList) {
            if (reference.getState() == state) {
               list.add(reference);
            }
         }
      }
      return list;
   }

   /**
    * The method is used to make an item in the bag "unavailable" for
    * borrowing.  It is primarily used when wanting to operate on items
    * returned by the {@link #values(int)} method.  Items that are
    * reserved can be removed from the bag via {@link #remove(IBagManagable)}
    * without the need to unreserve them.  Items that are not removed
    * from the bag can be make available for borrowing again by calling
    * the {@link #unreserve(IBagManagable)} method.
    *
    * @param value the item to reserve
    * @return true if the item was able to be reserved, false otherwise
    */
   public boolean reserve(T value)
   {
      return value.compareAndSetState(STATE_NOT_IN_USE, STATE_RESERVED);
   }

   /**
    * This method is used to make an item reserved via {@link #reserve(IBagManagable)}
    * available again for borrowing.
    *
    * @param value the item to unreserve
    */
   public void unreserve(T value)
   {
      final long checkInTime = System.nanoTime();
      if (!value.compareAndSetState(STATE_RESERVED, STATE_NOT_IN_USE)) {
         throw new IllegalStateException("Attempt to relinquish an object to the bag that was not reserved");
      }

      synchronizer.releaseShared(checkInTime);
   }

   /**
    * Add a listener to the bag.  There can only be one.  If this method is
    * called a second time, the original listener will be evicted.
    *
    * @param listener a listener to the bag
    */
   public void addBagStateListener(IBagStateListener listener)
   {
      this.listener = listener;
   }

   /**
    * Get the number of threads pending (waiting) for an item from the
    * bag to become available.
    *
    * @return the number of threads waiting for items from the bag
    */
   public int getPendingQueue()
   {
      return synchronizer.getQueueLength();
   }

   public int getCount(int state)
   {
      int count = 0;
      for (T reference : sharedList) {
         if (reference.getState() == state) {
            count++;
         }
      }
      return count;
   }

   /**
    * Get the total number of items in the bag.
    *
    * @return the number of items in the bag 
    */
   public int size()
   {
      return sharedList.size();
   }

   /**
    * Our private synchronizer that handles notify/wait type semantics.
    */
   private static class Synchronizer extends AbstractQueuedLongSynchronizer
   {
      private static final long serialVersionUID = 104753538004341218L;
      private static final boolean JAVA7;

      static {
         boolean b = false;
         try {
            b = AbstractQueuedLongSynchronizer.class.getMethod("hasQueuedPredecessors", new Class<?>[0]) != null;
         }
         catch (Exception e) {
         }

         JAVA7 = b;
      }

      @Override
      protected long tryAcquireShared(long startScanTime)
      {
         return getState() >= startScanTime && !java67hasQueuedPredecessors() ? 1L : -1L;
      }

      /** {@inheritDoc} */
      @Override
      protected boolean tryReleaseShared(long updateTime)
      {
         setState(updateTime);

         return true;
      }

      private boolean java67hasQueuedPredecessors()
      {
         if (JAVA7) {
            return hasQueuedPredecessors();
         }

         return false;
      }
   }
}
