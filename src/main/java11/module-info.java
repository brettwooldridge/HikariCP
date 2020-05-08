module com.zaxxer.hikari
{
   requires hibernate.core;
   requires java.sql;
   requires java.management;
   requires java.naming;
   requires javassist;
   requires simpleclient;
   requires slf4j.api;
   requires metrics.core;
   requires metrics.healthchecks;
   requires micrometer.core;

   exports com.zaxxer.hikari;
   exports com.zaxxer.hikari.hibernate;
   exports com.zaxxer.hikari.metrics;
   exports com.zaxxer.hikari.metrics.dropwizard;
   exports com.zaxxer.hikari.metrics.micrometer;
   exports com.zaxxer.hikari.metrics.prometheus;
   exports com.zaxxer.hikari.pool;
   exports com.zaxxer.hikari.util;
}
