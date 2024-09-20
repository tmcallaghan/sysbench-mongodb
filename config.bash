# You will most likely want to change these values

export SHARDED=no

# database in which to run the benchmark
export DB_NAME=sbtest10by1mm

# URI connection string
export URI=<replace-with-proper-URI>

# trust store file
#   leave this alone if not using TLS
export TRUST_STORE="./rds-truststore.jks"

# trust store password
#   leave this alone if not using TLS
export TRUST_STORE_PASSWORD="<replace-with-trust-store-password>"

# Use "yes" to load the collections
export DOLOAD=yes

# Use "yes" to execute the benchmark
export DOQUERY=yes

# number of collections to create for the benchmark
#   valid values : integer > 0
export NUM_COLLECTIONS=1

# number of documents to maintain per collection
#   valid values : integer > 0
export NUM_DOCUMENTS_PER_COLLECTION=1000000

# total number of documents to insert per "batch"
#   valid values : integer > 0
export NUM_DOCUMENTS_PER_INSERT=1000

# total number of simultaneous insertion threads (for loader)
#   valid values : integer > 0
export NUM_LOADER_THREADS=10

# total number of simultaneous benchmark threads
#   valid values : integer > 0
export NUM_WRITER_THREADS=10

# run the benchmark for this many minutes
#   valid values : intever > 0
export RUN_TIME_MINUTES=10
export RUN_TIME_SECONDS=$[RUN_TIME_MINUTES*60]

# total number of transactions per second, allows for the benchmark to be rate limited
#   valid values : integer > 0
export MAX_TPS=999999


# ------------------------------------------------------------------------------------
# Sysbench specific parameters
# ------------------------------------------------------------------------------------

# number of documents to retrieve in range queries
#   valid values : integer > 0
export SYSBENCH_RANGE_SIZE=100

# number of point queries per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_POINT_SELECTS=10

# number of simple range queries per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_SIMPLE_RANGES=1

# number of aggregation queries per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_SUM_RANGES=1

# number of ordered range queries per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_ORDER_RANGES=1

# number of distinct range queries per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_DISTINCT_RANGES=1

# set all of the following 3 parameters to zero for a read-only benchmark

# number of indexed updates per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_INDEX_UPDATES=1

# number of non-indexed updates per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_NON_INDEX_UPDATES=1

# number of delete/insert operations per sysbench "transaction"
#   valid values : integer >= 0
export SYSBENCH_INSERTS=1


# ------------------------------------------------------------------------------------
# Parameters to leave alone
# ------------------------------------------------------------------------------------

# Use a seed for the RNG, like $( date +%s )
#   This can be used to repeat, or not, the sequence of keys used per test.
export SEED=$( date +%s )

# display performance information every time the client application inserts this many documents
#   valid values : integer > 0, set to -1 if using NUM_SECONDS_PER_FEEDBACK
export NUM_INSERTS_PER_FEEDBACK=-1

# display performance information every time the client application has run for this many seconds
#   valid values : integer > 0, set to -1 if using NUM_INSERTS_PER_FEEDBACK
export NUM_SECONDS_PER_FEEDBACK=10


