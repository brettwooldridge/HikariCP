package com.zaxxer.hikari;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.performance.StubStatement;
import com.zaxxer.hikari.util.FastStatementList;

public class TestFastStatementList
{
    @Test
    public void testOverflow()
    {
        FastStatementList list = new FastStatementList();
        for (int i = 0; i < 100; i++)
        {
            list.add(new StubStatement());
        }

        for (int i = 0; i < 100; i++)
        {
            Assert.assertNotNull("Element " + i, list.get(i));
        }
    }
}
