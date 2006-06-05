/*
 * Created on 2005.6.17
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

//import java.io.*;
//import java.net.*;
import java.nio.channels.*;

public class Client {
	public boolean alive = false;
	
	public Account account;
	public String IP;
	public String localIP; // client's local IP which has to be send with LOGIN command (server can't figure out his local IP himself ofcourse) 
	public int UDPSourcePort; // client's public UDP source port used with some NAT traversal techniques (e.g. "hole punching")
	public int status; // see MYSTATUS command for actual values of status
	public int battleStatus; // see MYBATTLESTATUS command for actual values of battleStatus
	public int teamColor; // see MYBATTLESTATUS for info on this one
	public int battleID; // battle ID in which client is participating. Must be -1 if not participating in any battle.
	
	public SocketChannel sockChan;
	public SelectionKey selKey;
	public StringBuffer recvBuf;
	
	public long inGameTime; // in milliseconds. Used internally to remember time when user entered game using System.currentTimeMillis().
	public String country;
	public int cpu; // in MHz if possible, or in MHz*1.4 if AMD. 0 means the client can't figure out it's CPU speed.
	public String lobbyVersion; // e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	public long dataOverLastTimePeriod = 0; // how many bytes did client send over last recvRecordPeriod seconds. This is used with anti-flood protection.
	public long timeOfLastReceive; // time (System.currentTimeMillis()) when we last heard from client (last data received)
	
	public Client(SocketChannel sockChan) {
		alive = true;
		
		account = new Account("", "", Account.NIL_ACCESS, 0, "?", 0); // no info on user/pass, zero access
		this.sockChan = sockChan;
		IP = sockChan.socket().getInetAddress().getHostAddress();
		// this fixes the issue with local user connecting to server as "127.0.0.1" (he can't host battles with that IP):
		if (IP.equals("127.0.0.1") || IP.equals("localhost")) {
			String newIP = Misc.getLocalIPAddress();
			if (newIP != null) IP = newIP; else {
				System.out.println("Could not resolve local IP address. User may have problems \n" +
								   "with hosting battles.");
			}
		}
		localIP = new String(IP); // will be changed later once client logs in
		UDPSourcePort = 0; // yet unknown
		selKey = null;
		recvBuf = new StringBuffer("");
		status = 0;
		country = IP2Country.getCountryCode(Misc.IP2Long(IP));
		battleStatus = 0;
		teamColor = 0;
		inGameTime = 0;
	    battleID = -1;
	    cpu = 0;
	    
	    timeOfLastReceive = System.currentTimeMillis();
	}
	
	public boolean sendLine(String text) {
		if (!alive) return false;
		
		if (TASServer.DEBUG > 1) 
			if (account.accessLevel() != Account.NIL_ACCESS) System.out.println("[->" + account.user + "]" + " \"" + text + "\"");
			else System.out.println("[->" + IP + "]" + " \"" + text + "\"");
		try {
			if (!TASServer.sendLineToSocketChannel(text, sockChan)) {
				System.out.println("Error writing to socket. Line not sent! Killing the client next loop...");
				TASServer.killList.add(this);
				return false;
			}
		} catch (Exception e) {
			System.out.println("Error writing to socket. Line not sent! Killing the client next loop...");
			TASServer.killList.add(this);
			return false;
		}
		return true;
	}

	public void sendWelcomeMessage() {
		sendLine("TASServer " + TASServer.VERSION + " " + TASServer.NAT_TRAVERSAL_PORT);
	}
	
	/* should only be called by TASServer.killClient() method! */
	public void disconnect() {
		if (!alive) {
			System.out.println("PROBLEM DETECTED: disconnecting dead client. Skipping ...");
			return ;
		}
		
		try {
			sockChan.close();
		} catch (Exception e) {
			System.out.println("Error: cannot disconnect socket!");
		}
		
		sockChan = null;
		selKey = null;
	}
}
