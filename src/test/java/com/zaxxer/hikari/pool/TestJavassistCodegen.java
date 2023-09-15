package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.pool.TestElf.FauxWebClassLoader;
import com.zaxxer.hikari.util.JavassistProxyFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Stream;

public class TestJavassistCodegen {
   @Test
   public void testCodegen() throws Exception {
      String tmp = System.getProperty("java.io.tmpdir");
      JavassistProxyFactory.main(tmp + (tmp.endsWith("/") ? "" : "/"));

      Path base = Paths.get(tmp, "target/classes/com/zaxxer/hikari/pool".split("/"));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("HikariProxyConnection.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("HikariProxyStatement.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("HikariProxyCallableStatement.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("HikariProxyPreparedStatement.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("HikariProxyResultSet.class")));
      Assert.assertTrue("", Files.isRegularFile(base.resolve("ProxyFactory.class")));

      FauxWebClassLoader fauxClassLoader = new FauxWebClassLoader();

      int mod = fauxClassLoader.loadClass("com.zaxxer.hikari.pool.HikariProxyConnection").getModifiers();
      Assert.assertTrue("Generated proxy class should be public", Modifier.isPublic(mod));
      Assert.assertTrue("Generated proxy class should be final", Modifier.isFinal(mod));

      Class<?> proxyFactoryClass = fauxClassLoader.loadClass("com.zaxxer.hikari.pool.ProxyFactory");

      Connection connection = new StubConnection();

      Class<?> fastListClass = fauxClassLoader.loadClass("com.zaxxer.hikari.util.FastList");
      Object fastList = fastListClass.getConstructor(Class.class).newInstance(Statement.class);

      Object proxyConnection = getMethod(proxyFactoryClass, "getProxyConnection")
         .invoke(null,
            null /*poolEntry*/,
            connection,
            fastList,
            null /*leakTask*/,
            Boolean.FALSE /*isReadOnly*/,
            Boolean.FALSE /*isAutoCommit*/);
      Assert.assertNotNull(proxyConnection);

      Object proxyStatement = getMethod(proxyConnection.getClass(), "createStatement", 0)
         .invoke(proxyConnection);
      Assert.assertNotNull(proxyStatement);
   }

   private Method getMethod(Class<?> clazz, String methodName, Integer... parameterCount)
   {
      return Stream.of(clazz.getDeclaredMethods())
          .filter(method -> method.getName().equals(methodName))
          .filter(method -> (parameterCount.length == 0 || parameterCount[0] == method.getParameterCount()))
          .peek(method -> method.setAccessible(true))
          .findFirst()
          .orElseThrow(RuntimeException::new);
   }
}
