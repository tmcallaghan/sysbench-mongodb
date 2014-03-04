sysbench-mongodb
================

Sysbench for MongoDB and TokuMX


Requirements
=====================

* Java 1.6 or 1.7
* The MongoDB Java driver must exist and be in the CLASSPATH, as in "export CLASSPATH=/home/tcallaghan/java_goodies/mongo-2.11.4.jar:.". If you don't already have the MongoDB Java driver, then execute the following two commands:
    * wget http://central.maven.org/maven2/org/mongodb/mongo-java-driver/2.11.4/mongo-java-driver-2.11.4.jar
    * export CLASSPATH=$PWD/mongo-java-driver-2.11.4.jar:$CLASSPATH
* This example assumes that you already have a MongoDB or TokuMX server running on the same machine as the Sysbench client application.
* You can connect a different server or port by editing the run.simple.bash script. 


Running the benchmark
=====================

In the default configuration the benchmark creates 16 collections, each with 10 million documents.

* git clone https://github.com/tmcallaghan/sysbench-mongodb.git
* cd sysbench-mongodb
* *[optionally edit run.simple.bash to modify the benchmark behavior]*
* ./run.simple.bash
