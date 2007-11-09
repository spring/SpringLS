/*
 * Created on 2005.6.16
 * 
 * 
 * ---- INTERNAL CHANGELOG ----
 * *** 0.35 ***
 * * added 'servermode' argument to TASSERVER command
 * *** 0.34 ***
 * * message IDs are now actually working
 * * added TESTLOGIN, TESTLOGINACCEPT and TESTLOGINDENY commands
 * *** 0.33 ***
 * * added "update properties" (updateProperties object)
 * * added SETLATESTSPRINGVERSION and RELOADUPDATEPROPERTIES commands
 * *** 0.32 ***
 * * added option to mute by IP
 * * replaced CLIENTPORT command with CLIENTOIPPORT command and also 
 *   removed IP field from the ADDUSER command (this way IPs are no longer
 *   public unless you join a battle that uses nat traversal, where host
 *   needs to know your IP in order for the nat traversal trick to work)
 * *** 0.31 ***
 * * added new bot mode for accounts (increases traffic limit when using bot mode)
 * *** 0.30 ***
 * * added MAPGRADES command
 * * added FORCESPECTATORMODE command
 * *** 0.26 ***
 * * fixed some charset bug
 * * added UPDATEMOTD command
 * * fixed small bug with JOINBATTLE command not checking if battle is already in-game
 * * fixed minor bug with mute entries not expiring on the fly
 * * added MUTELISTSTART, MUTELIST, MUTELISTEND commands
 * *** 0.25 ***
 * * added -LANADMIN switch
 * * modified protocol to support arbitrary colors (RGB format)
 * *** 0.23 ***
 * * channel mute list now gets updated when user renames his account
 * *** 0.22 ***
 * * added SETCHANNELKEY command, also modified JOIN command to accept extra
 *   argument for locked channels
 * * added FORCELEAVECHANNEL command  
 * * LEFT command now contains (optional) "reason" parameter
 * * replaced CHANNELS command with CHANNEL and ENDOFCHANNELS commands (see
 *   protocol description)
 * * limited maximum length of chat messages  
 * *** 0.20 ***
 * * added CHANGEPASSWORD command
 * * GETINGAMETIME now also accepts no argument (to return your own in-game time)
 * * CHANNELTOPIC command now includes author name and time
 * * added -LOGMAIN switch 
 * * added GETLASTIP command, FINDIP is available to privileged users as well now
 * * fixed bug with /me being available even after being muted
 * * CHANNELMESSAGE command now available to moderators as well
 * *** 0.195 ***
 * * fixed RING command not working for battle hosts
 * *** 0.194 ***
 * * integrated ploticus graphics generator and a simple web server to give access to server's
 *   statistics.
 * * fixed RING command (only host can ring players participating in his own battle, unless
 *   the target is host himself)
 * * fixed KICK command so that now player who's been kicked is notified about it 
 *   (also kick command accepts "reason" string now)
 * * added "GETLASTLOGINTIME" command (for moderators)
 * * fixed bug with clients behind same NAT not getting local IPs in certain cases
 * * added simple UDP server to help with NAT traversing (see NATHelpServer.java)
 * * added UDPSOURCEPORT, CLIENTPORT and HOSTPORT commands (used with NAT traversing)      
 * *** 0.191 ***
 * * fixed bug with server allowing clients to have several battles open at the same time
 * *** 0.19 ***
 * * improved server code (meaning less "ambigious" commands)
 * * added RENAMEACCOUNT command, also username may now contain "[" and "]" characters
 * * added CHANNELMESSAGE command
 * * added MUTE, UNMUTE and MUTELIST commands
 * * clients behind same NAT now get local IPs instead of external one (from the server).
 *   This should resolve some issues with people playing games behind same NAT.
 * * added "agreement"  
 * *** 0.18 ***
 * * multiple mod side support (battle status bits have changed)
 * * user who flood are now automatically banned by server
 * *** 0.17 ***
 * * server now keeps in-game time even after player has reached maximum level
 *   (max. in-game time server can record is 2^20 minutes)
 * * rewrote the network code to use java.nio classes. This fixes several known
 *   problems with server and also fixes multiplayer replay option.
 * * implemented simple anti-flood protection  
 * * removed old file transfer commands      
 * *** 0.16 ***
 * * added new host option - diminishing metal maker returns
 * * switched to Webnet77's ip-to-country database, which seems to be more frequently 
 *   updated: http://software77.net/cgi-bin/ip-country/geo-ip.pl
 * * added "locked" parameter to UPDATEBATTLEINFO command   
 * * added "multiplayer replays" support
 * *** 0.152 ***
 * * fixed small bug with server not updating rank when maximum level has been reached
 * * added ban list
 * *** 0.151 ***
 * * added OFFERUPDATEEX command
 * * added country code support 
 * * added simple protection against rank exploiters
 * * added cpu info (LOGIN command now requires additional parameter)
 * * limited usernames/passwords to 20 chars
 * *** 0.141 ***
 * * fixed issue with server not notifying users about user's rank on login
 * * added command: CHANNELTOPIC
 * *** 0.14 ***
 * * added FORCETEAMCOLOR command
 * * fixed bug which allowed users to register accounts with username/password containing
 *   chars from 43 to 57 (dec), which should be numbers (the correct number range is 48 
 *   to 57). Invalid chars are "+", ",", "-", "." and "/".
 * * added ranking system  
 * *** 0.13 ***
 * * added AI support
 * * added KICKUSER command (admins only)
 * * fixed bug when server did not allow client to change its ally number if someone
 *   else used it, even if that was only a spectator.
 * * added away status bit  
 * * fixed bug when server denied  request to battle, if there were maxplayers+1 players
 *   already in the battle.
 * * added new commands: SERVERMSG, SERVERMSGBOX, REQUESTUPDATEFILE, GETFILE
 * * added some admin commands
 * * changed registration process so that now you can't register username which is same
 *   as someone elses, if we ignore case. Usernames are still case-sensitive though.  
 *
 *
 * ---- NOTES ----
 * 
 * * Client may participate in only one battle at the same time. If he is hosting a battle,
 *   he may not participate in other battles at the same time. Server checks for that 
 *   automatically.
 *   
 * * Lines sent and received may be of any length. I've tested it with 600 KB long strings
 *   and it worked in both directions. Nevertheless, commands like "CLIENTS" still try to
 *   divide data into several lines, just to make sure client will receive it. Since delphi
 *   lobby client now supports lines of any length, dividing data into several lines is not
 *   needed anymore. Nevertheless I kept it just in case, to be compatible with other clients 
 *   which may emerge in the future. I don't divide data when sending info on battles
 *   and clients in battles. This lines may get long, but not longer than a couple of hundreds
 *   of bytes (they should always be under 1 KB in length).
 *   
 * * Sentences must be separated by TAB characters. This also means there should be no TABs
 *   present in your sentences, since TABs are delimiters. That is why you should always
 *   replace any TABs with spaces (2 or 8 usually).
 *   
 * * Syncing works by clients comparing host's hash code with their own. If the two codes
 *   match, client should update his battle status and so telling other clients in the battle
 *   that he is synced (or unsynced otherwise). Hash code comes from hashing mod's file 
 *   and probably all the dependences too.
 * 
 * * Try not to edit account file manually! If you do, don't forget that access numbers
 *   must be in binary form!
 * 
 * * Team colors are currently set by players, perhaps it would be better if only host would
 *   be able to change them?
 * 
 * * Whenever you use killClient() within a for loop, don't forget to decrease loop
 *   counter as you will skip next client in the list otherwise. (this was the cause
 *   for some of the "ambigious data" errors). Or better, use the killClientDelayed()
 *   method.  
 * 
 * * Note that access to long-s is not guaranteed to be atomic, but you should use synchronization
 *   anyway if you use multiple threads.
 *   
 * 
 * ---- LINKS ----
 *  
 * Great article on how to handle network timeouts in Java: http://www.javacoffeebreak.com/articles/network_timeouts/
 * 
 * Another one on network timeouts and alike: http://www.mindprod.com/jgloss/socket.html
 * 
 * Great article on thread synchronization: http://today.java.net/pub/a/today/2004/08/02/sync1.html
 * 
 * Throwing exceptions: http://java.sun.com/docs/books/tutorial/essential/exceptions/throwing.html
 * 
 * Sun's tutorial on sockets: http://java.sun.com/docs/books/tutorial/networking/sockets/
 * 
 * How to redirect program's output by duplicating handles in windows' command prompt: http://www.microsoft.com/resources/documentation/windows/xp/all/proddocs/en-us/redirection.mspx
 * 
 * How to get local IP address (like "192.168.1.1" and not "127.0.0.1"): http://forum.java.sun.com/thread.jspa?threadID=619056&messageID=3477258 
 * 
 * IP-to-country databases: http://ip-to-country.webhosting.info, http://software77.net/cgi-bin/ip-country/geo-ip.pl
 * 
 * Another set of 232 country flags: http://www.ip2location.com/free.asp
 * 
 * Some source code on how to build client-server with java.nio classes (I used ChatterServer.java code from this link): http://brackeen.com/javagamebook/ch06src.zip
 * (found it through this link: http://www.gamedev.net/community/forums/topic.asp?topic_id=318099)
 * 
 * Source for some simple threaded UDP server: http://java.sun.com/docs/books/tutorial/networking/datagrams/example-1dot1/QuoteServerThread.java
 * 
 * How to properly document thread-safety when writing classes: http://www-128.ibm.com/developerworks/java/library/j-jtp09263.html
 * 
 * Good article on immutables (like String etc.): http://macchiato.com/columns/Durable2.html
 * 
 * General info on thread-safety in java: http://mindprod.com/jgloss/threadsafe.html
 * 
 * How to use ZIP with java: http://java.sun.com/developer/technicalArticles/Programming/compression/
 * 
 * How to download file from URL: http://schmidt.devlib.org/java/file-download.html
 * 
 * Very good article on exceptions: http://www.freshsources.com/Apr01.html
 * 
 * Short introduction to generics in JDK 1.5.0: http://java.sun.com/j2se/1.5.0/docs/guide/language/generics.html
 * 
 * ---- NAT TRAVERSAL ----
 * 
 * Primary NAT traversal technique that this lobby server/client implements is "hole punching"
 * technique. See these links for more info:
 * 
 * http://www.brynosaurus.com/pub/net/p2pnat/
 * http://www.potaroo.net/ietf/idref/draft-ford-natp2p/
 * http://www.newport-networks.com/whitepapers/nat-traversal1.html
 * 
 * See source code for implementation details.
 *
 *
 * ---- PROTOCOL ----
 * 
 * [this section was moved to a separate file: "LobbyProtocol.txt" in the SVN repository]
 * 
 */

