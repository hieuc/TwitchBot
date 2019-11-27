package bot;

/**
 * Twitch Bot that does random things.
 * 
 * @author Victor Chau
 * @version November 2019
 */
public class BotMain {
    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] %5$s %n"); //[%4$-4s]

        Bot bot = new Bot();   

        bot.connect();

        bot.joinChannel("destiny"); 

        bot.start();

    }
}