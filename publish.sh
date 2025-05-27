#!/bin/bash

mvn clean && mvn package -DskipTests -Dmaven.test.skip=true && mvn release:prepare -Prelease && mvn deploy -DperformRelease=true -DskipTests -Dmaven.test.skip=true
