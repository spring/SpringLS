/*
 * Created on 2005.6.17
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.CharacterCodingException;
import java.util.*;

/**
 * @author Betalord
 */
public class Client {

	private static final Log s_log  = LogFactory.getLog(Client.class);

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 */
	public boolean alive = false;
	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 */
	public boolean halfDead = false;

	public Account account;
	public String IP;
	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 */
	public String localIP;
	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 */
	public int UDPSourcePort;
	/**
	 * See the 'MYSTATUS' command for valid values
	 */
	public int status;
	/**
	 * See the 'MYBATTLESTATUS' command for valid values
	 */
	public int battleStatus;
	/**
	 * @see MYBATTLESTATUS
	 */
	public int teamColor;
	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 */
	public int battleID;
	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 */
	public int requestedBattleID;
	/**
	 * List of channels user is participating in.
	 */
	public List<Channel> channels = new ArrayList<Channel>();

	public SocketChannel sockChan;
	public SelectionKey selKey;
	public StringBuilder recvBuf;
	/**
	 * This is the message/command ID used when sending command
	 * as described in the "lobby protocol description" document.
	 * Use setSendMsgID() and resetSendMsgID() methods to manipulate it.
	 * NO_MSG_ID means no ID is used.
	 */
	private int msgID = TASServer.NO_MSG_ID;
	/**
	 * Queue of "delayed data".
	 * We failed sending this the first time, so we will have to try sending it
	 * again some time later.
	 */
	private Queue<ByteBuffer> sendQueue = new LinkedList<ByteBuffer>();
	/**
	 * Temporary StringBuilder used by some internal methods.
	 * @see beginFastWrite()
	 * @see endFastWrite()
	 */
	private StringBuilder fastWrite;

	/**
	 * In milliseconds.
	 * Used internally to remember time when the user entered the game.
	 * @see System.currentTimeMillis()
	 */
	public long inGameTime;
	public String country;
	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 */
	public int cpu;
	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 */
	public String lobbyVersion;
	/**
	 * How many bytes did this client send over the last recvRecordPeriod
	 * seconds. This is used with anti-flood protection.
	 */
	public long dataOverLastTimePeriod = 0;
	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see System.currentTimeMillis()
	 */
	public long timeOfLastReceive;
	/**
	 * Does the client accept accountIDs in ADDUSER command ?
	 */
	public boolean acceptAccountIDs;
	/**
	 * Does the client accept JOINBATTLEREQUEST command ?
	 */
	public boolean handleBattleJoinAuthorization;

