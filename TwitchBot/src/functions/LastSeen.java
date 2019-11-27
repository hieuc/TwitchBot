package functions;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A class to organize the lastseen function.
 * 
 * @author Victor Chau
 * @seen Oct 27 2019
 */

public class LastSeen {
    /** Main source of logs. */
    private static final String LOGS_MAIN_URL = "https://overrustlelogs.net";

    /** Stalk url. */
    private static final String STALK_URL = "/stalk?channel=%s&nick=%s";

    /** Keyword to grab items from HTML */
    private static final String KEY_CLASS_TAG = "list-group-item list-group-item-action";

    /** Keyword to grab alert from HTML. */
    private static final String ALERT_CLASS_TAG = "alert alert-danger alert-dismissible fade show";

    /** URL to a user log file. */
    private static final String LOG_FILE_URL = "https://overrustlelogs.net/%s chatlog/%s/userlogs/%s.txt";

    /** DateTime formatter. */
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Default list of channels. */
    private static final List<String> DEFAULT_CHANNELS = new ArrayList<>(Arrays.asList("destiny", "destinygg"));

    /** Delay of each call (in seconds). */
    private static final int CALL_DELAY = 5;

    /** List of all channels available. */
    private List<String> channels;

    /** Instant when the object was last used. */
    private Instant time;

    /**
     * Initialize fields.
     */
    public LastSeen() {
        channels = DEFAULT_CHANNELS;
        time = Instant.now();
        // updateChannels();
    }

    /**
     * Find the last chat line of username in a specific channel and return its
     * timestamp.
     * 
     * @param user       target username
     * @param targetChan target channel to search user
     * @return timestamp of user's last chat line
     */
    public String find(final String user, String channel) {
        this.channels = new ArrayList<>();
        channel = channel.replace("@", "");
        if (channel.contentEquals("dgg")) {
            channel = "destinygg";
        }
        this.channels.add(channel);

        final String result = find(user);

        this.channels = DEFAULT_CHANNELS;
        return result;
    }

    /**
     * Find the last chat line of username and return when it occurred.
     * 
     * @param username
     * @return timestamp of user's last chat line
     */
    public String find(final String user) {
        final String username = user.replace("@", "");
        final Instant startT = Instant.now();
        StringBuilder result = new StringBuilder("@" + username);
        final Map<LocalDateTime, String> timeMap = new TreeMap<>(Collections.reverseOrder());

        for (final String channel : this.channels) {
            final String stalkURL = String.format(LOGS_MAIN_URL + STALK_URL, channel, username);
            try {
                if (parseText(stalkURL, ALERT_CLASS_TAG).isEmpty()) {
                    final String date = getFirstMonth(parseBody(stalkURL));
                    final String logURL = String.format(LOG_FILE_URL, upperCaseFirstLetter(channel), date, username);
                    final String line = getLastLine(logURL);
                    final LocalDateTime dateO = makeDate(line.substring(1, line.indexOf('U') - 1));
                    timeMap.put(dateO, channel);
                }
            } catch (NullPointerException nullE) {
                // Possibly means targeted channel not found on overrustlelogs
            }
        }

        this.time = Instant.now();
        final Duration timeElapsed = Duration.between(startT, time);
        System.out.println("Finished in " + timeElapsed.toMillis() / 1000.0 + "s");

        if (timeMap.isEmpty()) {
            result = new StringBuilder("User not found.");
        } else {
            final Iterator<LocalDateTime> itr = timeMap.keySet().iterator();
            final LocalDateTime latestDate = itr.next();

            result.append(" was in channel #");
            result.append(timeMap.get(latestDate));
            result.append(" on ");
            result.append(latestDate.toString().replace("T", " at "));
            result.append(" (UTC) (");
            result.append(timeDifference(latestDate));
            result.append(") :^ )");
        }
        return result.toString();
    }

    /**
     * Check if the object is available for next call.
     * 
     * @return true if time since last call was longer than call delay, false
     *         otherwise.
     */
    public boolean isAvailable() {
        return Duration.between(time, Instant.now()).toSeconds() > CALL_DELAY;
    }

    /**
     * Get a stalk URL of a user without specify a channel (default destiny).
     * 
     * @param user person being stalked
     * @return the stalk URL
     */
    public String getStalkURL(final String user) {
        return getStalkURL(user, "destiny");
    }

    /**
     * Get a stalk URL of a user at a specific channel.
     * 
     * @param user    person being stalked
     * @param channel location being stalked
     * @return the stalk URL
     */
    public String getStalkURL(final String user, String channel) {
        channel = channel.replace("@", "");
        if (channel.contentEquals("dgg")) {
            channel = "destinygg";
        }

        return String.format(LOGS_MAIN_URL + STALK_URL, channel, user.replace("@", ""));
    }

