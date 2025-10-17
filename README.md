<h3>*DEPRECATED*</h3>

This repository is no longer maintained. A simpler Python implementation is available as [py-mongo-sysbench](https://github.com/tmcallaghan/py-mongo-sysbench)


sysbench-mongodb
================

Sysbench benchmark for MongoDB compatible databases


Requirements
=====================

* Java 17 (tested working with OpenJDK)
* MongoDB Java driver jars - specifically version 3.9.1 (for now)
  * they must be in the working directory (for now)
  * wget https://oss.sonatype.org/content/repositories/releases/org/mongodb/mongo-java-driver/3.9.1/mongo-java-driver-3.9.1.jar
  * wget https://oss.sonatype.org/content/repositories/releases/org/mongodb/mongodb-driver-core/3.9.1/mongodb-driver-core-3.9.1.jar
  * wget https://oss.sonatype.org/content/repositories/releases/org/mongodb/mongodb-driver-legacy/3.9.1/mongodb-driver-legacy-3.9.1.jar
* Modify config.bash file for URI (connection string)
* TLS is optional
  * If not using TLS, simply disable it in config.bash or your custom config file
  * Otherwise you'll need to create your key store file. Instructions are available at https://docs.aws.amazon.com/documentdb/latest/developerguide/connect_programmatically.html


Running the benchmark
=====================

In the default configuration the benchmark creates 1 collection with 1 million documents. You may want to watch the size of the database relative to your memory size to ensure you are testing just a memory based workload vs a workload that is exceeding memory and utilizing disk as well. All options are configurable in config.bash (or custom config file with the same options)

To run:

```bash
git clone https://github.com/tmcallaghan/sysbench-mongodb.git
cd sysbench-mongodb

```

Edit config.bash to match your environment, you must change the URI for connecting to your server.

```bash
./run.simple.bash

```

If you want to have multiple config files you can simply copy config.bash and specify the config you would like on the command line:

```bash
./run.simple.bash my_custom_config.bash

```
