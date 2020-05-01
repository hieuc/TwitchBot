package functions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

/**
 * Download logs from Overrustle logs. 
 * Can download from channels of choice, or download any channel possible. 
 * Name of channel must be capitalized first letter.
 * RIP Overrustlelogs  May 1st 2020.
 * 
 * @author Victor
 * @since 4/28/2020
 */
public class LogCrawler {
    /** Main source of log. */
    private static final String HOME = "https://overrustlelogs.net";
    
    /** Path to logger file. */
    private static final String LOG_PATH = System.getProperty("user.dir") + "/crawlerlog.txt";
    
    /** Path to store logs. */
    private static final String DESTINATION_PATH = "D:";
    
    /** Logger for download history. */
    private static Logger logger; 
    
    /** Total files download count. */
    private static int totalFiles = 0;
    
    /** File count of a channel. */
    private static int channelFiles = 0;
    
    /** Directory size. */
    private static BigDecimal size = BigDecimal.ZERO;
    
    /** Total downloaded size. */
    private static BigDecimal totalSize = BigDecimal.ZERO;
    
    /**
     * MAIN TO CONTROL.
     * 
     * @param strings
     */
    public static void main(String...strings) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] %5$s %n");
        
        // initialize logger
        logger = Logger.getLogger("crawl log");
        FileHandler fh; 
        
        try {
            File temp = new File(LOG_PATH);
            if (!temp.exists()) {
                temp.getParentFile().mkdirs();
                temp.createNewFile();
            }
            fh = new FileHandler(LOG_PATH, true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        
        logger.log(Level.INFO, "Start downloading...");
        
        // start the meme
        
        // To download channels of choice:
        //String[] channels = {"Destiny", "Destinygg"};
        //downloadChannels(channels)
        
        // To download everything
        downloadLogs();
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
            
            //int startIndex = Arrays.asList(channels).indexOf("Tsm_theoddone");
            //int endIndex = Arrays.asList(channels).indexOf("Ufc");
            //channels = Arrays.copyOfRange(channels, startIndex, endIndex);
            
            downloadChannels(channels);
            
        } catch (Exception e) {
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
                .ignoreContentType(true).ignoreHttpErrors(true).timeout(20000).maxBodySize(0).followRedirects(true)
                .execute();
        int statusCode = res.statusCode();
        int reconnected = 0;
        while (statusCode != 200 && reconnected < 20) {
            System.out.println("Status code: " + statusCode);
            System.out.println("Message: " + res.statusMessage());
            System.out.println(String.format("Attempting to reconnect... (#%s)", reconnected + 1));
            Thread.sleep(1000);
            res = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1")
                    .ignoreContentType(true).ignoreHttpErrors(true).timeout(20000).maxBodySize(0).followRedirects(true)
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
                channelFiles = 0;
                size = BigDecimal.ZERO;
                long startCTime = System.nanoTime();
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
                
                String cTime = String.format("Channel %17s took: %10.2f s to download %7d files (%9s MB) ",
                        channel, (System.nanoTime() - startCTime)/ 1_000_000_000.0, channelFiles, size.divide(BigDecimal.valueOf(1024 * 1024), 3, RoundingMode.HALF_EVEN).toString());
                logger.log(Level.INFO, cTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String time = String.format("Total time elapsed: %.2f s. Total files downloaded: %d (%s MB)",
                (System.nanoTime() - startTime)/ 1_000_000_000.0, totalFiles, totalSize.divide(BigDecimal.valueOf(1024 * 1024), 3, RoundingMode.HALF_EVEN).toString());
        logger.log(Level.INFO, time);
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
            File file = new File(String.format(DESTINATION_PATH + "/chatlogs/%s/%s/%s", channel, month, textname));
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
                    return;
                System.out.println(String.format("Attempting to reconnect... (#%s)", reconnected + 1));
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
            if (statusCode == 200) {
                BigDecimal fileSize = BigDecimal.valueOf(file.length());
                channelFiles++;
                totalFiles++;
                size = size.add(fileSize);
                totalSize = totalSize.add(fileSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
