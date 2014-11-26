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

import java.lang.reflect.Array;

/**
 * Fast list without range checking.
 *
 * @author Brett Wooldridge
 */
public final class FastList<T>
{
   private final Class<?> clazz;
   private T[] elementData;
   private int size;

   /**
    * Construct a FastList with a default size of 32.
    * @param clazz the Class stored in the collection
    */
   @SuppressWarnings("unchecked")
   public FastList(Class<?> clazz)
   {
      this.elementData = (T[]) Array.newInstance(clazz, 32);
      this.clazz = clazz;
   }

   /**
    * Construct a FastList with a specfied size.
    * @param clazz the Class stored in the collection
    * @param capacity the initial size of the FastList
    */
   @SuppressWarnings("unchecked")
   public FastList(Class<?> clazz, int capacity)
   {
      this.elementData = (T[]) Array.newInstance(clazz, capacity);
      this.clazz = clazz;
   }

   /**
    * Add an element to the tail of the FastList.
    *
    * @param element the element to add
    */
   public void add(T element)
   {
      try {
         elementData[size++] = element;
      }
      catch (ArrayIndexOutOfBoundsException e) {
         // overflow-conscious code
         final int oldCapacity = elementData.length;
         final int newCapacity = oldCapacity << 1;
         @SuppressWarnings("unchecked")
         final T[] newElementData = (T[]) Array.newInstance(clazz, newCapacity);
         System.arraycopy(elementData, 0, newElementData, 0, oldCapacity);
         newElementData[size - 1] = element;
         elementData = newElementData;
      }
   }

   /**
    * Get the element at the specified index.
    *
    * @param index the index of the element to get
    * @return the element, or ArrayIndexOutOfBounds is thrown if the index is invalid
    */
   public T get(int index)
   {
      return elementData[index];
   }

   /**
    * Remove the last element from the list.  No bound check is performed, so if this
    * method is called on an empty list and ArrayIndexOutOfBounds exception will be
    * thrown.
    *
    * @return the last element of the list
    */
   public T removeLast()
   {
      T element = elementData[--size];
      elementData[size] = null;
      return element;
   }

   /**
    * This remove method is most efficient when the element being removed
    * is the last element.  Equality is identity based, not equals() based.
    * Only the first matching element is removed.
    *
    * @param element the element to remove
    */
   public void remove(T element)
   {
      for (int index = size - 1; index >= 0; index--) {
         if (element == elementData[index]) {
            final int numMoved = size - index - 1;
            if (numMoved > 0) {
               System.arraycopy(elementData, index + 1, elementData, index, numMoved);
            }
            elementData[--size] = null;
            break;
         }
      }
   }

   /**
    * Clear the FastList.
    */
   public void clear()
   {
      for (int i = 0; i < size; i++) {
         elementData[i] = null;
      }

      size = 0;
   }

   /**
    * Get the current number of elements in the FastList.
    *
    * @return the number of current elements
    */
   public int size()
   {
      return size;
   }
}
