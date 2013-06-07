#!/bin/bash

if [ -z "$BENCHMARK_SUFFIX" ]; then
    #export BENCHMARK_SUFFIX=".anything-you-want"
    export BENCHMARK_SUFFIX=""
fi
if [ -z "$TARBALL" ]; then
    #export TARBALL=tokumx-1.0.0-rc.0-linux-x86_64
    export TARBALL=mongodb-linux-x86_64-2.2.3
fi
if [ -z "$MONGO_TYPE" ]; then
    #export MONGO_TYPE=tokumon
    export MONGO_TYPE=mongo
fi
if [ -z "$MONGO_DIR" ]; then
    echo "Need to set MONGO_DIR"
    exit 1
fi
if [ ! -d "$MONGO_DIR" ]; then
    echo "Need to create directory MONGO_DIR"
    exit 1
fi
if [ "$(ls -A $MONGO_DIR)" ]; then
    echo "$MONGO_DIR contains files, cannot run script"
    exit 1
fi

if [ -z "$MONGO_COMPRESSION" ]; then
    # lzma, quicklz, zlib, none
    export MONGO_COMPRESSION=zlib
fi
if [ -z "$MONGO_BASEMENT" ]; then
    # 131072, 65536
    export MONGO_BASEMENT=65536
fi

if [ -z "$NUM_COLLECTIONS" ]; then
    export NUM_COLLECTIONS=16
fi
if [ -z "$NUM_DOCUMENTS_PER_COLLECTION" ]; then
    export NUM_DOCUMENTS_PER_COLLECTION=10000000
    #export NUM_DOCUMENTS_PER_COLLECTION=250000
fi
if [ -z "$NUM_DOCUMENTS_PER_INSERT" ]; then
    export NUM_DOCUMENTS_PER_INSERT=1000
fi
if [ -z "$NUM_LOADER_THREADS" ]; then
    export NUM_LOADER_THREADS=4
fi
if [ -z "$threadCountList" ]; then
    export threadCountList="0001 0002 0004 0008 0016 0032 0064 0128 0256 0512 1024"
fi
if [ -z "$RUN_TIME_SECONDS" ]; then
    export RUN_TIME_SECONDS=300
fi
if [ -z "$DB_NAME" ]; then
    export DB_NAME=sbtest
fi
if [ -z "$BENCHMARK_NUMBER" ]; then
    export BENCHMARK_NUMBER=100
fi
if [ -z "$MONGO_REPLICATION" ]; then
    export MONGO_REPLICATION=N
fi

# unpack mongo files
echo "Creating mongo from ${TARBALL} in ${MONGO_DIR}"
pushd $MONGO_DIR
mkmon $TARBALL
popd

echo "Running loader"
./run.load.bash

echo "Running benchmark"
./run.benchmark.bash
