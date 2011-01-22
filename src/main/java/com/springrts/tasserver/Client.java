/*
 * Created on 2005.6.17
 */

package com.springrts.tasserver;


import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Client instance can be seen as a session of a human user, which has logged
 * into the Lobby using his <code>Account</code> info.
 *
 * @see Account
 * @author Betalord
 */
public class Client extends TeamController implements ContextReceiver {

	private static final Logger LOG  = LoggerFactory.getLogger(Client.class);
	/**
	 * Indicates a message is not using an ID
	 * (see protocol description on message/command IDs)
	 */
	public static final int NO_MSG_ID = -1;

	/**
	 * Indicates that no script-password is set.
	 */
	public static final String NO_SCRIPT_PASSWORD = "";

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 */
	private boolean alive = false;
	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 */
	private boolean halfDead = false;

	private Account account;
	/**
	 * External IP
	 */
	private InetAddress ip;
	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 */
	private InetAddress localIp;
	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 */
	private int udpSourcePort;
	private boolean inGame;
	private boolean away;
	private Account.Rank rank;
	private boolean access;
	private boolean bot;
	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 */
	private int battleID;
	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 */
	private int requestedBattleID;
	/**
	 * List of channels user is participating in.
	 */
	private List<Channel> channels = new ArrayList<Channel>();

	private SocketChannel sockChan;
	private SelectionKey selKey;
	private StringBuilder recvBuf;
	/**
	 * This is the message/command ID used when sending command
	 * as described in the "lobby protocol description" document.
	 * Use setSendMsgId() and resetSendMsgId() methods to manipulate it.
	 * NO_MSG_ID means no ID is used.
	 */
	private int myMsgId = NO_MSG_ID;
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
	private long inGameTime;
	/**
	 * Two letter country code, as defined in ISO 3166-1 alpha-2.
	 * "XX" is used for unknown country.
	 */
	private String country;
	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 */
	private int cpu;
	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 */
	private String lobbyVersion;
	/**
	 * How many bytes did this client send over the last recvRecordPeriod
	 * seconds. This is used with anti-flood protection.
	 */
	private long dataOverLastTimePeriod = 0;
	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see System.currentTimeMillis()
	 */
	private long timeOfLastReceive;
	/**
	 * Does the client accept accountIDs in ADDUSER command?
	 */
	private boolean acceptAccountIDs;
	/**
	 * Does the client accept JOINBATTLEREQUEST command?
	 */
	private boolean handleBattleJoinAuthorization;

	private Context context = null;

	/**
	 * Script password for the battle this client currently is in.
	 * If it is not currently in a battle, this is irrelevant.
	 * The script password is used for spoof-protection, which means
	 * someone illegally joining the battle under wrong user-name.
	 */
	private String scriptPassword;

	/**
	 * Does the client accept the scriptPassord argument to the
	 * JOINEDBATTLE command?
	 */
	private boolean scriptPasswordSupported;


	public Client(SocketChannel sockChan) {

		this.alive = true;

		// no info on user/pass, zero access
		this.account = new Account();
		this.sockChan = sockChan;
		this.ip = sockChan.socket().getInetAddress();
		// this fixes the issue with local user connecting to the server at
		// "127.0.0.1", as he can not host battles with that ip
		if (ip.isLoopbackAddress()) {
			InetAddress newIP = Misc.getLocalIpAddress();
			if (newIP != null) {
				ip = newIP;
			} else {
				LOG.warn("Could not resolve local IP address."
						+ " The user may have problems \n"
						+ "with hosting battles.");
			}
		}
		localIp = ip; // will be changed later once the client logs in
		udpSourcePort = 0; // yet unknown
		selKey = null;
		recvBuf = new StringBuilder();
		inGame = false;
		away = false;
		rank = Account.Rank.Newbie;
		access = false;
		bot = false;
		country = IP2Country.getInstance().getCountryCode(ip);
		inGameTime = 0;
		battleID = Battle.NO_BATTLE_ID;
		requestedBattleID = Battle.NO_BATTLE_ID;
		cpu = 0;
		scriptPassword = NO_SCRIPT_PASSWORD;
		scriptPasswordSupported = false;

		timeOfLastReceive = System.currentTimeMillis();
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
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
		if (this.sockChan != other.getSockChan() && (this.sockChan == null || !this.sockChan.equals(other.getSockChan()))) {
			return false;
		}
		return true;
	}

