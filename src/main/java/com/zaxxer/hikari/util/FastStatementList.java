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

import java.sql.Statement;


/**
 * Fast list without range checking.
 *
 * @author Brett Wooldridge
 */
public final class FastStatementList
{
    private Statement[] elementData;

    private int size;

    /**
     * Construct a FastList with a default size of 16.
     */
    public FastStatementList()
    {
        this.elementData = new Statement[32];
    }

    /**
     * Construct a FastList with a specfied size.
     *
     * @param size the initial size of the FastList
     */
    public FastStatementList(int size)
    {
        this.elementData = new Statement[size];
    }

    /**
     * Add an element to the tail of the FastList.
     *
     * @param element the element to add
     */
    public void add(Statement element)
    {
        try
        {
            elementData[size] = element;
            size++;
        }
        catch (ArrayIndexOutOfBoundsException oob)
        {
            // overflow-conscious code
            int oldCapacity = elementData.length;
            int newCapacity = oldCapacity << 1;
            Statement[] newElementData = new Statement[newCapacity];
            System.arraycopy(elementData, 0, newElementData, 0, oldCapacity);
            newElementData[size++] = element;
            elementData = (Statement[]) newElementData;
        }
    }

    /**
     * Get the element at the specified index.
     *
     * @param index the index of the element to get
     * @return the element, or ArrayIndexOutOfBounds is thrown if the index is invalid
     */
    public Statement get(int index)
    {
        return elementData[index];
    }

    /**
     * This remove method is most efficient when the element being removed
     * is the last element.  Equality is identity based, not equals() based.
     * Only the first matching element is removed.
     *
     * @param element the element to remove
     */
    public void remove(Object element)
    {
        for (int index = size - 1; index >= 0; index--)
        {
            if (element == elementData[index])
            {
                int numMoved = size - index - 1;
                if (numMoved > 0)
                {
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
        for (int i = 0; i < size; i++)
        {
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
