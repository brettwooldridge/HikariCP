#!/bin/bash

CLASSPATH=$CLASSPATH:~/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/org/slf4j/slf4j-simple/1.7.5/slf4j-simple-1.7.5.jar

CLASSPATH=$CLASSPATH:~/.m2/repository/com/jolbox/bonecp/0.8.0.RELEASE/bonecp-0.8.0.RELEASE.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/com/google/guava/guava/15.0/guava-15.0.jar

CLASSPATH=$CLASSPATH:~/.m2/repository/org/apache/tomcat/tomcat-jdbc/7.0.47/tomcat-jdbc-7.0.47.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/org/apache/tomcat/tomcat-juli/7.0.47/tomcat-juli-7.0.47.jar

CLASSPATH=$CLASSPATH:~/.m2/repository/com/mchange/c3p0/0.9.5-pre5/c3p0-0.9.5-pre5.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/com/mchange/mchange-commons-java/0.2.6.2/mchange-commons-java-0.2.6.2.jar

CLASSPATH=$CLASSPATH:~/.m2/repository/net/snaq/dbpool/5.1/dbpool-5.1.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar
CLASSPATH=$CLASSPATH:~/.m2/repository/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar

CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
CLASSPATH=$CLASSPATH:./target/HikariCP-1.3.0-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:./target/test-classes

java -classpath $CLASSPATH \
-client -XX:MaxInlineSize=0 -XX:+UseParallelGC -Xss256k -Xms128m -Xmx256m -Dorg.slf4j.simpleLogger.defaultLogLevel=info com.zaxxer.hikari.performance.Benchmark1 $1 $2 $3
