/*
 * Created on 2005.6.16
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * 
 * ---- CHANGELOG ----
 * *** 0.26 ***
 * * fixed some charset bug
 * * added UPDATEMOTD command
 * * fixed small bug with JOINBATTLE command not checking if battle is already in-game
 * * fixed minor bug with mute entries not expiring on the fly
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
 *   for some of the "ambigious data" errors)  
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
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class TASServer {
	
	static final String VERSION = "0.26";
	static byte DEBUG = 1; // 0 - no verbose, 1 - normal verbose, 2 - extensive verbose
	static String MOTD = "Enjoy your stay :-)";
	static String agreement = ""; // agreement which is sent to user upon first login. User must send CONFIRMAGREEMENT command to confirm the agreement before server allows him to log in. See LOGIN command implementation for more details.
	static long upTime;
	static final String MOTD_FILENAME = "motd.txt";
	static final String AGREEMENT_FILENAME = "agreement.rtf";
	static final String BAN_LIST_FILENAME = "banlist.txt";
	static final String ACCOUNTS_INFO_FILEPATH = "accounts.txt";
	static final int SERVER_PORT = 8200; // default server (TCP) port
	static int NAT_TRAVERSAL_PORT = 8201; // default UDP port used with some NAT traversal technique. If this port is not forwarded, hole punching technique will not work. 
	static final int TIMEOUT_LENGTH = 30000; // in milliseconds
	static boolean LAN_MODE = false;
	static boolean redirect = false; // if true, server is redirection clients to new IP
	static String redirectToIP = ""; // new IP to which clients are redirected if (redirected==true)
	static long saveAccountInfoInterval = 1000 * 60 * 60; // in milliseconds
	static long lastSaveAccountsTime = System.currentTimeMillis(); // time when we last saved accounts info to disk
	static boolean RECORD_STATISTICS = false; // if true, statistics are saved to disk on regular intervals
	static String PLOTICUS_FULLPATH = "./ploticus/bin/pl"; // see http://ploticus.sourceforge.net/ for more info on ploticus
	static String STATISTICS_FOLDER = "./stats/";
	static long saveStatisticsInterval = 1000 * 60 * 20; // in milliseconds
	static long lastStatisticsUpdate = System.currentTimeMillis(); // time (System.currentTimeMillis()) when we last updated statistics
	static boolean LOG_MAIN_CHANNEL = false; // if true, server will keep a log of all conversations from channel #main (in file "MainChanLog.log")
	static PrintStream mainChanLog;
	static String LanAdminUsername = "admin"; // default lan admin account. Can be overwritten with -LANADMIN switch. Used only when server is running in lan mode!
	static String LanAdminPassword = Misc.encodePassword("admin");
	static long purgeMutesInterval = 1000 * 3; // in miliseconds. On this interval, all channels' mute lists will be checked for expirations and purged accordingly. 
	static long lastMutesPurgeTime = System.currentTimeMillis(); // time when we last purged mute lists of all channels
	
    private static final int BYTE_BUFFER_SIZE = 256; // the size doesn't really matter. Server will work with any size (tested it with BUFFER_LENGTH=1), but too small buffer may impact the performance.
    private static final int SEND_BUFFER_SIZE = 65536 * 2; // socket's send buffer size
    private static final long CHANNEL_WRITE_SLEEP = 0L;
    private static final long MAIN_LOOP_SLEEP = 10L;
    
    private static final int recvRecordPeriod = 10; // in seconds. Length of period of time for which we keep record of bytes received from client. Used with anti-flood protection.
    private static final int maxBytesAlert = 20000; // maximum number of bytes received in the last recvRecordPeriod seconds from a single client before we raise "flood alert". Used with anti-flood protection.
    private static long lastFloodCheckedTime = System.currentTimeMillis(); // time (in same format as System.currentTimeMillis) when we last updated it. Used with anti-flood protection.
    private static long maxChatMessageLength = 1024; // used with basic anti-flood protection. Any chat messages (channel or private chat messages) longer than this are considered flooding. Used with following commands: SAY, SAYEX, SAYPRIVATE, SAYBATTLE, SAYBATTLEEX
    
    private static long lastTimeoutCheck = System.currentTimeMillis(); // time (System.currentTimeMillis()) when we last checked for timeouts from clients
    
    private static ServerSocketChannel sSockChan;
    private static Selector readSelector;
    //***private static SelectionKey selectKey;
    private static boolean running;
    private static ByteBuffer readBuffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE);
    private static ByteBuffer writeBuffer = ByteBuffer.allocateDirect(BYTE_BUFFER_SIZE);
    private static CharsetDecoder asciiDecoder;
    
	static Vector accounts = new Vector();
	static Vector clients = new Vector();
	static Vector channels = new Vector();
	static Vector battles = new Vector();
	static BanList banList = new BanList();
	static Vector killList = new Vector(); // a list of clients waiting to be killed (disconnected)
	/* killList is used when we want to kill a client but not immediately (within a loop, for example).
	 * Client on the list will get killed after main loop reaches its end. Server
	 * will empty the list in its main loop, so if the same client is added to the
	 * list more than once in the same loop, server will kill it only once and remove 
	 * redundant entries. */
	 
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
	
	/* (re)loads accounts information from disk */
	private static boolean readAccountsInfo()
	{
		try {
			BufferedReader in = new BufferedReader(new FileReader(ACCOUNTS_INFO_FILEPATH));

			accounts.clear();
			
			String line;
			String tokens[];
			
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	tokens = line.split(" ");
            	if (tokens.length != 6) continue; // this should not happen! If it does, we simply ignore this line.
            	addAccount(new Account(tokens[0], tokens[1], Integer.parseInt(tokens[2], 2), Long.parseLong(tokens[3]), tokens[4], Long.parseLong(tokens[5])));
	        }
            
            in.close();
			
		} catch (IOException e) {
			// catch possible io errors from readLine()
			System.out.println("IOException error while trying to update accounts info from " + ACCOUNTS_INFO_FILEPATH + "! Skipping ...");
			return false;
		}
		
		System.out.println(accounts.size() + " accounts information read from " + ACCOUNTS_INFO_FILEPATH);
		
		return true;
	}
	
	private static boolean writeAccountsInfo()
	{
		lastSaveAccountsTime = System.currentTimeMillis();
		
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ACCOUNTS_INFO_FILEPATH)));			

			for (int i = 0; i < accounts.size(); i++)
			{
				out.println(accounts.get(i).toString());
			}
			
			out.close();
			
		} catch (IOException e) {
			System.out.println("IOException error while trying to write accounts info to " + ACCOUNTS_INFO_FILEPATH + "!");
			return false;
		}
		
		System.out.println(accounts.size() + " accounts information written to " + ACCOUNTS_INFO_FILEPATH + ".");
		
		return true;
	}
	
	private static boolean removeAccount(String username) {
		int accIndex = getAccountIndex(username);
		if (accIndex == -1) return false;
		Account acc = (Account)accounts.get(accIndex);
		if (acc == null) return false;
		
		accounts.remove(accIndex);
		
		// if any user is connected to this account, kick him:
		for (int j = 0; j < clients.size(); j++) {
			if (((Client)clients.get(j)).account.user.equals(username)) {
				killClient((Client)clients.get(j));
				j--;
			}
		}
		
		return true;
	}
	
	private static boolean addAccount(Account acc) {
		if (!Misc.isValidName(acc.user)) return false;
		if (!Misc.isValidPass(acc.pass)) return false;
		
		// check for duplicate entries:
		//for (int i = 0; i < accounts.size(); i++) {
		//	if (((Account)accounts.get(i)).user.equals(acc.user)) return false;
		//}
		
		accounts.add(acc);
		return true;
	}
	
	private static Account findAccount(String username) {
		for (int i = 0; i < accounts.size(); i++)
			if (((Account)accounts.get(i)).user.equals(username)) return (Account)accounts.get(i);
		return null;
	}
	
	private static Account findAccountNoCase(String username) {
		for (int i = 0; i < accounts.size(); i++)
			if (((Account)accounts.get(i)).user.equalsIgnoreCase(username)) return (Account)accounts.get(i);
		return null;
	}
	
	public static void closeServerAndExit() {
		System.out.println("Server stopped.");
		if (helpUDPsrvr != null) helpUDPsrvr.stopServer();
		if (LOG_MAIN_CHANNEL) try {
			mainChanLog.close();
		} catch(Exception e) {
			// nevermind
		}
		running = false;
		System.exit(0);
	}
	
	private static boolean changeCharset(String newCharset) throws IllegalCharsetNameException, UnsupportedCharsetException {
		CharsetDecoder temp;
		temp = Charset.forName(newCharset).newDecoder(); // this line will throw IllegalCharsetNameException and UnsupportedCharsetException exceptions
		
		asciiDecoder = temp;
	    asciiDecoder.replaceWith("?");
	    asciiDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
	    asciiDecoder.onMalformedInput(CodingErrorAction.REPLACE);
		//***asciiDecoder.reset();
	
		return true;
	}
	
	private static boolean startServer(int port) {
		try {
			changeCharset("ISO-8859-1"); // initializes asciiDecoder
			
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
		
		System.out.println("Port " + port + " opened\n" +
				 "Listening for connections ...");
		
		return true;
	}
	
	private static Client addNewClient(SocketChannel chan) {
		
    	Client client = new Client(chan);
    	clients.add(client);
			
       	// register the channel with the selector 
       	// store a new Client as the Key's attachment
       	try {
       	    chan.configureBlocking(false);
       	    chan.socket().setSendBufferSize(SEND_BUFFER_SIZE);
       	    //***chan.socket().setSoTimeout(TIMEOUT_LENGTH); -> this doesn't seem to have an effect with java.nio
       	    client.selKey = chan.register(readSelector, SelectionKey.OP_READ, client);
       	} catch (ClosedChannelException cce) {
       		killClient(client);
       		return null;
       	} catch (IOException ioe) {
       		killClient(client);
       		return null;
       	} catch (Exception e) {
       		killClient(client);
       		return null;
       	}
       	
       	return client;
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
		    	
		    	Client client = addNewClient(clientChannel);
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
				if ((client.dataOverLastTimePeriod > TASServer.maxBytesAlert) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
					System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ")");
					sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + "). User's IP has been auto-banned.");
					banList.add(client.IP, "Auto-ban for flooding.");
					killClient(client, "Disconnected due to excessive flooding");
					continue;
				}

				// check for end-of-stream
				if (nbytes == -1) { 
					if (DEBUG > 0) System.out.println ("Socket disconnected - killing client");
					channel.close();
					killClient(client); // will also close the socket channel 
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
				    	if (pos == -1) pos = line.indexOf('\n');
				    	String command = line.substring(0, pos);
				    	if (pos < line.length()-1) if (line.charAt(pos+1) == '\n') pos++;
				    	client.recvBuf.delete(0, pos+1);

				    	tryToExecCommand(command, client);

				    	if (!client.alive) break; // in case client was killed within tryToExecCommand() method
					    line = client.recvBuf.toString();
				    }
				}
		    }
		} catch(IOException ioe) {
			System.out.println("exception during select(): possibly due to force disconnect. Killing the client ...");
			try {
				if (client != null) killClient(client, "Quit: connection lost");				
			} catch (Exception e) {
			}
		} catch(Exception e) {
			System.out.println("exception in readIncomingMessages(): killing the client ... (" + e.toString() + ")");
			try {
				if (client != null) killClient(client, "Quit: connection lost");		
				e.printStackTrace(); //*** DEBUG
			} catch (Exception ex) {
			}
		}
	}
	
	private static boolean prepWriteBuffer(String data) {
		// fills the buffer from the given string
		// and prepares it for a channel write
		try {
			writeBuffer.clear();
			writeBuffer.put(data.getBytes());
			writeBuffer.flip();
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
	
	private static void channelWrite(SocketChannel channel, ByteBuffer writeBuffer) {
		long nbytes = 0;
		long toWrite = writeBuffer.remaining();
		long time = System.currentTimeMillis();

		// loop on the channel.write() call since it will not necessarily
		// write all bytes in one shot
		try {
		    while (nbytes != toWrite) {
		    	nbytes += channel.write(writeBuffer);
			
		    	try {
		    		Thread.sleep(CHANNEL_WRITE_SLEEP);
		    	} catch (InterruptedException e) {
		    	}
		    	
		    	if (System.currentTimeMillis() - time > 1000) {
		    		System.out.println("WARNING: channelWrite() timed out. Ignoring ...");
					sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem: channelWrite() timed out.");
		    		return ;
		    	}
		    }
		} catch (ClosedChannelException cce) {
		} catch (Exception e) {
		} 
		
		// get ready for another write if needed
		writeBuffer.rewind();
	}
	
	public static boolean sendLineToSocketChannel(String line, SocketChannel channel) {
		String data = line + '\n';
		
		while (data.length() > 0) {
			String temp = data.substring(0, Math.min(BYTE_BUFFER_SIZE, data.length())); // extract the string from data
			if (temp.length() == data.length()) data = ""; else data = data.substring(temp.length(), data.length());
			try {
				if (!prepWriteBuffer(temp)) return false;

				if ((channel == null) || (!channel.isConnected())) {
					System.out.println("WARNING: SocketChannel is not ready to be written to. Ignoring ...");
				    return false;
				}
				
				channelWrite(channel, writeBuffer);
			} catch (Exception e) {
				return false;
			}
		}
		
		return true;
	}
	
	private static Account verifyLogin(String user, String pass) {
		for (int i = 0; i < accounts.size(); i++) 
			if (((Account)accounts.get(i)).user.equals(user))
				if (((Account)accounts.get(i)).pass.equals(pass)) { 
					return (Account)accounts.get(i); 					
				} else break;
		
		return null;
	}
	
	private static boolean isUserAlreadyLoggedIn(Account acc) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.user.equals(acc.user)) return true;
		}
		return false;
	}
	
	/* returns index of the channel with ChanName name or -1 if channel does not exist */
	private static int getChannelIndex(String chanName) {
		for (int i = 0; i < channels.size(); i++) {
			if (((Channel)channels.get(i)).name.equals(chanName)) return i;
		}
		return -1;
	}
	
	/* returns null if channel is not found */
	private static Channel getChannel(String chanName) {
		for (int i = 0; i < channels.size(); i++) {
			if (((Channel)channels.get(i)).name.equals(chanName)) return (Channel)channels.get(i);
		}
		return null;
	}
	
	/* joins a clients to a channel with chanName name. If channel with that name
	 * does not exist, it is created. Method returns channel as a result. If client is
	 * already in the channel, it returns null as a result. This method does not check
	 * for correct key if channel is locked, caller of this method should do that
	 * before calling it. */
	private static Channel joinChannel(String chanName, Client client) {
		Channel chan = getChannel(chanName);
		if (chan == null) {
			chan = new Channel(chanName);
			channels.add(chan);
		}
		else if (chan.isClientInThisChannel(client)) return null;
		
		chan.addClient(client);
		return chan;
	}
	
	/* removes user from a channel and sends messages to all other clients in a channel.
	 * If this was the last client in the channel, then channel is removed from channels list. 
	 * "reason" may be left blank ("") if no reason is to be specified. */
	private static boolean leaveChannelAndNotifyAll(Channel chan, Client client, String reason) {
		boolean result = chan.removeClient(client);
		
		if (result) {
			if (chan.clients.size() == 0) channels.remove(chan); // since channel is empty there is no point in keeping it in a channels list. If we would keep it, the channels list would grow larger and larger in time. We don't want that!
			else for (int i = 0; i < chan.clients.size(); i++) ((Client)chan.clients.get(i)).sendLine("LEFT " + chan.name + " " + client.account.user + (reason.equals("") ? "" : " " + reason));
		} 
		
		return result;
	}
	
	/* sends information on all clients in a channel (and the topic if it is set) to the client */
	private static boolean sendChannelInfoToClient(Channel chan, Client client) {
		// it always sends info about at least one client - the one to whom this list must be sent
		String s = "CLIENTS " + chan.name;
		int c = 0;
		
		for (int j = 0; j < chan.clients.size(); j++) {
			s = s.concat(" " + ((Client)chan.clients.get(j)).account.user);
			c++;
			if (c > 10) { // 10 is the maximum number of users in a single line
				client.sendLine(s);
				s = "CLIENTS " + chan.name;
				c = 0;
			}
		}
		if (c > 0) {
			client.sendLine(s);
		}
			
		// send the topic:
		if (chan.isTopicSet()) client.sendLine("CHANNELTOPIC " + chan.name + " " + chan.getTopicAuthor() + " " + chan.getTopicChangedTime() + " " + chan.getTopic());
		
		return true;
	}
	
	/* sends a list of all open channels to client */
	private static void sendChannelListToClient(Client client) {
		if (channels.size() == 0) return ; // nothing to send
		
		Channel chan;
		for (int i = 0; i < channels.size(); i++) {
			chan = (Channel)channels.get(i);
			client.sendLine("CHANNEL " + chan.name + " " + chan.clients.size() + (chan.isTopicSet() ? " " + chan.getTopic() : ""));
		}
		client.sendLine("ENDOFCHANNELS");
	}
	
	private static void sendToAllRegisteredUsers(String s) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			((Client)clients.get(i)).sendLine(s);
		}
	}
	
	private static void sendToAllAdministrators(String s) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.ADMIN_ACCESS) continue;
			((Client)clients.get(i)).sendLine(s);
		}
		
	}
	
	/* sends text to all registered users except for the client */
	private static void sendToAllRegisteredUsersExcept(Client client, String s) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			if (((Client)clients.get(i)) == client) continue; 
			((Client)clients.get(i)).sendLine(s);
		}
	}
	
	private static void notifyClientsOfNewClientInChannel(Channel chan, Client client) {
		for (int i = 0; i < chan.clients.size(); i++) {
			if (((Client)chan.clients.get(i)).equals(client)) continue;
			((Client)chan.clients.get(i)).sendLine("JOINED " + chan.name + " " + client.account.user);
		}
	}
	
	/* notifies client of all statuses, including his own (but only if they are different from 0) */
	private static void sendInfoOnStatusesToClient(Client client) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			if (((Client)clients.get(i)).status != 0) // only send it if not 0. User assumes that every new user's status is 0, so we don't need to tell him that explicitly.
				client.sendLine("CLIENTSTATUS " + ((Client)clients.get(i)).account.user + " " + ((Client)clients.get(i)).status);
		}
			
	}
	
	/* notifies all registered clients (including this client) of the client's new status */
	private static void notifyClientsOfNewClientStatus(Client client) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			((Client)clients.get(i)).sendLine("CLIENTSTATUS " + client.account.user + " " + client.status);
		}
			
	}
	
	/* sends a list of all users connected to the server to client (this list includes
	 * the client itself, assuming he is already logged in and on the list) */
	private static void sentListOfAllUsersToClient(Client client) {
		String ip;
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			// make sure that clients behind NAT get local IPs and not external ones:
			if (((Client)clients.get(i)).IP.equals(client.IP)) ip = ((Client)clients.get(i)).localIP;
			else ip = ((Client)clients.get(i)).IP;
			client.sendLine("ADDUSER " + ((Client)clients.get(i)).account.user + " " + ((Client)clients.get(i)).country + " " + ((Client)clients.get(i)).cpu + " " + ip);
		}
	}
	
	/* notifies all registered clients of a new client who just logged in. The new client
	 * is not notified (he is already notified by other method) */
	private static void notifyClientsOfNewClientOnServer(Client client) {
		String ip;
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			if ((Client)clients.get(i) == client) continue;
			// make sure that clients behind NAT get local IPs and not external ones:
			if (((Client)clients.get(i)).IP.equals(client.IP)) ip = client.localIP;
			else ip = client.IP;
			((Client)clients.get(i)).sendLine("ADDUSER " + client.account.user + " " + client.country + " " + client.cpu + " " + ip);
		}
	}
	
	/*------------------------------------ BATTLE RELATED ------------------------------------*/
	
	private static void closeBattleAndNotifyAll(Battle battle) {
		for (int i = 0; i < battle.clients.size(); i++) {
			((Client)battle.clients.get(i)).battleID = -1;
		}
		battle.founder.battleID = -1;
		sendToAllRegisteredUsers("BATTLECLOSED " + battle.ID);
		battles.remove(battle);
	}
	
	/* Removes client from a battle and notifies everyone. Also automatically checks if 
	 * client is a founder and closes the battle in that case. All client's bots in this
	 * battle are removed as well. */	
	private static boolean leaveBattle(Client client, Battle battle) {
		if (battle.founder == client) closeBattleAndNotifyAll(battle);
		else {
			if (client.battleID != battle.ID) return false;
			if (!battle.removeClient(client)) return false;
			client.battleID = -1;
			battle.removeClientBots(client);
			sendToAllRegisteredUsers("LEFTBATTLE " + battle.ID + " " + client.account.user);
		}
		
		return true;
	}
	
	/* The client who just joined the battle is also notified (he should also be notified
	 * with JOINBATTLE command. See protocol description) */
	private static void notifyClientsOfNewClientInBattle(Battle battle, Client client) {
		for (int i = 0; i < clients.size(); i++)  {
			if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
			((Client)clients.get(i)).sendLine("JOINEDBATTLE " + battle.ID + " " + client.account.user);
		}
	}
	
	private static void sendInfoOnBattlesToClient(Client client) {
		for (int i = 0; i < battles.size(); i++) {
			Battle bat = (Battle)battles.get(i);
			// make sure that clients behind NAT get local IPs and not external ones:
			boolean local = bat.founder.IP.equals(client.IP);
			client.sendLine(bat.createBattleOpenedCommandEx(local));
			// we have to send UPDATEBATTLEINFO command too in order to tell the user how many spectators are in the battle, for example.
			client.sendLine("UPDATEBATTLEINFO " + bat.ID + " " + bat.spectatorCount() + " " + Misc.boolToStr(bat.locked) + " " + bat.mapName);
			for (int j = 0; j < bat.clients.size(); j++) {
				client.sendLine("JOINEDBATTLE " + bat.ID + " " + ((Client)bat.clients.get(j)).account.user);
			}
		}
	}
	
	/*------------------------------------ MISCELLANEOUS ------------------------------------*/
	
	private static Battle getBattle(int BattleID) {
		for (int i = 0; i < battles.size(); i++)
			if (((Battle)battles.get(i)).ID == BattleID) return (Battle)battles.get(i);
		return null;	
	}
	
	private static int getIndexOfClient(String user) {
		if (user.equals("")) return -1;
		for (int i = 0; i < clients.size(); i++) 
			if (((Client)clients.get(i)).account.user.equals(user)) return i;
		return -1;
	}
	
	private static Client getClient(String username) {
		for (int i = 0; i < clients.size(); i++)
			if (((Client)clients.get(i)).account.user.equals(username)) return (Client)clients.get(i);
		return null;	
	}
	
	private static int getAccountIndex(String username) {
		for (int i = 0; i < accounts.size(); i++)
			if (((Account)accounts.get(i)).user.equals(username)) return i;
		return -1;
	}
	
	private static Account getAccount(String username) {
		for (int i = 0; i < accounts.size(); i++)
			if (((Account)accounts.get(i)).user.equals(username)) return (Account)accounts.get(i);
		return null;
	}
	
	/* Sends "message of the day" (MOTD) to client */
	private static boolean sendMOTDToClient(Client client) {
		client.sendLine("MOTD Welcome, " + client.account.user + "!");
		client.sendLine("MOTD There are currently " + (clients.size()-1) + " clients connected"); // -1 is because we shouldn't count the client to which we are sending MOTD
		client.sendLine("MOTD to server talking in " + channels.size() + " open channels and");
		client.sendLine("MOTD participating in " + battles.size() + " battles.");
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
	
	/* returns -1 if unsuccessful */
	public static int saveStatisticsToDisk() {
		long taken;
    	try {
	    	lastStatisticsUpdate = System.currentTimeMillis();
	    	taken = Statistics.autoUpdateStatisticsFile();
			if (taken == -1) return -1;
	    	Statistics.createAggregateFile(); // to simplify parsing
	    	Statistics.generatePloticusImages();
	    	System.out.println("*** Statistics saved to disk. Time taken: " + taken + " ms.");
    	} catch (Exception e) {
    		System.out.println("*** Error while saving statistics... Stack trace:");
    		e.printStackTrace();
    		return -1;
    	}
    	return new Long(taken).intValue();
	}
	
	/* add "synchronized" if more than 1 thread is going to call it at the same time! */
	public static boolean tryToExecCommand(String command, Client client) {
		if (command.trim().equals("")) return false;
		String[] commands = command.split(" ");
		commands[0] = commands[0].toUpperCase();
		
		if (DEBUG > 1) 
			if (client.account.accessLevel() != Account.NIL_ACCESS) System.out.println("[<-" + client.account.user + "]" + " \"" + command + "\"");
			else System.out.println("[<-" + client.IP + "]" + " \"" + command + "\"");
		
		
		if (commands[0].equals("PING")) {
			//***if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
		
			if (commands.length > 1) client.sendLine("PONG " + Misc.makeSentence(commands, 1));
			else client.sendLine("PONG");
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
			
			if ((!Misc.isValidName(commands[1])) || (!Misc.isValidPass(commands[2]))) {
				client.sendLine("REGISTRATIONDENIED Invalid username/password");
				return false;
			}
			
			if (commands[1].length() > 20) {
				client.sendLine("REGISTRATIONDENIED Too long username");
				return false;
			}
			
			if (commands[1].length() < 2) {
				client.sendLine("REGISTRATIONDENIED Too short username");
				return false;
			}
			 
			Account acc = findAccountNoCase(commands[1]);
			if (acc != null) {
				client.sendLine("REGISTRATIONDENIED Account already exists");
				return false;
			}
			
			acc = new Account(commands[1], commands[2], Account.NORMAL_ACCESS, System.currentTimeMillis(), client.IP, System.currentTimeMillis());
			accounts.add(acc);
			writeAccountsInfo(); // let's save new accounts info to disk
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
			
			Client target = getClient(commands[1]);
			String reason = "";
			if (commands.length > 2) reason = " (reason: " + Misc.makeSentence(commands, 2) + ")"; 
			if (target == null) return false;
			for (int i = 0; i < channels.size(); i++) {
				if (((Channel)channels.get(i)).isClientInThisChannel(target)) {
					((Channel)channels.get(i)).broadcast("<" + client.account.user + "> has kicked <" + target.account.user + "> from server" + reason);
				}
			}
			target.sendLine("SERVERMSG You've been kicked from server by <" + client.account.user + ">" + reason);
			killClient(target, "Quit: kicked from server");
		}
		else if (commands[0].equals("REMOVEACCOUNT")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 2) return false;
			
			if (!removeAccount(commands[1])) return false;
			writeAccountsInfo(); // let's save new accounts info to disk
			client.sendLine("SERVERMSG You have successfully removed <" + commands[1] + "> account!");
		}
		else if (commands[0].equals("STOPSERVER")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			
			if (!LAN_MODE) writeAccountsInfo();
			
			closeServerAndExit();
		}
		else if (commands[0].equals("WRITEACCOUNTSINFO")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			
			writeAccountsInfo();
			client.sendLine("SERVERMSG Accounts info successfully saved to disk");
		}
		else if (commands[0].equals("CHANGEACCOUNTPASS")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 3) return false;
			
			Account acc = getAccount(commands[1]);
			if (acc == null) return false; 
			if (!Misc.isValidPass(commands[2])) return false;
			
			acc.pass = commands[2];
			
			writeAccountsInfo(); // save changes
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
			
			Account acc = getAccount(commands[1]);
			if (acc == null) return false; 
			
			acc.access = value;
			
			writeAccountsInfo(); // save changes
			 // just in case if rank changed:
			client.status = Misc.setRankToStatus(client.status, client.account.getRank());
			notifyClientsOfNewClientStatus(client);
		}
		else if (commands[0].equals("GETACCOUNTACCESS")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 2) return false;

			Account acc = getAccount(commands[1]);
			if (acc == null) return false;
			
			client.sendLine("SERVERMSG " + commands[1] + "'s access code is " + acc.access);
		}
		else if (commands[0].equals("REDIRECT")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 2) return false;

			redirectToIP = commands[1];
			redirect = true;
			sendToAllRegisteredUsers("BROADCAST " + "Server has entered redirection mode");
		}
		else if (commands[0].equals("REDIRECTOFF")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;

			redirect = false;
			sendToAllRegisteredUsers("BROADCAST " + "Server has left redirection mode");
		}
		else if (commands[0].equals("BROADCAST")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length < 2) return false;

			sendToAllRegisteredUsers("BROADCAST " + Misc.makeSentence(commands, 1));
		}
		else if (commands[0].equals("BROADCASTEX")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length < 2) return false;

			sendToAllRegisteredUsers("SERVERMSGBOX " + Misc.makeSentence(commands, 1));
		}
		else if (commands[0].equals("ADMINBROADCAST")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length < 2) return false;

			sendToAllAdministrators("SERVERMSG [broadcast to all admins]: " + Misc.makeSentence(commands, 1));
		}
		else if (commands[0].equals("GETACCOUNTCOUNT")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 1) return false;

			client.sendLine("SERVERMSG " + accounts.size());
		}
		else if (commands[0].equals("FINDACCOUNT")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 2) return false;
			
			int index = getAccountIndex(commands[1]);
			
			client.sendLine("SERVERMSG " + commands[1] + "'s account index: " + index);
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
			
			for (int i = 0; i < clients.size(); i++)
			{
				String[] sp2 = ((Client)clients.get(i)).IP.split("\\.");

				if (!sp1[0].equals("*")) if (!sp1[0].equals(sp2[0])) continue;
				if (!sp1[1].equals("*")) if (!sp1[1].equals(sp2[1])) continue;
				if (!sp1[2].equals("*")) if (!sp1[2].equals(sp2[2])) continue;
				if (!sp1[3].equals("*")) if (!sp1[3].equals(sp2[3])) continue;
				
				found = true;
				client.sendLine("SERVERMSG " + IP + " is bound to: "+ ((Client)clients.get(i)).account.user);
			}
				
			// now let's check if this IP matches any recently used IP:
			for (int i = 0; i < accounts.size(); i++) {
				String[] sp2 = ((Account)accounts.get(i)).lastIP.split("\\.");

				if (!sp1[0].equals("*")) if (!sp1[0].equals(sp2[0])) continue;
				if (!sp1[1].equals("*")) if (!sp1[1].equals(sp2[1])) continue;
				if (!sp1[2].equals("*")) if (!sp1[2].equals(sp2[2])) continue;
				if (!sp1[3].equals("*")) if (!sp1[3].equals(sp2[3])) continue;				

				if (getClient(((Account)accounts.get(i)).user) == null) { // user is offline
					found = true;
					client.sendLine("SERVERMSG " + IP + " was recently bound to: "+ ((Account)accounts.get(i)).user + " (offline)");
				}
			}			

			if (!found) client.sendLine("SERVERMSG No client is/was recently using IP: " + IP); //*** perhaps add an explanation like "(note that server only keeps track of last used IP addresses)" ?
		}
		else if (commands[0].equals("GETLASTIP")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 2) return false;
			
			Account acc = getAccount(commands[1]);
			if (acc == null) {
				client.sendLine("SERVERMSG User " + commands[1] + " not found!");
				return false;
			}
			
			boolean online = isUserAlreadyLoggedIn(acc); 
			client.sendLine("SERVERMSG " + commands[1] + "'s last IP was " + acc.lastIP + " (" + (online ? "online)" : "offline)"));
		}		
		else if (commands[0].equals("GETACCOUNTINFO")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 2) return false;
			
			int index;
			try {
				index = Integer.parseInt(commands[1]); 
			} catch (NumberFormatException e) {
				return false; 
			}
			if (index >= accounts.size()) return false;

			client.sendLine("SERVERMSG " + "Account #" + index + " info: " + ((Account)accounts.get(index)).user + " " + ((Account)accounts.get(index)).pass + " " + ((Account)accounts.get(index)).access);
		}
		else if (commands[0].equals("FORGEMSG")) {
			/* this is a command used only for debugging purposes. It sends the string
			 * to client specified as first argument. */
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length < 3) return false;
			
			Client targetClient = getClient(commands[1]);
			if (targetClient == null) return false;
			
			targetClient.sendLine(Misc.makeSentence(commands, 2));
		}
		else if (commands[0].equals("GETIP")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 2) return false;
		
			Client targetClient = getClient(commands[1]);
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
				Account acc = findAccount(commands[1]);
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
			
			Battle bat = getBattle(battleID);
			if (bat == null) {
				client.sendLine("SERVERMSG Error: unknown BATTLE_ID!");
				return false;
			}
			
			closeBattleAndNotifyAll(bat);
			
		}
		else if (commands[0].equals("BANLISTADD")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length < 2) return false;
			
			String reason = "";
			if (commands.length > 2) reason = Misc.makeSentence(commands, 2); 
			banList.add(commands[1], reason);
			banList.saveToFile(BAN_LIST_FILENAME);
			client.sendLine("SERVERMSG IP " + commands[1] + " has been added to ban list. Use KICKUSER to kick user from server.");
		}
		else if (commands[0].equals("BANLISTREMOVE")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 2) return false;

			if (banList.remove(commands[1])) {
				banList.saveToFile(BAN_LIST_FILENAME);
				client.sendLine("SERVERMSG IP " + commands[1] + " has been removed from ban list.");
			} else {
				client.sendLine("SERVERMSG IP " + commands[1] + " couldn't be found in ban list!");				
			}
		}
		else if (commands[0].equals("BANLIST")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 1) return false;

			if (banList.size() == 0) client.sendLine("SERVERMSG Ban list is empty!");
			else {
				client.sendLine("SERVERMSG Ban list (" + banList.size() + " entries):");
				for (int i = 0; i < banList.size(); i++)
					client.sendLine("SERVERMSG * " + banList.getIP(i) + " (Reason: " + banList.getReason(i) + ")");
				client.sendLine("SERVERMSG End of ban list.");
			}
		}
		else if (commands[0].equals("MUTE")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 4) return false;
			
			Channel chan = getChannel(commands[1]);
			if (chan == null) { 
				client.sendLine("SERVERMSG MUTE failed: Channel #" + commands[1] + " does not exist!");
				return false;
			}
			
			String username = commands[2];
			if (chan.muteList.isMuted(username)) {
				client.sendLine("SERVERMSG MUTE failed: User <" + username + "> is already muted. Unmute first!");
				return false;
			}
			
			int minutes;
			try {
				minutes = Integer.parseInt(commands[3]); 
			} catch (NumberFormatException e) {
				client.sendLine("SERVERMSG MUTE failed: Invalid argument - should be an integer");
				return false; 
			}
			
			chan.muteList.mute(username, minutes*60);
			
			client.sendLine("SERVERMSG You have muted <" + username + "> on channel #" + chan.name + ".");
			chan.broadcast("<" + client.account.user + "> has muted <" + username + ">");
		}
		else if (commands[0].equals("UNMUTE")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 3) return false;

			Channel chan = getChannel(commands[1]);
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
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length != 2) {
				client.sendLine("SERVERMSG MUTELIST failed: Invalid arguments!");
				return false;
			}
			
			Channel chan = getChannel(commands[1]);
			if (chan == null) { 
				client.sendLine("SERVERMSG MUTELIST failed: Channel #" + commands[1] + " does not exist!");
				return false;
			}

			if (chan.muteList.size() == 0) client.sendLine("SERVERMSG Mute list for channel #" + chan.name + " is empty!");
			else {
				client.sendLine("SERVERMSG Mute list for #" + chan.name + " (" + chan.muteList.size() + " entries):");
				int size = chan.muteList.size(); // we shouldn't call muteList.size() in for loop since it will purge expired records each time and so we could have ArrayOutOfBounds exception
				for (int i = 0; i < size; i++) 
					if (chan.muteList.getRemainingSeconds(i) == 0) client.sendLine("SERVERMSG * " + (String)chan.muteList.getUsername(i) + ", indefinite time remaining");
					else client.sendLine("SERVERMSG * " + (String)chan.muteList.getUsername(i) + ", " + chan.muteList.getRemainingSeconds(i) + " seconds remaining");
					
				client.sendLine("SERVERMSG End of mute list for #" + chan.name + ".");
			}
		}
		else if (commands[0].equals("CHANNELMESSAGE")) {
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			if (commands.length < 3) return false;

			Channel chan = getChannel(commands[1]);
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

			Client targetClient = getClient(commands[1]);
			if (targetClient == null) {
				client.sendLine("SERVERMSG <" + commands[1] + "> not found!");
				return false;
			}
			client.sendLine("SERVERMSG <" + commands[1] + "> is using \"" + targetClient.lobbyVersion + "\"");
		}
		else if (commands[0].equals("UPDATESTATISTICS")) {
			if (client.account.accessLevel() < Account.ADMIN_ACCESS) return false;
			if (commands.length != 1) return false;

			int taken = saveStatisticsToDisk(); 
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

			Account acc = getAccount(commands[1]);
			if (acc == null) {
				client.sendLine("SERVERMSG GETLASTLOGINTIME failed: <" + commands[1] + "> not found!");
				return false;
			}
			
			if (getClient(acc.user) == null) {
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
			
			Channel chan = getChannel(commands[1]);
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
				if (!Misc.isValidName(commands[2])) {
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
			
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				client.sendLine("SERVERMSG Error: Channel does not exist: " + commands[1]);
				return false;
			}
			
			Client target = getClient(commands[2]);
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
			leaveChannelAndNotifyAll(chan, target, "kicked from channel");
		}
		else if (commands[0].equals("CHANNELS")) {
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			sendChannelListToClient(client);
		}
		else if (commands[0].equals("REQUESTUPDATEFILE")) {
			//***if (client.account.accessLevel() > Account.NIL_ACCESS) return false;
			if (commands.length != 2) return false;
			
			String version = commands[1];
			
			if (version.equals("0.12")) {
				client.sendLine("SERVERMSGBOX Your lobby client is outdated! Please update from: http://taspring.clan-sy.com/download.php");
				killClient(client);
				return false;
			} else if (version.equals("0.121")) {
				client.sendLine("SERVERMSGBOX Your lobby client is outdated! Please update from: http://taspring.clan-sy.com/download.php");
				killClient(client);
				return false;
/*
			} else if (version.equals("0.18")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/taspring_0.67b2_patch.exe	This is a 0.67b1->0.67b2 patch. It will update Spring and lobby client. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.181")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/taspring_0.67b2_patch.exe	This is a 0.67b1->0.67b2 patch. It will update Spring and lobby client. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.182")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/taspring_0.67b2_patch.exe	This is a 0.67b1->0.67b2 patch. It will update Spring and lobby client. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.19")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/taspring_0.67b3_patch.exe	This is a 0.67b2->0.67b3 patch which fixes bug with AIs not being loaded correctly. It will update Spring and lobby client. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.191")) {
				client.sendLine("OFFERFILE 7 *	http://spring.clan-sy.com/dl/LobbyUpdate_0191_0195.exe	This is a TASClient 0.195 patch which fixes some serious issue with last update. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.192")) {
				client.sendLine("OFFERFILE 7 *	http://spring.clan-sy.com/dl/LobbyUpdate_0192_0195.exe	This is a TASClient 0.195 patch which fixes some serious issue with last update. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.193")) {
				client.sendLine("OFFERFILE 7 *	http://spring.clan-sy.com/dl/LobbyUpdate_0193_0195.exe	This is a TASClient 0.195 patch which fixes some serious issue with last update. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.194")) {
				client.sendLine("OFFERFILE 7 *	http://spring.clan-sy.com/dl/LobbyUpdate_0194_0195.exe	This is a TASClient 0.195 patch which fixes some serious issue with last update. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.20")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/taspring_0.70b2_patch.exe	This is a 0.70b1->0.70b2 patch. It will update Spring and lobby client. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else if (version.equals("0.23")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/LobbyUpdate_023_024.exe	This is a TASClient 0.24 patch which fixes watching replays from the lobby. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
*/				
			} else if (version.equals("0.25")) {
				client.sendLine("OFFERFILE 7 *	http://taspring.clan-sy.com/dl/LobbyUpdate_025_026.exe	This is a TASClient 0.26 patch which fixes serious bug with hosting replays in the lobby. Alternatively you can download it from the Spring web site. All files are checked for viruses and are considered to be safe.");
			} else { // unknown client version
//				client.sendLine("SERVERMSGBOX No update available for your version of lobby. See official spring web site to get the latest lobby client!");
				client.sendLine("SERVERMSGBOX You are using an outdated Spring and lobby program, check the download section for new updates at the official Spring web site: http://taspring.clan-sy.com");
				killClient(client);
			}
		
			//*** not implemented yet;

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
			
			int cpu;
			try {
				cpu = Integer.parseInt(commands[3]); 
			} catch (NumberFormatException e) {
				client.sendLine("DENIED <cpu> field should be an integer");
				return false; 
			}
			
			if (!LAN_MODE) { // "normal", non-LAN mode
				Account acc = verifyLogin(commands[1], commands[2]);
				if (acc == null) {
					client.sendLine("DENIED Bad username/password");
					return false;
				}
				if (isUserAlreadyLoggedIn(acc)) {
					client.sendLine("DENIED Already logged in");
					return false;
				}
				if (banList.banned(client.IP)) {
					String reason = banList.getReason(banList.getIndex(client.IP));
					if (reason.equals("")) reason = "[not given]";
					client.sendLine("DENIED You are banned from this server! (Reason: " + reason + "). Please contact server administrator.");
					return false;
				}
				if ((!acc.getAgreement())  && (!client.account.getAgreement()) && (!agreement.equals(""))) {
					sendAgreementToClient(client);
					return false;
				}
				// everything is OK so far!
				if (!acc.getAgreement()) {
					// user has obviously accepted the agreement... Let's update it
					acc.setAgreement(true);
					writeAccountsInfo();
				}
				client.account = acc;
				client.status = Misc.setRankToStatus(client.status, client.account.getRank());
			} else { // LAN_MODE == true
				Account acc = findAccount(commands[1]);
				if (acc != null) {
					client.sendLine("DENIED Player with same name already logged in");
					return false;
				}
				if ((commands[1].equals(LanAdminUsername)) && (commands[2].equals(LanAdminPassword))) acc = new Account(commands[1], commands[2], Account.ADMIN_ACCESS, 0, "?", 0); 
				else acc = new Account(commands[1], commands[2], Account.NORMAL_ACCESS, 0, "?", 0);
				accounts.add(acc);
				client.account = acc;
			}
			
			// set client's status:
			client.status = Misc.setRankToStatus(client.status, client.account.getRank());
			if ((client.account.accessLevel() >= Account.PRIVILEGED_ACCESS) && (!LAN_MODE))
				client.status = Misc.setAccessToStatus(client.status, 1);
			else client.status = Misc.setAccessToStatus(client.status, 0);
			
			client.cpu = cpu;
			client.account.lastLogin = System.currentTimeMillis();
			client.account.lastIP = client.IP;
			if (commands[4].equals("*")) client.localIP = new String(client.IP);
			else client.localIP = commands[4];
			client.lobbyVersion = Misc.makeSentence(commands, 5);
			
			// do the notifying and all: 
			client.sendLine("ACCEPTED " + client.account.user);
			sendMOTDToClient(client);
			sentListOfAllUsersToClient(client);
			sendInfoOnBattlesToClient(client);
			sendInfoOnStatusesToClient(client);
			notifyClientsOfNewClientOnServer(client);

			// we have to notify everyone about client's status to let them know about his rank:
			notifyClientsOfNewClientStatus(client);
			
			if (DEBUG > 0) System.out.println("User just logged in: " + client.account.user);
		}
		else if (commands[0].equals("CONFIRMAGREEMENT")) {
			// update client's temp account (he is not logged in yet since he needs to confirm the agreement before server will allow him to log in):
			client.account.setAgreement(true);
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
			
			if (!Misc.isValidName(commands[1])) {
				client.sendLine("SERVERMSG RENAMEACCOUNT failed: Invalid username");
				return false;
			}
			
			if (commands[1].length() > 20) {
				client.sendLine("SERVERMSG RENAMEACCOUNT failed: Too long username");
				return false;
			}
			
			if (commands[1].length() < 2) {
				client.sendLine("SERVERMSG RENAMEACCOUNT failed: Too short username");
				return false;
			}			
			
			Account acc = findAccountNoCase(commands[1]);
			if (acc != null) {
				client.sendLine("SERVERMSG RENAMEACCOUNT failed: Account with same username already exists!");
				return false;
			}			

			// make sure all mutes are accordingly adjusted to new username:
			for (int i = 0; i < channels.size(); i++) {
				((Channel)channels.get(i)).muteList.rename(client.account.user, commands[1]);
			}
			
			acc = new Account(commands[1], client.account.pass, client.account.access, System.currentTimeMillis(), client.IP, client.account.registrationDate);
			accounts.add(acc);
			client.sendLine("SERVERMSG Your account has been renamed to <" + commands[1] + ">. Reconnect with new account (you will now be automatically disconnected)!");
			killClient(client, "Quit: renaming account");
			removeAccount(client.account.user);
			writeAccountsInfo(); // let's save new accounts info to disk
			sendToAllAdministrators("SERVERMSG [broadcast to all admins]: User <" + client.account.user + "> has just renamed his account to <" + commands[1] + ">");
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
			
			if (!(Misc.isValidPass(commands[2]))) {
				client.sendLine("SERVERMSG CHANGEPASSWORD failed: Illegal password");
				return false;
			}
			
			client.account.pass = commands[2];

			writeAccountsInfo(); // let's save new accounts info to disk
			client.sendLine("SERVERMSG Your password has been successfully updated!");
		}
		else if (commands[0].equals("JOIN")) {
			if (commands.length < 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;

			if (!Misc.isValidName(commands[1])) {
				client.sendLine("JOINFAILED " + commands[1] + " Bad channel name!");
				return false;
			}

			// check if key is correct (if channel is locked):
			Channel chan = getChannel(commands[1]);
			if ((chan != null) && (chan.isLocked()) && (client.account.accessLevel() < Account.ADMIN_ACCESS /* we will allow admins to join locked channels */)) {
				if (!Misc.makeSentence(commands, 2).equals(chan.getKey())) {
					client.sendLine("JOINFAILED " + commands[1] + " Wrong key (this channel is locked)!");
					return false;
				}
			}
			
			chan = joinChannel(commands[1], client);
			if (chan == null) {
				client.sendLine("JOINFAILED " + commands[1] + " Already in the channel!");
				return false;
			}
			client.sendLine("JOIN " + commands[1]);
			sendChannelInfoToClient(chan, client);
			notifyClientsOfNewClientInChannel(chan, client);
		}
		else if (commands[0].equals("LEAVE")) {
			if (commands.length < 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			Channel chan = getChannel(commands[1]);
			if (chan == null) return false;
			
			leaveChannelAndNotifyAll(chan, client, "");
		}
		else if (commands[0].equals("CHANNELTOPIC")) {
			if (commands.length < 3) return false;
			if (client.account.accessLevel() < Account.PRIVILEGED_ACCESS) return false;
			
			Channel chan = getChannel(commands[1]);
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
			
			Channel chan = getChannel(commands[1]);
			if (chan == null) return false;
			
			if (chan.muteList.isMuted(client.account.user)) {
				client.sendLine("SERVERMSG Message dropped. You are not allowed to talk in #" + chan.name + "! Please contact one of the moderators.");
				return false;
			}
			
			String s = Misc.makeSentence(commands, 2);
			// check for flooding:			
			if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
				System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
				client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
				sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
				return false;
			}
			chan.sendLineToClients("SAID " + chan.name + " " + client.account.user + " " + s);
		}
		else if (commands[0].equals("SAYEX")) {
			if (commands.length < 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			Channel chan = getChannel(commands[1]);
			if (chan == null) return false;

			if (chan.muteList.isMuted(client.account.user)) {
				client.sendLine("SERVERMSG Message dropped. You are not allowed to talk in #" + chan.name + "! Please contact one of the moderators.");
				return false;
			}
			
			String s = Misc.makeSentence(commands, 2);
			// check for flooding:			
			if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
				System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
				client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
				sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
				return false;
			}
			
			chan.sendLineToClients("SAIDEX " + chan.name + " " + client.account.user + " " + s);
		}
		else if (commands[0].equals("SAYPRIVATE")) {
			if (commands.length < 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			int i = getIndexOfClient(commands[1]);
			if (i == -1) return false;
			
			String s = Misc.makeSentence(commands, 2);
			// check for flooding:
			if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
				System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
				client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
				sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
				return false;
			}

			((Client)clients.get(i)).sendLine("SAIDPRIVATE " + client.account.user + " " + s);
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
			
			Battle bat = getBattle(battleID);
			
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
		 	client.sendLine("JOINBATTLE " + bat.ID + " " + bat.metal + " " + bat.energy + " " + bat.units + " " + bat.startPos + " " + bat.gameEndCondition + " " + Misc.boolToStr(bat.limitDGun)+ " " + Misc.boolToStr(bat.diminishingMMs) + " " + Misc.boolToStr(bat.ghostedBuildings) + " " + bat.hashCode); // notify client that he has successfully joined the battle
			notifyClientsOfNewClientInBattle(bat, client);
			bat.notifyOfBattleStatuses(client);
			bat.sendBotListToClient(client);
			// tell host about this client's UDP source port (if battle is hosted using "hole punching" NAT traversal technique):
			if (bat.natType == 1) {
				bat.founder.sendLine("CLIENTPORT " + client.account.user + " " + client.UDPSourcePort);
			}
			
			client.sendLine("REQUESTBATTLESTATUS");
			bat.sendDisabledUnitsListToClient(client);
			bat.sendStartRectsListToClient(client);
			
			if (bat.type == 1) bat.sendScriptToClient(client);

		}
		else if (commands[0].equals("LEAVEBATTLE")) {
			if (commands.length != 1) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;

			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			leaveBattle(client, bat); // automatically checks if client is a founder and closes battle
		}
		else if (commands[0].equals("OPENBATTLE")) {
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			if (client.battleID != -1) {
				client.sendLine("OPENBATTLEFAILED " + "You are already hosting a battle!");
				return false;
			}
			Battle bat = Battle.createBattleFromString(command, client);
			if (bat == null) {
				client.sendLine("OPENBATTLEFAILED " + "Invalid command format or bad arguments");
				return false;
			}
			battles.add(bat);
			client.battleStatus = 0; // reset client's battle status
			client.battleID = bat.ID;

			boolean local;
			for (int i = 0; i < clients.size(); i++) {
				if (((Client)clients.get(i)).account.accessLevel() < Account.NORMAL_ACCESS) continue;
				// make sure that clients behind NAT get local IPs and not external ones:
				local = client.IP.equals(((Client)clients.get(i)).IP);
				((Client)clients.get(i)).sendLine(bat.createBattleOpenedCommandEx(local));
			}
			
			client.sendLine("OPENBATTLE " + bat.ID); // notify client that he successfully opened a new battle
			client.sendLine("REQUESTBATTLESTATUS");
		}		
		else if (commands[0].equals("MYBATTLESTATUS")) {
			if (commands.length != 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;

			Battle bat = getBattle(client.battleID);
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
			if ((bat.clients.size()+1-bat.spectatorCount() > bat.maxPlayers) || (bat.type == 1)) {
				client.battleStatus = Misc.setModeOfBattleStatus(client.battleStatus, 0);
			}
			// if player has chosen team number which is already used by some other player/bot,
			// force his ally number and team color to be the same as of that player/bot:
			if (bat.founder != client)
				if ((Misc.getTeamNoFromBattleStatus(bat.founder.battleStatus) == Misc.getTeamNoFromBattleStatus(client.battleStatus)) && (Misc.getModeFromBattleStatus(bat.founder.battleStatus) != 0)) {
					client.battleStatus = Misc.setAllyNoOfBattleStatus(client.battleStatus, Misc.getAllyNoFromBattleStatus(bat.founder.battleStatus));
					client.teamColor = bat.founder.teamColor;
				} 
			for (int i = 0; i < bat.clients.size(); i++)
				if (((Client)bat.clients.get(i)) != client)
					if ((Misc.getTeamNoFromBattleStatus(((Client)bat.clients.get(i)).battleStatus) == Misc.getTeamNoFromBattleStatus(client.battleStatus)) && (Misc.getModeFromBattleStatus(((Client)bat.clients.get(i)).battleStatus) != 0)) {
						client.battleStatus = Misc.setAllyNoOfBattleStatus(client.battleStatus, Misc.getAllyNoFromBattleStatus(((Client)bat.clients.get(i)).battleStatus));
						client.teamColor = ((Client)bat.clients.get(i)).teamColor;
						break;
					}
			for (int i = 0; i < bat.bots.size(); i++)
				if (Misc.getTeamNoFromBattleStatus(((Bot)bat.bots.get(i)).battleStatus) == Misc.getTeamNoFromBattleStatus(client.battleStatus)) {
					client.battleStatus = Misc.setAllyNoOfBattleStatus(client.battleStatus, Misc.getAllyNoFromBattleStatus(((Bot)bat.bots.get(i)).battleStatus));
					client.teamColor = ((Bot)bat.bots.get(i)).teamColor;
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

			// we must preserve rank bits and access bit (client is not allowed to change them himself):
			int tmp = Misc.getRankFromStatus(client.status);
			int tmp2 = Misc.getInGameFromStatus(client.status);
			int tmp3 = Misc.getAccessFromStatus(client.status);
			client.status = Misc.setRankToStatus(newStatus, tmp);
			client.status = Misc.setAccessToStatus(client.status, tmp3);
			if (Misc.getInGameFromStatus(client.status) != tmp2) {
				// user changed his in-game status.
				if (tmp2 == 0) { // client just entered game
					Battle bat = getBattle(client.battleID);
					if ((bat != null) && (bat.clients.size() > 0))
							client.inGameTime = System.currentTimeMillis();
					else client.inGameTime = 0; // we won't update clients who play by themselves (or with bots), since some try to exploit the system by leaving computer alone in-battle for hours to increase their ranks
					// check if client is a battle host using "hole punching" technique:
					if ((bat != null) && (bat.founder == client) && (bat.natType == 1)) {
						// tell clients to replace battle port with founder's public UDP source port:
						bat.sendToAllExceptFounder("HOSTPORT " + client.UDPSourcePort);
					}
				} else { // back from game
					if (client.inGameTime != 0) { // we won't update clients who play by themselves (or with bots), since some try to exploit the system by leaving computer alone in-battle for hours to increase their ranks 
						int diff = new Long((System.currentTimeMillis() - client.inGameTime) / 60000).intValue(); // in minutes
						if (client.account.addMinsToInGameTime(diff)) {
							client.status = Misc.setRankToStatus(client.status, client.account.getRank());
						}
					}
				}
			}
			notifyClientsOfNewClientStatus(client);
		}
		else if (commands[0].equals("SAYBATTLE")) {
			if (commands.length < 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			
			String s = Misc.makeSentence(commands, 1); 
			// check for flooding:			
			if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
				System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
				client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
				sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
				return false;
			}				
			
			bat.sendToAllClients("SAIDBATTLE " + client.account.user + " " + s);
		}
		else if (commands[0].equals("SAYBATTLEEX")) {
			if (commands.length < 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;

			String s = Misc.makeSentence(commands, 1);
			// check for flooding:			
			if ((s.length() > maxChatMessageLength) && (client.account.accessLevel() < Account.ADMIN_ACCESS)) {
				System.out.println("WARNING: Flooding detected from " + client.IP + " (" + client.account.user + ") [exceeded max. chat message size]");
				client.sendLine("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (" + maxChatMessageLength + " bytes). Your message has been ignored.");
				sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Flooding has been detected from " + client.IP + " (" + client.account.user + ") - exceeded maximum chat message size. Ignoring ...");
				return false;
			}				
			
			bat.sendToAllClients("SAIDBATTLEEX " + client.account.user + " " + s);
		}
		else if (commands[0].equals("UPDATEBATTLEINFO")) {
			if (commands.length < 4) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder may change battle parameters!
			
			int spectatorCount = 0;
			boolean locked;
			try {
				spectatorCount = Integer.parseInt(commands[1]);
				locked = Misc.strToBool(commands[2]);
			} catch (NumberFormatException e) {
				return false; 
			}
			
			bat.mapName = Misc.makeSentence(commands, 3);
			bat.locked = locked;
			sendToAllRegisteredUsers("UPDATEBATTLEINFO " + bat.ID + " " + spectatorCount + " " + Misc.boolToStr(bat.locked) + " " + bat.mapName);
		}
		else if (commands[0].equals("UPDATEBATTLEDETAILS")) {
			if (commands.length != 9) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder may change battle parameters!

			int metal;
			int energy;
			int units;
			int startPos;
			int gameEndCondition;
			boolean limitDGun;
			boolean diminishingMMs;
			boolean ghostedBuildings;
			try {
				metal = Integer.parseInt(commands[1]);
				energy = Integer.parseInt(commands[2]);
				units = Integer.parseInt(commands[3]); 
				startPos = Integer.parseInt(commands[4]);
				gameEndCondition = Integer.parseInt(commands[5]);
				limitDGun = Misc.strToBool(commands[6]);
				diminishingMMs = Misc.strToBool(commands[7]);
				ghostedBuildings = Misc.strToBool(commands[8]);
			} catch (NumberFormatException e) {
				return false; 
			}
			if ((startPos < 0) || (startPos > 2)) return false;
			if ((gameEndCondition < 0) || (gameEndCondition > 1)) return false;
			
			
			bat.metal = metal;
			bat.energy = energy;
			bat.units = units;
			bat.startPos = startPos;
			bat.gameEndCondition = gameEndCondition;
			bat.limitDGun = limitDGun;
			bat.diminishingMMs = diminishingMMs;
			bat.ghostedBuildings = ghostedBuildings;
			bat.sendToAllClients("UPDATEBATTLEDETAILS " + bat.metal + " " + bat.energy + " " + bat.units + " " + bat.startPos + " " + bat.gameEndCondition + " " + Misc.boolToStr(bat.limitDGun) + " " + Misc.boolToStr(bat.diminishingMMs) + " " + Misc.boolToStr(bat.ghostedBuildings));
		}
		else if (commands[0].equals("HANDICAP")) {
			if (commands.length != 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can change handicap value of another user
			
			int value;
			try {
				value = Integer.parseInt(commands[2]);
			} catch (NumberFormatException e) {
				return false; 
			}
			if ((value < 0) || (value > 100)) return false;

			int tmp = getIndexOfClient(commands[1]);
			if (tmp == -1) return false;
			Client targetClient = (Client)clients.get(tmp);
			
			if (!bat.isClientInBattle(targetClient)) return false;
			
			targetClient.battleStatus = Misc.setHandicapOfBattleStatus(targetClient.battleStatus, value); 
			bat.notifyClientsOfBattleStatus(targetClient);
		}
		else if (commands[0].equals("KICKFROMBATTLE")) {
			if (commands.length != 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can kick other clients
			
			int tmp = getIndexOfClient(commands[1]);
			if (tmp == -1) return false;
			Client targetClient = (Client)clients.get(tmp);
			
			if (!bat.isClientInBattle(targetClient)) return false;
			bat.sendToAllClients("SAIDBATTLEEX " + client.account.user + " kicked " + targetClient.account.user + " from battle");
			targetClient.sendLine("FORCEQUITBATTLE");
		}
		else if (commands[0].equals("FORCETEAMNO")) {
			if (commands.length != 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can force team/ally numbers
			
			int value;
			try {
				value = Integer.parseInt(commands[2]);
			} catch (NumberFormatException e) {
				return false; 
			}
			if ((value < 0) || (value > 9)) return false;
			
			int tmp = getIndexOfClient(commands[1]); 
			if (tmp == -1) return false;
			Client targetClient = (Client)clients.get(tmp);
			
			if (!bat.isClientInBattle(targetClient)) return false;
			
			targetClient.battleStatus = Misc.setTeamNoOfBattleStatus(targetClient.battleStatus, value);
			bat.notifyClientsOfBattleStatus(targetClient); 
		}
		else if (commands[0].equals("FORCEALLYNO")) {
			if (commands.length != 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can force team/ally numbers
			
			int value;
			try {
				value = Integer.parseInt(commands[2]);
			} catch (NumberFormatException e) {
				return false; 
			}
			if ((value < 0) || (value > 9)) return false;
			
			int tmp = getIndexOfClient(commands[1]); 
			if (tmp == -1) return false;
			Client targetClient = (Client)clients.get(tmp);
			
			if (!bat.isClientInBattle(targetClient)) return false;
			
			targetClient.battleStatus = Misc.setAllyNoOfBattleStatus(targetClient.battleStatus, value);
			bat.notifyClientsOfBattleStatus(targetClient); 
		}		
		else if (commands[0].equals("FORCETEAMCOLOR")) {
			if (commands.length != 3) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can force team color change
			
			int value;
			try {
				value = Integer.parseInt(commands[2]);
			} catch (NumberFormatException e) {
				return false; 
			}
			
			int tmp = getIndexOfClient(commands[1]); 
			if (tmp == -1) return false;
			Client targetClient = (Client)clients.get(tmp);
			
			if (!bat.isClientInBattle(targetClient)) return false;
			
			targetClient.teamColor = value;
			bat.notifyClientsOfBattleStatus(targetClient); 
		}		
		else if (commands[0].equals("ADDBOT")) {
			if (commands.length < 5) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
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
			
			if (!Misc.isValidName(commands[1])) {
				client.sendLine("SERVERMSGBOX Bad bot name. Try another!");
				return false;
			}
			
			if (bat.getBot(commands[1]) != -1) {
				client.sendLine("SERVERMSGBOX Bot name already assigned. Choose another!");
				return false;
			}

			Bot bot = new Bot(commands[1], client.account.user, Misc.makeSentence(commands, 4), value, teamColor);
			bat.bots.add(bot);
			
			bat.sendToAllClients("ADDBOT " + bat.ID + " " + bot.name + " " + client.account.user + " " + bot.battleStatus + " " + bot.teamColor + " " + bot.AIDll);
			
		}
		else if (commands[0].equals("REMOVEBOT")) {
			if (commands.length != 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			
			int index = bat.getBot(commands[1]);
			if (index == -1) return false;
			
			Bot bot = (Bot)bat.bots.get(index);
			bat.bots.remove(index);
			
			bat.sendToAllClients("REMOVEBOT " + bat.ID + " " + bot.name);
		}
		else if (commands[0].equals("UPDATEBOT")) {
			if (commands.length != 4) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			
			int index = bat.getBot(commands[1]);
			if (index == -1) return false;
			
			Bot bot = (Bot)bat.bots.get(index);

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
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can disable/enable units

			// let's check if client didn't double the data (he shouldn't, but we can't
			// trust him, so we will check ourselves):
			for (int i = 1; i < commands.length; i++) {
				if (bat.getUnitIndexInDisabledList(commands[i]) != -1) continue;
				bat.disabledUnits.add(commands[i]);
			}
			
			bat.sendToAllExceptFounder(command);
		}		
		else if (commands[0].equals("ENABLEUNITS")) {
			if (commands.length < 2) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
			if (bat == null) return false;
			if (bat.founder != client) return false; // only founder can disable/enable units

			int tmp;
			for (int i = 1; i < commands.length; i++) {
				tmp = bat.getUnitIndexInDisabledList(commands[i]);
				if (tmp == -1) continue; // let's just ignore this unit
				bat.disabledUnits.remove(tmp);
			}
			
			bat.sendToAllExceptFounder(command);
		}		
		else if (commands[0].equals("ENABLEALLUNITS")) {
			if (commands.length != 1) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			Battle bat = getBattle(client.battleID);
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
				Client targetClient = getClient(commands[1]);
				if (targetClient == null) return false;

				if (client.battleID == -1) {
					client.sendLine("SERVERMSG RING command failed: You can only ring players participating in your own battle!");
					return false; 
				}
				
				Battle bat = getBattle(client.battleID);
				if (bat == null) {
					System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
					closeServerAndExit();
				}
				if (!bat.isClientInBattle(commands[1])) {
					client.sendLine("SERVERMSG RING command failed: You don't have permission to ring players other than those participating in your battle!");
					return false;
				}
				if (bat.founder != client) {
					/* only host can ring players participating in his own battle, unless target is host himself */
					client.sendLine("SERVERMSG RING command failed: Only battle host is allowed to ring players participating in his own battle!");
					return false;
				}
												
				targetClient.sendLine("RING " + client.account.user);
			} else { // privileged user
				Client targetClient = getClient(commands[1]);
				if (targetClient == null) return false;
				
				targetClient.sendLine("RING " + client.account.user);
			}
		}
		else if (commands[0].equals("ADDSTARTRECT")) {
			if (commands.length != 6) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
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
				killClient(client, "Quit: inconsistent data");
				return false; 
			}
			
			if (bat.startRects[allyno].enabled) {
				client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
				killClient(client, "Quit: inconsistent data");
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
			
			Battle bat = getBattle(client.battleID);
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
				killClient(client, "Quit: inconsistent data");
				return false; 
			}
			
			if (!bat.startRects[allyno].enabled) {
				client.sendLine("SERVERMSG Serious error: inconsistent data (" + commands[0] + " command). You will now be disconnected ...");
				killClient(client, "Quit: inconsistent data");
				return false; 
			}
			
			bat.startRects[allyno].enabled = false;
		
			bat.sendToAllExceptFounder("REMOVESTARTRECT " + allyno);
		}		
		else if (commands[0].equals("SCRIPTSTART")) {
			if (commands.length != 1) return false;
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			
			bat.tempReplayScript = new Vector();
		}				
		else if (commands[0].equals("SCRIPT")) {
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			
			bat.tempReplayScript.add(Misc.makeSentence(commands, 1));
		}				
		else if (commands[0].equals("SCRIPTEND")) {
			if (client.account.accessLevel() < Account.NORMAL_ACCESS) return false;
			
			if (client.battleID == -1) return false;
			
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			
			bat.replayScript = bat.tempReplayScript;
			bat.sendScriptToAllExceptFounder();
		}				
		else {
			// unknown command!
			return false;
		}
		
		return true;
		
	}
	
	/* see the other killClient() method! */
	public static boolean killClient(Client client) {
		return killClient(client, "");
	}
	
	/* this method disconnects and removes client from clients Vector. 
	 * Also cleans up after him (channels, battles) and notifies other
	 * users of his departure. "reason" is used with LEFT command to
	 * notify other users on same channel of this client's departure
	 * reason (it may be left blank ("") to give no reason). */
	public static boolean killClient(Client client, String reason) {
		int index = clients.indexOf(client);
		if (index == -1) return false;
		if (!client.alive) return false;
		client.disconnect();
		clients.remove(index);
		client.alive = false;
		if (reason.trim().equals("")) reason = "Quit";
		
		
		/* We have to remove client from all channels. This is O(n*m) (n - number of channels,
		 * m - average number of users in a channel) operation since
		 * we don't keep a list of all the channels client is in. We do this because
		 * it is much more simple and we don't care much about speed since this method
		 * is called very seldom.
		 *  */
		int lastSize = channels.size();
		int pos = 0;
		while (pos < channels.size())
		{
			leaveChannelAndNotifyAll((Channel)channels.get(pos), client, reason);
			if (channels.size() < lastSize) {
				lastSize = channels.size();
			} else pos++;
		}; /* why did we have to check the channels.size()? Because when we removed a client
		     from a channel, channel may have been removed from channels list, if the client
		     was the last client in the channel. That is why we must check the size (if we
		     would use simple for loop, we could get ArrayOutOfBounds exception!) */ 
	
		if (client.battleID != -1) {
			Battle bat = getBattle(client.battleID);
			if (bat == null) {
				System.out.println("Serious error occured: Invalid battle ID. Server will now exit!");
				closeServerAndExit();
			}
			leaveBattle(client, bat); // automatically checks if client is a founder and closes battle
		}
		
		if (client.account.accessLevel() != Account.NIL_ACCESS) {
			sendToAllRegisteredUsers("REMOVEUSER " + client.account.user);
			if (DEBUG > 0) System.out.println("Registered user killed: " + client.account.user);
		} else {
			if (DEBUG > 0) System.out.println("Unregistered user killed");
		}

		if (LAN_MODE) {
			accounts.remove(client.account);
		}
		
		return true;
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
	
	public static void main(String[] args) {

		int port = SERVER_PORT;

		// process command line arguments:
		try {
			String s;
			for (int i = 0; i < args.length; i++)
				if (args[i].charAt(0) == '-') {
					s = args[i].substring(1).toUpperCase();
					if (s.equals("PORT")) {
						int p = Integer.parseInt(args[i+1]);
						if ((p < 1) || (p > 65535)) throw new IOException();
						port = p;
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
						LanAdminUsername = args[i+1];
						LanAdminPassword = Misc.encodePassword(args[i+2]);
						
						if (!Misc.isValidName(LanAdminUsername)) throw new Exception();
						if (!Misc.isValidPass(LanAdminPassword)) throw new Exception();
						i += 2; // we must skip username and password parameters in next iteration
					}
					else throw new IOException();
				} else throw new IOException();
			
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
						
			closeServerAndExit();
		}

		System.out.println("TASServer " + VERSION + " started on " + Misc.easyDateFormat("yyyy.MM.dd 'at' hh:mm:ss z"));
		
		if (!LAN_MODE) {
			readAccountsInfo();
			banList.loadFromFile(BAN_LIST_FILENAME);
			readAgreement();
		} else {
			System.out.println("LAN mode enabled");
		}
		
		if (RECORD_STATISTICS) {
			// any special initialization required?
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
		
		readMOTD(MOTD_FILENAME);
		upTime = System.currentTimeMillis();
			if (!IP2Country.initializeAll("Merged2.csv")) {
			System.out.println("Unable to find <IP2Country> file. Skipping ...");			
		} else {
			System.out.println("<IP2Country> loaded.");
		}
		
		// start server:
		helpUDPsrvr = new NATHelpServer(NAT_TRAVERSAL_PORT);
		helpUDPsrvr.start();

		if (!startServer(port)) closeServerAndExit();

		
		// block while we wait for a client to connect
	    running = true;
	    while (running) { // main loop

	    	// check for new client connections
		    acceptNewConnections();
		    
		    // check for incoming msgs
		    readIncomingMessages();
		    
		    // reset received bytes count every n seconds
		    if (System.currentTimeMillis() - lastFloodCheckedTime > recvRecordPeriod * 1000) {
		    	lastFloodCheckedTime = System.currentTimeMillis();
		    	for (int i = 0; i < clients.size(); i++) ((Client)clients.get(i)).dataOverLastTimePeriod = 0;
		    }
		    
		    // check for timeouts:
		    if (System.currentTimeMillis() - lastTimeoutCheck > TIMEOUT_LENGTH) {
		    	lastTimeoutCheck = System.currentTimeMillis();
		    	long now = System.currentTimeMillis();
		    	for (int i = 0; i < clients.size(); i++) {
		    		if (now - ((Client)clients.get(i)).timeOfLastReceive > TIMEOUT_LENGTH) {
		    			System.out.println("Timeout detected from " + ((Client)clients.get(i)).account.user + " (" + ((Client)clients.get(i)).IP + "). Killing client ...");
		    			killClient((Client)clients.get(i), "Quit: timeout");		
		    			i--;
		    		}
		    	}
		    }
		    
		    // kill all clients on killList():
		    for (; killList.size() > 0;) {
		    	killClient((Client)killList.get(0), "Quit: undefined connection error");
		    	killList.remove(0);
		    }
		    
		    // update statistics:
		    if ((RECORD_STATISTICS) && (System.currentTimeMillis() - lastStatisticsUpdate > saveStatisticsInterval))
		    	saveStatisticsToDisk();
		    	
		    // check UDP server for any new packets:
		    while (NATHelpServer.msgList.size() > 0) {
		    	DatagramPacket packet = (DatagramPacket)NATHelpServer.msgList.remove(0); 
	            InetAddress address = packet.getAddress();
	            int p = packet.getPort();
	            String data = new String(packet.getData(), packet.getOffset(), packet.getLength());
	            if (DEBUG > 1) System.out.println("*** UDP packet received from " + address.getHostAddress() + " from port " + p);
	            Client client = getClient(data);
	            if (client == null) continue;
	            client.UDPSourcePort = p;
	            client.sendLine("UDPSOURCEPORT " + p);
		    }
		    
		    // save accounts info to disk on regular intervals:
		    if ((!LAN_MODE) && (System.currentTimeMillis() - lastSaveAccountsTime > saveAccountInfoInterval)) {
		    	writeAccountsInfo();
		    	// note: lastSaveAccountsTime will get updated in writeAccountsInfo() method!
		    }

		    // purge mute lists of all channels on regular intervals:
		    if (System.currentTimeMillis() - lastMutesPurgeTime > purgeMutesInterval) {
		    	lastMutesPurgeTime = System.currentTimeMillis();
		    	for (int i = 0; i < channels.size(); i++) {
		    		((Channel)channels.get(i)).muteList.clearExpiredOnes();
		    	}
		    }
		    
		    // sleep a bit
		    try {
		    	Thread.sleep(MAIN_LOOP_SLEEP);
		    } catch (InterruptedException ie) {
		    }
	    }

        System.out.println("Server closed!");
	}
}
