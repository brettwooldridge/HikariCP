package com.zaxxer.hikari;

import java.sql.Statement;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.performance.StubStatement;
import com.zaxxer.hikari.util.FastStatementList;

public class TestFastStatementList
{
    @Test
    public void testOverflow()
    {
        ArrayList<Statement> verifyList = new ArrayList<>();

        FastStatementList list = new FastStatementList();
        for (int i = 0; i < 100; i++)
        {
            StubStatement statement = new StubStatement();
            list.add(statement);
            verifyList.add(statement);
        }

        for (int i = 0; i < 100; i++)
        {
            Assert.assertNotNull("Element " + i, list.get(i));
            Assert.assertSame(verifyList.get(i), list.get(i));
        }
    }
}
