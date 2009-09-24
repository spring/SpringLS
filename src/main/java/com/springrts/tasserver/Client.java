/*
 * Created on 2005.6.17
 */

package com.springrts.tasserver;


import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.CharacterCodingException;
import java.util.*;

/**
 * @author Betalord
 */
public class Client {

	public boolean alive = false; // if false, then this client is not "valid" anymore (we already killed him and closed his socket).
	public boolean halfDead = false; // when we schedule client for kill (via Clients.killClientDelayed, for example) this flag is set to true. When true, we don't read or send any data to this client.

	public Account account;
	public String IP;
	public String localIP; // client's local IP which has to be send with LOGIN command (server can't figure out his local IP himself of course)
	public int UDPSourcePort; // client's public UDP source port used with some NAT traversal techniques (e.g. "hole punching")
	public int status; // see MYSTATUS command for actual values of status
	public int battleStatus; // see MYBATTLESTATUS command for actual values of battleStatus
	public int teamColor; // see MYBATTLESTATUS for info on this one
	public int battleID; // battle ID in which client is participating. Must be -1 if not participating in any battle.
	public ArrayList<Channel> channels = new ArrayList<Channel>(); // list of channels user is participating in

	public SocketChannel sockChan;
	public SelectionKey selKey;
	public StringBuilder recvBuf;
	private int msgID = TASServer.NO_MSG_ID; // -1 means no ID is used (NO_MSG_ID constant). This is the message/command ID used when sending command as described in the "lobby protocol description" document. Use setSendMsgID and resetSendMsgID methods to manipulate it.
	private Queue<ByteBuffer> sendQueue = new LinkedList<ByteBuffer>(); // queue of "delayed data". We failed sending this the first time, so we'll have to try sending it again some time.
	private StringBuilder fastWrite; // temporary StringBuilder used with beginFastWrite() and endFastWrite() methods.

	public long inGameTime; // in milliseconds. Used internally to remember time when user entered game using System.currentTimeMillis().
	public String country;
	public int cpu; // in MHz if possible, or in MHz*1.4 if AMD. 0 means the client can't figure out it's CPU speed.
	public String lobbyVersion; // e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	public long dataOverLastTimePeriod = 0; // how many bytes did client send over last recvRecordPeriod seconds. This is used with anti-flood protection.
	public long timeOfLastReceive; // time (System.currentTimeMillis()) when we last heard from client (last data received)
	public boolean acceptAccountIDs; // does the client accept accountIDs in ADDUSER command ?

	public Client(SocketChannel sockChan) {
		alive = true;

		account = new Account("", "", Account.NIL_ACCESS, Account.NO_USER_ID, 0, "?", 0, "XX", Account.NO_ACCOUNT_ID); // no info on user/pass, zero access
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
		recvBuf = new StringBuilder();
		status = 0;
		country = IP2Country.getCountryCode(Misc.IP2Long(IP));
		battleStatus = 0;
		teamColor = 0;
		inGameTime = 0;
		battleID = -1;
		cpu = 0;

		timeOfLastReceive = System.currentTimeMillis();
	}

	// any messages sent via sendLine() method will contain this ID. See "lobby protocol description" document for more info on message/command IDs.
	public void setSendMsgID(int ID) {
		this.msgID = ID;
	}

	public void resetSendMsgID(int ID) {
		this.msgID = ID;
	}

	/* will prefix the message with a msgID value, if it was previously set via setSendMsgID() method. */
	public boolean sendLine(String text) {
		return sendLine(text, msgID);
	}

