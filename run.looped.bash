#!/bin/bash

# enable passing different config files

if [ ! $1 ];
then
    FILE="config.bash"
else
    FILE=$1
fi

if [ -f $FILE ];
then
   echo "Loading config from $FILE....."
   source $FILE
else
   echo "Unable to read config $FILE"
   exit 1
fi

CLASSPATH="mongo-java-driver-3.9.1.jar:mongodb-driver-core-3.9.1.jar:mongodb-driver-legacy-3.9.1.jar:$CLASSPATH"
TAIL_LINES=21

javac -cp $CLASSPATH:$PWD/src src/jmongosysbenchload.java
javac --release 11 -cp $CLASSPATH:$PWD/src src/jmongosysbenchexecute.java


# load the data

if [[ $DOLOAD = "yes" ]]; then
    echo Do load at $( date )
    export LOG_NAME=mongoSysbenchLoad-${NUM_COLLECTIONS}-${NUM_DOCUMENTS_PER_COLLECTION}-${NUM_LOADER_THREADS}.txt
    export BENCHMARK_TSV=${LOG_NAME}.tsv
 
    rm -f $LOG_NAME
    rm -f $BENCHMARK_TSV

    T="$(date +%s)"
    java -cp $CLASSPATH:$PWD/src jmongosysbenchload $NUM_COLLECTIONS $DB_NAME $NUM_LOADER_THREADS $NUM_DOCUMENTS_PER_COLLECTION $NUM_DOCUMENTS_PER_INSERT $NUM_INSERTS_PER_FEEDBACK $NUM_SECONDS_PER_FEEDBACK $BENCHMARK_TSV $MONGO_COMPRESSION $MONGO_BASEMENT $WRITE_CONCERN $MONGO_SERVER $MONGO_PORT "$USERNAME" "$PASSWORD" "$TRUST_STORE" "$TRUST_STORE_PASSWORD" $USE_TLS
    echo "" | tee -a $LOG_NAME
    T="$(($(date +%s)-T))"
    printf "`date` | sysbench loader duration = %02d:%02d:%02d:%02d\n" "$((T/86400))" "$((T/3600%24))" "$((T/60%60))" "$((T%60))" | tee -a $LOG_NAME
    echo ""
    echo "************************************************************************"
    echo "final ${numTailLines} interval(s)"
    echo "************************************************************************"
    tail -n $TAIL_LINES $LOG_NAME
fi


# execute the benchmark

if [[ $DOQUERY = "yes" ]]; then
    #threadList="1 1 1 1 2 2 2 2 4 4 4 4 8 8 8 8 16 16 16 16 32 32 32 32 64 64 64 64 96 96 96 96 128 128 128 128 160 160 160 160 192 192 192 192 224 224 224 224 256 256 256 256 512 512 512 512"
    threadList="1 1 2 2 4 4 8 8 16 16 32 32 64 64"
    #threadList="1 1 2 2 4 4 8 8 16 16 32 32 64 64 96 96 128 128 160 160 192 192 224 224 256 256 512 512"
    #threadList="1 2 4"

    logDir="./log"
    mkdir $logDir

    for numThreads in $threadList; do
	NUM_WRITER_THREADS=$numThreads

        thisTimestamp=`date +%Y%m%d_%H%M%S`
        LOG_NAME="${logDir}/${thisTimestamp}_${NUM_WRITER_THREADS}_${MONGO_READ_PREFERENCE}_${RUN_TIME_SECONDS}.txt"
        export BENCHMARK_TSV=${LOG_NAME}.tsv

        echo "running with NUM_WRITER_THREADS=$NUM_WRITER_THREADS | MONGO_READ_PREFERENCE=$MONGO_READ_PREFERENCE | MONGO_SERVER=$MONGO_SERVER" | tee -a $thisLogName
 
        T="$(date +%s)"
        java -cp $CLASSPATH:$PWD/src jmongosysbenchexecute $NUM_COLLECTIONS $DB_NAME $NUM_WRITER_THREADS $NUM_DOCUMENTS_PER_COLLECTION $NUM_SECONDS_PER_FEEDBACK $BENCHMARK_TSV $SYSBENCH_AUTO_COMMIT $RUN_TIME_SECONDS $SYSBENCH_RANGE_SIZE $SYSBENCH_POINT_SELECTS $SYSBENCH_SIMPLE_RANGES $SYSBENCH_SUM_RANGES $SYSBENCH_ORDER_RANGES $SYSBENCH_DISTINCT_RANGES $SYSBENCH_INDEX_UPDATES $SYSBENCH_NON_INDEX_UPDATES $SYSBENCH_INSERTS $WRITE_CONCERN $MAX_TPS $MONGO_SERVER $MONGO_PORT $SEED "$USERNAME" "$PASSWORD" $MONGO_READ_PREFERENCE "$TRUST_STORE" "$TRUST_STORE_PASSWORD" $USE_TLS | tee -a $LOG_NAME
        echo "" | tee -a $LOG_NAME
        T="$(($(date +%s)-T))"
        printf "`date` | sysbench benchmark duration = %02d:%02d:%02d:%02d\n" "$((T/86400))" "$((T/3600%24))" "$((T/60%60))" "$((T%60))" | tee -a $LOG_NAME
        echo ""
        echo "************************************************************************"
        echo "final ${numTailLines} interval(s)"
        echo "************************************************************************"
        tail -n $TAIL_LINES $LOG_NAME
    done
fi

