package bot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

import functions.EmoteCombo;
import functions.LastSeen;

import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * The core of the bot.
 * 
 * @author Victor Chau
 * @since November 2019
 */
public class Bot 
{
    /** General log path. */
    private static final String LOG_PATH = System.getProperty("user.dir") + "/log/";
    
    /** Login info path. */
    private static final String LOGIN_INFO_PATH = System.getProperty("user.dir") 
            + "/resources/logininfo.txt";

    /** Random object for miscellaneous. */
    private static final Random rand = new Random();
    
    /** Output communicator to socket. */
    private BufferedWriter writer; 

    /** Input communicator to socket. */
    private BufferedReader reader;

    /** Run time of the bot */
    private long runTime;

    /** Idle status. For server response consistency. */
    private boolean idle;
    
    /** Login username. */
    private static String username;
    
    /** Login oauth access token. */
    private static String pass;
    
    /** */
    private FileHandler fh;
    private Logger chatLogger;
    private FileHandler bfh;

    /** Current connected channel. */
    private String channel;


    private static final Logger logger = Logger.getLogger(Bot.class.getName());


    /**
     * Public constructor. Initializes some fields.
     */
    public Bot() {
        idle = false;
        channel = "";
        chatLogger = Logger.getLogger("Chat Log");
        try {
            final BufferedReader input = Files.newBufferedReader(Paths.get(LOGIN_INFO_PATH));
            username = input.readLine().trim();
            pass = input.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Start a session.
     */
    public void connect() {
        connect("irc.twitch.tv", 6667);
    }  


    /**
     * Connect to socket.
     * 
     * @param host
     * @param port
     */
    private void connect(final String host, final int port) 
    {
        try {  
            // initialize sender and receiver
            Socket socket = new Socket(host, port);
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            // Send info to connect
            this.writer.write("PASS " + pass + "\r\n");
            this.writer.write("NICK " + username + "\r\n");
            this.writer.write("CAP REQ :twitch.tv/commands\r\n");
            //         this.writer.write("CAP REQ :twitch.tv/membership\r\n");
            this.writer.flush();
            // file handler for bug logging in general
            bfh = new FileHandler(LOG_PATH + "BugLog.txt", true);
            logger.addHandler(bfh);
            bfh.setFormatter(new SimpleFormatter());
            bfh.setEncoding("UTF-8");

            String line = "";
            // take greeting lines
            while ((line = this.reader.readLine()) != null) {
                logger.log(Level.INFO, line);
                if (line.contains("376"))
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 


    /**
     * Send message to a channel chat.
     * 
     * @param string message
     * @param string channel
     */
    public void sendMessage(final String message, final String channel)
    {  
        try 
        {
            this.writer.write("PRIVMSG #" + channel + " :" + "> " + message + "\r\n");
            this.writer.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "> MSG " + channel + " :" + "> " + message);
    }


    /**
     * Start receiving information from socket.
     */
    public void start()    
    {
        String line = "";
        runTime = System.currentTimeMillis(); // track the time

        // Functions
        final EmoteCombo ec = new EmoteCombo();
        final LastSeen ls = new LastSeen();


        try 
        {   
            while (true)
            {  //FIX THIS PART
                line = this.reader.readLine();

                if (System.currentTimeMillis() - runTime > 350000) // over 5 min
                    idle = true;

                if (idle) // send a ping to server every 10 min (if idle)
                {
                    this.writer.write("PING :tmi.twitch.tv\r\n");
                    this.writer.flush();
                    logger.log(Level.INFO, "< PING");
                    runTime = System.currentTimeMillis();
                    idle = false;
                }

                if (line == null) 
                {
                    logger.log(Level.INFO, "> Disconnected from server!");
                    break;
                }
                else if (line.toLowerCase().startsWith("ping")) // respond to server ping
                {
                    logger.log(Level.INFO, "> PING");
                    this.writer.write("PONG :" + line.substring(6) + "\r\n");
                    logger.log(Level.INFO, "< PONG");
                }             
                else if (line.contains("PRIVMSG")) // respond to chat activity 
                {
                    String chatUsername = line.split("!")[0].replace(":", "");
                    String message = line.substring(chatUsername.length()*3 + channel.length() + 29);

                    // last seen
                    if (message.trim().startsWith("!seen") && ls.isAvailable()) {
                        String response;
                        String[] msgTokens = message.split(" ");
                        if (msgTokens.length < 2) {
                            response = ls.find(chatUsername);
                        } else if (msgTokens.length > 2){
                            response = ls.find(msgTokens[1], msgTokens[2]);
                        } else {
                            response = ls.find(msgTokens[1]);
                        }
                        sendMessage(response, channel);
                    }

                    // stalk
                    if (message.trim().startsWith("!stalk") && ls.isAvailable()) {
                        // Because I'm not a mod
                        try {
                            Thread.sleep(1200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String response;
                        final String[] msgTokens = message.split(" ");
                        if (msgTokens.length < 2) {
                            response = ls.getStalkURL(chatUsername);
                        } else if (msgTokens.length > 2){
                            response = ls.getStalkURL(msgTokens[1], msgTokens[2]);
                        } else {
                            response = ls.getStalkURL(msgTokens[1]);
                        }
                        sendMessage(response, channel);
                    }


                    //check combo
                    //                    if (ec.input(message)) {
                    //                        String msg = ec.currentEmote() + " x" + ec.currentCombo();
                    //                        if (ec.currentEmote().equals("TriHard"))
                    //                            msg = msg.replace("TriHard", "racism is not funny :)");
                    //                        if (ec.isRepeated())
                    //                            msg = "/me " + msg;
                    //                        //msg += " c-combo";
                    //                        if (ec.currentCombo() >= 10)
                    //                            msg += "... PogU";
                    //                        sendMessage(msg, channel);
                    //                    } 
                    chatLogger.log(Level.INFO, chatUsername + ": " + message);
                }
                else 
                {
                    logger.log(Level.INFO, "> " + line); // log other server messages
                }   
            }      
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }     


    /**
     * Join a channel's chatroom.
     * 
     * @param channel
     */
    public void joinChannel(final String channel)
    {
        this.channel = channel;
        try
        {
            // create file handler for chat logging, in append mode
            fh = new FileHandler(LOG_PATH + "chatlogs/" + channel + ".txt", true); 
            chatLogger.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());      
            fh.setEncoding("UTF-8");

            this.writer.write("JOIN #" + channel.toLowerCase() + " \r\n");
            this.writer.flush();
            logger.log(Level.INFO, "> JOIN " + channel);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Get the name of bot.
     * 
     * @return twitch username that the bot is logged in as.
     */
    public String getName() 
    {
        return username;
    }


    /**
     * Reconnect to server in case of errors
     * 
     * WIP
     */
    public void reconnect() {
        int delay = 30; // in seconds

        try {

        } catch (Exception e) {

        }
    }
}