	/* the 'msgID' param overrides any previously set ID (via setSendMsgID method). Use NO_MSG_ID (which should equal to -1) for none.  */
	public boolean sendLine(String text, int msgID) {
		if (!alive) return false;
		if (halfDead) return false;

		// prefix message with a message ID:
		if (msgID != TASServer.NO_MSG_ID) text = "#" + msgID + " " + text;

		if (fastWrite != null) {
			if (fastWrite.length() != 0)
				fastWrite.append(Misc.EOL);
			fastWrite.append(text);
			return true;
		}

		if (TASServer.DEBUG > 1)
			if (account.accessLevel() != Account.NIL_ACCESS) System.out.println("[->" + account.user + "]" + " \"" + text + "\"");
			else System.out.println("[->" + IP + "]" + " \"" + text + "\"");

		try {
			// prepare data and add it to the send queue

			String data = text + Misc.EOL;

			if ((sockChan == null) || (!sockChan.isConnected())) {
				System.out.println("WARNING: SocketChannel is not ready to be written to. Killing the client next loop ...");
				Clients.killClientDelayed(this, "Quit: undefined connection error");
				return false;
			}

			ByteBuffer buf;
			try{
				buf = TASServer.asciiEncoder.encode(CharBuffer.wrap(data));
			} catch (CharacterCodingException e) {
				System.out.println("WARNING: Unable to encode message. Killing the client next loop ...");
				Clients.killClientDelayed(this, "Quit: undefined encoder error");
				return false;
			}

			if (sendQueue.size() != 0) {
				sendQueue.add(buf);
			} else {
				sendQueue.add(buf);
				boolean empty = tryToFlushData();
				if (!empty) {
					Clients.enqueueDelayedData(this);
				}
			}
		} catch (Exception e) {
			System.out.println("Error sending data (undefined). Killing the client next loop ...");
			Clients.killClientDelayed(this, "Quit: undefined connection error");
			e.printStackTrace(); //*** DEBUG
			return false;
		}
		return true;
	}

	public void sendWelcomeMessage() {
		sendLine("TASServer " + TASServer.VERSION + " " + TASServer.latestSpringVersion + " " + TASServer.NAT_TRAVERSAL_PORT + " " + (TASServer.LAN_MODE ? 1 : 0));
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

	/* tries to send the data from the sendQueue. Returns true if all data has been flushed or false otherwise. */
	public boolean tryToFlushData() {
		if (!alive || halfDead) {
			// disregard any other scheduled writes:
			while (sendQueue.size() != 0)
				sendQueue.remove();
			return true; // no more data left to be flushed, so return true
		}

		ByteBuffer buf;
		while ((buf = sendQueue.peek()) != null) {
			try {
				sockChan.write(buf);

				if (buf.hasRemaining()) // this happens when send buffer is full and no more data can be written to it
					break; // lets just skip it without removing the packet from the send queue (we will retry sending it later)

				// remove element from queue (it was sent entirely)
				sendQueue.remove();
			} catch (ClosedChannelException cce) {
				// no point sending the rest to the closed channel
				if (alive) {
					Clients.killClientDelayed(this, "Quit: socket channel closed exception");
				}
				break;
			} catch (IOException io) {
				if (alive) {
					Clients.killClientDelayed(this, "Quit: socket channel closed exception");
				}
				break;
			}
		}

		return sendQueue.size() == 0;
	}

	public void beginFastWrite() {
		if (fastWrite != null) {
			System.out.println("Serious error detected: invalid use of beginFastWrite(). Check your code! Shutting down the server ...");
			TASServer.closeServerAndExit();
		}

		fastWrite = new StringBuilder();
	}

	public void endFastWrite() {
		if (fastWrite == null) {
			System.out.println("Serious error detected: invalid use of endFastWrite(). Check your code! Shutting down the server ...");
			TASServer.closeServerAndExit();
		}

		String data = fastWrite.toString();
		fastWrite = null;
		if (data.equals("")) return ;
		sendLine(data);
	}


	/* various methods dealing with client status: */

	public boolean getInGameFromStatus() {
		return (status & 0x1) == 1;
	}

	public boolean getAwayBitFromStatus() {
		return ((status & 0x2) >> 1) == 1;
	}

	public int getRankFromStatus() {
		return (status & 0x1C) >> 2;
	}

	public boolean getAccessFromStatus() {
		return ((status & 0x20) >> 5) == 1;
	}

	public boolean getBotModeFromStatus() {
		return ((status & 0x40) >> 6) == 1;
	}

	public void setInGameToStatus(boolean inGame) {
		status = (status & 0xFFFFFFFE) | (inGame ? 1 : 0);
	}

	public void setAwayBitToStatus(boolean away) {
		status = (status & 0xFFFFFFFD) | ((away ? 1 : 0) << 1);
	}

	public void setRankToStatus(int rank) {
		status = (status & 0xFFFFFFE3) | (rank << 2);
	}

	public void setAccessToStatus(boolean access) {
		status = (status & 0xFFFFFFDF) | ((access ? 1 : 0) << 5);
	}

	public void setBotModeToStatus(boolean isBot) {
		status = (status & 0xFFFFFFBF) | ((isBot ? 1 : 0) << 6);
	}
}
