package functions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
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
        
        
        // grab list of channels
        
        
        try {
            // grab channel list
            Response res = connect(HOME + "/api/v1/channels.json");
            
            String[] channels = res.body().replace("\"", "").replace("[", "").replace("]", "").split(",");
            
            downloadChannels(channels);
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }
    
    /**
     * Connect to url, grab a response object. May handle some connection issues.
     * 
     * @param url
     * @return Response object 
     * @throws IOException on establishing connection 
     * @throws InterruptedException on reconnect delay
     */
    private static Response connect(String url) throws IOException, InterruptedException {
        System.out.println(url);
        Response res =  Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1")
                .ignoreContentType(true).ignoreHttpErrors(true).timeout(10000).maxBodySize(0).followRedirects(true)
                .execute();
        int statusCode = res.statusCode();
        int reconnected = 0;
        while (statusCode != 200 && reconnected < 20) {
            System.out.println("Status code: " + statusCode);
            System.out.println("Message: " + res.statusMessage());
            System.out.println(String.format("Attempting (#%s) to reconnect...", reconnected + 1));
            Thread.sleep(1000);
            res = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1")
                    .ignoreContentType(true).ignoreHttpErrors(true).timeout(10000).maxBodySize(0).followRedirects(true)
                    .execute();
            statusCode = res.statusCode();
            reconnected++;
        }
        return res;
    }
    
    /**
     * Download logs form a list of channels.
     * 
     * @param channels
     */
    private static void downloadChannels(String[] channels) {
        long startTime = System.nanoTime();
        
        try {
            for (String channel : channels) {
                channel = channel.trim();
                
                // grab the months
                final Response res = connect(String.format(HOME + "/api/v1/%s/months.json", channel));
                String[] months = res.body().replace("\"", "").replace("[", "").replace("]", "").split(",");
                
                // iterate thru each months
                for (String month : months) {
                    month = month.trim();
                    
                    // grab list of dates/items
                    final Response res1 = connect(String.format(HOME + "/api/v1/%s/%s/days.json", channel, month));
                    String[] items = res1.body().replace("\"", "").replace("[", "").replace("]", "").split(",");
                    
                    // iterate thru list of items
                    for (String item : items) {
                        item = item.trim();
                        
                        // start download text files
                        download(String.format(HOME + "/%s chatlog/%s/%s", channel, month, item));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Time elapsed: " + (System.nanoTime() - startTime)/ 1_000_000_000 + "s");
    }

    private static void download(String url) {
        if (!url.contains(".txt"))
            return;
        
        System.out.println(url);
        try {
            String[] parts = url.split("/");
            String channel = parts[3];
            String month = parts[4];
            String textname = parts[5];
            // prepare file
            File file = new File(String.format("D:" + "/chatlogs/%s/%s/%s", channel, month, textname));
            file.getParentFile().mkdirs();
            
            // connect to url
            URL website = new URL(url); 
            HttpURLConnection http = (HttpURLConnection) website.openConnection();
            int statusCode = http.getResponseCode();
            int reconnected = 0;
            while (statusCode != 200 && reconnected < 30) {
                System.out.println("Status code: " + statusCode);
                System.out.println("Message: " + http.getResponseMessage());
                // code 500, file not available
                if (statusCode == 500) 
                    break;
                System.out.println(String.format("Attempting (#%s) to reconnect...", reconnected + 1));
                Thread.sleep(1000);
                http = (HttpURLConnection) website.openConnection();
                statusCode = http.getResponseCode();
                reconnected++;
            }
            //website.openStream();
            ReadableByteChannel rbc = Channels.newChannel(http.getInputStream());
            // download text stream
            
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
