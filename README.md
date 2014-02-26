sysbench-mongodb
================

Sysbench for MongoDB


Running the benchmark
=====================

This example assumes that you already have a MongoDB or TokuMX server running on the same machine as the sysbench client application.  You can connect a different server or port by editing the run.simple.bash script.

In it's default configuration it creates 16 collections, each with 10 million documents.

Note, you need to have the MongoDB Java driver in your CLASSPATH, as in "export CLASSPATH=/home/tcallaghan/java_goodies/mongo-2.11.2.jar:.".

tcallaghan@tmcdsk:~/temp/test$ git clone https://github.com/tmcallaghan/sysbench-mongodb.git

tcallaghan@tmcdsk:~/temp/test$ cd sysbench-mongodb

[optionally edit run.simple.bash to modify the benchmark behavior]

tcallaghan@tmcdsk:~/temp/test/sysbench-mongodb$ ./run.simple.bash

