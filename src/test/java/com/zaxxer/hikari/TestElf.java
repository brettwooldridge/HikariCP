package com.zaxxer.hikari;

import java.lang.reflect.Field;

import com.zaxxer.hikari.pool.HikariPool;

public final class TestElf
{
    private TestElf()
    {
       // default constructor   
    }

    public static HikariPool getPool(HikariDataSource ds)
    {
        try
        {
            Field field = ds.getClass().getDeclaredField("pool");
            field.setAccessible(true);
            return (HikariPool) field.get(ds);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
