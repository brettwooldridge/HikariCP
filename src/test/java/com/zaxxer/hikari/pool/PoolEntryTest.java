package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConfigTest;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.StubDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static com.zaxxer.hikari.pool.TestElf.getPool;
import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;

public class PoolEntryTest {

   private static final HikariPool poolBase;
   static {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(10);
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      HikariDataSource ds = new HikariDataSource(config);
      poolBase = getPool(ds);
   }

   private Connection getConnection(int major, int minor) throws Exception {
      DatabaseMetaData databaseMetaData = Mockito.mock(DatabaseMetaData.class);
      Mockito.when(databaseMetaData.getJDBCMajorVersion()).thenReturn(major);
      Mockito.when(databaseMetaData.getJDBCMinorVersion()).thenReturn(minor);
      Connection connection = Mockito.mock(Connection.class);
      Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
      return connection;
   }

   @Test
   public void BeginEndRequestJDBC42Test() throws Exception {
      Connection connection = getConnection(4,2);
      PoolEntry poolEntry = new PoolEntry(connection, poolBase, false, false);
      Connection proxyConnection = poolEntry.createProxyConnection(ProxyLeakTask.NO_LEAK);
      Mockito.verify(connection, Mockito.never()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      poolEntry.recycle();
      Mockito.verify(connection, Mockito.never()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).getMetaData();
   }

   @Test
   public void BeginEndRequestJDBC38Test() throws Exception {
      Connection connection = getConnection(3,8);
      PoolEntry poolEntry = new PoolEntry(connection, poolBase, false, false);
      Connection proxyConnection = poolEntry.createProxyConnection(ProxyLeakTask.NO_LEAK);
      Mockito.verify(connection, Mockito.never()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      poolEntry.recycle();
      Mockito.verify(connection, Mockito.never()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).getMetaData();
   }

   @Test
   public void BeginEndRequestJDBC43Test() throws Exception {
      Connection connection = getConnection(4,3);
      PoolEntry poolEntry = new PoolEntry(connection, poolBase, false, false);
      Connection proxyConnection = poolEntry.createProxyConnection(ProxyLeakTask.NO_LEAK);
      Mockito.verify(connection, Mockito.atMostOnce()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      poolEntry.recycle();
      Mockito.verify(connection, Mockito.atMostOnce()).beginRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).endRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).getMetaData();
   }

   @Test
   public void BeginEndRequestJDBC47Test() throws Exception {
      Connection connection = getConnection(4,7);
      PoolEntry poolEntry = new PoolEntry(connection, poolBase, false, false);
      Connection proxyConnection = poolEntry.createProxyConnection(ProxyLeakTask.NO_LEAK);
      Mockito.verify(connection, Mockito.atMostOnce()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      poolEntry.recycle();
      Mockito.verify(connection, Mockito.atMostOnce()).beginRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).endRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).getMetaData();
   }

   @Test
   public void BeginEndRequestJDBC50Test() throws Exception {
      Connection connection = getConnection(5,0);
      PoolEntry poolEntry = new PoolEntry(connection, poolBase, false, false);
      Connection proxyConnection = poolEntry.createProxyConnection(ProxyLeakTask.NO_LEAK);
      Mockito.verify(connection, Mockito.atMostOnce()).beginRequest();
      Mockito.verify(connection, Mockito.never()).endRequest();
      poolEntry.recycle();
      Mockito.verify(connection, Mockito.atMostOnce()).beginRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).endRequest();
      Mockito.verify(connection, Mockito.atMostOnce()).getMetaData();
   }

}
