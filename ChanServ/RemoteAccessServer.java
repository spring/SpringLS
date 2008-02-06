/*
 * Created on 2006.11.17
 *
 * Protocol description:
 * 
 * - communication is meant to be short - 1 or 2 commands and then connection should be terminated
 * - connection gets closed automatically after 30 seconds of inactivity.
 * - communication is strictly synchronous - when client sends a command server responds with another command.
 *   If same client must query multiple commands, he should either wait for a response after each command
 *   before sending new command, or use new connection for next command.
 * 
 * *** COMMAND LIST: ***
 * 
 * * IDENTIFY key
 *   This is the first command that client should send immediately after connecting. 'key' parameter
 *   is a key provided to the user with which he identifies himself to the server. Server will reply
 *   with either PROCEED or FAILED command.
 *   
 * * PROCEED
 *   Sent by server upon accepting connection.
 *   
 * * FAILED
 *   Sent by server if IDENTIFY command failed (either because of incorrect key or because service is unavailable).
 *
 * * TESTLOGIN username password
 *   Sent by client when trying to figure out if login info is correct. Server will respond with either LOGINOK or LOGINBAD command.
 *
 * * LOGINOK
 *   Sent by server as a response to a successful TESTLOGIN command.
 *   
 * * LOGINBAD
 *   Sent by server as a response to a failed TESTLOGIN command.
 *   
 * * OK
 *   Sent by server as a response to commands that don't return anything else. 
 *   Serves also as positive confirmation (also see "NOTOK" command).
 *   
 * * NOTOK
 *   Sent by server as a response to commands that require some positive/negative response (in this case, the response is negative).
 *   Also see "OK" command.  
 *   
 * * ISONLINE username
 *   Queries the server trying to find out if user <username> is currently online.
 *   Returns either OK or NOTOK (OK if user is online, NOTOK otherwise).  
 *   
 * * GETACCESS username
 *   Sent by client trying to figure out access status of some user.
 *   Returns 1 for "normal", 2 for "moderator", 3 for "admin".
 *   If user is not found, returns 0 as a result.
 *   If the operation fails for some reason, socket will simply get disconnected.
 *
 * * GENERATEUSERID username
 *   Will send acquireuserid command to <username>. This command doesn't return anything, you won't
 *   be notified if the command succeeded or not (but new notification will be added to TASServer if
 *   specified user responded with a USERID command properly).  
 *   
 * * QUERYSERVER {server command}  
 *   This will forward the given command directly to server and then forward server's response back to the client.
 *   This command means potential security risk. Will be replaced in the future by a set of user specific commands.
 *   Currently commands that may be passed to this function are limited - only some command are allowed. This is so
 *   in order to avoid some security risks with the command.
 *   If the operation fails for some reason, socket will simply get disconnected.   
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class RemoteAccessServer extends Thread {
	
	final static int TIMEOUT = 30000; // in milliseconds
	final static boolean DEBUG = true;  
	
	public static Vector remoteAccounts = new Vector(); // contains String-s of keys for remote server access
	
	// used with QUERYSERVER command
	public static final String allowedQueryCommands[] = {
		"GETREGISTRATIONDATE",
		"GETINGAMETIME",
		"GETLASTIP",
		"GETLASTLOGINTIME",
		"RELOADUPDATEPROPERTIES",
		"GETLOBBYVERSION",
		"UPDATEMOTD",
		"RETRIEVELATESTBANLIST",
		"GETUSERID"
		};
	
	public Vector threads = new Vector(); // here we keep a list of all currently running client threads
	private int port;
	
	public RemoteAccessServer(int port) {
		this.port = port;
	}
	
	public void run() {
		try {
	        ServerSocket ss = new ServerSocket(port);

	        while (true) {
	            Socket cs = ss.accept();
	            RemoteClientThread thread = new RemoteClientThread(this, cs);
	        	threads.add(thread);
	        	thread.start();
	        }
		} catch (IOException e) {
			Log.error("Error occured in RemoteAccessServer: " + e.getMessage());
		}
	}
	
}

class RemoteClientThread extends Thread {
    static final int BUF_SIZE = 2048;

    static final byte[] EOL = {(byte)'\r', (byte)'\n' };

    /* unique ID which we will use as a message ID when sending commands to TASServer */
    public int ID = (int)((Math.random() * 65535));
    
    /* reply queue which gets filled by ChanServ automatically */
    Queue replyQueue = new Queue();    
    
    /* socket for the client which we are handling */
    private Socket socket;
    private String IP;
    private boolean identified = false; // if remote client has already identified

    private PrintWriter out;
    private BufferedReader in;
    
    private RemoteAccessServer parent; // so we will know which object spawned this thread
    
    
    RemoteClientThread(RemoteAccessServer parent, Socket s) {
    	this.socket = s;
    	this.parent = parent;
        try {
            socket.setSoTimeout(RemoteAccessServer.TIMEOUT);
            socket.setTcpNoDelay(true);
        } catch (SocketException e) {
        	Log.error("Serious error in RemoteClient constructor (SocketException): " + e.getMessage());
        }
        IP = socket.getInetAddress().getHostAddress();
        
	    try {
	    	out = new PrintWriter(socket.getOutputStream(), true);
	    	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    } catch (IOException e) {
	    	Log.error("Serious error: cannot associate input/output with client socket! Program will now exit ...");
    	    System.exit(1);
    	}
    }
    
	public String readLine() throws IOException {
		return in.readLine();
	}
	
	public void sendLine(String text) {
		if (RemoteAccessServer.DEBUG) System.out.println("RAS: \"" + text + "\"");
		out.println(text);
	}    
    
	private void disconnect() {
		try {
			if (!socket.isClosed()) {
	    	    out.close();
	    	    in.close();
	    	    socket.close();
			}
		} catch (IOException e) {
			// ignore it
		}
	}
	
	private void kill() {
		disconnect();
		parent.threads.remove(this);
	}
    
    public void run() {
   	    String input;
   	    
		try
		{
			while (true) {
				input = readLine();
				if (input == null) throw new IOException();
				if (RemoteAccessServer.DEBUG) System.out.println(IP + ": \"" + input + "\"");
				processCommand(input);
			}
		}
		catch (InterruptedIOException e)
		{
			kill();
			return;
		}    	    
		catch (IOException e)
		{
			kill();
			return;
		}    	   
    }
    
    private void queryTASServer(String command) {
    	ChanServ.sendLine("#" + ID + " " + command);
    	
    }
    private String waitForReply() {
    	return (String)replyQueue.pull(); // will wait until queue doesn't contain a response
    }
    
    public void processCommand(String command) {
    	command = command.trim();
    	if (command.equals("")) return ;
    	
		String[] params = command.split(" ");
		params[0] = params[0].toUpperCase();
		
		if (params[0].equals("IDENTIFY")) {
			if (params.length != 2) return ; // malformed command
			if (!ChanServ.isConnected()) {
				sendLine("FAILED");
				return ;
			}
			for (int i = 0; i < RemoteAccessServer.remoteAccounts.size(); i++)
				if (((String)RemoteAccessServer.remoteAccounts.get(i)).equals(params[1])) {
					sendLine("PROCEED");
					identified = true; // client has successfully identified
					return ;
				}
			sendLine("FAILED");
		} else if (params[0].equals("TESTLOGIN")) {
			if (!identified) return ;
			if (params.length != 3) return ; // malformed command
			if (!ChanServ.isConnected()) {
				sendLine("LOGINBAD");
				return ;
			}
			queryTASServer("TESTLOGIN " + params[1] + " " + params[2]);
			String reply = waitForReply().toUpperCase();
			if (reply.equals("TESTLOGINACCEPT")) {
				sendLine("LOGINOK");
			}
			else
				sendLine("LOGINBAD");
		} else if (params[0].equals("GETACCESS")) {
			if (!identified) return ;
			if (params.length != 2) return ; // malformed command
			if (!ChanServ.isConnected()) {
				kill();
				return ;
			}
			queryTASServer("GETACCOUNTACCESS " + params[1]);
			String reply = waitForReply();

			// if user not found:
			if (reply.equals("User <" + params[1] + "> not found!")) {
				sendLine("0");
				return ;
			}
			
			String[] tmp = reply.split(" ");
			int access = 0;
			try {
				access = Integer.parseInt(tmp[tmp.length-1]);
			} catch (NumberFormatException e) { // should not happen
				kill();
				return ; 
			}
			sendLine("" + (access & 0x7));
		} else if (params[0].equals("GENERATEUSERID")) {
			if (!identified) return ;
			if (params.length != 2) return ; // malformed command
			if (!ChanServ.isConnected()) {
				kill();
				return ;
			}
			queryTASServer("FORGEMSG " + params[1] + " ACQUIREUSERID");
			sendLine("OK");
		} else if (params[0].equals("ISONLINE")) {
			if (!identified) return ;
			if (params.length != 2) return ; // malformed command
			if (!ChanServ.isConnected()) {
				kill();
				return ;
			}

			boolean success = false;
			synchronized(ChanServ.clients) {
				for (int i = 0; i < ChanServ.clients.size(); i++) {
					if (((Client)ChanServ.clients.get(i)).name.equals(params[1])) {
						success = true;
						break;
					}
				}
			} // end of synchronized
			sendLine(success ? "OK" : "NOTOK");
		} else if (params[0].equals("QUERYSERVER")) {
			if (!identified) return ;
			if (params.length < 2) return ; // malformed command
			if (!ChanServ.isConnected()) {
				kill();
				return ;
			}
			boolean allow = false;
			for (int i = 0; i < RemoteAccessServer.allowedQueryCommands.length; i++) {
				if (RemoteAccessServer.allowedQueryCommands[i].equals(params[1])) {
					allow = true;
					break;
				}
			}
			
			if (!allow) kill(); // client is trying to execute a command that is not allowed!
			
			queryTASServer(Misc.makeSentence(params, 1));
			String reply = waitForReply();
			sendLine(reply);

			// quick fix for ChanServ crash on adding ban entry in the web interface:
			if (Misc.makeSentence(params, 1).equalsIgnoreCase("RETRIEVELATESTBANLIST")) {
				reply = waitForReply(); // wait for the second line of reply	
			}
			
		} else {
			// unknown command!
		}
    }
    
}
