//import com.mongodb.Mongo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

public class jmongosysbenchload {
    public static AtomicLong globalInserts = new AtomicLong(0);
    public static AtomicLong globalWriterThreads = new AtomicLong(0);

    public static Writer writer = null;
    public static boolean outputHeader = true;

    public static int numCollections;
    public static String dbName;
    public static int writerThreads;
    public static Integer numMaxInserts;
    public static int documentsPerInsert;
    public static long insertsPerFeedback;
    public static long secondsPerFeedback;
    public static String compressionType;
    public static int basementSize;
    public static String logFileName;
    public static String indexTechnology;
    public static String myWriteConcern;
    public static String serverName;
    public static int serverPort;
    public static String userName;
    public static String passWord;

    public static int allDone = 0;

    public jmongosysbenchload() {
    }

    public static void main (String[] args) throws Exception {
        if (args.length != 15) {
            logMe("*** ERROR : CONFIGURATION ISSUE ***");
            logMe("jsysbenchload [number of collections] [database name] [number of writer threads] [documents per collection] [documents per insert] [inserts feedback] [seconds feedback] [log file name] [compression type] [basement node size (bytes)]  [writeconcern] [server] [port] [username] [password]");
            System.exit(1);
        }

        numCollections = Integer.valueOf(args[0]);
        dbName = args[1];
        writerThreads = Integer.valueOf(args[2]);
        numMaxInserts = Integer.valueOf(args[3]);
        documentsPerInsert = Integer.valueOf(args[4]);
        insertsPerFeedback = Long.valueOf(args[5]);
        secondsPerFeedback = Long.valueOf(args[6]);
        logFileName = args[7];
        compressionType = args[8];
        basementSize = Integer.valueOf(args[9]);
        myWriteConcern = args[10];
        serverName = args[11];
        serverPort = Integer.valueOf(args[12]);
        userName = args[13];
        passWord = args[14];

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
        logMe("--------------------------------------------------");
        logMe("  %d collections",numCollections);
        logMe("  database name = %s",dbName);
        logMe("  %d writer thread(s)",writerThreads);
        logMe("  %,d documents per collection",numMaxInserts);
        logMe("  Documents Per Insert = %d",documentsPerInsert);
        logMe("  Feedback every %,d seconds(s)",secondsPerFeedback);
        logMe("  Feedback every %,d inserts(s)",insertsPerFeedback);
        logMe("  logging to file %s",logFileName);
        logMe("  write concern = %s",myWriteConcern);
        logMe("  Server:Port = %s:%d",serverName,serverPort);
        logMe("  Username = %s",userName);

        MongoClientOptions clientOptions = new MongoClientOptions.Builder().connectionsPerHost(2048).socketTimeout(60000).writeConcern(myWC).build();
        ServerAddress srvrAdd = new ServerAddress(serverName,serverPort);

        // Credential login is optional.
        MongoClient m;
        if (userName.isEmpty() || userName.equalsIgnoreCase("none")) {
            m = new MongoClient(srvrAdd);
        } else {
            MongoCredential credential = MongoCredential.createCredential(userName, dbName, passWord.toCharArray());
            m = new MongoClient(srvrAdd, Arrays.asList(credential));
        }

        logMe("mongoOptions | " + m.getMongoOptions().toString());
        logMe("mongoWriteConcern | " + m.getWriteConcern().toString());

        DB db = m.getDB(dbName);

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

        logMe("  index technology = %s",indexTechnology);

        if (indexTechnology.toLowerCase().equals("tokumx")) {
            logMe("  + compression type = %s",compressionType);
            logMe("  + basement node size (bytes) = %d",basementSize);
        }

        logMe("--------------------------------------------------");

        try {
            writer = new BufferedWriter(new FileWriter(new File(logFileName)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ((!indexTechnology.toLowerCase().equals("tokumx")) && (!indexTechnology.toLowerCase().equals("mongo"))) {
            // unknown index technology, abort
            logMe(" *** Unknown Indexing Technology %s, shutting down",indexTechnology);
            System.exit(1);
        }

        jmongosysbenchload t = new jmongosysbenchload();

        Thread reporterThread = new Thread(t.new MyReporter());
        reporterThread.start();

        Thread[] tWriterThreads = new Thread[writerThreads];

        for (int collectionNumber = 0; collectionNumber < numCollections; collectionNumber++) {
            // if necessary, wait for an available slot for this loader
            boolean waitingForSlot = true;
            while (globalWriterThreads.get() >= writerThreads) {
                if (waitingForSlot) {
                    logMe("  collection %d is waiting for an available loader slot",collectionNumber+1);
                    waitingForSlot = false;
                }
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // start the loader
            for (int i=0; i<writerThreads; i++) {
                if ((tWriterThreads[i] == null) || (!tWriterThreads[i].isAlive())) {
                    globalWriterThreads.incrementAndGet();
                    tWriterThreads[i] = new Thread(t.new MyWriter(collectionNumber+1, writerThreads, i, numMaxInserts, db));
                    tWriterThreads[i].start();
                    break;
                }
            }

        }

        // sleep a bit, then wait for all the writers to complete
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (globalWriterThreads.get() > 0) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // wait for writer threads to terminate
        //for (int i=0; i<writerThreads; i++) {
        //    if (tWriterThreads[i].isAlive())
        //        tWriterThreads[i].join();
        //}

        // all the writers are finished
        allDone = 1;

        if (reporterThread.isAlive())
            reporterThread.join();

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // m.dropDatabase("mydb");

        m.close();

        logMe("Done!");
    }

    class MyWriter implements Runnable {
        int collectionNumber;
        int threadCount;
        int threadNumber;
        int numTables;
        int numMaxInserts;
        DB db;

        java.util.Random rand;

        MyWriter(int collectionNumber, int threadCount, int threadNumber, int numMaxInserts, DB db) {
            this.collectionNumber = collectionNumber;
            this.threadCount = threadCount;
            this.threadNumber = threadNumber;
            this.numMaxInserts = numMaxInserts;
            this.db = db;
            rand = new java.util.Random((long) collectionNumber);
        }
        public void run() {
            String collectionName = "sbtest" + Integer.toString(collectionNumber);

            if (indexTechnology.toLowerCase().equals("tokumx")) {
                DBObject cmd = new BasicDBObject();
                cmd.put("create", collectionName);
                cmd.put("compression", compressionType);
                cmd.put("readPageSize", basementSize);
                //cmd.put("basementSize", basementSize);
                CommandResult result = db.command(cmd);
                //logMe(result.toString());
            } else if (indexTechnology.toLowerCase().equals("mongo")) {
                // nothing special to do for a regular mongo collection

            } else {
                // unknown index technology, abort
                logMe(" *** Unknown Indexing Technology %s, shutting down",indexTechnology);
                System.exit(1);
            }

            logMe("Writer thread %d : creating collection %s",threadNumber, collectionName);

            DBCollection coll = db.getCollection(collectionName);

            BasicDBObject idxOptions = new BasicDBObject();
            idxOptions.put("background",false);

            if (indexTechnology.toLowerCase().equals("tokumx")) {
                idxOptions.put("compression",compressionType);
                idxOptions.put("readPageSize",basementSize);
            }

            logMe("Writer thread %d : creating collection %s secondary index",threadNumber, collectionName);

            coll.ensureIndex(new BasicDBObject("k", 1), idxOptions);

            long numInserts = 0;
            int id = 0;

            try {
                logMe("Writer thread %d : started to load collection %s",threadNumber, collectionName);

                BasicDBObject[] aDocs = new BasicDBObject[documentsPerInsert];

                int numRounds = numMaxInserts / documentsPerInsert;

                for (int roundNum = 0; roundNum < numRounds; roundNum++) {
                    for (int i = 0; i < documentsPerInsert; i++) {
                        id++;
                        BasicDBObject doc = new BasicDBObject();
                        doc.put("_id",id);
                        doc.put("k",rand.nextInt(numMaxInserts)+1);
                        String cVal = sysbenchString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");
                        doc.put("c",cVal);
                        String padVal = sysbenchString(rand, "###########-###########-###########-###########-###########");
                        doc.put("pad",padVal);
                        aDocs[i]=doc;
                    }

                    coll.insert(aDocs);
                    numInserts += documentsPerInsert;
                    globalInserts.addAndGet(documentsPerInsert);
                }

            } catch (Exception e) {
                logMe("Writer thread %d : EXCEPTION",threadNumber);
                e.printStackTrace();
            }

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


    // reporting thread, outputs information to console and file
    class MyReporter implements Runnable {
        public void run()
        {
            long t0 = System.currentTimeMillis();
            long lastInserts = 0;
            long lastMs = t0;
            long intervalNumber = 0;
            long nextFeedbackMillis = t0 + (1000 * secondsPerFeedback * (intervalNumber + 1));
            long nextFeedbackInserts = lastInserts + insertsPerFeedback;
            long thisInserts = 0;

            while (allDone == 0)
            {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long now = System.currentTimeMillis();
                thisInserts = globalInserts.get();
                if (((now > nextFeedbackMillis) && (secondsPerFeedback > 0)) ||
                    ((thisInserts >= nextFeedbackInserts) && (insertsPerFeedback > 0)))
                {
                    intervalNumber++;
                    nextFeedbackMillis = t0 + (1000 * secondsPerFeedback * (intervalNumber + 1));
                    nextFeedbackInserts = (intervalNumber + 1) * insertsPerFeedback;

                    long elapsed = now - t0;
                    long thisIntervalMs = now - lastMs;

                    long thisIntervalInserts = thisInserts - lastInserts;
                    double thisIntervalInsertsPerSecond = thisIntervalInserts/(double)thisIntervalMs*1000.0;
                    double thisInsertsPerSecond = thisInserts/(double)elapsed*1000.0;

                    if (secondsPerFeedback > 0)
                    {
                        logMe("%,d inserts : %,d seconds : cum ips=%,.2f : int ips=%,.2f", thisInserts, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
                    } else {
                        logMe("%,d inserts : %,d seconds : cum ips=%,.2f : int ips=%,.2f", intervalNumber * insertsPerFeedback, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
                    }

                    try {
                        if (outputHeader)
                        {
                            writer.write("tot_inserts\telap_secs\tcum_ips\tint_ips\n");
                            outputHeader = false;
                        }

                        String statusUpdate = "";

                        if (secondsPerFeedback > 0)
                        {
                            statusUpdate = String.format("%d\t%d\t%.2f\t%.2f\n",thisInserts, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
                        } else {
                            statusUpdate = String.format("%d\t%d\t%.2f\t%.2f\n",intervalNumber * insertsPerFeedback, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
                        }
                        writer.write(statusUpdate);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    lastInserts = thisInserts;

                    lastMs = now;
                }
            }

            // output final numbers...
            long now = System.currentTimeMillis();
            thisInserts = globalInserts.get();
            intervalNumber++;
            nextFeedbackMillis = t0 + (1000 * secondsPerFeedback * (intervalNumber + 1));
            nextFeedbackInserts = (intervalNumber + 1) * insertsPerFeedback;
            long elapsed = now - t0;
            long thisIntervalMs = now - lastMs;
            long thisIntervalInserts = thisInserts - lastInserts;
            double thisIntervalInsertsPerSecond = thisIntervalInserts/(double)thisIntervalMs*1000.0;
            double thisInsertsPerSecond = thisInserts/(double)elapsed*1000.0;
            if (secondsPerFeedback > 0)
            {
                logMe("%,d inserts : %,d seconds : cum ips=%,.2f : int ips=%,.2f", thisInserts, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
            } else {
                logMe("%,d inserts : %,d seconds : cum ips=%,.2f : int ips=%,.2f", intervalNumber * insertsPerFeedback, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
            }
            try {
                if (outputHeader)
                {
                    writer.write("tot_inserts\telap_secs\tcum_ips\tint_ips\n");
                    outputHeader = false;
                }
                String statusUpdate = "";
                if (secondsPerFeedback > 0)
                {
                    statusUpdate = String.format("%d\t%d\t%.2f\t%.2f\n",thisInserts, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
                } else {
                    statusUpdate = String.format("%d\t%d\t%.2f\t%.2f\n",intervalNumber * insertsPerFeedback, elapsed / 1000l, thisInsertsPerSecond, thisIntervalInsertsPerSecond);
                }
                writer.write(statusUpdate);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public static void logMe(String format, Object... args) {
        System.out.println(Thread.currentThread() + String.format(format, args));
    }
}