	/**
	 * Any messages sent via sendLine() method will contain this ID.
	 * See "lobby protocol description" document for more info
	 * on message/command IDs.
	 */
	public void setSendMsgId(int msgId) {
		this.myMsgId = msgId;
	}

	public void resetSendMsgId(int msgId) {
		this.myMsgId = msgId;
	}

	/**
	 * Will prefix the message with a msgId value, if it was previously
	 * set via setSendMsgId() method.
	 */
	public boolean sendLine(String text) {
		return sendLine(text, myMsgId);
	}

	/**
	 * @param msgId overrides any previously set message ID,
	 *        use NO_MSG_ID for none.
	 * @see #setSendMsgId(int msgId)
	 */
	public boolean sendLine(String text, int msgId) {
		if (!alive || halfDead) {
			return false;
		}

		// prefix message with a message ID:
		if (msgId != NO_MSG_ID) {
			text = "#" + msgId + " " + text;
		}

		if (fastWrite != null) {
			if (fastWrite.length() != 0) {
				fastWrite.append(Misc.EOL);
			}
			fastWrite.append(text);
			return true;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("[->{}] \"{}\"",
					((account.getAccess() != Account.Access.NONE)
						? account.getName()
						: ip),
					text);
		}

		try {
			// prepare data and add it to the send queue

			String data = text + Misc.EOL;

			if ((sockChan == null) || (!sockChan.isConnected())) {
				LOG.warn("SocketChannel is not ready to be written to."
						+ " Killing the client next loop ...");
				context.getClients().killClientDelayed(this, "Quit: undefined connection error");
				return false;
			}

			ByteBuffer buf;
			try{
				buf = context.getServer().getAsciiEncoder().encode(CharBuffer.wrap(data));
			} catch (CharacterCodingException ex) {
				LOG.warn("Unable to encode message. Killing the client next loop ...", ex);
				context.getClients().killClientDelayed(this, "Quit: undefined encoder error");
				return false;
			}

			if (sendQueue.size() != 0) {
				sendQueue.add(buf);
			} else {
				sendQueue.add(buf);
				boolean empty = tryToFlushData();
				if (!empty) {
					context.getClients().enqueueDelayedData(this);
				}
			}
		} catch (Exception ex) {
			LOG.error("Failed sending data (undefined). Killing the client next loop ...", ex);
			context.getClients().killClientDelayed(this, "Quit: undefined connection error");
			return false;
		}
		return true;
	}

	public void sendWelcomeMessage() {
		sendLine(new StringBuilder(Server.getApplicationName()).append(" ")
				.append(Misc.getAppVersion()).append(" ")
				.append(context.getEngine().getVersion()).append(" ")
				.append(context.getNatHelpServer().getPort()).append(" ")
				.append(context.getServer().isLanMode() ? 1 : 0).toString());
	}

	/** Should only be called by Clients.killClient() method! */
	public void disconnect() {
		if (!alive) {
			LOG.error("PROBLEM DETECTED: disconnecting dead client. Skipping ...");
			return;
		}

		try {
			sockChan.close();
		} catch (Exception ex) {
			LOG.error("Failed disconnecting socket!", ex);
		}

		sockChan = null;
		selKey = null;
	}

