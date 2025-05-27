#!/bin/bash

mvn release:prepare -Prelease && mvn deploy -DperformRelease=true

