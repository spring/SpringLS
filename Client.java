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
import java.util.*;

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
	public ArrayList<Channel> channels = new ArrayList<Channel>(); // list of channels user is participating in
	
	public SocketChannel sockChan;
	public SelectionKey selKey;
	public StringBuffer recvBuf;
	
	public long inGameTime; // in milliseconds. Used internally to remember time when user entered game using System.currentTimeMillis().
	public String country;
	public int cpu; // in MHz if possible, or in MHz*1.4 if AMD. 0 means the client can't figure out it's CPU speed.
	public String lobbyVersion; // e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	public long dataOverLastTimePeriod = 0; // how many bytes did client send over last recvRecordPeriod seconds. This is used with anti-flood protection.
	public long timeOfLastReceive; // time (System.currentTimeMillis()) when we last heard from client (last data received)
	public long lastMapGradesReceived = 0; // time when we last received MAPGRADES command from this user. This is needed to ensure user doesn't send this command too often as it creates much load on the server.
	
	public Client(SocketChannel sockChan) {
		alive = true;
		
		account = new Account("", "", Account.NIL_ACCESS, 0, "?", 0, new MapGradeList()); // no info on user/pass, zero access
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
				Clients.killClientDelayed(this, "Quit: undefined connection error");
				return false;
			}
		} catch (ChannelWriteTimeoutException e) {
    		System.out.println("WARNING: channelWrite() timed out. Disconnecting client ...");
			Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem: channelWrite() timed out [" + IP + ", <" + account.user + ">]");
			
			// add server notification:
			ServerNotification sn = new ServerNotification("channelWrite() timeout");
			sn.addLine("Serious problem detected: channelWrite() has timed out.");
			sn.addLine("Client: " + IP + "(" + (account.user.equals("") ? "user not logged in" : account.user) + ")");
			ServerNotifications.addNotification(sn);
			Clients.killClientDelayed(this, "Quit: channelWrite() timed out");
			
		} catch (Exception e) {
			System.out.println("Error writing to socket (exception). Line not sent! Killing the client next loop...");
			Clients.killClientDelayed(this, "Quit: undefined connection error");
			return false;
		}
		return true;
	}

	public void sendWelcomeMessage() {
		sendLine("TASServer " + TASServer.VERSION + " " + TASServer.NAT_TRAVERSAL_PORT);
	}
	
	/* should only be called by Clients.killClient() method! */
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
	
	/* joins client to <chanName> channel. If channel with that name
	 * does not exist, it is created. Method returns channel object as a result. 
	 * If client is already in the channel, it returns null as a result. 
	 * This method does not check for a correct key in case the channel is locked, 
	 * caller of this method should do that before calling it.
	 * This method also doesn't do any notificating of other clients in the channel,
	 * caller must do all that. */
	public Channel joinChannel(String chanName) {
		Channel chan = Channels.getChannel(chanName);
		if (chan == null) {
			chan = new Channel(chanName);
			Channels.addChannel(chan);
		}
		else if (this.channels.indexOf(chan) != -1) return null; // already in the channel 

		chan.addClient(this);
		this.channels.add(chan);
		return chan;
	}
	
	/* removes client from the channel and notifies all other clients in the channel about it.
	 * If this was the last client in the channel, then channel is removed from channels list. 
	 * "reason" may be left blank ("") if no reason is to be given. */
	public boolean leaveChannel(Channel chan, String reason) {
		boolean result = chan.removeClient(this);
		
		if (result) {
			if (chan.getClientsSize() == 0) Channels.removeChannel(chan); // since channel is empty there is no point in keeping it in a channels list. If we would keep it, the channels list would grow larger and larger in time. We don't want that!
			else for (int i = 0; i < chan.getClientsSize(); i++) chan.getClient(i).sendLine("LEFT " + chan.name + " " + this.account.user + (reason.equals("") ? "" : " " + reason));
			this.channels.remove(chan);
		} 
		
		return result;
	}
	
	/* calls leaveChannel() for every channel client is participating in.
	 * Also notifies all clients of his departure.
	 * Also see comments for leaveChannel() method. */
	public void leaveAllChannels(String reason) {
		while (channels.size() != 0) {
			leaveChannel(channels.get(0), reason);
		}
		this.channels.clear();
	}
	
	/* will search the list of channels this user is participating in
	 * and return the specified channel or null if client is not participating
	 * in this channel. */
	public Channel getChannel(String chanName) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).name.equals(chanName)) return channels.get(i);
		}
		return null;		
	}
	
}