    //-----------------------------------------------------------------------------------------------------------

    /**
     * Return a LocalDateTime object.
     * 
     * @param date string in format yyyy-MM-dd HH:mm:ss
     * @return LocalDateTime object
     */
    private LocalDateTime makeDate(final String date) {
        return LocalDateTime.parse(date, dateFormatter);
    }

    /**
     * Update list of available channels.
     */
    private void updateChannels() {
        final List<String> parsedChannelList = parseList(LOGS_MAIN_URL);

        for (final String s : parsedChannelList) {
            final String chan = s.split(" ")[0].trim().toLowerCase();
            channels.add(chan);
        }
    }

    /**
     * Calculate time difference of timestamp respect to current time.
     * 
     * @param date LocalDateTime object of timestamp
     * @return difference of time as a String
     */
    private String timeDifference(final LocalDateTime date) {
        long diff = Duration.between(date, LocalDateTime.now(Clock.systemUTC())).toSeconds();
        StringBuilder result = new StringBuilder();
        int element = 0;

        final long years = TimeUnit.SECONDS.toDays(diff) / 365;
        diff -= TimeUnit.DAYS.toSeconds(years * 365);
        if (years > 0) {
            if (years < 2)
                result.append("a year ");
            else
                result.append(years + " years ");
            element++;
        }

        final long months = TimeUnit.SECONDS.toDays(diff) / 30;
        diff -= TimeUnit.DAYS.toSeconds(months * 30);
        if (months > 0) {
            if (element != 1)
                result.append("~ ");
            if (months < 2)
                result.append("a month ");
            else
                result.append(months + " months ");
            element++;
        }

        final long days = TimeUnit.SECONDS.toDays(diff);
        diff -= TimeUnit.DAYS.toSeconds(days);
        if (days > 0 && element < 2) {
            if (days < 2)
                result.append("a day ");
            else
                result.append(days + " days ");
            element++;
        }

        final long hours = TimeUnit.SECONDS.toHours(diff);
        diff -= TimeUnit.HOURS.toSeconds(hours);
        if (hours > 0 && element < 2) {
            if (hours < 2)
                result.append("an hour ");
            else
                result.append(hours + " hours ");
            element++;
        }

        final long minutes = TimeUnit.SECONDS.toMinutes(diff);
        diff -= TimeUnit.MINUTES.toMillis(minutes);
        if (minutes > 0 && element < 2) {
            if (minutes < 2)
                result.append("a minutes ");
            else
                result.append(minutes + " minutes ");
            element++;
        }

        if (diff > 0 && element < 2) {
            if (diff < 8)
                result.append("a few seconds ");
            else
                result.append(diff + " seconds ");
            element++;
        }
        result.append("ago");
        return result.toString();
    }

    /**
     * Parse a list of elements from a given link
     * 
     * @param URL to pasre list
     * @return resulted list (null if not found)
     */
    private List<String> parseList(final String url) {
        return parsebyClass(url, KEY_CLASS_TAG).eachText();
    }

    /**
     * 
     * @param URL to target location.
     * @param tag class tag to parse.
     * @return a String parsed from the HTML.
     */
    private String parseText(final String url, final String tag) {
        return parsebyClass(url, tag).text();
    }

    /**
     * Parse HTML by class.
     * 
     * @param URL   to target location.
     * @param class tag to parse.
     * @return an Elements object contain parsed content.
     */
    private Elements parsebyClass(final String url, final String tag) {
        return parseBody(url).getElementsByClass(tag);
    }

    /**
     * Parse the body of HTML.
     * 
     * @param URL to target location.
     * @return body of HTML as an Element object.
     */
    private Element parseBody(final String url) {
        Element result = null;

        try {
            final Connection con = Jsoup.connect(url);
            final Document doc = con.get();
            result = doc.body();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Return the month/year
     * 
     * @param e parsed Element object
     * @return time as a string in format "m yyyy"
     */
    private String getFirstMonth(final Element e) {
        final String id = "month0";
        final String clas = "card-header";

        return e.getElementById(id).getElementsByClass(clas).text().replace("Load", "").trim();
    }

    /**
     * Capitalize the first letter in given string.
     * 
     * @param string input
     * @return string result
     */
    private String upperCaseFirstLetter(final String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Return the last in log file.
     * 
     * @param url of log file
     * @return last line
     */
    private String getLastLine(final String url) {
        //        String before = "";
        //        String current = "";

        try {
            String text = Jsoup.connect(url).execute().body();
            return text.substring(text.substring(0, text.length() - 2).lastIndexOf("\n") + 1);
            //            final BufferedReader input = new BufferedReader(new StringReader(text));
            //            while ((current = input.readLine()) != null) {
            //                before = current;
            //            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
        //        return before;
    }

}
