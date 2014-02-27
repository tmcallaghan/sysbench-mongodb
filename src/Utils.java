import java.util.Random;

import org.bson.types.Binary;

import com.mongodb.BasicDBObject;


public class Utils {

    public static String sysbenchString(Random rand, String thisMask) {
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

    public static BasicDBObject buildDocument(Random rand, int startId, int numMaxInserts, String bigPadding) {
        BasicDBObject doc = new BasicDBObject();
        doc.put("_id",startId);
        doc.put("k",rand.nextInt(numMaxInserts)+1);
        String cVal = Utils.sysbenchString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");
        doc.put("c",cVal);
        String padVal = Utils.sysbenchString(rand, "###########-###########-###########-###########-###########");
        doc.put("pad",padVal);
        doc.put("bigPadding", bigPadding); 
        return doc;
    }
    
    
    public static String generateRandomString(int length, double compressibility) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        
        int spaceSize = (int)(length * compressibility);
        for (int i = 0; i < length; ++i) {
            if (i < spaceSize) {
                sb.append(' ');
            } else {
                sb.append(r.nextInt(10));
            }
        }
        return sb.toString();
    }

}
