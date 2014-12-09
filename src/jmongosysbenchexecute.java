import com.codahale.metrics.*;
import org.apache.log4j.BasicConfigurator;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

//import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoClientOptions;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.CommandResult;
import com.mongodb.AggregationOutput;
import com.mongodb.WriteResult;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.Writer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class jmongosysbenchexecute {
    static final MetricRegistry metrics = new MetricRegistry();
    private final Timer insertLatencies = metrics.timer(MetricRegistry.name("sysbench", "inserts"));
    private final Timer deleteLatencies = metrics.timer(MetricRegistry.name("sysbench", "deletes"));
    private final Timer updateLatencies = metrics.timer(MetricRegistry.name("sysbench", "updates"));
    private final Timer pointQueryLatencies = metrics.timer(MetricRegistry.name("sysbench", "ptqueries"));
    private final Timer rangeQueryLatencies = metrics.timer(MetricRegistry.name("sysbench", "rgqueries"));
    private final Timer globalSysbenchTransactions = metrics.timer(MetricRegistry.name("sysbench", "tps"));

    public static AtomicLong globalWriterThreads = new AtomicLong(0);

    public static Writer writer = null;
    public static boolean outputHeader = true;

    public static int numCollections;
    public static String dbName;
    public static int writerThreads;
    public static Integer numMaxInserts;
    public static long secondsPerFeedback;
    public static String logFileName;
    public static String indexTechnology;
    public static String autoCommit;
    public static int runSeconds;
    public static String myWriteConcern;
    public static Integer maxTPS;
    public static Integer maxThreadTPS;
    public static String serverName;
    public static int serverPort;
    public static String userName;
    public static String passWord;

    public static int oltpRangeSize;
    public static int oltpPointSelects;
    public static int oltpSimpleRanges;
    public static int oltpSumRanges;
    public static int oltpOrderRanges;
    public static int oltpDistinctRanges;
    public static int oltpIndexUpdates;
    public static int oltpNonIndexUpdates;
    public static int oltpInserts;

    public static boolean bIsTokuMX = false;

    public static int allDone = 0;

    public static long rngSeed = 0;
    
    public jmongosysbenchexecute() {
    }

    public static void main (String[] args) throws Exception {
        BasicConfigurator.configure();

        if (args.length != 24) {
            logMe("*** ERROR : CONFIGURATION ISSUE ***");
            logMe("jsysbenchexecute [number of collections] [database name] [number of writer threads] [documents per collection] [seconds feedback] "+
                                   "[log file name] [auto commit Y/N] [runtime (seconds)] [range size] [point selects] "+
                                   "[simple ranges] [sum ranges] [order ranges] [distinct ranges] [index updates] [non index updates] [inserts] [writeconcern] "+
                                   "[max tps] [server] [port] [seed] [username] [password]");
            System.exit(1);
        }
        
        numCollections = Integer.valueOf(args[0]);
        dbName = args[1];
        writerThreads = Integer.valueOf(args[2]);
        numMaxInserts = Integer.valueOf(args[3]);
        secondsPerFeedback = Long.valueOf(args[4]);
        logFileName = args[5];
        autoCommit = args[6];
        runSeconds = Integer.valueOf(args[7]);
        oltpRangeSize = Integer.valueOf(args[8]);
        oltpPointSelects = Integer.valueOf(args[9]);
        oltpSimpleRanges = Integer.valueOf(args[10]);
        oltpSumRanges = Integer.valueOf(args[11]);
        oltpOrderRanges = Integer.valueOf(args[12]);
        oltpDistinctRanges = Integer.valueOf(args[13]);
        oltpIndexUpdates = Integer.valueOf(args[14]);
        oltpNonIndexUpdates = Integer.valueOf(args[15]);
        oltpInserts = Integer.valueOf(args[16]);
        myWriteConcern = args[17];
        maxTPS = Integer.valueOf(args[18]);
        serverName = args[19];
        serverPort = Integer.valueOf(args[20]);
        rngSeed = Long.valueOf(args[21]);
        userName = args[22];
        passWord = args[23];

        maxThreadTPS = (maxTPS / writerThreads) + 1;

        WriteConcern myWC = new WriteConcern();
        if (myWriteConcern.toLowerCase().equals("fsync_safe")) {
            myWC = WriteConcern.FSYNC_SAFE;
        }
        else if ((myWriteConcern.toLowerCase().equals("none"))) {
            myWC = WriteConcern.NONE;
        }
        else if ((myWriteConcern.toLowerCase().equals("normal"))) {
            myWC = WriteConcern.NORMAL;
        }
        else if ((myWriteConcern.toLowerCase().equals("replicas_safe"))) {
            myWC = WriteConcern.REPLICAS_SAFE;
        }
        else if ((myWriteConcern.toLowerCase().equals("safe"))) {
            myWC = WriteConcern.SAFE;
        }
        else {
            logMe("*** ERROR : WRITE CONCERN ISSUE ***");
            logMe("  write concern %s is not supported",myWriteConcern);
            System.exit(1);
        }

        logMe("Application Parameters");
        logMe("-------------------------------------------------------------------------------------------------");
        logMe("  collections              = %d",numCollections);
        logMe("  database name            = %s",dbName);
        logMe("  writer threads           = %d",writerThreads);
        logMe("  documents per collection = %,d",numMaxInserts);
        logMe("  feedback seconds         = %,d",secondsPerFeedback);
        logMe("  log file                 = %s",logFileName);
        logMe("  auto commit              = %s",autoCommit);
        logMe("  run seconds              = %d",runSeconds);
        logMe("  oltp range size          = %d",oltpRangeSize);
        logMe("  oltp point selects       = %d",oltpPointSelects);
        logMe("  oltp simple ranges       = %d",oltpSimpleRanges);
        logMe("  oltp sum ranges          = %d",oltpSumRanges);
        logMe("  oltp order ranges        = %d",oltpOrderRanges);
        logMe("  oltp distinct ranges     = %d",oltpDistinctRanges);
        logMe("  oltp index updates       = %d",oltpIndexUpdates);
        logMe("  oltp non index updates   = %d",oltpNonIndexUpdates);
        logMe("  oltp inserts             = %d",oltpInserts);
        logMe("  write concern            = %s",myWriteConcern);
        logMe("  maximum tps (global)     = %d",maxTPS);
        logMe("  maximum tps (per thread) = %d",maxThreadTPS);
        logMe("  Server:Port = %s:%d",serverName,serverPort);
        logMe("  seed                     = %d",rngSeed);
        logMe("  userName                 = %s",userName);

        MongoClientOptions clientOptions = new MongoClientOptions.Builder().connectionsPerHost(2048).socketTimeout(60000).writeConcern(myWC).build();
        ServerAddress srvrAdd = new ServerAddress(serverName,serverPort);
        MongoCredential credential = MongoCredential.createMongoCRCredential(userName, dbName, passWord.toCharArray());
        MongoClient m = new MongoClient(srvrAdd, Arrays.asList(credential));

        logMe("mongoOptions | " + m.getMongoOptions().toString());
        logMe("mongoWriteConcern | " + m.getWriteConcern().toString());

        DB db = m.getDB(dbName);

        final ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
        consoleReporter.start(10, TimeUnit.SECONDS);

        final CsvReporter csvReporter = CsvReporter.forRegistry(metrics)
            .formatFor(Locale.US)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build(new File(logFileName));
        csvReporter.start(1, TimeUnit.SECONDS);

        // determine server type : mongo or tokumx
        DBObject checkServerCmd = new BasicDBObject();
        CommandResult commandResult = db.command("buildInfo");

        // check if tokumxVersion exists, otherwise assume mongo
        if (commandResult.toString().contains("tokumxVersion")) {
            indexTechnology = "tokumx";
        }
        else
        {
            indexTechnology = "mongo";
        }

        logMe("  index technology         = %s",indexTechnology);
        logMe("-------------------------------------------------------------------------------------------------");

        if ((!indexTechnology.toLowerCase().equals("tokumx")) && (!indexTechnology.toLowerCase().equals("mongo"))) {
            // unknown index technology, abort
            logMe(" *** Unknown Indexing Technology %s, shutting down",indexTechnology);
            System.exit(1);
        }

        if (indexTechnology.toLowerCase().equals("tokumx")) {
            bIsTokuMX = true;
        }

        jmongosysbenchexecute t = new jmongosysbenchexecute();

        Thread[] tWriterThreads = new Thread[writerThreads];

        for (int i=0; i<writerThreads; i++) {
            tWriterThreads[i] = new Thread(t.new MyWriter(writerThreads, i, numMaxInserts, db, numCollections, rngSeed));
            tWriterThreads[i].start();
        }

        // wait for writer threads to terminate
        for (int i=0; i<writerThreads; i++) {
            if (tWriterThreads[i].isAlive())
                tWriterThreads[i].join();
        }

        m.close();

        logMe("Done!");
    }

    class MyWriter implements Runnable {
        int threadCount;
        int threadNumber;
        int numTables;
        int numMaxInserts;
        int numCollections;
        DB db;

        long numInserts = 0;
        long numDeletes = 0;
        long numUpdates = 0;
        long numPointQueries = 0;
        long numRangeQueries = 0;

        java.util.Random rand;
        
        MyWriter(int threadCount, int threadNumber, int numMaxInserts, DB db, int numCollections, long rngSeed) {
            this.threadCount = threadCount;
            this.threadNumber = threadNumber;
            this.numMaxInserts = numMaxInserts;
            this.db = db;
            this.numCollections = numCollections;
            rand = new java.util.Random((long) threadNumber + rngSeed);
        }
        public void run() {
            logMe("Writer thread %d : started",threadNumber);
            globalWriterThreads.incrementAndGet();

            long numTransactions = 0;
            long numLastTransactions = 0;
            long nextMs = System.currentTimeMillis() + 1000;

            boolean auto_commit = !autoCommit.toLowerCase().equals("n");

            while (allDone == 0) {
                if ((numTransactions - numLastTransactions) >= maxThreadTPS) {
                    // pause until a second has passed
                    while (System.currentTimeMillis() < nextMs) {
                        try {
                            Thread.sleep(20);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    numLastTransactions = numTransactions;
                    nextMs = System.currentTimeMillis() + 1000;
                }

                // if TokuMX, lock onto current connection (do not pool)
                if (bIsTokuMX && !auto_commit) {
                    db.requestStart();
                    db.command("beginTransaction");
                }

                String collectionName = "sbtest" + Integer.toString(rand.nextInt(numCollections)+1);
                DBCollection coll = db.getCollection(collectionName);

                final Timer.Context txnContext = globalSysbenchTransactions.time();
                try {
                    if (bIsTokuMX && !auto_commit) {
                        // make sure a connection is available, given that we are not pooling
                        db.requestEnsureConnection();
                    }

                    for (int i=1; i <= oltpPointSelects; i++) {
                        //for i=1, oltp_point_selects do
                        //   rs = db_query("SELECT c FROM ".. table_name .." WHERE id=" .. sb_rand(1, oltp_table_size))
                        //end

                        // db.sbtest8.find({_id: 554312}, {c: 1, _id: 0})

                        int startId = rand.nextInt(numMaxInserts)+1;

                        BasicDBObject query = new BasicDBObject("_id", startId);
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);

                        final Timer.Context context = pointQueryLatencies.time();
                        try {
                            DBObject myDoc = coll.findOne(query, columns);
                        } finally {
                            context.stop();
                        }
                    }

                    for (int i=1; i <= oltpSimpleRanges; i++) {
                        //for i=1, oltp_simple_ranges do
                        //   range_start = sb_rand(1, oltp_table_size)
                        //   rs = db_query("SELECT c FROM ".. table_name .." WHERE id BETWEEN " .. range_start .. " AND " .. range_start .. "+" .. oltp_range_size - 1)
                        //end

                        //db.sbtest8.find({_id: {$gte: 5523412, $lte: 5523512}}, {c: 1, _id: 0})

                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId));
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);

                        final Timer.Context context = rangeQueryLatencies.time();
                        try {
                            DBCursor cursor = coll.find(query, columns);
                            try {
                                while(cursor.hasNext()) {
                                    cursor.next();
                                    //System.out.println(cursor.next());
                                }
                            } finally {
                                cursor.close();
                            }
                        } finally {
                            context.stop();
                        }
                    }

                    for (int i=1; i <= oltpSumRanges; i++) {
                        //for i=1, oltp_sum_ranges do
                        //   range_start = sb_rand(1, oltp_table_size)
                        //   rs = db_query("SELECT SUM(K) FROM ".. table_name .." WHERE id BETWEEN " .. range_start .. " AND " .. range_start .. "+" .. oltp_range_size - 1)
                        //end

                        //db.sbtest8.aggregate([ {$match: {_id: {$gt: 5523412, $lt: 5523512}}}, { $group: { _id: null, total: { $sum: "$k"}} } ])

                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        // create our pipeline operations, first with the $match
                        DBObject match = new BasicDBObject("$match", new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId)));

                        // build the $projection operation
                        DBObject fields = new BasicDBObject("k", 1);
                        fields.put("_id", 0);
                        DBObject project = new BasicDBObject("$project", fields );

                        // Now the $group operation
                        DBObject groupFields = new BasicDBObject( "_id", null);
                        groupFields.put("average", new BasicDBObject( "$sum", "$k"));
                        DBObject group = new BasicDBObject("$group", groupFields);

                        final Timer.Context context = rangeQueryLatencies.time();
                        try {
                            // run aggregation
                            AggregationOutput output = coll.aggregate( match, project, group );

                            //System.out.println(output.getCommandResult());
                        } finally {
                            context.stop();
                        }
                    }

                    for (int i=1; i <= oltpOrderRanges; i++) {
                        //for i=1, oltp_order_ranges do
                        //   range_start = sb_rand(1, oltp_table_size)
                        //   rs = db_query("SELECT c FROM ".. table_name .." WHERE id BETWEEN " .. range_start .. " AND " .. range_start .. "+" .. oltp_range_size - 1 .. " ORDER BY c")
                        //end

                        //db.sbtest8.find({_id: {$gte: 5523412, $lte: 5523512}}, {c: 1, _id: 0}).sort({c: 1})

                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId));
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);

                        final Timer.Context context = rangeQueryLatencies.time();
                        try {
                            DBCursor cursor = coll.find(query, columns).sort(new BasicDBObject("c",1));
                            try {
                                while(cursor.hasNext()) {
                                    cursor.next();
                                    //System.out.println(cursor.next());
                                }
                            } finally {
                                cursor.close();
                            }
                        } finally {
                            context.stop();
                        }
                    }

                    for (int i=1; i <= oltpDistinctRanges; i++) {
                        //for i=1, oltp_distinct_ranges do
                        //   range_start = sb_rand(1, oltp_table_size)
                        //   rs = db_query("SELECT DISTINCT c FROM ".. table_name .." WHERE id BETWEEN " .. range_start .. " AND " .. range_start .. "+" .. oltp_range_size - 1 .. " ORDER BY c")
                        //end

                        //db.sbtest8.distinct("c",{_id: {$gt: 5523412, $lt: 5523512}}).sort()

                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId));
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);

                        final Timer.Context context = rangeQueryLatencies.time();
                        try {
                            List lstDistinct = coll.distinct("c", query);
                            //System.out.println(lstDistinct.toString());
                        } finally {
                            context.stop();
                        }
                    }

                    for (int i=1; i <= oltpIndexUpdates; i++) {
                        //for i=1, oltp_index_updates do
                        //   rs = db_query("UPDATE " .. table_name .. " SET k=k+1 WHERE id=" .. sb_rand(1, oltp_table_size))
                        //end

                        //db.sbtest8.update({_id: 5523412}, {$inc: {k: 1}}, false, false)

                        int startId = rand.nextInt(numMaxInserts)+1;

                        final Timer.Context context = updateLatencies.time();
                        try {
                            WriteResult wrUpdate = coll.update(new BasicDBObject("_id", startId), new BasicDBObject("$inc", new BasicDBObject("k",1)), false, false);
    
                            //System.out.println(wrUpdate.toString());
                        } finally {
                            context.stop();
                        }
                    }
    
                    for (int i=1; i <= oltpNonIndexUpdates; i++) {
                        //for i=1, oltp_non_index_updates do
                        //   c_val = sb_rand_str("###########-###########-###########-###########-###########-###########-###########-###########-###########-###########")
                        //   query = "UPDATE " .. table_name .. " SET c='" .. c_val .. "' WHERE id=" .. sb_rand(1, oltp_table_size)
                        //   rs = db_query(query)
                        //   if rs then
                        //     print(query)
                        //   end
                        //end

                        //db.sbtest8.update({_id: 5523412}, {$set: {c: "hello there"}}, false, false)

                        int startId = rand.nextInt(numMaxInserts)+1;

                        String cVal = sysbenchString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");

                        final Timer.Context context = updateLatencies.time();
                        try {
                            WriteResult wrUpdate = coll.update(new BasicDBObject("_id", startId), new BasicDBObject("$set", new BasicDBObject("c",cVal)), false, false);

                            //System.out.println(wrUpdate.toString());
                        } finally {
                            context.stop();
                        }
                    }

                    for (int i=1; i <= oltpInserts; i++) {
                        //i = sb_rand(1, oltp_table_size)
                        //rs = db_query("DELETE FROM " .. table_name .. " WHERE id=" .. i)
                      
                        //db.sbtest8.remove({_id: 5523412})

                        int startId = rand.nextInt(numMaxInserts)+1;

                        WriteResult wrRemove = coll.remove(new BasicDBObject("_id", startId));

                        //c_val = sb_rand_str([[###########-###########-###########-###########-###########-###########-###########-###########-###########-###########]])
                        //pad_val = sb_rand_str([[###########-###########-###########-###########-###########]])
                        //rs = db_query("INSERT INTO " .. table_name ..  " (id, k, c, pad) VALUES " .. string.format("(%d, %d, '%s', '%s')",i, sb_rand(1, oltp_table_size) , c_val, pad_val))

                        BasicDBObject doc = new BasicDBObject();
                        doc.put("_id",startId);
                        doc.put("k",rand.nextInt(numMaxInserts)+1);
                        String cVal = sysbenchString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");
                        doc.put("c",cVal);
                        String padVal = sysbenchString(rand, "###########-###########-###########-###########-###########");
                        doc.put("pad",padVal);

                        final Timer.Context context = insertLatencies.time();
                        try {
                            WriteResult wrInsert = coll.insert(doc);
                        } finally {
                            context.stop();
                        }
                    }

                    numTransactions += 1;

                } finally {
                    if (bIsTokuMX && !auto_commit) {
                        // commit the transaction and release current connection in the pool
                        db.command("commitTransaction");
                        //--db.command("rollbackTransaction")
                        db.requestDone();
                    }
                    txnContext.stop();
                }
            }

            //} catch (Exception e) {
            //    logMe("Writer thread %d : EXCEPTION",threadNumber);
            //    e.printStackTrace();
            //}

            globalWriterThreads.decrementAndGet();
        }
    }


    public static String sysbenchString(java.util.Random rand, String thisMask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = thisMask.length() ; i < n ; i++) { 
            char c = thisMask.charAt(i); 
            if (c == '#') {
                sb.append(String.valueOf(rand.nextInt(10)));
            } else if (c == '@') {
                sb.append((char) (rand.nextInt(26) + 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void logMe(String format, Object... args) {
        System.out.println(Thread.currentThread() + String.format(format, args));
    }
}
