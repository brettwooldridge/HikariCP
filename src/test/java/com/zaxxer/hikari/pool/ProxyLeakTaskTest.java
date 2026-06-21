package com.zaxxer.hikari.pool;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Tests for {@link ProxyLeakTask}.
 */
public class ProxyLeakTaskTest
{
   @Test
   public void runDoesNotThrowWhenStackTraceHasFewerThanFiveFrames() throws Exception
   {
      final Exception shortStack = new Exception();
      shortStack.setStackTrace(new StackTraceElement[] {
         new StackTraceElement("Foo", "a", "Foo.java", 1),
         new StackTraceElement("Foo", "b", "Foo.java", 2)
      });

      final Constructor<ProxyLeakTask> ctor = ProxyLeakTask.class.getDeclaredConstructor();
      ctor.setAccessible(true);
      final ProxyLeakTask task = ctor.newInstance();
      set(task, "exception", shortStack);
      set(task, "threadName", "test-thread");
      set(task, "connectionName", "test-connection");

      // run() trims the first 5 (HikariCP-internal) frames; with fewer than 5 it must not throw.
      task.run();
   }

   private static void set(final Object target, final String field, final Object value) throws Exception
   {
      final Field f = ProxyLeakTask.class.getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
   }
}
