package com.zaxxer.hikari.metrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.zaxxer.hikari.util.PropertyElf;

/**
 * Hikari Metrics Configuration
 */
public class HikariMetricsConfig
{
   private boolean metricNaming2;

   public HikariMetricsConfig()
   {
      metricNaming2 = false;
      String systemProp = System.getProperty("hikaricp.configurationFile");
      if (systemProp != null) {
         loadProperties(systemProp);
      }
      String isMetricName2 = System.getProperty("hikari.metrics.naming2");
      if (isMetricName2 != null) {
         metricNaming2 = Boolean.parseBoolean(isMetricName2);
      }
   }

   /**
    * Construct a HikariMetricsConfig from the specified properties object.
    *
    * @param properties the name of the property file
    */
   public HikariMetricsConfig(Properties properties)
   {
      this();
      PropertyElf.setTargetFromProperties(this, properties);
   }

   /**
    * Construct a HikariMetricsConfig from the specified property file name.  <code>propertyFileName</code>
    * will first be treated as a path in the file-system, and if that fails the
    * Class.getResourceAsStream(propertyFileName) will be tried.
    *
    * @param propertyFileName the name of the property file
    */
   public HikariMetricsConfig(String propertyFileName)
   {
      this();

      loadProperties(propertyFileName);
   }

   public boolean isMetricNaming2()
   {
      return metricNaming2;
   }


   public void setMetricNaming2(boolean isMetricNaming2)
   {
      this.metricNaming2 = isMetricNaming2;
   }


   private void loadProperties(String propertyFileName)
   {
      final File propFile = new File(propertyFileName);
      try (final InputStream is = propFile.isFile() ? new FileInputStream(propFile) : this.getClass().getResourceAsStream(propertyFileName)) {
         if (is != null) {
            Properties props = new Properties();
            props.load(is);
            PropertyElf.setTargetFromProperties(this, props);
         } else {
            throw new IllegalArgumentException("Cannot find property file: " + propertyFileName);
         }
      }
      catch (IOException io) {
         throw new RuntimeException("Failed to read property file", io);
      }
   }
}
