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
    private static final String HOME = "https://overrustlelogs.net/";
    
    /** Keyword to grab items from HTML */
    private static final String KEY_CLASS_TAG = "list-group-item list-group-item-action";
    
    /**
     * MAIN TO CONTROL.
     * 
     * @param strings
     */
    public static void main(String...strings) {
        String[] channels = {"Destiny", "Destinygg"};
        downloadChannels(Arrays.asList(channels));
        //downloadLogs();
    }
    
    /**
     * Download all logs.
     */
    public static void downloadLogs() {
        long startTime = System.nanoTime();
        
        // grab list of channels
        final Elements channels = grabList(HOME);
        
        // iterate thru list of channels
        for (Element c : channels) {
            String channelUrl = c.attr("abs:href");
            // grab list of months
            downloadChannel(channelUrl);
        }
        
        System.out.println("Time elapsed: " + (System.nanoTime() - startTime)/ 1_000_000_000 + "s");
    }
    
    /**
     * Download logs from a list of channels.
     * 
     * @param channels
     */
    private static void downloadChannels(List<String> channels) {
        long startTime = System.nanoTime();
        for (String c : channels) {
            String channelUrl = String.format("https://overrustlelogs.net/%s chatlog", c);
            
            downloadChannel(channelUrl);
        }
        System.out.println("Time elapsed: " + (System.nanoTime() - startTime)/ 1_000_000_000 + "s");
    }
    
    /**
     * Download logs form a specific channel.
     * 
     * @param channel URL
     */
    private static void downloadChannel(String url) {
        final Elements months = grabList(url);
        
        // iterate thru each months
        for (Element m : months) {
            String monthUrl = m.attr("abs:href");
            // grab list of items
            final Elements items = grabList(monthUrl);
            
            // iterate thru list of items
            for (Element i : items) {
                String itemUrl = i.attr("abs:href");
                // start download text files
                if (!itemUrl.contains("userlog")) {
                    itemUrl += ".txt";
                    download(itemUrl);
                }
            }
        }
    }
    
    /**
     * Grab list of items.
     * 
     * @param url
     * @return Elements
     */
    private static Elements grabList(String url) {
        try {
            // establish connection and get document
            final Document doc = Jsoup.connect(url).get();
            // get list
            return doc.getElementsByClass(KEY_CLASS_TAG);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    private static void download(String url) {
        System.out.println(url);
        try {
            String[] parts = url.split("/");
            String channel = parts[3];
            String month = parts[4];
            String textname = parts[5];
            // connect to url
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            // download text stream
            File file = new File(String.format(System.getProperty("user.dir") + "/chatlogs/%s/%s/%s", channel, month, textname));
            file.getParentFile().mkdirs();
            
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
