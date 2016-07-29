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

package com.zaxxer.hikari.pool;

import java.sql.Statement;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.mocks.StubStatement;
import com.zaxxer.hikari.util.FastList;

public class TestFastList
{
    @Test
    public void testAddRemove()
    {
        ArrayList<Statement> verifyList = new ArrayList<>();

        FastList<Statement> list = new FastList<>(Statement.class);
        for (int i = 0; i < 32; i++)
        {
            StubStatement statement = new StubStatement(null);
            list.add(statement);
            verifyList.add(statement);
        }

        for (int i = 0; i < 32; i++)
        {
            Assert.assertNotNull("Element " + i + " was null but should be " + verifyList.get(i), list.get(0));
            int size = list.size();
            list.remove(verifyList.get(i));
            Assert.assertSame(size - 1, list.size());
        }
    }

    @Test
    public void testAddRemoveTail()
    {
        ArrayList<Statement> verifyList = new ArrayList<>();

        FastList<Statement> list = new FastList<>(Statement.class);
        for (int i = 0; i < 32; i++)
        {
            StubStatement statement = new StubStatement(null);
            list.add(statement);
            verifyList.add(statement);
        }

        for (int i = 31; i >= 0; i--)
        {
            Assert.assertNotNull("Element " + i, list.get(i));
            int size = list.size();
            list.remove(verifyList.get(i));
            Assert.assertSame(size - 1, list.size());
        }
    }

    @Test
    public void testOverflow()
    {
        ArrayList<Statement> verifyList = new ArrayList<>();

        FastList<Statement> list = new FastList<>(Statement.class);
        for (int i = 0; i < 100; i++)
        {
            StubStatement statement = new StubStatement(null);
            list.add(statement);
            verifyList.add(statement);
        }

        for (int i = 0; i < 100; i++)
        {
            Assert.assertNotNull("Element " + i, list.get(i));
            Assert.assertSame(verifyList.get(i), list.get(i));
        }
    }

    @Test
    public void testClear()
    {
       FastList<Statement> list = new FastList<>(Statement.class);
       for (int i = 0; i < 100; i++)
       {
           StubStatement statement = new StubStatement(null);
           list.add(statement);
       }

       Assert.assertNotEquals(0, list.size());
       list.clear();
       Assert.assertEquals(0, list.size());
    }

    @Test
    public void testRemoveLast()
    {
       FastList<Statement> list = new FastList<>(Statement.class);

       Statement last = null;
       for (int i = 0; i < 100; i++)
       {
           StubStatement statement = new StubStatement(null);
           list.add(statement);
           last = statement;
       }

       Assert.assertEquals(last, list.removeLast());
       Assert.assertEquals(99, list.size());
    }

    @Test
    public void testPolyMorphism1()
    {
       class Foo implements Base2 {

       }

       class Bar extends Foo {

       }

       FastList<Base> list = new FastList<>(Base.class, 2);
       list.add(new Foo());
       list.add(new Foo());
       list.add(new Bar());
    }

    interface Base
    {

    }

    interface Base2 extends Base
    {

    }
}
