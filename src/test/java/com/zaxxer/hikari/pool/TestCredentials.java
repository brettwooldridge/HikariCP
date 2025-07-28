package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariCredentialsProvider;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static org.apache.commons.lang3.ThreadUtils.sleepQuietly;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestCredentials {
   @Test
   public void testCredentialsProvider() {
      HikariConfig config = newHikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTimeout(2500);
      config.setConnectionTestQuery("VALUES 1");
      config.setInitializationFailTimeout(Long.MAX_VALUE);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");
      config.setCredentialsProvider(new TestCredentialsProvider());

      try (HikariDataSource ds = new HikariDataSource(config)) {
         sleepQuietly(Duration.ofMillis(250L)); // Allow time for the credentials provider to be called

         assertTrue("CredentialsProvider was not called", ((TestCredentialsProvider) ds.getCredentialsProvider()).called.get());
      } catch (Exception e) {
         fail("Exception occurred: " + e.getMessage());
      }
   }

   public static class TestCredentialsProvider implements HikariCredentialsProvider {
      AtomicBoolean called = new AtomicBoolean();

      @Override
      public Credentials getCredentials() {
         called.set(true);
         return new Credentials("user", "password");
      }
   }
}
