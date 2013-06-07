#!/bin/bash

export MONGO_COMPRESSION=zlib
export MONGO_BASEMENT=65536
export NUM_COLLECTIONS=16
export NUM_DOCUMENTS_PER_COLLECTION=10000000
export NUM_DOCUMENTS_PER_INSERT=1000
export NUM_LOADER_THREADS=8
export threadCountList="0001 0002 0004 0008 0016 0032 0064 0128 0256 0512 1024"
export RUN_TIME_SECONDS=300
export DB_NAME=sbtest
export BENCHMARK_NUMBER=100

export TOKUMON_CACHE_SIZE=12884901888

# lock out all but 16G
if [ -z "$LOCK_MEM_SIZE_16" ]; then
    echo "Need to set LOCK_MEM_SIZE_16"
    exit 1
fi

export BENCHMARK_SUFFIX=".${LOCK_MEM_SIZE_16}-lock"



# need to lockout memory for pure mongo tests
sudo pkill -9 lockmem
sudo ~/bin/lockmem $LOCK_MEM_SIZE_16 &


# TOKUMX
#export TARBALL=tokumx-1.0.0-rc.0-linux-x86_64
#export MONGO_TYPE=tokumon
#export MONGO_REPLICATION=N
#export BENCH_ID=tokumon-1.0.0.rc0-${MONGO_COMPRESSION}
#./doit.bash
#mongo-clean

# MONGODB 2.2
#export TARBALL=mongodb-linux-x86_64-2.2.3
#export MONGO_TYPE=mongo
#export MONGO_REPLICATION=N
#export BENCH_ID=mongo-2.2.3
#./doit.bash
#mongo-clean

# MONGODB 2.4
export TARBALL=mongodb-linux-x86_64-2.4.4
export MONGO_TYPE=mongo
export MONGO_REPLICATION=N
export BENCH_ID=mongo-2.4.4
./doit.bash
mongo-clean


# unlock memory
sudo pkill -9 lockmem