/**
 * @author Betalord
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;

public class TASServer {
	
	static final String VERSION = "0.35";
	static byte DEBUG = 1; // 0 - no verbose, 1 - normal verbose, 2 - extensive verbose
	static String MOTD = "Enjoy your stay :-)";
	static String agreement = ""; // agreement which is sent to user upon first login. User must send CONFIRMAGREEMENT command to confirm the agreement before server allows him to log in. See LOGIN command implementation for more details.
	static long upTime;
	static String latestSpringVersion = "*"; // this is sent via welcome message to every new client who connects to the server
	static final String MOTD_FILENAME = "motd.txt";
	static final String AGREEMENT_FILENAME = "agreement.rtf";
	static final String BAN_LIST_FILENAME = "banlist.txt";
	static final String ACCOUNTS_INFO_FILEPATH = "accounts.txt";
	static final String SERVER_NOTIFICATION_FOLDER = "./notifs";
	static final String IP2COUNTRY_FILENAME = "ip2country.dat";
	static final String UPDATE_PROPERTIES_FILENAME = "updates.xml";
	static final int DEFAULT_SERVER_PORT = 8200; // default server (TCP) port
	static int serverPort = DEFAULT_SERVER_PORT; // actual server (TCP) port to be used (or currently in use)
	static int NAT_TRAVERSAL_PORT = 8201; // default UDP port used with some NAT traversal technique. If this port is not forwarded, hole punching technique will not work. 
	static final int TIMEOUT_LENGTH = 30000; // in milliseconds
	static boolean LAN_MODE = false;
	static boolean redirect = false; // if true, server is redirection clients to new IP
	static String redirectToIP = ""; // new IP to which clients are redirected if (redirected==true)
	static boolean RECORD_STATISTICS = false; // if true, statistics are saved to disk on regular intervals
	static String PLOTICUS_FULLPATH = "./ploticus/bin/pl"; // see http://ploticus.sourceforge.net/ for more info on ploticus
	static String STATISTICS_FOLDER = "./stats/";
	static long saveStatisticsInterval = 1000 * 60 * 20; // in milliseconds
	static boolean LOG_MAIN_CHANNEL = false; // if true, server will keep a log of all conversations from channel #main (in file "MainChanLog.log")
	static PrintStream mainChanLog;
	static String lanAdminUsername = "admin"; // default lan admin account. Can be overwritten with -LANADMIN switch. Used only when server is running in lan mode!
	static String lanAdminPassword = Misc.encodePassword("admin");
	static long purgeMutesInterval = 1000 * 3; // in miliseconds. On this interval, all channels' mute lists will be checked for expirations and purged accordingly. 
	static long lastMutesPurgeTime = System.currentTimeMillis(); // time when we last purged mute lists of all channels
	static String[] reservedAccountNames = {"TASServer", "Server", "server"}; // accounts with these names cannot be registered (since they may be used internally by the server) 
	static final long minSleepTimeBetweenMapGrades = 5; // minimum time (in seconds) required between two consecutive MAPGRADES command sent by the client. We need this to ensure that client doesn't send MAPGRADES command too often as it creates much load on the server.
	private static int MAX_TEAMS = 16; // max. teams/allies numbers supported by Spring 
	public static boolean initializationFinished = false; // we set this to 'true' just before we enter the main loop. We need this information when saving accounts for example, so that we don't dump empty accounts to disk when an error has occured before initialization has been completed
	static ArrayList<FailedLoginAttempt> failedLoginAttempts = new ArrayList<FailedLoginAttempt>(); // here we store information on latest failed login attempts. We use it to block users from brute-forcing other accounts
	static long lastFailedLoginsPurgeTime = System.currentTimeMillis(); // time when we last purged list of failed login attempts
	
	// database related:
	public static DBInterface database;
	private static String DB_URL = "jdbc:mysql://127.0.0.1/spring";
	private static String DB_username = "";
	private static String DB_password = "";
	
    private static final int READ_BUFFER_SIZE = 256; // size of the ByteBuffer used to read data from the socket channel. This size doesn't really matter - server will work with any size (tested with READ_BUFFER_SIZE==1), but too small buffer size may impact the performance.
    private static final int SEND_BUFFER_SIZE = 65536; // socket's send buffer size
    private static final long CHANNEL_WRITE_SLEEP = 20L;
    private static final long MAIN_LOOP_SLEEP = 10L;
    public static final int NO_MSG_ID = -1; // meaning message isn't using an ID (see protocol description on message/command IDs)
    
    private static final int recvRecordPeriod = 10; // in seconds. Length of time period for which we keep record of bytes received from client. Used with anti-flood protection.
    private static final int maxBytesAlert = 20000; // maximum number of bytes received in the last recvRecordPeriod seconds from a single client before we raise "flood alert". Used with anti-flood protection.
    private static final int maxBytesAlertForBot = 50000; // same as 'maxBytesAlert' but is used for clients in "bot mode" only (see client.status bits)
    private static long lastFloodCheckedTime = System.currentTimeMillis(); // time (in same format as System.currentTimeMillis) when we last updated it. Used with anti-flood protection.
    private static long maxChatMessageLength = 1024; // used with basic anti-flood protection. Any chat messages (channel or private chat messages) longer than this are considered flooding. Used with following commands: SAY, SAYEX, SAYPRIVATE, SAYBATTLE, SAYBATTLEEX
    
    private static long lastTimeoutCheck = System.currentTimeMillis(); // time (System.currentTimeMillis()) when we last checked for timeouts from clients
    
    private static ServerSocketChannel sSockChan;
    private static Selector readSelector;
    //***private static SelectionKey selectKey;
    private static boolean running;
    private static ByteBuffer readBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE); // see http://java.sun.com/j2se/1.5.0/docs/api/java/nio/ByteBuffer.html for difference between direct and non-direct buffers. In this case we should use direct buffers, this is also used by the author of java.nio chat example (see links) upon which this code is built on.
    private static CharsetDecoder asciiDecoder;
    private static CharsetEncoder asciiEncoder;
    
    /* in 'updateProperties' we store a list of Spring versions and server responses to them.
     * We use it when client doesn't have the latest Spring version or the lobby program 
     * and requests an update from us. The XML file should normally contain at least the "default" key
     * which contains a standard response in case no suitable response is found.
     * Each text field associated with a key contains a full string that will be send to the client
     * as a response, so it should contain a full server command. 
     */    
    private static Properties updateProperties = new Properties(); 
    
	static NATHelpServer helpUDPsrvr;
	
	public static void writeMainChanLog(String text) {
		if (!LOG_MAIN_CHANNEL) return;
		
		try {
			mainChanLog.println(Misc.easyDateFormat("<HH:mm:ss> ") + text);
		} catch (Exception e) {
			TASServer.LOG_MAIN_CHANNEL = false;
			System.out.println("$ERROR: Unable to write main channel log file (MainChanLog.log)");
		}		
	}
	
	/* reads MOTD from disk (if file is found) */
	private static boolean readMOTD(String fileName)
	{
		String newMOTD = ""; 
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String line;
            while ((line = in.readLine()) != null) {
            	newMOTD = newMOTD.concat(line + '\n');
	        }
            in.close();
		} catch (IOException e) {
			System.out.println("Couldn't find " + fileName + ". Using default MOTD");
			return false;
		}
		MOTD = newMOTD;
		return true;
	}
	
	private static boolean readUpdateProperties(String fileName) {
		FileInputStream fStream = null; 
		try {  
			fStream = new FileInputStream(fileName); 
			updateProperties.loadFromXML(fStream); 
		} catch (IOException e) {
			return false;
		} finally {  
			if (fStream != null) {  
				try {  
					fStream.close(); 
				} catch (IOException e) {  
				}  
			}  
		} 		
		return true;
	}
	
	private static boolean writeUpdateProperties(String fileName) {
		FileOutputStream fStream = null; 
		try {  
			fStream = new FileOutputStream(fileName); 
			updateProperties.storeToXML(fStream, null); 
		} catch (IOException e) {
			return false;
		} finally {  
			if (fStream != null) {  
				try {  
					fStream.close(); 
				} catch (IOException e) {  
				}  
			}  
		} 		
		return true;
	}	

	/* reads agreement from disk (if file is found) */
	private static void readAgreement()
	{
		String newAgreement = ""; 
		try {
			BufferedReader in = new BufferedReader(new FileReader(AGREEMENT_FILENAME));
			String line;
            while ((line = in.readLine()) != null) {
            	newAgreement = newAgreement.concat(line + '\n');
	        }
            in.close();
		} catch (IOException e) {
			System.out.println("Couldn't find " + AGREEMENT_FILENAME + ". Using no agreement.");
			return ;
		}
		if (newAgreement.length() > 2) agreement = newAgreement;
	}
	
	public static void closeServerAndExit() {
		System.out.println("Server stopped.");
		if (!LAN_MODE && initializationFinished) Accounts.saveAccounts(true); // we need to check if initialization has completed so that we don't save empty accounts array and so overwrite actual accounts 
		if (helpUDPsrvr != null && helpUDPsrvr.isAlive()) {
			helpUDPsrvr.stopServer();
			try {
				helpUDPsrvr.join(1000); // give it 1 second to shut down gracefully
		    } catch (InterruptedException e) {
		    }
		}
		if (LOG_MAIN_CHANNEL) try {
			mainChanLog.close();

			// add server notification:
			ServerNotification sn = new ServerNotification("Server stopped");
			sn.addLine("Server has just been stopped. See server log for more info.");
			ServerNotifications.addNotification(sn);
		} catch(Exception e) {
			// nevermind
		}
		running = false;
		System.exit(0);
	}
	
	private static boolean changeCharset(String newCharset) throws IllegalCharsetNameException, UnsupportedCharsetException {
		CharsetDecoder dec;
		CharsetEncoder enc;
		
		dec = Charset.forName(newCharset).newDecoder();
		enc = Charset.forName(newCharset).newEncoder();
		
		asciiDecoder = dec;
	    asciiDecoder.replaceWith("?");
	    asciiDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
	    asciiDecoder.onMalformedInput(CodingErrorAction.REPLACE);
	    
		asciiEncoder = enc;
	    asciiEncoder.replaceWith(new byte[] { (byte)'?' });
	    asciiEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
	    asciiEncoder.onMalformedInput(CodingErrorAction.REPLACE);
	
		return true;
	}
	
	private static boolean startServer(int port) {
		try {
			changeCharset("ISO-8859-1"); // initializes asciiDecoder and asciiEncoder
			
		    // open a non-blocking server socket channel
		    sSockChan = ServerSocketChannel.open();
		    sSockChan.configureBlocking(false);

		    // bind to localhost on designated port
		    //***InetAddress addr = InetAddress.getLocalHost();
		    //***sSockChan.socket().bind(new InetSocketAddress(addr, port));
		    sSockChan.socket().bind(new InetSocketAddress(port));

		    // get a selector for multiplexing the client channels
		    readSelector = Selector.open();
		    
		} catch (IOException e) {
		    System.out.println("Could not listen on port: " + port);
		    return false;
		}
		
		System.out.println("Port " + port + " is open\n" +
				 "Listening for connections ...");
		
		return true;
	}
	
	private static void acceptNewConnections() {
		try {
		    SocketChannel clientChannel;
		    // since sSockChan is non-blocking, this will return immediately 
		    // regardless of whether there is a connection available
		    while ((clientChannel = sSockChan.accept()) != null) {
	        	if (redirect) {
	        		if (DEBUG > 0) System.out.println("Client redirected to " + redirectToIP + ": " + clientChannel.socket().getInetAddress().getHostAddress());
	        		redirectAndKill(clientChannel.socket());
	        		continue;
	        	}
		    	
		    	Client client = Clients.addNewClient(clientChannel, readSelector, SEND_BUFFER_SIZE);
				if (client == null) continue;
	        	
		    	// from this point on, we know that client has been successfully connected
				client.sendWelcomeMessage();
				
	        	if (DEBUG > 0) System.out.println("New client connected: " + client.IP);
		    }		
		}
		catch (IOException ioe) {
		    System.out.println("error during accept(): " + ioe.toString());
		}
		catch (Exception e) {
		    System.out.println("exception in acceptNewConnections()" + e.toString());
		}
	}

	private static void readIncomingMessages() {
		Client client = null;
		
		try {
		    // non-blocking select, returns immediately regardless of how many keys are ready
		    readSelector.selectNow();
		    
		    // fetch the keys
		    Set readyKeys = readSelector.selectedKeys();
		    
		    // run through the keys and process each one
		    Iterator i = readyKeys.iterator();
		    while (i.hasNext()) {
				SelectionKey key = (SelectionKey)i.next();
				i.remove();
				SocketChannel channel = (SocketChannel)key.channel();
				client = (Client)key.attachment(); 
				readBuffer.clear();

				client.timeOfLastReceive = System.currentTimeMillis();
				
				// read from the channel into our buffer
				long nbytes = channel.read(readBuffer);
				client.dataOverLastTimePeriod += nbytes;
				
				// basic anti-flood protection:
				if ((client.account.accessLevel() < Account.ADMIN_ACCESS) 
					&& (((client.getBotModeFromStatus() == false) && (client.dataOverLastTimePeriod > TASServer.maxBytesAlert)) ||
					((client.getBotModeFromStatus() == true) && (client.dataOverLastTimePeriod > TASServer.maxBytesAlertForBot)))) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ")");
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + "). User's IP has been auto-banned.");
					Clients.banList.add(client.IP, "Auto-ban for flooding.");
					Clients.killClient(client, "Disconnected due to excessive flooding");

					// add server notification:
					ServerNotification sn = new ServerNotification("Flooding detected");
					sn.addLine("Flooding detected from " + client.IP + " (" + client.account.user + ").");
					sn.addLine("User has been automatically banned.");
					ServerNotifications.addNotification(sn);
					
					continue;
				}

				// check for end-of-stream
				if (nbytes == -1) { 
					if (DEBUG > 0) System.out.println ("Socket disconnected - killing client");
					channel.close();
					Clients.killClient(client); // will also close the socket channel 
				} else {
				    // use a CharsetDecoder to turn those bytes into a string
				    // and append to client's StringBuffer
				    readBuffer.flip();
				    String str = asciiDecoder.decode(readBuffer).toString();
				    readBuffer.clear();
				    client.recvBuf.append(str);

				    // check for a full line
				    String line = client.recvBuf.toString();
				    while ((line.indexOf('\n') != -1) || (line.indexOf('\r') != -1)) {
				    	int pos = line.indexOf('\r');
				    	int npos = line.indexOf('\n');
				    	if (pos == -1 || ((npos != -1) && (npos < pos))) pos = npos;
				    	String command = line.substring(0, pos);
				    	while (pos+1 < line.length() && (line.charAt(pos+1) == '\r' || line.charAt(pos+1) == '\n')) ++pos;
				    	client.recvBuf.delete(0, pos+1);
				    	
				    	long time = System.currentTimeMillis();
				    	tryToExecCommand(command, client);
				    	time = System.currentTimeMillis() - time;
				    	if (time > 200) {
				    		Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: (DEBUG) User <" + client.account.user + "> caused " + time + " ms load on the server. Command issued: " + command);
				    	}

				    	if (!client.alive) break; // in case client was killed within tryToExecCommand() method
					    line = client.recvBuf.toString();
				    }
				}
		    }
		} catch(IOException ioe) {
			System.out.println("exception during select(): possibly due to force disconnect. Killing the client ...");
			try {
				if (client != null) Clients.killClient(client, "Quit: connection lost");				
			} catch (Exception e) {
			}
		} catch(Exception e) {
			System.out.println("exception in readIncomingMessages(): killing the client ... (" + e.toString() + ")");
			try {
				if (client != null) Clients.killClient(client, "Quit: connection lost");		
				e.printStackTrace(); //*** DEBUG
			} catch (Exception ex) {
			}
		}
	}
	
	private static void channelWrite(SocketChannel channel, ByteBuffer writeBuffer) throws ChannelWriteTimeoutException {
		long nbytes = 0;
		long toWrite = writeBuffer.remaining();
		long time = System.currentTimeMillis();

		// loop on the channel.write() call since it will not necessarily
		// write all bytes in one shot
		try {
		    while (nbytes != toWrite) {
		    	long last = nbytes;
		    	nbytes += channel.write(writeBuffer);
		    	if (nbytes-last == 0) { // no bytes written
		    		// sleep a bit to avoid creating 100% CPU usage peak:
			    	try {
			    		Thread.sleep(CHANNEL_WRITE_SLEEP);
			    	} catch (InterruptedException e) {
			    	}
		    	}
			
		    	if (System.currentTimeMillis() - time > 1000) throw new ChannelWriteTimeoutException();
		    }
		} catch (ClosedChannelException cce) {
			// do nothing
		} catch (IOException io) {
			// do nothing
		}  
		
		// get ready for another write if needed
		writeBuffer.rewind();
	}
	
	public static boolean sendLineToSocketChannel(String line, SocketChannel channel) throws ChannelWriteTimeoutException {
		String data = line + '\n';
		
		if ((channel == null) || (!channel.isConnected())) {
			System.out.println("WARNING: SocketChannel is not ready to be written to. Ignoring ...");
		    return false;
		}
		
		ByteBuffer buf;
		try{
			buf = asciiEncoder.encode(CharBuffer.wrap(data));
		} catch (CharacterCodingException e) {
			return false;
		}
				
		channelWrite(channel, buf);
		
		return true;
	}
	
	private static Account verifyLogin(String user, String pass) {
		Account acc = Accounts.getAccount(user);
		if (acc == null) return null;
		if (acc.pass.equals(pass)) return acc;
		else return null;
	}
	
	private static void recordFailedLoginAttempt(String username) {
		FailedLoginAttempt attempt = findFailedLoginAttempt(username);
		if (attempt == null) {
			attempt = new FailedLoginAttempt(username, 0, 0);
			failedLoginAttempts.add(attempt);
		}
		attempt.timeOfLastFailedAttempt = System.currentTimeMillis();
		attempt.numOfFailedAttempts++;
	}
	
	/** return null if no record found */
	private static FailedLoginAttempt findFailedLoginAttempt(String username) {
		for (int i = 0; i < failedLoginAttempts.size(); i++) {
			if (failedLoginAttempts.get(i).username.equals(username)) {
				return failedLoginAttempts.get(i);
			}
		}
		return null;
	}
	
	/* Sends "message of the day" (MOTD) to client */
	private static boolean sendMOTDToClient(Client client) {
		client.sendLine("MOTD Welcome, " + client.account.user + "!");
		client.sendLine("MOTD There are currently " + (Clients.getClientsSize()-1) + " clients connected"); // -1 is because we shouldn't count the client to which we are sending MOTD
		client.sendLine("MOTD to server talking in " + Channels.getChannelsSize() + " open channels and");
		client.sendLine("MOTD participating in " + Battles.getBattlesSize() + " battles.");
		client.sendLine("MOTD Server's uptime is " + Misc.timeToDHM(System.currentTimeMillis() - upTime) + ".");
		client.sendLine("MOTD");
		String[] sl = MOTD.split("\n");
		for (int i = 0; i < sl.length; i++) {
			client.sendLine("MOTD " + sl[i]);
		}

		return true;
	}

	private static void sendAgreementToClient(Client client) {
		String[] sl = agreement.split("\n");
		for (int i = 0; i < sl.length; i++) {
			client.sendLine("AGREEMENT " + sl[i]);
		}
		client.sendLine("AGREEMENTEND");
	}	
	
	public static boolean redirectAndKill(Socket socket) {
		if (!redirect) return false;
		try {
			(new PrintWriter(socket.getOutputStream(), true)).println("REDIRECT " + redirectToIP);
			socket.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}	
	
	/* Note: this method is not synchronized! 
	 * Note2: this method may be called recursively! */
	public static boolean tryToExecCommand(String command, Client client) {
		command = command.trim();
		if (command.equals("")) return false;

		if (DEBUG > 1) 
			if (client.account.accessLevel() != Account.NIL_ACCESS) System.out.println("[<-" + client.account.user + "]" + " \"" + command + "\"");
			else System.out.println("[<-" + client.IP + "]" + " \"" + command + "\"");
		
		int ID = NO_MSG_ID;
		if (command.charAt(0) == '#') try {
			if (!command.matches("^#\\d+\\s[\\s\\S]*")) return false; // malformed command
			ID = Integer.parseInt(command.substring(1).split("\\s")[0]);
			// remove ID field from the rest of command:
			command = command.replaceFirst("#\\d+\\s", "");
		} catch (NumberFormatException e) {
			return false; // this means that the command is malformed
		} catch (PatternSyntaxException e) {
			return false; // this means that the command is malformed
		}

		// parse command into tokens:
		String[] commands = command.split(" ");
		commands[0] = commands[0].toUpperCase();
		
		client.setSendMsgID(ID);
		
		try {
			if (commands[0].equals("PING")) {
				//***if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
				client.sendLine("PONG");
			}
			if (commands[0].equals("REGISTER")) {
				if (commands.length != 3) {
					client.sendLine("REGISTRATIONDENIED Bad command arguments");
					return false;
				}
							
				if (client.account.accessLevel() != Account.NIL_ACCESS) { // only clients which aren't logged-in can register
					client.sendLine("REGISTRATIONDENIED You are already logged-in, no need to register new account");
					return false;
				}
				
				if (LAN_MODE) { // no need to register account in LAN mode since it accepts any username
					client.sendLine("REGISTRATIONDENIED Can't register in LAN-mode. Login with any username and password to proceed");
					return false;
				}

				// validate username:
				String valid = Accounts.isOldUsernameValid(commands[1]);
				if (valid != null) {
					client.sendLine("REGISTRATIONDENIED Invalid username (reason: " + valid + ")");
					return false;
				}
				
				// validate password:
				valid = Accounts.isPasswordValid(commands[2]);
				if (valid != null) {
					client.sendLine("REGISTRATIONDENIED Invalid password (reason: " + valid + ")");
					return false;
				}
				
				Account acc = Accounts.findAccountNoCase(commands[1]);
				if (acc != null) {
					client.sendLine("REGISTRATIONDENIED Account already exists");
					return false;
				}
				
				// check for reserved names:
				for (int i = 0; i < reservedAccountNames.length; i++)
					if (reservedAccountNames[i].equals(commands[1])) {
						client.sendLine("REGISTRATIONDENIED Invalid account name - you are trying to register a reserved account name");
						return false;
					}
				
				acc = new Account(commands[1], commands[2], Account.NORMAL_ACCESS, Account.NO_USER_ID, System.currentTimeMillis(), client.IP, System.currentTimeMillis(), client.country, new MapGradeList());
				Accounts.addAccount(acc);
				Accounts.saveAccounts(false); // let's save new accounts info to disk
				client.sendLine("REGISTRATIONACCEPTED");
			}
			else if (commands[0].equals("UPTIME")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				if (commands.length != 1) return false;
				
				client.sendLine("SERVERMSG Server's uptime is " + Misc.timeToDHM(System.currentTimeMillis() - upTime));
			}
			/* some admin/moderator specific commands: */
			else if (commands[0].equals("KICKUSER")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length < 2) return false;
				
				Client target = Clients.getClient(commands[1]);
				String reason = "";
				if (commands.length > 2) reason = " (reason: " + Misc.makeSentence(commands, 2) + ")"; 
				if (target == null) return false;
				for (int i = 0; i < Channels.getChannelsSize(); i++) {
					if (Channels.getChannel(i).isClientInThisChannel(target)) {
						Channels.getChannel(i).broadcast("<" + client.account.user + "> has kicked <" + target.account.user + "> from server" + reason);
					}
				}
				target.sendLine("SERVERMSG You've been kicked from server by <" + client.account.user + ">" + reason);
				Clients.killClient(target, "Quit: kicked from server");
			}
			else if (commands[0].equals("REMOVEACCOUNT")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;
				
				if (!Accounts.removeAccount(commands[1])) return false;
				
				// if any user is connected to this account, kick him:
				for (int j = 0; j < Clients.getClientsSize(); j++) {
					if (Clients.getClient(j).account.user.equals(commands[1])) {
						Clients.killClient(Clients.getClient(j));
						j--;
					}
				}
				
				Accounts.saveAccounts(false); // let's save new accounts info to disk
				client.sendLine("SERVERMSG You have successfully removed <" + commands[1] + "> account!");
			}
			else if (commands[0].equals("STOPSERVER")) {
				// stop server gracefully:
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				
				running = false;
			}
			else if (commands[0].equals("FORCESTOPSERVER")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				
				closeServerAndExit();
			}
			else if (commands[0].equals("SAVEACCOUNTS")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				
				Accounts.saveAccounts(false);
				client.sendLine("SERVERMSG Accounts will be saved in a background thread.");
			}
			else if (commands[0].equals("CHANGEACCOUNTPASS")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 3) return false;
				
				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) return false; 
				// validate password:
				if (Accounts.isPasswordValid(commands[2]) != null) return false;
				
				acc.pass = commands[2];
				
				Accounts.saveAccounts(false); // save changes
				
				// add server notification:
				ServerNotification sn = new ServerNotification("Account password changed by admin");
				sn.addLine("Admin <" + client.account.user + "> has changed password for account <" + acc.user + ">");
				ServerNotifications.addNotification(sn);
			}
			else if (commands[0].equals("CHANGEACCOUNTACCESS")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 3) return false;

				int value;
				try {
					value = Integer.parseInt(commands[2]); 
				} catch (NumberFormatException e) {
					return false; 
				}
				
				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) return false; 
				
				int oldAccess = acc.access;
				acc.access = value;
				
				Accounts.saveAccounts(false); // save changes
				 // just in case if rank got changed:
				client.setRankToStatus(client.account.getRank());
				
				Clients.notifyClientsOfNewClientStatus(client);

				client.sendLine("SERVERMSG You have changed password for <" + commands[1] + "> successfully.");
				
				// add server notification:
				ServerNotification sn = new ServerNotification("Account access changed by admin");
				sn.addLine("Admin <" + client.account.user + "> has changed access/status bits for account <" + acc.user + ">.");
				sn.addLine("Old access code: " + oldAccess + ". New code: " + value);
				ServerNotifications.addNotification(sn);
			}
			else if (commands[0].equals("GETACCOUNTACCESS")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG User <" + commands[1] + "> not found!");
					return false;
				}
				
				client.sendLine("SERVERMSG " + commands[1] + "'s access code is " + acc.access);
			}
			else if (commands[0].equals("REDIRECT")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				redirectToIP = commands[1];
				redirect = true;
				Clients.sendToAllRegisteredUsers("BROADCAST " + "Server has entered redirection mode");
				
				// add server notification:
				ServerNotification sn = new ServerNotification("Entered redirection mode");
				sn.addLine("Admin <" + client.account.user + "> has enabled redirection mode. New address: " + redirectToIP);
				ServerNotifications.addNotification(sn);			
			}
			else if (commands[0].equals("REDIRECTOFF")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				redirect = false;
				Clients.sendToAllRegisteredUsers("BROADCAST " + "Server has left redirection mode");
				
				// add server notification:
				ServerNotification sn = new ServerNotification("Redirection mode disabled");
				sn.addLine("Admin <" + client.account.user + "> has disabled redirection mode.");
				ServerNotifications.addNotification(sn);			
			}
			else if (commands[0].equals("BROADCAST")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 2) return false;

				Clients.sendToAllRegisteredUsers("BROADCAST " + Misc.makeSentence(commands, 1));
			}
			else if (commands[0].equals("BROADCASTEX")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 2) return false;

				Clients.sendToAllRegisteredUsers("SERVERMSGBOX " + Misc.makeSentence(commands, 1));
			}
			else if (commands[0].equals("ADMINBROADCAST")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 2) return false;

				Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: " + Misc.makeSentence(commands, 1));
			}
			else if (commands[0].equals("GETACCOUNTCOUNT")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 1) return false;

				client.sendLine("SERVERMSG " + Accounts.getAccountsSize());
			}
			else if (commands[0].equals("FINDIP")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 2) return false;
				
				boolean found = false;
				String IP = commands[1];
				String[] sp1 = IP.split("\\.");
				if (sp1.length != 4) {
					client.sendLine("SERVERMSG Invalid IP address/range: " + IP);
					return false;
				}
				
				for (int i = 0; i < Clients.getClientsSize(); i++)
				{
					String[] sp2 = Clients.getClient(i).IP.split("\\.");

					if (!sp1[0].equals("*")) if (!sp1[0].equals(sp2[0])) continue;
					if (!sp1[1].equals("*")) if (!sp1[1].equals(sp2[1])) continue;
					if (!sp1[2].equals("*")) if (!sp1[2].equals(sp2[2])) continue;
					if (!sp1[3].equals("*")) if (!sp1[3].equals(sp2[3])) continue;
					
					found = true;
					client.sendLine("SERVERMSG " + IP + " is bound to: "+ Clients.getClient(i).account.user);
				}
					
				// now let's check if this IP matches any recently used IP:
				for (int i = 0; i < Accounts.getAccountsSize(); i++) {
					String[] sp2 = Accounts.getAccount(i).lastIP.split("\\.");

					if (!sp1[0].equals("*")) if (!sp1[0].equals(sp2[0])) continue;
					if (!sp1[1].equals("*")) if (!sp1[1].equals(sp2[1])) continue;
					if (!sp1[2].equals("*")) if (!sp1[2].equals(sp2[2])) continue;
					if (!sp1[3].equals("*")) if (!sp1[3].equals(sp2[3])) continue;				

					if (Clients.getClient(Accounts.getAccount(i).user) == null) { // user is offline
						found = true;
						client.sendLine("SERVERMSG " + IP + " was recently bound to: "+ Accounts.getAccount(i).user + " (offline)");
					}
				}			

				if (!found) client.sendLine("SERVERMSG No client is/was recently using IP: " + IP); //*** perhaps add an explanation like "(note that server only keeps track of last used IP addresses)" ?
			}
			else if (commands[0].equals("GETLASTIP")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 2) return false;
				
				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG User " + commands[1] + " not found!");
					return false;
				}
				
				boolean online = Clients.isUserLoggedIn(acc); 
				client.sendLine("SERVERMSG " + commands[1] + "'s last IP was " + acc.lastIP + " (" + (online ? "online)" : "offline)"));
			}		
			else if (commands[0].equals("GETACCOUNTINFO")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;
				
				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG Account <" + commands[1] + "> does not exist.");
					return false;
				}

				client.sendLine("SERVERMSG Full account info for <" + acc.user + ">: " + acc.toString());
			}
			else if (commands[0].equals("FORGEMSG")) {
				/* this command is used only for debugging purposes. It sends the string
				 * to client specified as first argument. */
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 3) return false;
				
				Client targetClient = Clients.getClient(commands[1]);
				if (targetClient == null) return false;
				
				targetClient.sendLine(Misc.makeSentence(commands, 2));
			}
			else if (commands[0].equals("FORGEREVERSEMSG")) {
				/* this command is used only for debugging purposes. It forces server to process
				 * string passed to this command as if it were sent by the user specified 
				 * in this command. */
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 3) return false;
				
				Client targetClient = Clients.getClient(commands[1]);
				if (targetClient == null) return false;
				
				tryToExecCommand(Misc.makeSentence(commands, 2), targetClient);
			}
			else if (commands[0].equals("GETIP")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 2) return false;
			
				Client targetClient = Clients.getClient(commands[1]);
				if (targetClient == null) return false;
				
				client.sendLine("SERVERMSG " + targetClient.account.user + "'s IP is " + targetClient.IP);
			}
			else if (commands[0].equals("GETINGAMETIME")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (commands.length == 1) {
					client.sendLine("SERVERMSG " + "Your in-game time is " + client.account.getInGameTime() + " minutes.");
				} else {
					if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) {
						client.sendLine("SERVERMSG You have no access to see other player's in-game time!");
						return false;
					} 
					
					if (commands.length != 2) return false;
					Account acc = Accounts.getAccount(commands[1]);
					if (acc == null) {
						client.sendLine("SERVERMSG " + "GETINGAMETIME failed: user " + commands[1] + " not found!");	
						return false;
					}
						
					client.sendLine("SERVERMSG " + acc.user + "'s in-game time is " + acc.getInGameTime() + " minutes.");
				}
			}
			else if (commands[0].equals("FORCECLOSEBATTLE")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				int battleID;
				try {
					battleID = Integer.parseInt(commands[1]); 
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG Invalid BattleID!");
					return false; 
				}
				
				Battle bat = Battles.getBattleByID(battleID);
				if (bat == null) {
					client.sendLine("SERVERMSG Error: unknown BATTLE_ID!");
					return false;
				}
				
				Battles.closeBattleAndNotifyAll(bat);
				
			}
			else if (commands[0].equals("BANLISTADD")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length < 2) return false;
				
				String reason = "";
				if (commands.length > 2) reason = Misc.makeSentence(commands, 2); 
				Clients.banList.add(commands[1], reason);
				Clients.banList.saveToFile(BAN_LIST_FILENAME);
				client.sendLine("SERVERMSG IP " + commands[1] + " has been added to ban list. Use KICKUSER to kick user from server.");
			}
			else if (commands[0].equals("BANLISTREMOVE")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 2) return false;

				if (Clients.banList.remove(commands[1])) {
					Clients.banList.saveToFile(BAN_LIST_FILENAME);
					client.sendLine("SERVERMSG IP " + commands[1] + " has been removed from ban list.");
				} else {
					client.sendLine("SERVERMSG IP " + commands[1] + " couldn't be found in ban list!");				
				}
			}
			else if (commands[0].equals("BANLIST")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 1) return false;

				if (Clients.banList.size() == 0) client.sendLine("SERVERMSG Ban list is empty!");
				else {
					client.sendLine("SERVERMSG Ban list (" + Clients.banList.size() + " entries):");
					for (int i = 0; i < Clients.banList.size(); i++)
						client.sendLine("SERVERMSG * " + Clients.banList.getIP(i) + " (Reason: " + Clients.banList.getReason(i) + ")");
					client.sendLine("SERVERMSG End of ban list.");
				}
			}
			else if (commands[0].equals("MUTE")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length < 4) return false;
				
				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) { 
					client.sendLine("SERVERMSG MUTE failed: Channel #" + commands[1] + " does not exist!");
					return false;
				}
				
				String username = commands[2];
				if (chan.muteList.isMuted(username)) {
					client.sendLine("SERVERMSG MUTE failed: User <" + username + "> is already muted. Unmute first!");
					return false;
				}
				
				Account targetAccount = Accounts.getAccount(username);
				if (targetAccount == null) {
					client.sendLine("SERVERMSG MUTE failed: User <" + username + "> does not exist");
					return false;
				}
				
				boolean muteByIP = false;
				if (commands.length > 4) {
					String option = commands[4];
					if (option.toUpperCase().equals("IP")) muteByIP = true;
					else {
						client.sendLine("SERVERMSG MUTE failed: Invalid argument: " + option + "\"");
						return false;
					}
				}
				
				int minutes;
				try {
					minutes = Integer.parseInt(commands[3]); 
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG MUTE failed: Invalid argument - should be an integer");
					return false; 
				}
				
				chan.muteList.mute(username, minutes*60, (muteByIP ? targetAccount.lastIP : null));
				
				client.sendLine("SERVERMSG You have muted <" + username + "> on channel #" + chan.name + ".");
				chan.broadcast("<" + client.account.user + "> has muted <" + username + ">");
			}
			else if (commands[0].equals("UNMUTE")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 3) return false;

				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) { 
					client.sendLine("SERVERMSG UNMUTE failed: Channel #" + commands[1] + " does not exist!");
					return false;
				}
				
				String username = commands[2];
				if (!chan.muteList.isMuted(username)) {
					client.sendLine("SERVERMSG UNMUTE failed: User <" + username + "> is not on the mute list!");
					return false;
				}
				
				chan.muteList.unmute(username);
				client.sendLine("SERVERMSG You have unmuted <" + username + "> on channel #" + chan.name + ".");
				chan.broadcast("<" + client.account.user + "> has unmuted <" + username + ">");
			}
			else if (commands[0].equals("MUTELIST")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				if (commands.length != 2) {
					client.sendLine("SERVERMSG MUTELIST failed: Invalid arguments!");
					return false;
				}
				
				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) { 
					client.sendLine("SERVERMSG MUTELIST failed: Channel #" + commands[1] + " does not exist!");
					return false;
				}

				client.sendLine("MUTELISTBEGIN " + chan.name);
				
				int size = chan.muteList.size(); // we mustn't call muteList.size() in for loop since it will purge expired records each time and so we could have ArrayOutOfBounds exception
				for (int i = 0; i < size; i++) 
					if (chan.muteList.getRemainingSeconds(i) == 0) client.sendLine("MUTELIST " + (String)chan.muteList.getUsername(i) + ", indefinite time remaining");
					else client.sendLine("MUTELIST " + (String)chan.muteList.getUsername(i) + ", " + chan.muteList.getRemainingSeconds(i) + " seconds remaining");
						
				client.sendLine("MUTELISTEND");
			}
			else if (commands[0].equals("CHANNELMESSAGE")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length < 3) return false;

				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) { 
					client.sendLine("SERVERMSG CHANNELMESSAGE failed: Channel #" + commands[1] + " does not exist!");
					return false;
				}
				
				chan.broadcast(Misc.makeSentence(commands, 2));
			}		
			else if (commands[0].equals("IP2COUNTRY")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				client.sendLine("SERVERMSG Country = " + IP2Country.getCountryCode(Misc.IP2Long(Misc.makeSentence(commands, 1))));		
			}
			else if (commands[0].equals("REINITIP2COUNTRY")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 2) return false;

				if (IP2Country.initializeAll(Misc.makeSentence(commands, 1)))
					client.sendLine("SERVERMSG IP2COUNTRY database initialized successfully!");
				else 
					client.sendLine("SERVERMSG Error while initializing IP2COUNTRY database!");
			}
			else if (commands[0].equals("UPDATEIP2COUNTRY")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 1) return false;

				if (IP2Country.updateInProgress()) {
					client.sendLine("SERVERMSG IP2Country database update is already in progress, try again later.");
					return false;
				}
				
				client.sendLine("SERVERMSG Updating IP2country database ... Server will notify of success via server notification system.");
				IP2Country.updateDatabase();
			}
			else if (commands[0].equals("CHANGECHARSET")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				try {
					changeCharset(commands[1]);
				} catch (IllegalCharsetNameException e) {
					client.sendLine("SERVERMSG Error: Illegal charset name: " + commands[1]);
					return false;
				} catch (UnsupportedCharsetException e) {
					client.sendLine("SERVERMSG Error: Unsupported charset: " + commands[1]);
					return false;
				}
				
				client.sendLine("SERVERMSG Charset set to " + commands[1]);
			}
			else if (commands[0].equals("GETLOBBYVERSION")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				Client targetClient = Clients.getClient(commands[1]);
				if (targetClient == null) {
					client.sendLine("SERVERMSG <" + commands[1] + "> not found!");
					return false;
				}
				client.sendLine("SERVERMSG <" + commands[1] + "> is using \"" + targetClient.lobbyVersion + "\"");
			}
			else if (commands[0].equals("UPDATESTATISTICS")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 1) return false;

				int taken = Statistics.saveStatisticsToDisk(); 
				if (taken == -1)
					client.sendLine("SERVERMSG Unable to update statistics!");
				else 
					client.sendLine("SERVERMSG Statistics have been updated. Time taken to calculate: " + taken + " ms.");
			}
			else if (commands[0].equals("UPDATEMOTD")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;
				
				if (!readMOTD(commands[1])) {
					client.sendLine("SERVERMSG Error: unable to read MOTD from " + commands[1]);
					return false;
				} else {
					client.sendLine("SERVERMSG MOTD has been successfully updated from " + commands[1]);
				}
			}		
			else if (commands[0].equals("LONGTIMETODATE")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) return false;

				long time;
				try {
					time = Long.parseLong(commands[1]);
				} catch (Exception e) {
					client.sendLine("SERVERMSG LONGTIMETODATE failed: invalid argument.");
					return false;
				}
				
	     		client.sendLine("SERVERMSG LONGTIMETODATE result: " + Misc.easyDateFormat(time, "d MMM yyyy HH:mm:ss z"));
			}
			else if (commands[0].equals("GETLASTLOGINTIME")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 2) return false;

				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG GETLASTLOGINTIME failed: <" + commands[1] + "> not found!");
					return false;
				}
				
				if (Clients.getClient(acc.user) == null) {
					client.sendLine("SERVERMSG <" + acc.user + ">'s last login was on " + Misc.easyDateFormat(acc.lastLogin, "d MMM yyyy HH:mm:ss z"));
				} else {
					client.sendLine("SERVERMSG <" + acc.user + "> is currently online");
				}
			}
			else if (commands[0].equals("SETCHANNELKEY")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length != 3) {
					client.sendLine("SERVERMSG Bad arguments (command SETCHANNELKEY)");
					return false;
				}
				
				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) {
					client.sendLine("SERVERMSG Error: Channel does not exist: " + commands[1]);
					return false;
				}
				
				if (commands[2].equals("*")) {
					if (chan.getKey().equals("")) {
						client.sendLine("SERVERMSG Error: Unable to unlock channel - channel is not locked!");
						return false;
					}
					chan.setKey("");
					chan.broadcast("<" + client.account.user + "> has just unlocked #" + chan.name);
				} else {
					if (!commands[2].matches("^[A-Za-z0-9_]+$")) {
						client.sendLine("SERVERMSG Error: Invalid key: " + commands[2]);
						return false;
					}
					chan.setKey(commands[2]);
					chan.broadcast("<" + client.account.user + "> has just locked #" + chan.name + " with private key");
				}
			}
			else if (commands[0].equals("FORCELEAVECHANNEL")) {
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				if (commands.length < 3) {
					client.sendLine("SERVERMSG Bad arguments (command FORCELEAVECHANNEL)");
					return false;
				}
				
				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) {
					client.sendLine("SERVERMSG Error: Channel does not exist: " + commands[1]);
					return false;
				}
				
				Client target = Clients.getClient(commands[2]);
				if (target == null) {
					client.sendLine("SERVERMSG Error: <" + commands[2] + "> not found!");
					return false;
				}
				
				if (!chan.isClientInThisChannel(target)) {
					client.sendLine("SERVERMSG Error: <" + commands[2] + "> is not in the channel #" + chan.name + "!");
					return false;
				}
				
				String reason = "";
				if (commands.length > 3) reason = " " + Misc.makeSentence(commands, 3);
				chan.broadcast("<" + client.account.user + "> has kicked <" + target.account.user + "> from the channel" + (reason.equals("") ? "" : " (reason:" + reason + ")"));
				target.sendLine("FORCELEAVECHANNEL " + chan.name + " " + client.account.user + reason);
				target.leaveChannel(chan, "kicked from channel");
			}
			else if (commands[0].equals("LAUNCHPROCESS")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 2) {
					client.sendLine("SERVERMSG Error: arguments missing (LAUNCHPROCESS command)");
					return false;
				}
				try {
					Runtime.getRuntime().exec(Misc.makeSentence(commands, 1));
				} catch (IOException e) {
					client.sendLine("SERVERMSG LAUNCHPROCESS failed: IOException occured (" + e.toString() + ")");
					return false;
				}
				client.sendLine("SERVERMSG Process started: \"" + Misc.makeSentence(commands, 1) + "\"");
			}
			else if (commands[0].equals("ADDNOTIFICATION")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length < 2) {
					client.sendLine("SERVERMSG Error: arguments missing (ADDNOTIFICATION command)");
					return false;
				}
				
				if (ServerNotifications.addNotification(new ServerNotification("Admin notification", client.account.user, Misc.makeSentence(commands, 1))))
					client.sendLine("SERVERMSG Notification added.");
				else
					client.sendLine("SERVERMSG Error while adding notification! Notification not added.");
			}
			else if (commands[0].equals("GETSENDBUFFERSIZE")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) {
					client.sendLine("SERVERMSG Error: this method requires exactly 2 arguments!");
					return false;
				}

				Client c = Clients.getClient(commands[1]);
				if (c == null) {
					client.sendLine("SERVERMSG Error: user <" + commands[1] + "> not found online!");
					return false;
				}
				
				int size;
				try {
					size = c.sockChan.socket().getSendBufferSize();
				} catch (Exception e) {
					// this could perhaps happen if user just disconnected or something
					client.sendLine("SERVERMSG Error: exception raised while trying to get send buffer size for <" + commands[1] + ">!");
					return false;
				}
				
				client.sendLine("SERVERMSG Send buffer size for <" + c.account.user + "> is set to " + size + " bytes.");
			}
			else if (commands[0].equals("MEMORYAVAILABLE")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 1) {
					return false;
				}
				
				client.sendLine("SERVERMSG Amount of free memory in Java Virtual Machine: " + Runtime.getRuntime().freeMemory() + " bytes");
			}		
			else if (commands[0].equals("CALLGARBAGECOLLECTOR")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 1) {
					return false;
				}
				
				long time = System.nanoTime();
				System.gc();
				time = (System.nanoTime() - time) / 1000000;
				
				client.sendLine("SERVERMSG Garbage collector invoked (time taken: " + time + " ms)");
			}
			else if (commands[0].equals("TESTLOGIN")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				if (verifyLogin(commands[1], commands[2]) == null) {
					client.sendLine("TESTLOGINDENY");
					return false;
				}

				// we don't check here if agreement bit is set yet or if user is banned, we only verify if login info is correct
				client.sendLine("TESTLOGINACCEPT");
			}
			else if (commands[0].equals("SETBOTMODE")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				int mode;
				try {
					mode = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG Invalid 'mode' parameter (must be 0 or 1)!");
					return false;
				}
				if ((mode != 0) && (mode != 1)) {
					client.sendLine("SERVERMSG Invalid 'mode' parameter (must be 0 or 1)!");
					return false;
				}
				
				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG User <" + commands[1] + "> not found!");
					return false;
				}
				
				acc.setBotMode((mode == 0) ? false : true);
				
				client.sendLine("SERVERMSG Bot mode set to "  + mode + " for user <" + commands[1] + ">");
			}
			else if (commands[0].equals("GETREGISTRATIONDATE")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG User <" + commands[1] + "> not found!");
					return false;
				}

				client.sendLine("SERVERMSG Registration timestamp for <" + commands[1] + "> is " + acc.registrationDate + " (" + Misc.easyDateFormat(acc.registrationDate, "d MMM yyyy HH:mm:ss z") + ")");
			}			
			else if (commands[0].equals("SETLATESTSPRINGVERSION")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
				if (commands.length != 2) {
					client.sendLine("SERVERMSG Bad arguments to SETLATESTSPRINGVERSION command!");
					return false;
				}

				latestSpringVersion = commands[1];
				
				client.sendLine("SERVERMSG Latest spring version has been set to " + latestSpringVersion);
			}
			else if (commands[0].equals("RELOADUPDATEPROPERTIES")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				if (readUpdateProperties(UPDATE_PROPERTIES_FILENAME)) {
					System.out.println("\"Update properties\" read from " + UPDATE_PROPERTIES_FILENAME);
					client.sendLine("SERVERMSG \"Update properties\" have been successfully loaded from " + UPDATE_PROPERTIES_FILENAME);
				} else {
					client.sendLine("SERVERMSG Unable to load \"Update properties\" from " + UPDATE_PROPERTIES_FILENAME + "!");
				}
			}
			else if (commands[0].equals("GETUSERID")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				Account acc = Accounts.getAccount(commands[1]);
				if (acc == null) {
					client.sendLine("SERVERMSG User <" + commands[1] + "> not found!");
					return false;
				}

				client.sendLine("SERVERMSG Last user ID for <" + commands[1] + "> was " + acc.lastUserID);
			}
			else if (commands[0].equals("KILLALL")) {
				if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

				String reason = "";
				if (commands.length > 1) reason = " (reason: " + Misc.makeSentence(commands, 1) + ")";

				while (Clients.getClientsSize() > 0) {
					Clients.killClient(Clients.getClient(0), (reason.length() == 0 ? "Disconnected by server" : "Disconnected by server: " + reason));
				}
			}			
			else if (commands[0].equals("CHANNELS")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				Channels.sendChannelListToClient(client);
			}
			else if (commands[0].equals("REQUESTUPDATEFILE")) {
				//***if (client.account.accessLevel() > Account.NIL_ACCESS) return false;
				if (commands.length < 2) return false;
				
				String version = Misc.makeSentence(commands, 1);
				String response = updateProperties.getProperty(version);
				if (response == null)
					response = updateProperties.getProperty("default"); // use general response ("default"), if it exists.
				// if still no response has been found, use some default response:
				if (response == null)
					response = "SERVERMSGBOX No update available. Please download the latest version of the software from official Spring web site: http://spring.clan-sy.com";

				// send a response to the client:
				client.sendLine(response);
				
				// kill client if no update has been found for him:
				if (response.substring(0, 12).toUpperCase().equals("SERVERMSGBOX")) {
					Clients.killClient(client);
				}
			}
			else if (commands[0].equals("LOGIN")) {
				if (client.account.accessLevel() != Account.NIL_ACCESS) {
					client.sendLine("DENIED Already logged in");
					return false; // user with accessLevel > 0 cannot re-login
				}
				
				if (commands.length < 6) {
					client.sendLine("DENIED Bad command arguments");
					return false;
				}
				
				String[] args2 = Misc.makeSentence(commands, 5).split("\t");
				String lobbyVersion = args2[0];
				int userID = Account.NO_USER_ID;
				if (args2.length > 1) try {
					long temp = Long.parseLong(args2[1], 16);
					userID = (int)temp; // we transform unsigned 32 bit integer to a signed one
				} catch (NumberFormatException e) {
					client.sendLine("DENIED <userID> field should be an integer");
					return false; 
				}
				
				int cpu;
				try {
					cpu = Integer.parseInt(commands[3]); 
				} catch (NumberFormatException e) {
					client.sendLine("DENIED <cpu> field should be an integer");
					return false; 
				}
				
				if (!LAN_MODE) { // "normal", non-LAN mode
					String username = commands[1];
					String password = commands[2];
					
					// protection from brute-forcing the account:
					FailedLoginAttempt attempt = findFailedLoginAttempt(username);
					if ((attempt != null) && (attempt.numOfFailedAttempts >= 3)) {
						client.sendLine("DENIED Too many failed login attempts. Wait for 30 seconds before trying again!");
						recordFailedLoginAttempt(username);
						if (!attempt.logged) {
							attempt.logged = true;
							Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Too many failed login attempts for <" + username + "> from " + client.IP + ". Blocking for 30 seconds. Will not notify any longer.");
							// add server notification:
							ServerNotification sn = new ServerNotification("Excessive failed login attempts");
							sn.addLine("Too many failed login attempts for <" + username + "> from " + client.IP + ". Blocking for 30 seconds.");
							ServerNotifications.addNotification(sn);
						}
						return false;
					}
					
					Account acc = verifyLogin(username, password);
					if (acc == null) {
						client.sendLine("DENIED Bad username/password");
						recordFailedLoginAttempt(username);
						return false;
					}
					if (Clients.isUserLoggedIn(acc)) {
						client.sendLine("DENIED Already logged in");
						return false;
					}
					if (Clients.banList.banned(client.IP)) {
						String reason = Clients.banList.getReason(Clients.banList.getIndex(client.IP));
						if (reason.equals("")) reason = "[not given]";
						client.sendLine("DENIED You are banned from this server! (Reason: " + reason + "). Please contact server administrator.");
						recordFailedLoginAttempt(username);
						return false;
					}
					if ((!acc.getAgreement()) && (!client.account.getAgreement()) && (!agreement.equals(""))) {
						sendAgreementToClient(client);
						return false;
					}
					// everything is OK so far!
					if (!acc.getAgreement()) {
						// user has obviously accepted the agreement... Let's update it
						acc.setAgreement(true);
						Accounts.saveAccounts(false);
					}
					client.account = acc;
				} else { // LAN_MODE == true
					Account acc = Accounts.getAccount(commands[1]);
					if (acc != null) {
						client.sendLine("DENIED Player with same name already logged in");
						return false;
					}
					if ((commands[1].equals(lanAdminUsername)) && (commands[2].equals(lanAdminPassword))) acc = new Account(commands[1], commands[2], Account.ADMIN_ACCESS, Account.NO_USER_ID, 0, "?", 0, "XX", new MapGradeList()); 
					else acc = new Account(commands[1], commands[2], Account.NORMAL_ACCESS, Account.NO_USER_ID, 0, "?", 0, "XX", new MapGradeList());
					Accounts.addAccount(acc);
					client.account = acc;
				}
				
				// set client's status:
				client.setRankToStatus(client.account.getRank());
				client.setBotModeToStatus(client.account.getBotMode());
				client.setAccessToStatus((((client.account.accessLevel() >= Account.PRIVILEGED_ACCESS) && (!LAN_MODE)) ? true : false));
				
				client.cpu = cpu;
				client.account.lastLogin = System.currentTimeMillis();
				client.account.lastCountry = client.country;
				client.account.lastIP = client.IP;
				if (commands[4].equals("*")) client.localIP = new String(client.IP);
				else client.localIP = commands[4];
				client.lobbyVersion = lobbyVersion;
				client.account.lastUserID = userID;
				
				// do the notifying and all: 
				client.sendLine("ACCEPTED " + client.account.user);
				sendMOTDToClient(client);
				Clients.sendListOfAllUsersToClient(client);
				Battles.sendInfoOnBattlesToClient(client);
				Clients.sendInfoOnStatusesToClient(client);
				Clients.notifyClientsOfNewClientOnServer(client);
				// notify client that we've finished sending login info:
				client.sendLine("LOGININFOEND");
				
				// notify everyone about client's status:
				Clients.notifyClientsOfNewClientStatus(client);
				
				if (DEBUG > 0) System.out.println("User just logged in: " + client.account.user);
			}
			else if (commands[0].equals("CONFIRMAGREEMENT")) {
				// update client's temp account (he is not logged in yet since he needs to confirm the agreement before server will allow him to log in):
				client.account.setAgreement(true);
			}
			else if (commands[0].equals("USERID")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (commands.length != 2) {
					client.sendLine("SERVERMSG Bad USERID command - too many or too few parameters");
					return false;
				}
				
				int userID = Account.NO_USER_ID;
				try {
					long temp = Long.parseLong(commands[1], 16);
					userID = (int)temp; // we transform unsigned 32 bit integer to a signed one
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG Bad USERID command - userID field should be an integer");
					return false; 
				}
				
				client.account.lastUserID = userID;
				
				// add server notification:
				ServerNotification sn = new ServerNotification("User ID received");
				sn.addLine("<" + client.account.user + "> has generated a new user ID: " + commands[1] + "(" + userID + ")");
				ServerNotifications.addNotification(sn);			
			}			
			else if (commands[0].equals("RENAMEACCOUNT")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (commands.length != 2) {
					client.sendLine("SERVERMSG Bad RENAMEACCOUNT command - too many or too few parameters");
					return false;
				}
				
				if (LAN_MODE) {
					client.sendLine("SERVERMSG RENAMEACCOUNT failed: You cannot rename your account while server is running in LAN mode since you have no account!");
					return false;
				}
				
				// validate new username:
				String valid = Accounts.isOldUsernameValid(commands[1]);
				if (valid != null) {
					client.sendLine("SERVERMSG RENAMEACCOUNT failed: Invalid username (reason: " + valid + ")");
					return false;
				}
				
				Account acc = Accounts.findAccountNoCase(commands[1]);
				if (acc != null) {
					client.sendLine("SERVERMSG RENAMEACCOUNT failed: Account with same username already exists!");
					return false;
				}			

				// make sure all mutes are accordingly adjusted to new username:
				for (int i = 0; i < Channels.getChannelsSize(); i++) {
					Channels.getChannel(i).muteList.rename(client.account.user, commands[1]);
				}
				
				acc = new Account(commands[1], client.account.pass, client.account.access, client.account.lastUserID, System.currentTimeMillis(), client.IP, client.account.registrationDate, client.account.lastCountry, client.account.mapGrades);
				client.sendLine("SERVERMSG Your account has been renamed to <" + commands[1] + ">. Reconnect with new account (you will now be automatically disconnected)!");
				Clients.killClient(client, "Quit: renaming account");
				Accounts.replaceAccount(client.account, acc);
				Accounts.saveAccounts(false); // let's save new accounts info to disk
				Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: User <" + client.account.user + "> has just renamed his account to <" + commands[1] + ">");
				
				// add server notification:
				ServerNotification sn = new ServerNotification("Account renamed");
				sn.addLine("User <" + client.account.user + "> has renamed his account to <" + commands[1] + ">");
				ServerNotifications.addNotification(sn);
			}
			else if (commands[0].equals("CHANGEPASSWORD")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (commands.length != 3) {
					client.sendLine("SERVERMSG Bad CHANGEPASSWORD command - too many or too few parameters");
					return false;
				}
				
				if (LAN_MODE) {
					client.sendLine("SERVERMSG CHANGEPASSWORD failed: You cannot change your password while server is running in LAN mode!");
					return false;
				}
				
				if (!(commands[1].equals(client.account.pass))) {
					client.sendLine("SERVERMSG CHANGEPASSWORD failed: Old password is incorrect!");
					return false;
				}
				
				// validate password:
				String valid = Accounts.isPasswordValid(commands[2]);
				if (valid != null) {
					client.sendLine("SERVERMSG CHANGEPASSWORD failed: Invalid password (reason: " + valid + ")");
					return false;
				}
				
				client.account.pass = commands[2];

				Accounts.saveAccounts(false); // let's save new accounts info to disk
				client.sendLine("SERVERMSG Your password has been successfully updated!");
			}
			else if (commands[0].equals("JOIN")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;

				// check if channel name is OK:
				String valid = Channels.isChanNameValid(commands[1]);
				if (valid != null) {
					client.sendLine("JOINFAILED Bad channel name (\"#" + commands[1] + "\"). Reason: " + valid);
					return false;
				}

				// check if key is correct (if channel is locked):
				Channel chan = Channels.getChannel(commands[1]);
				if ((chan != null) && (chan.isLocked()) && (client.account.accessLevel() < Account.ADMIN_ACCESS /* we will allow admins to join locked channels */)) {
					if (!Misc.makeSentence(commands, 2).equals(chan.getKey())) {
						client.sendLine("JOINFAILED " + commands[1] + " Wrong key (this channel is locked)!");
						return false;
					}
				}

				chan = client.joinChannel(commands[1]);
				if (chan == null) {
					client.sendLine("JOINFAILED " + commands[1] + " Already in the channel!");
					return false;
				}
				client.sendLine("JOIN " + commands[1]);
				Channels.sendChannelInfoToClient(chan, client);
				Channels.notifyClientsOfNewClientInChannel(chan, client);
			}
			else if (commands[0].equals("LEAVE")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) return false;
				
				client.leaveChannel(chan, "");
			}
			else if (commands[0].equals("CHANNELTOPIC")) {
				if (commands.length < 3) return false;
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
				
				Channel chan = Channels.getChannel(commands[1]);
				if (chan == null) {
					client.sendLine("SERVERMSG Error: Channel does not exist: " + commands[1]);
					return false;
				}
				
				if (!chan.setTopic(Misc.makeSentence(commands, 2), client.account.user)) {
					client.sendLine("SERVERMSG You've just disabled the topic for channel #" + chan.name);
					chan.broadcast("<" + client.account.user + "> has just disabled topic for #" + chan.name);
				} else {
					client.sendLine("SERVERMSG You've just changed the topic for channel #" + chan.name);				
					chan.broadcast("<" + client.account.user + "> has just changed topic for #" + chan.name);
					chan.sendLineToClients("CHANNELTOPIC " + chan.name + " " + chan.getTopicAuthor() + " " + chan.getTopicChangedTime() + " " + chan.getTopic());
				}
			}
			else if (commands[0].equals("SAY")) {
				if (commands.length < 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				Channel chan = client.getChannel(commands[1]);
				if (chan == null) return false;
				
				if (chan.muteList.isMuted(client.account.user)) {
					client.sendLine("SERVERMSG Message dropped. You are not allowed to talk in #" + chan.name + "! Please contact one of the moderators.");
					return false;
				} else if (chan.muteList.isIPMuted(client.IP)) {
					client.sendLine("SERVERMSG Message dropped. You are not allowed to talk in #" + chan.name + " (muted by IP address)! If you believe this is an error, contact one of the moderators.");
					return false;
				}
				
				
				String s = Misc.makeSentence(commands, 2);
				// check for flooding:			
				if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
					client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
					return false;
				}
				chan.sendLineToClients("SAID " + chan.name + " " + client.account.user + " " + s);
			}
			else if (commands[0].equals("SAYEX")) {
				if (commands.length < 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				Channel chan = client.getChannel(commands[1]);
				if (chan == null) return false;

				if (chan.muteList.isMuted(client.account.user)) {
					client.sendLine("SERVERMSG Message dropped. You are not allowed to talk in #" + chan.name + "! Please contact one of the moderators.");
					return false;
				} else if (chan.muteList.isIPMuted(client.IP)) {
					client.sendLine("SERVERMSG Message dropped. You are not allowed to talk in #" + chan.name + " (muted by IP address)! If you believe this is an error, contact one of the moderators.");
					return false;
				}
				
				String s = Misc.makeSentence(commands, 2);
				// check for flooding:			
				if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
					client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
					return false;
				}
				
				chan.sendLineToClients("SAIDEX " + chan.name + " " + client.account.user + " " + s);
			}
			else if (commands[0].equals("SAYPRIVATE")) {
				if (commands.length < 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				
				String s = Misc.makeSentence(commands, 2);
				// check for flooding:
				if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
					client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
					return false;
				}

				target.sendLine("SAIDPRIVATE " + client.account.user + " " + s);
				client.sendLine(command); // echo the command. See protocol description!
			}
			else if (commands[0].equals("JOINBATTLE")) {
				if (commands.length < 2) return false; // requires 1 or 2 arguments (password is optional)
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				int battleID;
				
				try {
					battleID = Integer.parseInt(commands[1]); 
				} catch (NumberFormatException e) {
					client.sendLine("JOINBATTLEFAILED " + "No battle ID!");
					return false; 
				}
				
				if (client.battleID != -1) { // can't join a battle if already participating in another battle
					client.sendLine("JOINBATTLEFAILED " + "Cannot participate in multiple battles at the same time!");
					return false; 
				}
				
				Battle bat = Battles.getBattleByID(battleID);
				
				if (bat == null) { 
					client.sendLine("JOINBATTLEFAILED " + "Invalid battle ID!");
					return false; 
				}
				
				if (bat.restricted()) {
					if (commands.length < 3) {
						client.sendLine("JOINBATTLEFAILED " + "Password required");
						return false; 
					}
					
					if (!bat.password.equals(commands[2])) {
						client.sendLine("JOINBATTLEFAILED " + "Invalid password");
						return false; 
					}
				}
				
				if (bat.locked) {
					client.sendLine("JOINBATTLEFAILED " + "You cannot join locked battles!");
					return false; 
				}
				
				if (bat.inGame()) {
					client.sendLine("JOINBATTLEFAILED " + "Battle is already in-game!");
					return false; 
				}

				// do the actually joining and notifying:
				client.battleStatus = 0; // reset client's battle status
				client.battleID = battleID;
				bat.addClient(client);
			 	client.sendLine("JOINBATTLE " + bat.ID + " " + bat.hashCode); // notify client that he has successfully joined the battle
				Clients.notifyClientsOfNewClientInBattle(bat, client);
				bat.notifyOfBattleStatuses(client);
				bat.sendBotListToClient(client);
				// tell host about this client's IP and UDP source port (if battle is hosted using one of the NAT traversal techniques):
				if ((bat.natType == 1) || (bat.natType == 2)) {
					// make sure that clients behind NAT get local IPs and not external ones:
					bat.founder.sendLine("CLIENTIPPORT " + client.account.user + " " + (bat.founder.IP.equals(client.IP) ? client.localIP : client.IP) + " " + client.UDPSourcePort);
				}
				
				client.sendLine("REQUESTBATTLESTATUS");
				bat.sendDisabledUnitsListToClient(client);
				bat.sendStartRectsListToClient(client);
				bat.sendScriptTagsToClient(client);
				
				if (bat.type == 1) bat.sendScriptToClient(client);

			}
			else if (commands[0].equals("LEAVEBATTLE")) {
				if (commands.length != 1) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;

				if (client.battleID == -1) return false; // this may happen when client sent LEAVEBATTLE command right after he was kicked from the battle, for example.
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				Battles.leaveBattle(client, bat); // automatically checks if client is a founder and closes battle
			}
			else if (commands[0].equals("OPENBATTLE")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				if (client.battleID != -1) {
					client.sendLine("OPENBATTLEFAILED " + "You are already hosting a battle!");
					return false;
				}
				Battle bat = Battles.createBattleFromString(command, client);
				if (bat == null) {
					client.sendLine("OPENBATTLEFAILED " + "Invalid command format or bad arguments");
					return false;
				}
				Battles.addBattle(bat);
				client.battleStatus = 0; // reset client's battle status
				client.battleID = bat.ID;

				boolean local;
				for (int i = 0; i < Clients.getClientsSize(); i++) {
					if (Clients.getClient(i).account.accessLevel() < Account.NORMAL_ACCESS) continue;
					// make sure that clients behind NAT get local IPs and not external ones:
					local = client.IP.equals(Clients.getClient(i).IP);
					Clients.getClient(i).sendLine(bat.createBattleOpenedCommandEx(local));
				}
				
				client.sendLine("OPENBATTLE " + bat.ID); // notify client that he successfully opened a new battle
				client.sendLine("REQUESTBATTLESTATUS");
			}		
			else if (commands[0].equals("MYBATTLESTATUS")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;

				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				
				int newTeamColor;
				try {
					newTeamColor = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false; 
				}
				client.teamColor = newTeamColor;
				
				int newStatus;
				try {
					newStatus = Integer.parseInt(commands[1]);
				} catch (NumberFormatException e) {
					return false; 
				}
				// update new battle status. Note: we ignore handicap value as it can be changed only by founder with HANDICAP command!
				client.battleStatus = Misc.setHandicapOfBattleStatus(newStatus, Misc.getHandicapFromBattleStatus(client.battleStatus));
				
				// if game is full or game type is "battle replay", force player's mode to spectator:
				if ((bat.getClientsSize()+1-bat.spectatorCount() > bat.maxPlayers) || (bat.type == 1)) {
					client.battleStatus = Misc.setModeOfBattleStatus(client.battleStatus, 0);
				}
				// if player has chosen team number which is already used by some other player/bot,
				// force his ally number and team color to be the same as of that player/bot:
				if (bat.founder != client)
					if ((Misc.getTeamNoFromBattleStatus(bat.founder.battleStatus) == Misc.getTeamNoFromBattleStatus(client.battleStatus)) && (Misc.getModeFromBattleStatus(bat.founder.battleStatus) != 0)) {
						client.battleStatus = Misc.setAllyNoOfBattleStatus(client.battleStatus, Misc.getAllyNoFromBattleStatus(bat.founder.battleStatus));
						client.teamColor = bat.founder.teamColor;
					} 
				for (int i = 0; i < bat.getClientsSize(); i++)
					if (bat.getClient(i) != client)
						if ((Misc.getTeamNoFromBattleStatus(bat.getClient(i).battleStatus) == Misc.getTeamNoFromBattleStatus(client.battleStatus)) && (Misc.getModeFromBattleStatus(bat.getClient(i).battleStatus) != 0)) {
							client.battleStatus = Misc.setAllyNoOfBattleStatus(client.battleStatus, Misc.getAllyNoFromBattleStatus(bat.getClient(i).battleStatus));
							client.teamColor = bat.getClient(i).teamColor;
							break;
						}
				for (int i = 0; i < bat.getBotsSize(); i++)
					if (Misc.getTeamNoFromBattleStatus(bat.getBot(i).battleStatus) == Misc.getTeamNoFromBattleStatus(client.battleStatus)) {
						client.battleStatus = Misc.setAllyNoOfBattleStatus(client.battleStatus, Misc.getAllyNoFromBattleStatus(bat.getBot(i).battleStatus));
						client.teamColor = bat.getBot(i).teamColor;
						break;
					}
						
				bat.notifyClientsOfBattleStatus(client);
			}
			else if (commands[0].equals("MYSTATUS")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				int newStatus;
				try {
					newStatus = Integer.parseInt(commands[1]);
				} catch (NumberFormatException e) {
					return false; 
				}

				// we must preserve rank bits, access bit and bot mode bit (client is not allowed to change them himself):
				int tmp = client.getRankFromStatus();
				boolean tmp2 = client.getInGameFromStatus();
				boolean tmp3 = client.getAccessFromStatus();
				boolean tmp4 = client.getBotModeFromStatus();
				
				client.status = newStatus;
				
				client.setRankToStatus(tmp);
				client.setAccessToStatus(tmp3);
				client.setBotModeToStatus(tmp4);
				
				if (client.getInGameFromStatus() != tmp2) {
					// user changed his in-game status.
					if (tmp2 == false) { // client just entered game
						Battle bat = Battles.getBattleByID(client.battleID);
						if ((bat != null) && (bat.getClientsSize() > 0))
								client.inGameTime = System.currentTimeMillis();
						else client.inGameTime = 0; // we won't update clients who play by themselves (or with bots), since some try to exploit the system by leaving computer alone in-battle for hours to increase their ranks
						// check if client is a battle host using "hole punching" technique:
						if ((bat != null) && (bat.founder == client) && (bat.natType == 1)) {
							// tell clients to replace battle port with founder's public UDP source port:
							bat.sendToAllExceptFounder("HOSTPORT " + client.UDPSourcePort);
						}
						if (bat != null) client.mapHashUponEnteringGame = Misc.intToHex(bat.mapHash);
					} else { // back from game
						if (client.inGameTime != 0) { // we won't update clients who play by themselves (or with bots only), since some try to exploit the system by leaving computer alone in-battle for hours to increase their ranks 
							int diff = new Long((System.currentTimeMillis() - client.inGameTime) / 60000).intValue(); // in minutes
							if (client.account.addMinsToInGameTime(diff)) {
								client.setRankToStatus(client.account.getRank());
							}
							// we will also update map in-game time for this client here:
							if(client.mapHashUponEnteringGame != null) MapGrading.updateLocalMapGradeMins(client, client.mapHashUponEnteringGame, diff);
						}
						client.mapHashUponEnteringGame = null;
					}
				}
				Clients.notifyClientsOfNewClientStatus(client);
			}
			else if (commands[0].equals("SAYBATTLE")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				
				String s = Misc.makeSentence(commands, 1); 
				// check for flooding:			
				if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
					client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
					return false;
				}				
				
				bat.sendToAllClients("SAIDBATTLE " + client.account.user + " " + s);
			}
			else if (commands[0].equals("SAYBATTLEEX")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;

				String s = Misc.makeSentence(commands, 1);
				// check for flooding:			
				if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
					client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
					return false;
				}				
				
				bat.sendToAllClients("SAIDBATTLEEX " + client.account.user + " " + s);
			}
			else if (commands[0].equals("UPDATEBATTLEINFO")) {
				if (commands.length < 5) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder may change battle parameters!
				
				int spectatorCount = 0;
				boolean locked;
				int maphash;
				try {
					spectatorCount = Integer.parseInt(commands[1]);
					locked = Misc.strToBool(commands[2]);
					maphash = Integer.parseInt(commands[3]);
				} catch (NumberFormatException e) {
					return false; 
				}
				
				bat.mapName = Misc.makeSentence(commands, 4);
				bat.locked = locked;
				bat.mapHash = maphash;
				Clients.sendToAllRegisteredUsers("UPDATEBATTLEINFO " + bat.ID + " " + spectatorCount + " " + Misc.boolToStr(bat.locked) + " " + maphash + " " + bat.mapName);
			}
			else if (commands[0].equals("HANDICAP")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can change handicap value of another user
				
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false; 
				}
				if ((value < 0) || (value > 100)) return false;

				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				if (!bat.isClientInBattle(target)) return false;
				
				target.battleStatus = Misc.setHandicapOfBattleStatus(target.battleStatus, value); 
				bat.notifyClientsOfBattleStatus(target);
			}
			else if (commands[0].equals("KICKFROMBATTLE")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can kick other clients
				
				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				if (!bat.isClientInBattle(target)) return false;
				
				bat.sendToAllClients("SAIDBATTLEEX " + client.account.user + " kicked " + target.account.user + " from battle");
				// notify client that he was kicked from the battle:
				target.sendLine("FORCEQUITBATTLE");
				// force client to leave battle:
				tryToExecCommand("LEAVEBATTLE", target);
			}
			else if (commands[0].equals("FORCETEAMNO")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can force team/ally numbers
				
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false; 
				}
				if ((value < 0) || (value > TASServer.MAX_TEAMS-1)) return false;
				
				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				if (!bat.isClientInBattle(target)) return false;
				
				target.battleStatus = Misc.setTeamNoOfBattleStatus(target.battleStatus, value);
				bat.notifyClientsOfBattleStatus(target); 
			}
			else if (commands[0].equals("FORCEALLYNO")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can force team/ally numbers
				
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false; 
				}
				if ((value < 0) || (value > TASServer.MAX_TEAMS-1)) return false;
				
				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				if (!bat.isClientInBattle(target)) return false;
				
				target.battleStatus = Misc.setAllyNoOfBattleStatus(target.battleStatus, value);
				bat.notifyClientsOfBattleStatus(target); 
			}		
			else if (commands[0].equals("FORCETEAMCOLOR")) {
				if (commands.length != 3) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can force team color change
				
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false; 
				}
				
				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				if (!bat.isClientInBattle(target)) return false;
				
				target.teamColor = value;
				bat.notifyClientsOfBattleStatus(target); 
			}	
			else if (commands[0].equals("FORCESPECTATORMODE")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can force spectator mode
				
				Client target = Clients.getClient(commands[1]);
				if (target == null) return false;
				if (!bat.isClientInBattle(target)) return false;
				
				if (Misc.getModeFromBattleStatus(target.battleStatus) == 0) return false; // no need to change it, it's already set to spectator mode!
				
				target.battleStatus = Misc.setModeOfBattleStatus(target.battleStatus, 0);
				bat.notifyClientsOfBattleStatus(target); 
			}		
			else if (commands[0].equals("ADDBOT")) {
				if (commands.length < 5) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				int value;
				try {
					value = Integer.parseInt(commands[2]); 
				} catch (NumberFormatException e) {
					return false; 
				}
				
				int teamColor;
				try {
					teamColor = Integer.parseInt(commands[3]); 
				} catch (NumberFormatException e) {
					return false; 
				}

				if (!commands[1].matches("^[A-Za-z0-9_]+$")) {
					client.sendLine("SERVERMSGBOX Bad bot name. Try another!");
					return false;
				}
				
				if (bat.getBot(commands[1]) != null) {
					client.sendLine("SERVERMSGBOX Bot name already assigned. Choose another!");
					return false;
				}

				Bot bot = new Bot(commands[1], client.account.user, Misc.makeSentence(commands, 4), value, teamColor);
				bat.addBot(bot);
				
				bat.sendToAllClients("ADDBOT " + bat.ID + " " + bot.name + " " + client.account.user + " " + bot.battleStatus + " " + bot.teamColor + " " + bot.AIDll);
				
			}
			else if (commands[0].equals("REMOVEBOT")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				Bot bot = bat.getBot(commands[1]);
				if (bot == null) return false;
				
				bat.removeBot(bot);
				
				bat.sendToAllClients("REMOVEBOT " + bat.ID + " " + bot.name);
			}
			else if (commands[0].equals("UPDATEBOT")) {
				if (commands.length != 4) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				Bot bot = bat.getBot(commands[1]);
				if (bot == null) return false;

				int value;
				try {
					value = Integer.parseInt(commands[2]); 
				} catch (NumberFormatException e) {
					return false; 
				}

				int teamColor;
				try {
					teamColor = Integer.parseInt(commands[3]); 
				} catch (NumberFormatException e) {
					return false; 
				}
				
				// only bot owner and battle host are allowed to update bot: 
				if (!((client.account.user.equals(bot.ownerName)) || (client.account.user.equals(bat.founder.account.user)))) return false; 
						
				bot.battleStatus = value;
				bot.teamColor = teamColor;

				//*** add: force ally and color number if someone else is using his team number already 
				
				bat.sendToAllClients("UPDATEBOT " + bat.ID + " " + bot.name + " " + bot.battleStatus + " " + bot.teamColor);
			}
			else if (commands[0].equals("DISABLEUNITS")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can disable/enable units

				// let's check if client didn't double the data (he shouldn't, but we can't
				// trust him, so we will check ourselves):
				for (int i = 1; i < commands.length; i++) {
					if (bat.disabledUnits.indexOf(commands[i]) != -1) continue;
					bat.disabledUnits.add(commands[i]);
				}
				
				bat.sendToAllExceptFounder(command);
			}		
			else if (commands[0].equals("ENABLEUNITS")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can disable/enable units

				for (int i = 1; i < commands.length; i++) {
					bat.disabledUnits.remove(commands[i]); // will ignore it if string is not found in the list
				}
				
				bat.sendToAllExceptFounder(command);
			}		
			else if (commands[0].equals("ENABLEALLUNITS")) {
				if (commands.length != 1) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) return false;
				if (bat.founder != client) return false; // only founder can disable/enable units

				bat.disabledUnits.clear();
				
				bat.sendToAllExceptFounder(command);
			}		
			else if (commands[0].equals("RING")) {
				if (commands.length != 2) return false;
				// privileged users can ring anyone, "normal" users can ring only when they are hosting
				// and only clients who are participating in their battle
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) { // normal user
					Client target = Clients.getClient(commands[1]);
					if (target == null) return false;

					if (client.battleID == -1) {
						client.sendLine("SERVERMSG RING command failed: You can only ring players participating in your own battle!");
						return false; 
					}
					
					Battle bat = Battles.getBattleByID(client.battleID);
					if (bat == null) {
						System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
						closeServerAndExit();
					}
					
					if (!bat.isClientInBattle(commands[1])) {
						client.sendLine("SERVERMSG RING command failed: You don't have permission to ring players other than those participating in your battle!");
						return false;
					}
					
					// only host can ring players participating in his own battle, unless target is host himself:
					if ((client != bat.founder) && (target != bat.founder)) {
						client.sendLine("SERVERMSG RING command failed: You can ring only battle host, or if you are the battle host, only players participating in your own battle!");
						return false;
					}
													
					target.sendLine("RING " + client.account.user);
				} else { // privileged user
					Client target = Clients.getClient(commands[1]);
					if (target == null) return false;
					
					target.sendLine("RING " + client.account.user);
				}
			}
			else if (commands[0].equals("ADDSTARTRECT")) {
				if (commands.length != 6) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				if (bat.founder != client) return false;

				int allyno, left, top, right, bottom;
				try {
					allyno = Integer.parseInt(commands[1]);
					left = Integer.parseInt(commands[2]);
					top = Integer.parseInt(commands[3]);
					right = Integer.parseInt(commands[4]);
					bottom = Integer.parseInt(commands[5]);
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
					Clients.killClient(client, "Quit: inconsistent data");
					return false; 
				}
				
				if (bat.startRects[allyno].enabled) {
					client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
					Clients.killClient(client, "Quit: inconsistent data");
					return false; 
				}
				
				bat.startRects[allyno].enabled = true;
				bat.startRects[allyno].left = left;
				bat.startRects[allyno].top = top;
				bat.startRects[allyno].right = right;
				bat.startRects[allyno].bottom = bottom;
				
				bat.sendToAllExceptFounder("ADDSTARTRECT " + allyno + " " + left + " " + top + " " + right + " " + bottom);
			}		
			else if (commands[0].equals("REMOVESTARTRECT")) {
				if (commands.length != 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				if (bat.founder != client) return false;

				int allyno;
				try {
					allyno = Integer.parseInt(commands[1]);
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
					Clients.killClient(client, "Quit: inconsistent data");
					return false; 
				}
				
				if (!bat.startRects[allyno].enabled) {
					client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
					Clients.killClient(client, "Quit: inconsistent data");
					return false; 
				}
				
				bat.startRects[allyno].enabled = false;
			
				bat.sendToAllExceptFounder("REMOVESTARTRECT " + allyno);
			}		
			else if (commands[0].equals("SCRIPTSTART")) {
				if (commands.length != 1) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				bat.tempReplayScript.clear();
			}				
			else if (commands[0].equals("SCRIPT")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				bat.tempReplayScript.add(Misc.makeSentence(commands, 1));
			}				
			else if (commands[0].equals("SCRIPTEND")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				
				// copy temp script to active script:
				bat.ratifyTempScript();
				
				bat.sendScriptToAllExceptFounder();
			} 				
			else if (commands[0].equals("SETSCRIPTTAGS")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}

				if (bat.founder != client) return false;

				if (commands.length < 2) {
					// kill client since it is not using this command correctly
					client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
					Clients.killClient(client, "Quit: inconsistent data");
					return false;
				}

				int pairsStart = command.indexOf(' ');
				if (pairsStart < 0) {
					return false;
				}
				String[] pairs = command.substring(pairsStart + 1).split("\t");
				String validPairs = "";
				
				for (int i = 0; i < pairs.length; i++) {

					String s = pairs[i];

					int equalPos = s.indexOf('=');
					if (equalPos < 1) { continue; }

					// parse the key
					String key = s.substring(0, equalPos).toLowerCase();
					if (key.length() <= 0)      { continue; }
					if (key.indexOf(' ')  >= 0) { continue; }
					if (key.indexOf('=')  >= 0) { continue; }
					if (key.indexOf(';')  >= 0) { continue; }
					if (key.indexOf('{')  >= 0) { continue; }
					if (key.indexOf('}')  >= 0) { continue; }
					if (key.indexOf('[')  >= 0) { continue; }
					if (key.indexOf(']')  >= 0) { continue; }
					if (key.indexOf('\n') >= 0) { continue; }
					if (key.indexOf('\r') >= 0) { continue; }

					// parse the value
					String value = s.substring(equalPos + 1);
					if (value != value.trim())    { continue; } // forbid trailing/leading spaces
					if (value.indexOf(';')  >= 0) { continue; }
					if (value.indexOf('}')  >= 0) { continue; }
					if (value.indexOf('[')  >= 0) { continue; }
					if (value.indexOf('\n') >= 0) { continue; }
					if (value.indexOf('\r') >= 0) { continue; }

					// insert the tag data into the map
					bat.scriptTags.put(key, value);

					// add to the validPairs string
					if (validPairs.length() > 0) {
						validPairs += "\t";
					}
					validPairs += key + "=" + value; 
				}

				// relay the valid pairs
				if (validPairs.length() > 0) {
					bat.sendToAllClients("SETSCRIPTTAGS " + validPairs);
				}
			}				
			else if (commands[0].equals("REMOVESCRIPTTAGS")) {
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (client.battleID == -1) return false;
				
				Battle bat = Battles.getBattleByID(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}

				if (bat.founder != client) return false;

				if (commands.length < 2) {
					// kill client since it is not using this command correctly
					client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
					Clients.killClient(client, "Quit: inconsistent data");
					return false;
				}

				String loweyKeyCommand = "REMOVESCRIPTTAGS";
				for (int i = 1; i < commands.length; i++) {
					String lowerKey = commands[i].toLowerCase();
					loweyKeyCommand += " " + lowerKey;
					bat.scriptTags.remove(lowerKey);
				}

				// relay the command
				bat.sendToAllClients(loweyKeyCommand);
			}				
			else if (commands[0].equals("MAPGRADES")) {
				if (commands.length < 2) return false;
				if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
				
				if (LAN_MODE) {
					client.sendLine("MAPGRADESFAILED Unable to synchronize map grades - server is running in LAN mode!");
					return false;
				}

				String[] tokens = Misc.makeSentence(commands, 1).split(" ");
				if (tokens.length % 2 != 0) {
					client.sendLine("MAPGRADESFAILED Invalid params to MAPGRADES command!");
					return false;
				}
				
				if (System.currentTimeMillis() - client.lastMapGradesReceived < minSleepTimeBetweenMapGrades * 1000) {
					client.sendLine("MAPGRADESFAILED Less than " + minSleepTimeBetweenMapGrades + " seconds have passed since your last synchronization, try again later!");
					return false;
				}
				
				String respond = "MAPGRADES"; // message that we will send back to the client
				try {
					for (int i = 0; i < tokens.length / 2; i++) {
						String hash = tokens[i*2].toUpperCase();
						int grade;
						try {
							Long.parseLong(hash, 16);
							grade = Integer.parseInt(tokens[i*2+1]);
						} catch (NumberFormatException e) {
							return false;
						}
						if ((grade < 0) || (grade > 10)) return false;
						MapGrading.updateLocalAndGlobalGrade(client, hash, grade);
						respond += " " + hash + " " + MapGrading.getAvarageMapGrade(hash) + " " + MapGrading.getNumberOfMapVotes(hash); 
					}
					client.sendLine(respond);
					client.lastMapGradesReceived = System.currentTimeMillis();
				} catch (Exception e) {
					return false;
				}
			} 	
			else {
				// unknown command!
				return false;
			}
		} finally {
			client.setSendMsgID(NO_MSG_ID);
		}
		
		
		return true;
		
	} // tryToExecCommand()
	
	/* processes all arguments from string 'args'. Raises an exception in case of errors. */
	public static void processCommandLineArguments(String[] args) throws IOException, Exception {
		// process command line arguments:
		String s;
		for (int i = 0; i < args.length; i++) {
			if (args[i].charAt(0) == '-') {
				s = args[i].substring(1).toUpperCase();
				if (s.equals("PORT")) {
					int p = Integer.parseInt(args[i+1]);
					if ((p < 1) || (p > 65535)) throw new IOException();
					serverPort = p;
					i++; // we must skip port number parameter in the next iteration
				}
				else if (s.equals("LAN")) {
					LAN_MODE = true;						
				}
				else if (s.equals("DEBUG")) {
					int level = Integer.parseInt(args[i+1]);											
					if ((level < 0) || (level > 127)) throw new IOException();
					DEBUG = (byte)level;
					i++;  // we must skip debug level parameter in the next iteration
				}
				else if (s.equals("STATISTICS")) {
					RECORD_STATISTICS = true;
				}
				else if (s.equals("NATPORT")) {
					int p = Integer.parseInt(args[i+1]);
					if ((p < 1) || (p > 65535)) throw new IOException();
					NAT_TRAVERSAL_PORT = p;
					i++; // we must skip port number parameter in the next iteration
				}
				else if (s.equals("LOGMAIN")) {
					LOG_MAIN_CHANNEL = true;
				}
				else if (s.equals("LANADMIN")) {
					lanAdminUsername = args[i+1];
					lanAdminPassword = Misc.encodePassword(args[i+2]);

					if (Accounts.isOldUsernameValid(lanAdminUsername) != null) {
						System.out.println("Lan admin username is not valid: " + Accounts.isOldUsernameValid(lanAdminUsername));
						throw new Exception();
					}
					if (Accounts.isPasswordValid(lanAdminPassword) != null) {
						System.out.println("Lan admin password is not valid: " + Accounts.isPasswordValid(lanAdminPassword));
						throw new Exception();
					}
					i += 2; // we must skip username and password parameters in next iteration
				}
				else if (s.equals("LOADARGS")) {
					try {
						BufferedReader in = new BufferedReader(new FileReader(args[i+1]));
						String line;
						while ((line = in.readLine()) != null) {
							try{
								processCommandLineArguments(line.split(" "));
							} catch (Exception e) {
								System.out.println("Error in reading " + args[i+1] + " (invalid line)");
								System.out.println(e.getMessage());
								throw e;
							}
						}
		 				in.close();
					} catch (IOException e) {
						throw e;
					}
					i++; // we must skip filename parameter in the next iteration
				}
				else if (s.equals("LATESTSPRINGVERSION")) {
					latestSpringVersion = args[i+1];
					i++; // to skip Spring version argument
				}
				else if (s.equals("DBURL")) {
					DB_URL = args[i+1];
					i++; // to skip argument
				}
				else if (s.equals("DBUSERNAME")) {
					DB_username = args[i+1];
					i++; // to skip the argument
				}
				else if (s.equals("DBPASSWORD")) {
					DB_password = args[i+1];
					i++; // to skip the argument
				}
				else {
					System.out.println("Invalid commandline argument");
					throw new IOException();
				}
			} else {
				System.out.println("Commandline argument does not start with a hyphen");
				throw new IOException();
			}
		}
	}
	
	public static void main(String[] args) {

		// process command line arguments:
		try {
			processCommandLineArguments(args);
		} catch (Exception e) {
			System.out.println("Bad arguments. Usage:");
			System.out.println("");
			System.out.println("-PORT [number]");
			System.out.println("  Server will host on port [number]. If command is omitted,\n" +
							   "  default port will be used.");
			System.out.println("");
			System.out.println("-LAN");
			System.out.println("  Server will run in \"LAN mode\", meaning any user can login as\n" +
							   "  long as he uses unique username (password is ignored).\n" +
							   "  Note: Server will accept users from outside the local network too.");
			System.out.println("");
			System.out.println("-DEBUG [number]");
			System.out.println("  Use 0 for no verbose, 1 for normal and 2 for extensive verbose.");
			System.out.println("");
			System.out.println("-STATISTICS");
			System.out.println("  Server will create and save statistics on disk on predefined intervals.");
			System.out.println("");
			System.out.println("-NATPORT [number]");
			System.out.println("  Server will use this port with some NAT traversal techniques. If command is omitted,\n" +
							   "  default port will be used.");
			System.out.println("");
			System.out.println("-LOGMAIN");
			System.out.println("  Server will log all conversations from channel #main to MainChanLog.log");
			System.out.println("");
			System.out.println("-LANADMIN [username] [password]");
			System.out.println("  Will override default lan admin account. Use this account to set up your lan server\n");
			System.out.println("  at runtime.");
			System.out.println("");
			System.out.println("-LOADARGS [filename]");
			System.out.println("  Will read command-line arguments from the specified file. You can freely combine actual\n");
			System.out.println("  command-line arguments with the ones from the file (if duplicate args are specified, the last\n");
			System.out.println("  one will prevail).");
			System.out.println("");
			System.out.println("-LATESTSPRINGVERSION [version]");
			System.out.println("  Will set latest Spring version to this string. By default no value is set (defaults to \"*\").\n");
			System.out.println("  This is used to tell clients which version is the latest one so that they know when to update.\n");
			System.out.println("");
			System.out.println("-DBURL [url]");
			System.out.println("  Will set URL of the database (used only in \"normal mode\", not LAN mode).\n");
			System.out.println("");
			System.out.println("-DBUSERNAME [username]");
			System.out.println("  Will set username for the database (used only in \"normal mode\", not LAN mode).\n");
			System.out.println("");
			System.out.println("-DBPASSWORD [password]");
			System.out.println("  Will set password for the database (used only in \"normal mode\", not LAN mode).\n");
			System.out.println("");
						
			closeServerAndExit();
		}

		System.out.println("TASServer " + VERSION + " started on " + Misc.easyDateFormat("yyyy.MM.dd 'at' hh:mm:ss z"));
		
		// switch to lan mode if user accounts information is not present:
		if (!LAN_MODE) {
			if (!(new File(ACCOUNTS_INFO_FILEPATH)).exists()) {
				System.out.println("Accounts info file not found, switching to \"lan mode\" ...");
				LAN_MODE = true;
			}
		}
		
		if (!LAN_MODE) {
			Accounts.loadAccounts();
			Clients.banList.loadFromFile(BAN_LIST_FILENAME);
			readAgreement();
		} else {
			System.out.println("LAN mode enabled");
		}
		
		if (RECORD_STATISTICS) {
			// create statistics folder if it doesn't exist yet:
			File file = new File(STATISTICS_FOLDER);
			if (!file.exists()) {
				boolean success = (file.mkdir());
				if (!success) 
					System.out.println("Error: unable to create folder: " + STATISTICS_FOLDER);
				else 
					System.out.println("Created missing folder: " + STATISTICS_FOLDER);
			}
		}
		
		if (LOG_MAIN_CHANNEL) {
			try {
				mainChanLog = new PrintStream(new BufferedOutputStream(new FileOutputStream("MainChanLog.log", true)), true);
				writeMainChanLog("Log started on " + Misc.easyDateFormat("dd/MM/yy"));
			} catch (Exception e) {
				LOG_MAIN_CHANNEL = false;
				System.out.println("$ERROR: Unable to open main channel log file (MainChanLog.log)");
				e.printStackTrace();
			}
		}
		
		// create notifications folder if it doesn't exist yet:
		if (!LAN_MODE) {
			File file = new File(SERVER_NOTIFICATION_FOLDER);
			if (!file.exists()) {
				boolean success = (file.mkdir());
				if (!success) 
					System.out.println("Error: unable to create folder: " + SERVER_NOTIFICATION_FOLDER);
				else 
					System.out.println("Created missing folder: " + SERVER_NOTIFICATION_FOLDER);
			}
		}
		
		readMOTD(MOTD_FILENAME);
		upTime = System.currentTimeMillis();
		
		if (readUpdateProperties(UPDATE_PROPERTIES_FILENAME)) {
			System.out.println("\"Update properties\" read from " + UPDATE_PROPERTIES_FILENAME);
		}
		
		long tempTime = System.currentTimeMillis();
		if (!IP2Country.initializeAll(IP2COUNTRY_FILENAME)) {
			System.out.println("Unable to find or read <IP2Country> file. Skipping ...");			
		} else {
			tempTime = System.currentTimeMillis() - tempTime;
			System.out.println("<IP2Country> loaded in " + tempTime + " ms.");
		}
			
		// construct global map grade list:	
		MapGrading.reconstructGlobalMapGrades();	

		// establish connection with database:
		if (!LAN_MODE) {
			database = new DBInterface();
			if (!database.loadJDBCDriver()) {
				closeServerAndExit();
			}
			if (!database.connectToDatabase(DB_URL, DB_username, DB_password)) {
				closeServerAndExit();
			}
		}
		
		// start "help UDP" server:
		helpUDPsrvr = new NATHelpServer(NAT_TRAVERSAL_PORT);
		helpUDPsrvr.start();
		
		// start server:
		if (!startServer(serverPort)) closeServerAndExit();
		
		// add server notification:
		ServerNotification sn = new ServerNotification("Server started");
		sn.addLine("Server has been started on port " + serverPort + ". There are " + Accounts.getAccountsSize() + " accounts currently loaded. See server log for more info.");
		ServerNotifications.addNotification(sn);
		
		initializationFinished = true; // we're through the initialization part
		
	    running = true;
	    while (running) { // main loop

	    	// check for new client connections
		    acceptNewConnections();
		    
		    // check for incoming msgs
		    readIncomingMessages();
		    
		    // reset received bytes count every n seconds
		    if (System.currentTimeMillis() - lastFloodCheckedTime > recvRecordPeriod * 1000) {
		    	lastFloodCheckedTime = System.currentTimeMillis();
		    	for (int i = 0; i < Clients.getClientsSize(); i++) Clients.getClient(i).dataOverLastTimePeriod = 0;
		    }
		    
		    // check for timeouts:
		    if (System.currentTimeMillis() - lastTimeoutCheck > TIMEOUT_LENGTH) {
		    	lastTimeoutCheck = System.currentTimeMillis();
		    	long now = System.currentTimeMillis();
		    	for (int i = 0; i < Clients.getClientsSize(); i++) {
		    		if (now - Clients.getClient(i).timeOfLastReceive > TIMEOUT_LENGTH) {
		    			System.out.println("Timeout detected from " + Clients.getClient(i).account.user + " (" + Clients.getClient(i).IP + "). Killing client ...");
		    			Clients.killClient(Clients.getClient(i), "Quit: timeout");		
		    			i--;
		    		}
		    	}
		    }
		    
		    // kill all clients scheduled to be killed:
		    Clients.processKillList();
		    
		    // update statistics:
		    if ((RECORD_STATISTICS) && (System.currentTimeMillis() - Statistics.lastStatisticsUpdate > saveStatisticsInterval))
		    	Statistics.saveStatisticsToDisk();
		    	
		    // check UDP server for any new packets:
		    while (NATHelpServer.msgList.size() > 0) {
		    	DatagramPacket packet = (DatagramPacket)NATHelpServer.msgList.remove(0); 
	            InetAddress address = packet.getAddress();
	            int p = packet.getPort();
	            String data = new String(packet.getData(), packet.getOffset(), packet.getLength());
	            if (DEBUG > 1) System.out.println("*** UDP packet received from " + address.getHostAddress() + " from port " + p);
	            Client client = Clients.getClient(data);
	            if (client == null) continue;
	            client.UDPSourcePort = p;
	            client.sendLine("UDPSOURCEPORT " + p);
		    }
		    
		    // save accounts info to disk on regular intervals:
		    Accounts.saveAccountsIfNeeded();

		    // purge mute lists of all channels on regular intervals:
		    if (System.currentTimeMillis() - lastMutesPurgeTime > purgeMutesInterval) {
		    	lastMutesPurgeTime = System.currentTimeMillis();
		    	for (int i = 0; i < Channels.getChannelsSize(); i++) {
		    		Channels.getChannel(i).muteList.clearExpiredOnes();
		    	}
		    }
		    
		    // pure list of failed login attempts:
		    if (System.currentTimeMillis() - lastFailedLoginsPurgeTime > 1000) {
		    	lastFailedLoginsPurgeTime = System.currentTimeMillis();
		    	for (int i = 0; i < failedLoginAttempts.size(); i++) {
		    		FailedLoginAttempt attempt = failedLoginAttempts.get(i);
		    		if (System.currentTimeMillis() - attempt.timeOfLastFailedAttempt > 30000) {
		    			failedLoginAttempts.remove(i);
		    			i--;
		    		}
		    	}
		    }
		    
		    // sleep a bit
		    try {
		    	Thread.sleep(MAIN_LOOP_SLEEP);
		    } catch (InterruptedException ie) {
		    }
	    }

	    // close everything:
		if (!LAN_MODE) Accounts.saveAccounts(true);
		if (helpUDPsrvr.isAlive()) {
			helpUDPsrvr.stopServer();
			try {
				helpUDPsrvr.join(1000); // give it 1 second to shut down gracefully
		    } catch (InterruptedException e) {
		    }
		}
		if (LOG_MAIN_CHANNEL) try {
			mainChanLog.close();
		} catch (Exception e) {
		  // ignore
		}
	    
		// add server notification:
		sn = new ServerNotification("Server stopped");
		sn.addLine("Server has just been stopped gracefully. See server log for more info.");
		ServerNotifications.addNotification(sn);
		
        System.out.println("Server closed gracefully!");
	}
}
