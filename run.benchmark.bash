#!/bin/bash

if [ -z "$MONGO_DIR" ]; then
    echo "Need to set MONGO_DIR"
    exit 1
fi
if [ -z "$MONGO_TYPE" ]; then
    echo "Need to set MONGO_TYPE"
    exit 1
fi
if [ -z "$DB_NAME" ]; then
    echo "Need to set DB_NAME"
    exit 1
fi
if [ -z "$NUM_COLLECTIONS" ]; then
    echo "Need to set NUM_COLLECTIONS"
    exit 1
fi
if [ -z "$NUM_DOCUMENTS_PER_COLLECTION" ]; then
    echo "Need to set NUM_DOCUMENTS_PER_COLLECTION"
    exit 1
fi
if [ -z "$RUN_TIME_SECONDS" ]; then
    echo "Need to set RUN_TIME_SECONDS"
    exit 1
fi
if [ -z "$BENCHMARK_NUMBER" ]; then
    echo "Need to set BENCHMARK_NUMBER"
    exit 1
fi


if [ -z "$NUM_SECONDS_PER_FEEDBACK" ]; then
    export NUM_SECONDS_PER_FEEDBACK=10
fi
if [ -z "$SYSBENCH_READ_ONLY" ]; then
    export SYSBENCH_READ_ONLY=N
fi
if [ -z "$SYSBENCH_RANGE_SIZE" ]; then
    export SYSBENCH_RANGE_SIZE=100
fi
if [ -z "$SYSBENCH_POINT_SELECTS" ]; then
    export SYSBENCH_POINT_SELECTS=10
fi
if [ -z "$SYSBENCH_SIMPLE_RANGES" ]; then
    export SYSBENCH_SIMPLE_RANGES=1
fi
if [ -z "$SYSBENCH_SUM_RANGES" ]; then
    export SYSBENCH_SUM_RANGES=1
fi
if [ -z "$SYSBENCH_ORDER_RANGES" ]; then
    export SYSBENCH_ORDER_RANGES=1
fi
if [ -z "$SYSBENCH_DISTINCT_RANGES" ]; then
    export SYSBENCH_DISTINCT_RANGES=1
fi
if [ -z "$SYSBENCH_INDEX_UPDATES" ]; then
    export SYSBENCH_INDEX_UPDATES=1
fi
if [ -z "$SYSBENCH_NON_INDEX_UPDATES" ]; then
    export SYSBENCH_NON_INDEX_UPDATES=1
fi
if [ -z "$PAUSE_BETWEEN_RUNS" ]; then
    export PAUSE_BETWEEN_RUNS=60
fi
if [ -z "$COMMIT_SYNC" ]; then
    export COMMIT_SYNC=1
fi
if [ -z "$threadCountList" ]; then
    export threadCountList="0001 0002 0004 0008 0016 0032 0064 0128 0256 0512 1024"
fi
if [ -z "$SCP_FILES" ]; then
    export SCP_FILES=Y
fi

IOSTAT_INTERVAL=10
IOSTAT_ROUNDS=$[RUN_TIME_SECONDS/IOSTAT_INTERVAL+1]

ant clean default

export MINI_LOG_NAME=${MACHINE_NAME}-mongoSysbenchExecute-${NUM_COLLECTIONS}-${NUM_DOCUMENTS_PER_COLLECTION}-${MONGO_TYPE}
export MONGO_LOG=${MINI_LOG_NAME}.mongolog

echo "`date` | starting the ${MONGO_TYPE} server at ${MONGO_DIR}"
if [ ${MONGO_TYPE} == "tokumon" ]; then
    mongo-start-tokumon-fork
else
    mongo-start-pure-numa-fork
fi

mongo-is-up
echo "`date` | server is available"

for threadCount in ${threadCountList}; do
    export NUM_WRITER_THREADS=$threadCount

    export MINI_LOG_NAME=${MACHINE_NAME}-mongoSysbenchExecute-${NUM_COLLECTIONS}-${NUM_DOCUMENTS_PER_COLLECTION}-${NUM_WRITER_THREADS}-${MONGO_TYPE}
        
    if [ ${MONGO_TYPE} == "tokumon" ]; then
        if [ ${COMMIT_SYNC} == "1" ]; then
            LOG_NAME=${MINI_LOG_NAME}-SYNC_COMMIT.log
            BENCH_ID=${MINI_LOG_NAME}-SYNC_COMMIT
        else
            LOG_NAME=${MINI_LOG_NAME}-NOSYNC_COMMIT.log
            BENCH_ID=${MINI_LOG_NAME}-NOSYNC_COMMIT
        fi
    else
        LOG_NAME=${MINI_LOG_NAME}.log
        BENCH_ID=${MINI_LOG_NAME}
    fi
        
    export BENCHMARK_TSV=${LOG_NAME}.tsv
    LOG_NAME_IOSTAT=${LOG_NAME}.iostat
        
    rm -f $LOG_NAME
    rm -f $BENCHMARK_TSV
    
    iostat -dxm $IOSTAT_INTERVAL $IOSTAT_ROUNDS  > $LOG_NAME_IOSTAT &
        
    ant execute | tee -a $LOG_NAME
    
    echo "`date` | pausing for ${PAUSE_BETWEEN_RUNS} seconds"
    sleep ${PAUSE_BETWEEN_RUNS}
    
    pkill -f iostat
done

T="$(date +%s)"
echo "`date` | shutting down the server" | tee -a $LOG_NAME
mongo-stop
mongo-is-down
T="$(($(date +%s)-T))"
printf "`date` | shutdown duration = %02d:%02d:%02d:%02d\n" "$((T/86400))" "$((T/3600%24))" "$((T/60%60))" "$((T%60))" | tee -a $LOG_NAME

echo "" | tee -a $LOG_NAME
echo "-------------------------------" | tee -a $LOG_NAME
echo "Sizing Information" | tee -a $LOG_NAME
echo "-------------------------------" | tee -a $LOG_NAME

SIZE_BYTES=`du -c --block-size=1 ${MONGO_DATA_DIR} | tail -n 1 | cut -f1`
SIZE_APPARENT_BYTES=`du -c --block-size=1 --apparent-size ${MONGO_DATA_DIR} | tail -n 1 | cut -f1`
SIZE_MB=`echo "scale=2; ${SIZE_BYTES}/(1024*1024)" | bc `
SIZE_APPARENT_MB=`echo "scale=2; ${SIZE_APPARENT_BYTES}/(1024*1024)" | bc `

echo "`date` | post-load sizing (SizeMB / ASizeMB) = ${SIZE_MB} / ${SIZE_APPARENT_MB}" | tee -a $LOG_NAME


if [ ${SCP_FILES} == "Y" ]; then
    DATE=`date +"%Y%m%d%H%M%S"`
    tarFileName="${MACHINE_NAME}-${BENCHMARK_NUMBER}-${DATE}-mongoSysbench-${BENCH_ID}.tar.gz"

    tar czvf ${tarFileName} ${MACHINE_NAME}*
    scp ${tarFileName} tcallaghan@192.168.1.242:~

    rm -f ${tarFileName}
    rm -f ${MACHINE_NAME}*
    rm -f ${MONGO_LOG}

    #movecores
fi
