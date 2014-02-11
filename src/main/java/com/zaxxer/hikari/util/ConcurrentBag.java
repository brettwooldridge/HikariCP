package com.zaxxer.hikari.util;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentBag<T>
{
    private static sun.misc.Unsafe unsafe = getUnsafe();

    private LinkedList<LinkedList<T>> sharedList;

    private ThreadLocal<LinkedList<T>> threadList = new ThreadLocal<LinkedList<T>>() {
        protected java.util.LinkedList<T> initialValue()
        {
            LinkedList<T> list = new LinkedList<T>();
            sharedList.add(list);
            return list;
        }
    };

    public ConcurrentBag()
    {
        sharedList = new LinkedList<>();
    }

    @SuppressWarnings("restriction")
    private static sun.misc.Unsafe getUnsafe()
    {
        try
        {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (sun.misc.Unsafe) f.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot access sun.misc.Unsafe");
        }
    }

    private static class SinglyLinkedList<T>
    {
        private ReentrantLock putLock = new ReentrantLock();
        private ReentrantLock takeLock = new ReentrantLock();

        Node<T> head;
        Node<T> tail;

        void add(T value)
        {
            Node<T> node = new Node<T>(value);
            final ReentrantLock putLock = this.putLock;
            putLock.lock();
            try
            {
                if (head == null)
                {
                    head = tail = node;
                }
                else
                {
                    tail.next = node;
                }
            }
            finally
            {
                putLock.unlock();
            }
        }

        void remove(T value)
        {
            final ReentrantLock putLock = this.putLock;
            final ReentrantLock takeLock = this.takeLock;
            putLock.lock();
            takeLock.lock();
            try
            {
                Node<T> node = head;
                Node<T> prev = null;
                while (node != null)
                {
                    if (node.value == value)
                    {
                        if (prev == null)
                        {
                            head = node;
                        }
                        else
                        {
                            prev.next = node.next;
                        }
                        break;
                    }
                    node = node.next;
                }
            }
            finally
            {
                takeLock.unlock();
                putLock.unlock();
            }
        }
    }

    private static class Node<E>
    {
        E value;
        Node<E> next;

        Node(E value)
        {
            this.value = value;
        }
    }
}
