package functions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Download all logs from Overrustle logs. 
 * 
 * @author Victor
 * 
 */
public class LogCrawler {
    /** Main source of log. */
    private static final String HOME = "https://overrustlelogs.net";
    
    /**
     * MAIN TO CONTROL.
     * 
     * @param strings
     */
    public static void main(String...strings) {
        String[] channels = {"Destiny", "Destinygg"};
        downloadChannels(channels);
        //downloadLogs();
    }
    
    /**
     * Download all logs.
     */
    public static void downloadLogs() {
        long startTime = System.nanoTime();
        
        // grab list of channels
        final Connection con = Jsoup.connect(HOME + "/api/v1/channels.json");
        
        try {
            String res = con.ignoreContentType(true).execute().body();
            String[] channels = res.replace("\"", "").replace("[", "").replace("]", "").split(",");
            
            downloadChannels(channels);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("Time elapsed: " + (System.nanoTime() - startTime)/ 1_000_000_000 + "s");
    }

    
    /**
     * Download logs form a list of channels.
     * 
     * @param channels
     */
    private static void downloadChannels(String[] channels) {
        try {
            for (String channel : channels) {
                // grab the months
                final Connection con = Jsoup.connect(String.format(HOME + "/api/v1/%s/months.json", channel));
                String res = con.ignoreContentType(true).execute().body();
                String[] months = res.replace("\"", "").replace("[", "").replace("]", "").split(",");
                
                // iterate thru each months
                for (String month : months) {
                    
                    // grab list of dates/items
                    final Connection con1 = Jsoup.connect(String.format(HOME + "/api/v1/%s/%s/days.json", channel, month));
                    String res1 = con1.ignoreContentType(true).execute().body();
                    String[] items = res1.replace("\"", "").replace("[", "").replace("]", "").split(",");
                    
                    // iterate thru list of items
                    for (String item : items) {
                        // start download text files
                        download(String.format(HOME + "/%s chatlog/%s/%s", channel, month, item));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void download(String url) {
        System.out.println(url);
        try {
            String[] parts = url.split("/");
            String channel = parts[3];
            String month = parts[4];
            String textname = parts[5];
            // prepare file
            File file = new File(String.format(System.getProperty("user.dir") + "/chatlogs/%s/%s/%s", channel, month, textname));
            file.getParentFile().mkdirs();
            
            // connect to url
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            // download text stream
            
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