	/**
	 * joins client to <chanName> channel. If channel with that name
	 * does not exist, it is created. Method returns channel object as a result.
	 * If client is already in the channel, it returns null as a result.
	 * This method does not check for a correct key in case the channel is
	 * locked, caller of this method should do that before calling it.
	 * This method also does not do any notifying of other clients in the
	 * channel, the caller must do all that.
	 */
	public Channel joinChannel(String chanName) {

		Channel chan = context.getChannels().getChannel(chanName);
		if (chan == null) {
			chan = new Channel(chanName);
			context.getChannels().addChannel(chan);
		} else if (this.channels.indexOf(chan) != -1) {
			// already in the channel
			return null;
		}

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
				context.getChannels().removeChannel(chan);
			} else {
				if (!reason.equals("")) {
					reason = " " + reason;
				}
				for (int i = 0; i < chan.getClientsSize(); i++) {
					chan.getClient(i).sendLine(new StringBuilder("LEFT ")
							.append(chan.getName()).append(" ")
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

		while (!channels.isEmpty()) {
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
			if (channels.get(i).getName().equals(chanName)) {
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
			while (sendQueue.size() != 0) {
				sendQueue.remove();
			}
			return true; // no more data left to be flushed, so return true
		}

		ByteBuffer buf;
		while ((buf = sendQueue.peek()) != null) {
			try {
				sockChan.write(buf);

				if (buf.hasRemaining()) {
					// This happens when send buffer is full and no more data
					// can be written to it.
					// Lets just skip it without removing the packet
					// from the send queue (we will retry sending it later).
					break;
				}
				// remove element from queue (it was sent entirely)
				sendQueue.remove();
			} catch (ClosedChannelException cce) {
				// no point sending the rest to the closed channel
				if (alive) {
					context.getClients().killClientDelayed(this, "Quit: socket channel closed exception");
				}
				break;
			} catch (IOException io) {
				if (alive) {
					context.getClients().killClientDelayed(this, "Quit: socket channel closed exception");
				}
				break;
			}
		}

		return sendQueue.size() == 0;
	}

	public void beginFastWrite() {

		if (fastWrite != null) {
			LOG.error("Invalid use of beginFastWrite()."
					+ " Check your code! Shutting down the server ...");
			context.getServerThread().closeServerAndExit();
		}

		fastWrite = new StringBuilder();
	}

	public void endFastWrite() {

		if (fastWrite == null) {
			LOG.error("Invalid use of endFastWrite()."
					+ " Check your code! Shutting down the server ...");
			context.getServerThread().closeServerAndExit();
		}

		String data = fastWrite.toString();
		fastWrite = null;
		if (data.equals("")) {
			return;
		}
		sendLine(data);
	}


	// various methods dealing with client status

	public boolean isInGame() {
		return inGame;
	}

	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public boolean isAway() {
		return away;
	}

	public void setAway(boolean away) {
		this.away = away;
	}

	public Account.Rank getRank() {
		return rank;
	}

	public void setRank(Account.Rank rank) {
		this.rank = rank;
	}

	public boolean isAccess() {
		return access;
	}

	public void setAccess(boolean access) {
		this.access = access;
	}

	public boolean isBot() {
		return bot;
	}

	public void setBot(boolean bot) {
		this.bot = bot;
	}

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 * @return the alive
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * If false, then this client is not "valid" anymore.
	 * We already killed him and closed his socket.
	 * @param alive the alive to set
	 */
	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 * @return the halfDead
	 */
	public boolean isHalfDead() {
		return halfDead;
	}

	/**
	 * When we schedule client for kill (via Clients.killClientDelayed(),
	 * for example) this flag is set to true.
	 * When true, we do not read or send any data to this client.
	 * @param halfDead the halfDead to set
	 */
	public void setHalfDead(boolean halfDead) {
		this.halfDead = halfDead;
	}

	/**
	 * @return the account
	 */
	public Account getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(Account account) {
		this.account = account;
	}

	/**
	 * External IP
	 * @return the IP
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * External IP
	 * @param ip the IP to set
	 */
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 * @return the localIP
	 */
	public InetAddress getLocalIp() {
		return localIp;
	}

	/**
	 * Local IP, which has to be sent with LOGIN command
	 * The server can not figure out the clients local IP by himself of course.
	 * @param localIp the local IP to set
	 */
	public void setLocalIP(InetAddress localIp) {
		this.localIp = localIp;
	}

	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 * @return the udpSourcePort
	 */
	public int getUdpSourcePort() {
		return udpSourcePort;
	}

	/**
	 * Public UDP source port used with some NAT traversal techniques,
	 * e.g. "hole punching".
	 * @param udpSourcePort the udpSourcePort to set
	 */
	public void setUdpSourcePort(int udpSourcePort) {
		this.udpSourcePort = udpSourcePort;
	}

	/**
	 * See the 'MYSTATUS' command for valid values
	 * @return the status
	 */
	public int getStatus() {

		int status = 0;

		status +=  isInGame() ? 1 : 0;
		status += (isAway()   ? 1 : 0) << 1;
		status +=  getRank().ordinal() << 2;
		status += (isAccess() ? 1 : 0) << 5;
		status += (isBot()    ? 1 : 0) << 6;

		return status;
	}

	/**
	 * See the 'MYSTATUS' command for valid values
	 * @param status the status to set
	 */
	public void setStatus(int status) {

		setInGame( (status & 0x1)        == 1);
		setAway(  ((status & 0x2)  >> 1) == 1);
		// use highest rank if a too high value was specified
		int rankIndex = (status & 0x1C) >> 2;
		Account.Rank newRank = (rankIndex < Account.Rank.values().length)
				? Account.Rank.values()[rankIndex]
				: Account.Rank.values()[Account.Rank.values().length - 1];
		setRank(newRank);
		setAccess(((status & 0x20) >> 5) == 1);
		setBot(   ((status & 0x40) >> 6) == 1);
	}

	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 * @return the battleID
	 */
	public int getBattleID() {
		return battleID;
	}

	/**
	 * ID of the battle in which this client is participating.
	 * Has to be -1 if not participating in any battle.
	 * @param battleID the battleID to set
	 */
	public void setBattleID(int battleID) {

		this.battleID = battleID;
		if (battleID == Battle.NO_BATTLE_ID) {
			setScriptPassword(NO_SCRIPT_PASSWORD);
		}
	}

	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 * @return the requestedBattleID
	 */
	public int getRequestedBattleID() {
		return requestedBattleID;
	}

	/**
	 * ID of the battle which this client is requesting to join.
	 * Must be -1 if not requesting to join any battle.
	 * @param requestedBattleID the requestedBattleID to set
	 */
	public void setRequestedBattleID(int requestedBattleID) {
		this.requestedBattleID = requestedBattleID;
	}

	/**
	 * @return the sockChan
	 */
	public SocketChannel getSockChan() {
		return sockChan;
	}

	/**
	 * @param selKey the selKey to set
	 */
	public void setSelKey(SelectionKey selKey) {
		this.selKey = selKey;
	}

	/**
	 * @return the recvBuf
	 */
	public StringBuilder getRecvBuf() {
		return recvBuf;
	}

	/**
	 * In milliseconds.
	 * Used internally to remember time when the user entered the game.
	 * @see java.lang.System#currentTimeMillis()
	 * @return the inGameTime
	 */
	public long getInGameTime() {
		return inGameTime;
	}

	/**
	 * In milliseconds.
	 * Used internally to remember time when the user entered the game.
	 * @see java.lang.System#currentTimeMillis()
	 * @param inGameTime the inGameTime to set
	 */
	public void setInGameTime(long inGameTime) {
		this.inGameTime = inGameTime;
	}

	/**
	 * Two letter country code, as defined in ISO 3166-1 alpha-2.
	 * "XX" is used for unknown country.
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * Two letter country code, as defined in ISO 3166-1 alpha-2.
	 * "XX" is used for unknown country.
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 * @return the CPU
	 */
	public int getCpu() {
		return cpu;
	}

	/**
	 * In MHz if possible, or in MHz*1.4 if AMD.
	 * 0 means the client can not figure out its CPU speed.
	 * @param cpu the CPU to set
	 */
	public void setCpu(int cpu) {
		this.cpu = cpu;
	}

	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 * @return the lobbyVersion
	 */
	public String getLobbyVersion() {
		return lobbyVersion;
	}

	/**
	 * e.g. "TASClient 1.0" (gets updated when server receives LOGIN command)
	 * @param lobbyVersion the lobbyVersion to set
	 */
	public void setLobbyVersion(String lobbyVersion) {
		this.lobbyVersion = lobbyVersion;
	}

	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see java.lang.System#currentTimeMillis()
	 * @return the timeOfLastReceive
	 */
	public long getTimeOfLastReceive() {
		return timeOfLastReceive;
	}

	/**
	 * Time (in milli-seconds) when we last heard from client
	 * (last data received).
	 * @see java.lang.System#currentTimeMillis()
	 * @param timeOfLastReceive the timeOfLastReceive to set
	 */
	public void setTimeOfLastReceive(long timeOfLastReceive) {
		this.timeOfLastReceive = timeOfLastReceive;
	}

	/**
	 * Returns the clients script password for the battle it currently is in.
	 * If it is not currently in a battle, this is irrelevant.
	 * The script password is used for spoof-protection, which means
	 * someone illegally joining the battle under wrong user-name.
	 */
	public String getScriptPassword() {
		return scriptPassword;
	}

	/**
	 * Sets the clients script password for the battle it currently is in.
	 * If it is not currently in a battle, this is irrelevant.
	 * The script password is used for spoof-protection, which means
	 * someone illegally joining the battle under wrong user-name.
	 */
	public void setScriptPassword(String scriptPassword) {
		this.scriptPassword = scriptPassword;
	}

	/**
	 * Does the client accept accountIDs in ADDUSER command?
	 * @return the acceptAccountIDs
	 */
	public boolean isAcceptAccountIDs() {
		return acceptAccountIDs;
	}

	/**
	 * Does the client accept accountIDs in ADDUSER command?
	 * @param acceptAccountIDs the acceptAccountIDs to set
	 */
	public void setAcceptAccountIDs(boolean acceptAccountIDs) {
		this.acceptAccountIDs = acceptAccountIDs;
	}

	/**
	 * Does the client accept JOINBATTLEREQUEST command?
	 * @return the handleBattleJoinAuthorization
	 */
	public boolean isHandleBattleJoinAuthorization() {
		return handleBattleJoinAuthorization;
	}

	/**
	 * Does the client accept JOINBATTLEREQUEST command?
	 * @param handleBattleJoinAuthorization the handleBattleJoinAuthorization to set
	 */
	public void setHandleBattleJoinAuthorization(boolean handleBattleJoinAuthorization) {
		this.handleBattleJoinAuthorization = handleBattleJoinAuthorization;
	}

	/**
	 * Does the client accept the scriptPassord argument to the
	 * JOINEDBATTLE command?
	 */
	public boolean isScriptPassordSupported() {
		return scriptPasswordSupported;
	}

	/**
	 * Does the client accept the scriptPassord argument to the
	 * JOINEDBATTLE command?
	 */
	public void setScriptPassordSupported(boolean supported) {
		scriptPasswordSupported = supported;
	}

	/**
	 * How many bytes did this client send over the last recvRecordPeriod
	 * seconds. This is used with anti-flood protection.
	 * @return the dataOverLastTimePeriod
	 */
	public long getDataOverLastTimePeriod() {
		return dataOverLastTimePeriod;
	}

	/**
	 * Reset the amount of bytes this client sent.
	 */
	public void resetDataOverLastTimePeriod() {
		this.dataOverLastTimePeriod = 0;
	}

	/**
	 * @param nBytes to add number of bytes
	 */
	public void addToDataOverLastTimePeriod(long nBytes) {
		this.dataOverLastTimePeriod += nBytes;
	}
}
