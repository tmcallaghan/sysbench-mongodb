#!/bin/bash

wget https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/2.13.0/mongo-java-driver-2.13.0.jar

export CLASSPATH=$PWD/mongo-java-driver-2.13.0.jar:$CLASSPATH

exit 0
