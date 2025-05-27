#!/bin/bash

mvn release:prepare -Prelease && mvn deploy -DperformRelease=true -DskipTests -Dmaven.test.skip=true
