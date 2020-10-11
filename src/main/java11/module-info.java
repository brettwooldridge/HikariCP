module com.zaxxer.hikari
{
   requires static hibernate.core;
   requires java.sql;
   requires java.management;
   requires java.naming;
   requires static javassist;
   requires static simpleclient;
   requires slf4j.api;
   requires static metrics.core;
   requires static metrics.healthchecks;
   requires static micrometer.core;

   exports com.zaxxer.hikari;
   exports com.zaxxer.hikari.hibernate;
   exports com.zaxxer.hikari.metrics;
   exports com.zaxxer.hikari.metrics.dropwizard;
   exports com.zaxxer.hikari.metrics.micrometer;
   exports com.zaxxer.hikari.metrics.prometheus;
   exports com.zaxxer.hikari.pool;
   exports com.zaxxer.hikari.util;
}
