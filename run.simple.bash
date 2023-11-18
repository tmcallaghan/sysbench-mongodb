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

if [ $2 ];
then
   echo "Loading config overrides from $2....."
    source $2
fi

CLASSPATH="mongo-java-driver-3.9.1.jar:mongodb-driver-core-3.9.1.jar:mongodb-driver-legacy-3.9.1.jar:$CLASSPATH"
TAIL_LINES=21

javac -cp $CLASSPATH:$PWD/src src/jmongosysbenchload.java
javac -cp $CLASSPATH:$PWD/src src/jmongosysbenchexecute.java
javac -cp $CLASSPATH:$PWD/src src/jmongosysbenchshardedload.java
javac -cp $CLASSPATH:$PWD/src src/jmongosysbenchshardedexecute.java


# load the data

if [[ $DOLOAD = "yes" ]]; then
    echo Do load at $( date )
    export LOG_NAME=mongoSysbenchLoad-${NUM_COLLECTIONS}-${NUM_DOCUMENTS_PER_COLLECTION}-${NUM_LOADER_THREADS}.txt
    export BENCHMARK_TSV=${LOG_NAME}.tsv
 
    rm -f $LOG_NAME
    rm -f $BENCHMARK_TSV

    T="$(date +%s)"

    if [[ $SHARDED = "no" ]]; then
        echo "executing standard sysbench load (not sharded)" | tee -a $LOG_NAME
        java -cp $CLASSPATH:$PWD/src jmongosysbenchload $NUM_COLLECTIONS $DB_NAME $NUM_LOADER_THREADS $NUM_DOCUMENTS_PER_COLLECTION $NUM_DOCUMENTS_PER_INSERT $NUM_INSERTS_PER_FEEDBACK $NUM_SECONDS_PER_FEEDBACK $BENCHMARK_TSV $MONGO_COMPRESSION $MONGO_BASEMENT $WRITE_CONCERN $MONGO_SERVER $MONGO_PORT "$USERNAME" "$PASSWORD" "$TRUST_STORE" "$TRUST_STORE_PASSWORD" $USE_TLS "$MONGODB_REPLICASETNAME"
    else
        echo "executing sharded sysbench load" | tee -a $LOG_NAME
        java -cp $CLASSPATH:$PWD/src jmongosysbenchshardedload $NUM_COLLECTIONS $DB_NAME $NUM_LOADER_THREADS $NUM_DOCUMENTS_PER_COLLECTION $NUM_DOCUMENTS_PER_INSERT $NUM_INSERTS_PER_FEEDBACK $NUM_SECONDS_PER_FEEDBACK $BENCHMARK_TSV $MONGO_COMPRESSION $MONGO_BASEMENT $WRITE_CONCERN $MONGO_SERVER $MONGO_PORT "$USERNAME" "$PASSWORD" "$TRUST_STORE" "$TRUST_STORE_PASSWORD" $USE_TLS $MAX_SHARD_KEY $DOCS_PER_SHARD "$MONGODB_REPLICASETNAME"
    fi

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
    echo Do query at $( date )
    export LOG_NAME=mongoSysbenchExecute-${NUM_COLLECTIONS}-${NUM_DOCUMENTS_PER_COLLECTION}-${NUM_WRITER_THREADS}.txt
    export BENCHMARK_TSV=${LOG_NAME}.tsv
 
    rm -f $LOG_NAME
    rm -f $BENCHMARK_TSV

    T="$(date +%s)"

    if [[ $SHARDED = "no" ]]; then
        echo "executing standard sysbench" | tee -a $LOG_NAME
        java -cp $CLASSPATH:$PWD/src jmongosysbenchexecute $NUM_COLLECTIONS $DB_NAME $NUM_WRITER_THREADS $NUM_DOCUMENTS_PER_COLLECTION $NUM_SECONDS_PER_FEEDBACK $BENCHMARK_TSV $SYSBENCH_AUTO_COMMIT $RUN_TIME_SECONDS $SYSBENCH_RANGE_SIZE $SYSBENCH_POINT_SELECTS $SYSBENCH_SIMPLE_RANGES $SYSBENCH_SUM_RANGES $SYSBENCH_ORDER_RANGES $SYSBENCH_DISTINCT_RANGES $SYSBENCH_INDEX_UPDATES $SYSBENCH_NON_INDEX_UPDATES $SYSBENCH_INSERTS $WRITE_CONCERN $MAX_TPS $MONGO_SERVER $MONGO_PORT $SEED "$USERNAME" "$PASSWORD" $MONGO_READ_PREFERENCE "$TRUST_STORE" "$TRUST_STORE_PASSWORD" $USE_TLS "$MONGODB_REPLICASETNAME" | tee -a $LOG_NAME
     else
        echo "executing standard sysbench" | tee -a $LOG_NAME
        java -cp $CLASSPATH:$PWD/src jmongosysbenchshardedexecute $NUM_COLLECTIONS $DB_NAME $NUM_WRITER_THREADS $NUM_DOCUMENTS_PER_COLLECTION $NUM_SECONDS_PER_FEEDBACK $BENCHMARK_TSV $SYSBENCH_AUTO_COMMIT $RUN_TIME_SECONDS $SYSBENCH_RANGE_SIZE $SYSBENCH_POINT_SELECTS $SYSBENCH_SIMPLE_RANGES $SYSBENCH_SUM_RANGES $SYSBENCH_ORDER_RANGES $SYSBENCH_DISTINCT_RANGES $SYSBENCH_INDEX_UPDATES $SYSBENCH_NON_INDEX_UPDATES $SYSBENCH_INSERTS $WRITE_CONCERN $MAX_TPS $MONGO_SERVER $MONGO_PORT $SEED "$USERNAME" "$PASSWORD" $MONGO_READ_PREFERENCE "$TRUST_STORE" "$TRUST_STORE_PASSWORD" $USE_TLS $MAX_SHARD_KEY $DOCS_PER_SHARD "$MONGODB_REPLICASETNAME" | tee -a $LOG_NAME
    fi

    echo "" | tee -a $LOG_NAME
    T="$(($(date +%s)-T))"
    printf "`date` | sysbench benchmark duration = %02d:%02d:%02d:%02d\n" "$((T/86400))" "$((T/3600%24))" "$((T/60%60))" "$((T%60))" | tee -a $LOG_NAME
    echo ""
    echo "************************************************************************"
    echo "final ${numTailLines} interval(s)"
    echo "************************************************************************"
    tail -n $TAIL_LINES $LOG_NAME

    cat $LOG_NAME | grep '170 seconds' >> results.txt
fi

