package functions;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The class detect combo of an emote in a chat room.
 * 
 * @author Victor Chau
 * @version October 2019
 */
public class EmoteCombo {
    private static final String EMOTE_PATH = System.getProperty("user.dir") 
            + "/resources/emotes.txt";

    private Set<String> currentEmote; // handle the stream of emotes
    private int combo;
    private int prevCombo; // previous combo
    private Set<String> emotes; // collection of emotes
    private String emote; // current selected emote
    private String prevEmote; // previous selected emote
    private long timeStamp;

    /**
     * Initialize variables
     */
    public EmoteCombo() {
        this.currentEmote = new TreeSet<String>();
        this.combo = 0;
        this.timeStamp = 0;
        this.emote = "";
        this.prevCombo = 0;
        this.prevEmote = "";

        // set up all considered emotes
        emotes = new TreeSet<>();
        try {
            BufferedReader read = Files.newBufferedReader(Paths.get(EMOTE_PATH));
            String emote = "";
            while ((emote = read.readLine()) != null)
                emotes.add(emote.trim());
            read.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Take in a string of message and determine combo
     * 
     * @param string message line
     * @return true if combo is broken and worth printing
     * @return false if else
     */
    public boolean input(String line) {
        Scanner cursor = new Scanner(line);
        String word = "";

        // make sure the combo streak is restarted
        if (currentEmote.isEmpty())
            this.combo = 0;

        // time control
        if (timeStamp == 0)
            timeStamp = System.nanoTime();

        long deltaTime = (long) ((System.nanoTime() - timeStamp) / Math.pow(10, 9));
        // word control
        boolean emoteInLine = false;

        while (cursor.hasNext()) {
            word = cursor.next();
            // "clap" is ignored if an emote is already presented in line
            if (isEmote(word) && !(word.equals("Clap") && emoteInLine)) {
                if (!currentEmote.isEmpty() && !word.equals(currentEmoteInSet()) || deltaTime > 5) {
                    // end a streak
                    conclude();
                    // continue a new streak if previous one is too short
                    if (this.combo < 3 && !emoteInLine) {
                        this.currentEmote.add(word);
                        timeStamp = System.nanoTime();
                        this.combo = 0;
                    }
                    cursor = new Scanner("");
                } else {
                    // continue a streak
                    timeStamp = System.nanoTime();
                    this.currentEmote.add(word);
                }
                emoteInLine = true;
            }
        }
        cursor.close();

        // analyze result
        if (!emoteInLine)
            conclude();

        if (currentEmote.size() == 1)
            this.combo++;

        System.out.println("current emote set: " + this.currentEmote + " combo " + this.combo);
        if (combo > 2 && currentEmote.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * return first in emote set
     */
    private String currentEmoteInSet() {
        for (String e : currentEmote)
            return e;
        return "";
    }

    /**
     * choose the emote and restart the set, only use when a combo is broken
     */
    private void conclude() {
        this.emote = currentEmoteInSet();
        this.currentEmote = new TreeSet<String>();
    }

    /**
     * check current emote
     * 
     * @return emote current emote that is being considered as a string
     */
    public String currentEmote() {
        return this.emote;
    }

    /**
     * return combo
     * 
     * @return combo current combo
     */
    public int currentCombo() {
        return this.combo;
    }

    /**
     * check if last combo is identical and update the current one
     * 
     * @return true if (condition) false otherwise
     */
    public boolean isRepeated() {
        boolean result = this.combo == this.prevCombo && this.emote.contentEquals(this.prevEmote);

        // only need to check for twice repeated
        if (result)
            this.prevCombo = 0;
        else // update
            this.prevCombo = this.combo;

        this.prevEmote = this.emote;

        return result;
    }

    /**
     * check if the string is an emote
     * 
     * @param s the string being checked
     * @return true if the string is an emote, false otherwise
     */
    public boolean isEmote(String s) {
        return emotes.contains(s);
    }
}
