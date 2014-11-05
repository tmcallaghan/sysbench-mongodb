
# You will most likely want to change these values

# database in which to run the benchmark
export DB_NAME=sbtest

# database username on DB_NAME
export USERNAME=myuser
# database password to use for USERNAME
export PASSWORD=mypass

# name of the server to connect to
export MONGO_SERVER=localhost

# port of the server to connect to
export MONGO_PORT=27017



# if running TokuMX, need to select compression for collection and secondary indexes (zlib is default)
#   valid values : lzma, quicklz, zlib, none
export MONGO_COMPRESSION=quicklz

# if running TokuMX, need to select basement node size (65536 is default)
#   valid values : integer > 0 : 65536 for 64K
export MONGO_BASEMENT=65536

# number of collections to create for the benchmark
#   valid values : integer > 0
export NUM_COLLECTIONS=16

# number of documents to maintain per collection
#   valid values : integer > 0
export NUM_DOCUMENTS_PER_COLLECTION=1000000

# total number of documents to insert per "batch"
#   valid values : integer > 0
export NUM_DOCUMENTS_PER_INSERT=1000

# total number of simultaneous insertion threads (for loader)
#   valid values : integer > 0
export NUM_LOADER_THREADS=8

# total number of simultaneous benchmark threads
#   valid values : integer > 0
export NUM_WRITER_THREADS=64

# run the benchmark for this many minutes
#   valid values : intever > 0
export RUN_TIME_MINUTES=10
export RUN_TIME_SECONDS=$[RUN_TIME_MINUTES*60]

# write concern for the benchmark client
#   valid values : FSYNC_SAFE, NONE, NORMAL, REPLICAS_SAFE, SAFE
export WRITE_CONCERN=SAFE

# total number of transactions per second, allows for the benchmark to be rate limited
#   valid values : integer > 0
export MAX_TPS=999999

# display performance information every time the client application inserts this many documents
#   valid values : integer > 0, set to -1 if using NUM_SECONDS_PER_FEEDBACK
export NUM_INSERTS_PER_FEEDBACK=-1

# display performance information every time the client application has run for this many seconds
#   valid values : integer > 0, set to -1 if using NUM_INSERTS_PER_FEEDBACK
export NUM_SECONDS_PER_FEEDBACK=10

# set to Y to run in "read only" mode, just queries
#   valid values : N or Y
export SYSBENCH_READ_ONLY=N

# number of documents to retrieve in range queries
#   valud values : integer > 0
export SYSBENCH_RANGE_SIZE=100

# number of point queries per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_POINT_SELECTS=10

# number of simple range queries per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_SIMPLE_RANGES=1

# number of aggregation queries per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_SUM_RANGES=1

# number of ordered range queries per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_ORDER_RANGES=1

# number of distinct range queries per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_DISTINCT_RANGES=1

# number of indexed updates per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_INDEX_UPDATES=1

# number of non-indexed updates per sysbench "transaction"
#   valud values : integer > 0
export SYSBENCH_NON_INDEX_UPDATES=1