	public Client(SocketChannel sockChan) {
		alive = true;

		// no info on user/pass, zero access
		account = new Account();
		this.sockChan = sockChan;
		IP = sockChan.socket().getInetAddress().getHostAddress();
		// this fixes the issue with local user connecting to server as "127.0.0.1" (he can't host battles with that IP):
		if (IP.equals("127.0.0.1") || IP.equals("localhost")) {
			String newIP = Misc.getLocalIPAddress();
			if (newIP != null) {
				IP = newIP;
			} else {
				s_log.warn("Could not resolve local IP address. User may have problems \n" +
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
		requestedBattleID = -1;
		cpu = 0;

		timeOfLastReceive = System.currentTimeMillis();
	}


	@Override
	public int hashCode() {
		int hash = 5;
		hash = 67 * hash + (this.sockChan != null ? this.sockChan.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Client other = (Client) obj;
		if (this.sockChan != other.sockChan && (this.sockChan == null || !this.sockChan.equals(other.sockChan))) {
			return false;
		}
		return true;
	}

	/**
	 * Any messages sent via sendLine() method will contain this ID.
	 * See "lobby protocol description" document for more info
	 * on message/command IDs.
	 */
	public void setSendMsgID(int ID) {
		this.msgID = ID;
	}

	public void resetSendMsgID(int ID) {
		this.msgID = ID;
	}

	/**
	 * Will prefix the message with a msgID value, if it was previously
	 * set via setSendMsgID() method.
	 */
	public boolean sendLine(String text) {
		return sendLine(text, msgID);
	}

	/**
	 * The 'msgID' param overrides any previously set ID
	 * (via setSendMsgID method).
	 * Use NO_MSG_ID (which should equal to -1) for none.
	 */
	public boolean sendLine(String text, int msgID) {
		if (!alive || halfDead) {
			return false;
		}

		// prefix message with a message ID:
		if (msgID != TASServer.NO_MSG_ID) {
			text = "#" + msgID + " " + text;
		}

		if (fastWrite != null) {
			if (fastWrite.length() != 0)
				fastWrite.append(Misc.EOL);
			fastWrite.append(text);
			return true;
		}

		if (s_log.isDebugEnabled()) {
			if (account.getAccess() != Account.Access.NONE) {
				s_log.debug("[->" + account.getName() + "]" + " \"" + text + "\"");
			} else {
				s_log.debug("[->" + IP + "]" + " \"" + text + "\"");
			}
		}

		try {
			// prepare data and add it to the send queue

			String data = text + Misc.EOL;

			if ((sockChan == null) || (!sockChan.isConnected())) {
				s_log.warn("SocketChannel is not ready to be written to. Killing the client next loop ...");
				Clients.killClientDelayed(this, "Quit: undefined connection error");
				return false;
			}

			ByteBuffer buf;
			try{
				buf = TASServer.asciiEncoder.encode(CharBuffer.wrap(data));
			} catch (CharacterCodingException e) {
				s_log.warn("Unable to encode message. Killing the client next loop ...", e);
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
			s_log.error("Failed sending data (undefined). Killing the client next loop ...", e);
			Clients.killClientDelayed(this, "Quit: undefined connection error");
			return false;
		}
		return true;
	}

	public void sendWelcomeMessage() {
		sendLine(new StringBuilder("TASServer ")
				.append(TASServer.getAppVersion()).append(" ")
				.append(TASServer.latestSpringVersion).append(" ")
				.append(TASServer.NAT_TRAVERSAL_PORT).append(" ")
				.append(TASServer.LAN_MODE ? 1 : 0).toString());
	}

	/** Should only be called by Clients.killClient() method! */
	public void disconnect() {
		if (!alive) {
			s_log.error("PROBLEM DETECTED: disconnecting dead client. Skipping ...");
			return ;
		}

		try {
			sockChan.close();
		} catch (Exception e) {
			s_log.error("Failed disconnecting socket!");
		}

		sockChan = null;
		selKey = null;
	}

	/**
	 * joins client to <chanName> channel. If channel with that name
	 * does not exist, it is created. Method returns channel object as a result.
	 * If client is already in the channel, it returns null as a result.
	 * This method does not check for a correct key in case the channel is locked,
	 * caller of this method should do that before calling it.
	 * This method also doesn't do any notificating of other clients in the channel,
	 * caller must do all that
	 */
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

	/**
	 * Removes client from the channel and notifies all other clients in the channel about it.
	 * If this was the last client in the channel, then channel is removed from channels list.
	 * "reason" may be left blank ("") if no reason is to be given.
	 */
	public boolean leaveChannel(Channel chan, String reason) {

		boolean result = chan.removeClient(this);

		if (result) {
			if (chan.getClientsSize() == 0) {
				// since channel is empty, there is no point in keeping it
				// in a channels list. If we would keep it, the channels list
				// would grow larger and larger in time.
				// We don't want that!
				Channels.removeChannel(chan);
			} else {
				if (!reason.equals("")) {
					reason = " " + reason;
				}
				for (int i = 0; i < chan.getClientsSize(); i++) {
					chan.getClient(i).sendLine(new StringBuilder("LEFT ")
							.append(chan.name).append(" ")
							.append(this.account.getName())
							.append(reason).toString());
				}
			}
			this.channels.remove(chan);
		}

		return result;
	}

	/**
	 * Calls leaveChannel() for every channel client is participating in.
	 * Also notifies all clients of his departure.
	 * Also see comments for leaveChannel() method.
	 */
	public void leaveAllChannels(String reason) {

		while (channels.size() != 0) {
			leaveChannel(channels.get(0), reason);
		}
		this.channels.clear();
	}

	/**
	 * Will search the list of channels this user is participating in
	 * and return the specified channel or 'null' if client is not participating
	 * in this channel.
	 */
	public Channel getChannel(String chanName) {

		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).name.equals(chanName)) {
				return channels.get(i);
			}
		}
		return null;
	}

	/**
	 * Tries to send the data from the sendQueue.
	 * @return true if all data has been flushed; false otherwise.
	 */
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
			s_log.fatal("Invalid use of beginFastWrite(). Check your code! Shutting down the server ...");
			TASServer.closeServerAndExit();
		}

		fastWrite = new StringBuilder();
	}

	public void endFastWrite() {

		if (fastWrite == null) {
			s_log.fatal("Invalid use of endFastWrite(). Check your code! Shutting down the server ...");
			TASServer.closeServerAndExit();
		}

		String data = fastWrite.toString();
		fastWrite = null;
		if (data.equals("")) {
			return;
		}
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
