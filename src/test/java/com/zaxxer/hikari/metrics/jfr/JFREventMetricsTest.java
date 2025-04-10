package com.zaxxer.hikari.metrics.jfr;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JFREventMetricsTest {

   private static final Logger LOG = Logger.getLogger(JFREventMetricsTest.class.getName());

   private static final DockerImageName IMAGE_NAME = DockerImageName.parse("postgres:16");

   private PostgreSQLContainer<?> postgres;
   private HikariConfig config;

   @Before
   public void beforeTest() {
      postgres = new PostgreSQLContainer<>(IMAGE_NAME);
      postgres.start();
      config = createConfig(postgres);
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(5);
      config.setConnectionTimeout(3000);
      config.setIdleTimeout(SECONDS.toMillis(10));
      config.setValidationTimeout(SECONDS.toMillis(2));
      config.setMetricsTrackerFactory(new JFREventMetricsTrackerFactory());
   }

   @After
   public void afterTest() {
      postgres.stop();
   }

   @Test
   public void testJFRMetrics() throws Exception {
      File recordingFile = createJFRRecordingFile();
      recordJFREvents(recordingFile);
      List<RecordedEvent> hikariEvents = readHikariJFREvents(recordingFile);
      assertFalse(hikariEvents.isEmpty());
      LOG.info(() -> "Events found: " + hikariEvents);
   }

   private static @NotNull List<RecordedEvent> readHikariJFREvents(File tmp) throws IOException {
      List<RecordedEvent> hikariEvents = new ArrayList<>();
      try (RecordingFile recordingFile = new RecordingFile(tmp.toPath())) {
         while (recordingFile.hasMoreEvents()) {
            RecordedEvent recordedEvent = recordingFile.readEvent();
            if (recordedEvent.getEventType().getName().startsWith("com.zaxxer")) {
               hikariEvents.add(recordedEvent);
            }
         }
      }
      return hikariEvents;
   }

   private void recordJFREvents(File recordingFile) throws IOException, ParseException, SQLException {
      Configuration configuration = Configuration.getConfiguration("default");
      try (Recording recording = new Recording(configuration)) {
         recording.setToDisk(true);
         recording.setDestination(recordingFile.toPath());
         recording.start();
         try (final HikariDataSource ds = new HikariDataSource(config)) {
            assertTrue(ds.isRunning());
            try (Connection c = ds.getConnection()) {
               Awaitility.await().timeout(Duration.ofSeconds(1));
            }
         }
         recording.stop();
      }
   }

   private static @NotNull File createJFRRecordingFile() throws IOException {
      File tmp = File.createTempFile("hikari-jfr-events", ".jfr");
      tmp.deleteOnExit();
      return tmp;
   }

   private static HikariConfig createConfig(PostgreSQLContainer<?> postgres) {
      HikariConfig config = newHikariConfig();
      config.setJdbcUrl(postgres.getJdbcUrl());
      config.setUsername(postgres.getUsername());
      config.setPassword(postgres.getPassword());
      config.setDriverClassName(postgres.getDriverClassName());
      return config;
   }

}
