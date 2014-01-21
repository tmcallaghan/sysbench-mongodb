sysbench-mongodb
================

Sysbench for MongoDB


Running the benchmark
=====================

This example assumes that you already have a MongoDB or TokuMX server running on the same machine as the iiBench client application.

Note, you must have ant and Java 1.7 installed to run the benchmark application.  If using Java 1.6, change the build.xml file's target="1.7" to target="1.6".  You also need to have the MongoDB Java driver in your CLASSPATH, as in "export CLASSPATH=/home/tcallaghan/java_goodies/mongo-2.10.1.jar:.".

tcallaghan@tmcdsk:~/temp/test$ git clone https://github.com/tmcallaghan/sysbench-mongodb.git

tcallaghan@tmcdsk:~/temp/test$ cd sysbench-mongodb

[optionally edit run.simple.bash to modify the benchmark behavior]

tcallaghan@tmcdsk:~/temp/test$ ./run.simple.bash

