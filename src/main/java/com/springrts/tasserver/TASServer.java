/*
 * Created on 2005.6.16
 */

package com.springrts.tasserver;


import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.CommandProcessor;
import com.springrts.tasserver.commands.CommandProcessors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

/**
 * @author Betalord
 */
public class TASServer implements LiveStateListener {

	private final String UPDATE_PROPERTIES_FILENAME = "updates.xml";
	/**
	 * If true, the server will keep a log of all conversations from
	 * the channel #main (in file "MainChanLog.log")
	 */
	//boolean logMainChannel = false;
	//private List<String> whiteList = new LinkedList<String>();
	/**
	 * In this interval, all channel mute lists will be checked
	 * for expirations and purged accordingly. In milliseconds.
	 */
	private long purgeMutesInterval = 1000 * 3;
	/**
	 * Time when we last purged mute lists of all channels.
	 * @see System.currentTimeMillis()
	 */
	private long lastMutesPurgeTime = System.currentTimeMillis();
	private static final Log s_log  = LogFactory.getLog(TASServer.class);

	private final int READ_BUFFER_SIZE = 256; // size of the ByteBuffer used to read data from the socket channel. This size doesn't really matter - server will work with any size (tested with READ_BUFFER_SIZE==1), but too small buffer size may impact the performance.
	private final int SEND_BUFFER_SIZE = 8192 * 2; // socket's send buffer size
	private final long MAIN_LOOP_SLEEP = 10L;
	private ServerSocketChannel sSockChan;
	private Selector readSelector;
	private boolean running;
	/**
	 * See
	 * <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/nio/ByteBuffer.html">
	 * ByteBuffer JavaDoc</a> for the difference between direct and non-direct
	 * buffers. In this case, we should use direct buffers. They are also used
	 * by the author of the <code>java.nio</code> chat example (see links) upon
	 * which this code is built on.
	 */
	private ByteBuffer readBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE);

	private Context context = null;
	private CommandProcessors commandProcessors = null;

	/**
	 * In 'updateProperties' we store a list of Spring versions and server responses to them.
	 * We use it when client doesn't have the latest Spring version or the lobby program
	 * and requests an update from us. The XML file should normally contain at least the "default" key
	 * which contains a standard response in case no suitable response is found.
	 * Each text field associated with a key contains a full string that will be send to the client
	 * as a response, so it should contain a full server command.
	 */
	private Properties updateProperties = new Properties();

	private boolean readUpdateProperties(String fileName) {

		boolean success = false;

		FileInputStream fStream = null;
		try {
			fStream = new FileInputStream(fileName);
			updateProperties.loadFromXML(fStream);
			success = true;
		} catch (IOException ex) {
			s_log.warn("Could not find or read from file '" + fileName + "'.", ex);
		} finally {
			if (fStream != null) {
				try {
					fStream.close();
				} catch (IOException ex) {
					s_log.debug("unimportant", ex);
				}
			}
		}

		return success;
	}

	@Override
	public void starting() {
		s_log.info("starting...");
	}
	@Override
	public void started() {

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		DateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss z");

		s_log.info(new StringBuilder("TASServer ")
				.append(Misc.getAppVersion()).append(" started on ")
				.append(dateTimeFormat.format(new Date())).toString());

		createAdminIfNoUsers();
	}

	@Override
	public void stopping() {
		s_log.info("stopping...");
	}
	@Override
	public void stopped() {

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		DateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss z");

		running = false;
		s_log.info("stopped on " + dateTimeFormat.format(new Date()));
	}

	private boolean startServer() {

		context.starting();

		int port = context.getServer().getPort();

		try {
			context.getServer().setCharset("ISO-8859-1"); // initializes asciiDecoder and asciiEncoder

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
			s_log.error(new StringBuilder("Could not listen on port: ").append(port).toString(), e);
			return false;
		}

		s_log.info(new StringBuilder("Listening for connections on TCP port ")
				.append(port)
				.append(" ...").toString());

		context.started();

		return true;
	}

	private void acceptNewConnections() {
		try {
			SocketChannel clientChannel;
			// since sSockChan is non-blocking, this will return immediately
			// regardless of whether there is a connection available
			while ((clientChannel = sSockChan.accept()) != null) {
				if (context.getServer().isRedirectActive()) {
					if (s_log.isDebugEnabled()) {
						s_log.debug(new StringBuilder("Client redirected to ")
								.append(context.getServer().getRedirectAddress().getHostAddress()).append(": ")
								.append(clientChannel.socket().getInetAddress().getHostAddress()).toString());
					}
					redirectAndKill(clientChannel.socket());
					continue;
				}

				Client client = context.getClients().addNewClient(clientChannel, readSelector, SEND_BUFFER_SIZE);
				if (client == null) {
					continue;
				}

				// from this point on, we know that client has been successfully connected
				client.sendWelcomeMessage();

				if (s_log.isDebugEnabled()) {
					s_log.debug(new StringBuilder("New client connected: ").append(client.getIp()).toString());
				}
			}
		} catch (IOException ioe) {
			s_log.error(new StringBuilder("Error during accept(): ").append(ioe.toString()).toString(), ioe);
		} catch (Exception e) {
			s_log.error(new StringBuilder("Exception in acceptNewConnections()").append(e.toString()).toString(), e);
		}
	}

	private void readIncomingMessages() {
		Client client = null;

		try {
			// non-blocking select, returns immediately regardless of how many keys are ready
			readSelector.selectNow();

			// fetch the keys
			Set<SelectionKey> readyKeys = readSelector.selectedKeys();

			// run through the keys and process each one
			while (!readyKeys.isEmpty()) {
				SelectionKey key = readyKeys.iterator().next();
				readyKeys.remove(key);
				SocketChannel channel = (SocketChannel) key.channel();
				client = (Client) key.attachment();
				if (client.isHalfDead()) {
					continue;
				}
				readBuffer.clear();

				client.setTimeOfLastReceive(System.currentTimeMillis());

				// read from the channel into our buffer
				long nbytes = channel.read(readBuffer);
				client.addToDataOverLastTimePeriod(nbytes);

				// basic anti-flood protection:
				if (context.getServer().getFloodProtection().isFlooding(client)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(")").toString());
					context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append("). User has been kicked.").toString());
					context.getClients().killClient(client, "Disconnected due to excessive flooding");

					// add server notification:
					ServerNotification sn = new ServerNotification("Flooding detected");
					sn.addLine(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(").").toString());
					sn.addLine("User has been kicked from the server.");
					context.getServerNotifications().addNotification(sn);

					continue;
				}

				// check for end-of-stream
				if (nbytes == -1) {
					if (s_log.isDebugEnabled()) {
						s_log.debug("Socket disconnected - killing client");
					}
					channel.close();
					context.getClients().killClient(client); // will also close the socket channel
				} else {
					// use a CharsetDecoder to turn those bytes into a string
					// and append to client's StringBuilder
					readBuffer.flip();
					String str = context.getServer().getAsciiDecoder().decode(readBuffer).toString();
					readBuffer.clear();
					client.getRecvBuf().append(str);

					// check for a full line
					String line = client.getRecvBuf().toString();
					while ((line.indexOf('\n') != -1) || (line.indexOf('\r') != -1)) {
						int pos = line.indexOf('\r');
						int npos = line.indexOf('\n');
						if (pos == -1 || ((npos != -1) && (npos < pos))) {
							pos = npos;
						}
						String command = line.substring(0, pos);
						while (pos + 1 < line.length() && (line.charAt(pos + 1) == '\r' || line.charAt(pos + 1) == '\n')) {
							++pos;
						}
						client.getRecvBuf().delete(0, pos + 1);

						long time = System.currentTimeMillis();
						tryToExecCommand(command, client);
						time = System.currentTimeMillis() - time;
						if (time > 200) {
							context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: (DEBUG) User <")
									.append(client.getAccount().getName()).append("> caused ")
									.append(time).append(" ms load on the server. Command issued: ")
									.append(command).toString());
						}

						if (!client.isAlive()) {
							break; // in case client was killed within tryToExecCommand() method
						}
						line = client.getRecvBuf().toString();
					}
				}
			}
		} catch (IOException ioe) {
			s_log.info("exception during select(): possibly due to force disconnect. Killing the client ...");
			try {
				if (client != null) {
					context.getClients().killClient(client, "Quit: connection lost");
				}
			} catch (Exception e) {
				// do nothing
			}
			s_log.debug("... the exception was:", ioe);
		} catch (Exception e) {
			s_log.info("exception in readIncomingMessages(): killing the client ... ", e);
			try {
				if (client != null) {
					context.getClients().killClient(client, "Quit: connection lost");
				}
			} catch (Exception ex) {
				// do nothing
			}
			s_log.debug("... the exception was:", e);
		}
	}

	public boolean redirectAndKill(Socket socket) {
		if (!context.getServer().isRedirectActive()) {
			return false;
		}
		try {
			(new PrintWriter(socket.getOutputStream(), true)).println("REDIRECT " + context.getServer().getRedirectAddress().getHostAddress());
			socket.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/* Note: this method is not synchronized!
	 * Note2: this method may be called recursively! */
	public boolean tryToExecCommand(String command, Client client) {
		command = command.trim();
		if (command.equals("")) {
			return false;
		}

		if (s_log.isDebugEnabled()) {
			if (client.getAccount().getAccess() != Account.Access.NONE) {
				s_log.debug(new StringBuilder("[<-")
						.append(client.getAccount().getName()).append("] \"")
						.append(command).append("\"").toString());
			} else {
				s_log.debug(new StringBuilder("[<-")
						.append(client.getIp()).append("] \"")
						.append(command).append("\"").toString());
			}
		}

		int msgId = Client.NO_MSG_ID;
		if (command.charAt(0) == '#') {
			try {
				if (!command.matches("^#\\d+\\s[\\s\\S]*")) {
					return false; // malformed command
				}
				msgId = Integer.parseInt(command.substring(1).split("\\s")[0]);
				// remove id field from the rest of command:
				command = command.replaceFirst("#\\d+\\s", "");
			} catch (NumberFormatException e) {
				return false; // this means that the command is malformed
			} catch (PatternSyntaxException e) {
				return false; // this means that the command is malformed
			}
		}

		// parse command into tokens:
		String[] commands = command.split(" ");
		commands[0] = commands[0].toUpperCase();

		client.setSendMsgId(msgId);

		try {
			CommandProcessor cp = commandProcessors.get(commands[0]);
			if (cp != null) {
				List<String> args = new ArrayList<String>(Arrays.asList(commands));
				args.remove(0);
				try {
					boolean ret = cp.process(client, args);
					if (!ret) {
						return false;
					}
				} catch (CommandProcessingException ex) {
					s_log.debug(cp.getClass().getCanonicalName() +
							" failed to handle command from client: \"" +
							Misc.makeSentence(commands) + "\"", ex);
					return false;
				}
			} else if (commands[0].equals("WHITELIST")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}
//				if (commands.length == 2) {
//					whiteList.add(commands[1]);
//					client.sendLine("SERVERMSG IP successfully whitelisted from REGISTER constraints");
//				} else {
//					client.sendLine(new StringBuilder("SERVERMSG Whitelist is: ").append(whiteList.toString()).toString());
//				}
				client.sendLine("SERVERMSG IP white-listing is disabled");
			} else if (commands[0].equals("UNWHITELIST")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}
//				if (commands.length == 2) {
//					client.sendLine((whiteList.remove(commands[1])) ? "SERVERMSG IP removed from whitelist" : "SERVERMSG IP not in whitelist");
//				} else {
//					client.sendLine("SERVERMSG Bad command- UNWHITELIST IP");
//				}
				client.sendLine("SERVERMSG IP white-listing is disabled");
			} else if (commands[0].equals("STOPSERVER")) {
				// stop server gracefully:
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				running = false;
			} else if (commands[0].equals("FORGEREVERSEMSG")) {
				/* this command is used only for debugging purposes. It forces server to process
				 * string passed to this command as if it were sent by the user specified
				 * in this command. */
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}
				if (commands.length < 3) {
					return false;
				}

				Client targetClient = context.getClients().getClient(commands[1]);
				if (targetClient == null) {
					return false;
				}

				tryToExecCommand(Misc.makeSentence(commands, 2), targetClient);
			} else if (commands[0].equals("RETRIEVELATESTBANLIST")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				client.sendLine("SERVERMSG Fetching ban entries is not needed anymore. Therefore, this is a no-op now.");
			} else if (commands[0].equals("RELOADUPDATEPROPERTIES")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				if (readUpdateProperties(UPDATE_PROPERTIES_FILENAME)) {
					s_log.info("\"Update properties\" read from " + UPDATE_PROPERTIES_FILENAME);
					client.sendLine("SERVERMSG \"Update properties\" have been successfully loaded from " + UPDATE_PROPERTIES_FILENAME);
				} else {
					client.sendLine(new StringBuilder("SERVERMSG Unable to load \"Update properties\" from ")
							.append(UPDATE_PROPERTIES_FILENAME).append("!").toString());
				}
			} else if (commands[0].equals("OUTPUTDBDRIVERSTATUS")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				client.sendLine("SERVERMSG This command is not supported anymore, as JPA is used for DB access for bans. Therefore, this is a no-op now.");
			} else if (commands[0].equals("CHANNELS")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				context.getChannels().sendChannelListToClient(client);
			} else if (commands[0].equals("REQUESTUPDATEFILE")) {
				//***if (client.account.getAccess() > Account.Access.NONE) return false;
				if (commands.length < 2) {
					return false;
				}

				String version = Misc.makeSentence(commands, 1);
				String response = updateProperties.getProperty(version);
				if (response == null) {
					response = updateProperties.getProperty("default"); // use general response ("default"), if it exists.
				}				// if still no response has been found, use some default response:
				if (response == null) {
					response = "SERVERMSGBOX No update available. Please download the latest version of the software from official Spring web site: http://spring.clan-sy.com";
				}

				// send a response to the client:
				client.sendLine(response);

				// kill client if no update has been found for him:
				if (response.substring(0, 12).toUpperCase().equals("SERVERMSGBOX")) {
					context.getClients().killClient(client);
				}
			} else if (commands[0].equals("UPDATEBATTLEINFO")) {
				if (commands.length < 5) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder may change battle parameters!
				}
				int spectatorCount = 0;
				boolean locked;
				int maphash;
				try {
					spectatorCount = Integer.parseInt(commands[1]);
					locked = Misc.strToBool(commands[2]);
					maphash = Integer.decode(commands[3]);
				} catch (NumberFormatException e) {
					return false;
				}

				bat.setMapName(Misc.makeSentence(commands, 4));
				bat.setLocked(locked);
				bat.setMapHash(maphash);
				context.getClients().sendToAllRegisteredUsers(new StringBuilder("UPDATEBATTLEINFO ")
						.append(bat.getId()).append(" ")
						.append(spectatorCount).append(" ")
						.append(Misc.boolToStr(bat.isLocked())).append(" ")
						.append(maphash).append(" ")
						.append(bat.getMapName()).toString());
			} else if (commands[0].equals("HANDICAP")) {
				if (commands.length != 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can change handicap value of another user
				}
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false;
				}
				if ((value < 0) || (value > 100)) {
					return false;
				}

				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}
				if (!bat.isClientInBattle(target)) {
					return false;
				}

				target.setBattleStatus(Misc.setHandicapOfBattleStatus(target.getBattleStatus(), value));
				bat.notifyClientsOfBattleStatus(target);
			} else if (commands[0].equals("KICKFROMBATTLE")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can kick other clients
				}
				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}
				if (!bat.isClientInBattle(target)) {
					return false;
				}

				bat.sendToAllClients(new StringBuilder("SAIDBATTLEEX ")
						.append(client.getAccount().getName()).append(" kicked ")
						.append(target.getAccount().getName()).append(" from battle").toString());
				// notify client that he was kicked from the battle:
				target.sendLine("FORCEQUITBATTLE");
				// force client to leave battle:
				tryToExecCommand("LEAVEBATTLE", target);
			} else if (commands[0].equals("FORCETEAMNO")) {
				if (commands.length != 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can force team/ally numbers
				}
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false;
				}
				if ((value < 0) || (value > context.getEngine().getMaxTeams() - 1)) {
					return false;
				}

				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}
				if (!bat.isClientInBattle(target)) {
					return false;
				}

				target.setBattleStatus(Misc.setTeamNoOfBattleStatus(target.getBattleStatus(), value));
				bat.notifyClientsOfBattleStatus(target);
			} else if (commands[0].equals("FORCEALLYNO")) {
				if (commands.length != 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can force team/ally numbers
				}
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false;
				}
				if ((value < 0) || (value > context.getEngine().getMaxTeams() - 1)) {
					return false;
				}

				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}
				if (!bat.isClientInBattle(target)) {
					return false;
				}

				target.setBattleStatus(Misc.setAllyNoOfBattleStatus(target.getBattleStatus(), value));
				bat.notifyClientsOfBattleStatus(target);
			} else if (commands[0].equals("FORCETEAMCOLOR")) {
				if (commands.length != 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can force team color change
				}
				int value;
				try {
					value = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false;
				}

				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}
				if (!bat.isClientInBattle(target)) {
					return false;
				}

				target.setTeamColor(value);
				bat.notifyClientsOfBattleStatus(target);
			} else if (commands[0].equals("FORCESPECTATORMODE")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can force spectator mode
				}
				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}
				if (!bat.isClientInBattle(target)) {
					return false;
				}

				if (Misc.getModeFromBattleStatus(target.getBattleStatus()) == 0) {
					return false; // no need to change it, it's already set to spectator mode!
				}
				target.setBattleStatus(Misc.setModeOfBattleStatus(target.getBattleStatus(), 0));
				bat.notifyClientsOfBattleStatus(target);
			} else if (commands[0].equals("ADDBOT")) {
				if (commands.length < 5) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

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

				Bot bot = new Bot(commands[1], client.getAccount().getName(), Misc.makeSentence(commands, 4), value, teamColor);
				bat.addBot(bot);

				bat.sendToAllClients(new StringBuilder("ADDBOT ")
						.append(bat.getId()).append(" ")
						.append(bot.getName()).append(" ")
						.append(bot.getOwnerName()).append(" ")
						.append(bot.getBattleStatus()).append(" ")
						.append(bot.getTeamColor()).append(" ")
						.append(bot.getSpecifier()).toString());

			} else if (commands[0].equals("REMOVEBOT")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				Bot bot = bat.getBot(commands[1]);
				if (bot == null) {
					return false;
				}

				bat.removeBot(bot);

				bat.sendToAllClients(new StringBuilder("REMOVEBOT ")
						.append(bat.getId()).append(" ")
						.append(bot.getName()).toString());
			} else if (commands[0].equals("UPDATEBOT")) {
				if (commands.length != 4) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				Bot bot = bat.getBot(commands[1]);
				if (bot == null) {
					return false;
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

				// only bot owner and battle host are allowed to update bot:
				if (!((client.getAccount().getName().equals(bot.getOwnerName())) || (client.getAccount().getName().equals(bat.getFounder().getAccount().getName())))) {
					return false;
				}

				bot.setBattleStatus(value);
				bot.setTeamColor(teamColor);

				//*** add: force ally and color number if someone else is using his team number already

				bat.sendToAllClients(new StringBuilder("UPDATEBOT ")
						.append(bat.getId()).append(" ")
						.append(bot.getName()).append(" ")
						.append(bot.getBattleStatus()).append(" ")
						.append(bot.getTeamColor()).toString());
			} else if (commands[0].equals("DISABLEUNITS")) {
				if (commands.length < 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can disable/enable units
				}
				// let's check if client didn't double the data (he shouldn't, but we can't
				// trust him, so we will check ourselves):
				for (int i = 1; i < commands.length; i++) {
					if (bat.getDisabledUnits().indexOf(commands[i]) != -1) {
						continue;
					}
					bat.getDisabledUnits().add(commands[i]);
				}

				bat.sendToAllExceptFounder(command);
			} else if (commands[0].equals("ENABLEUNITS")) {
				if (commands.length < 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can disable/enable units
				}
				for (int i = 1; i < commands.length; i++) {
					bat.getDisabledUnits().remove(commands[i]); // will ignore it if string is not found in the list
				}

				bat.sendToAllExceptFounder(command);
			} else if (commands[0].equals("ENABLEALLUNITS")) {
				if (commands.length != 1) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				}
				if (bat.getFounder() != client) {
					return false; // only founder can disable/enable units
				}
				bat.getDisabledUnits().clear();

				bat.sendToAllExceptFounder(command);
			} else if (commands[0].equals("RING")) {
				if (commands.length != 2) {
					return false;
				}
				// privileged users can ring anyone, "normal" users can ring only when they are hosting
				// and only clients who are participating in their battle
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.PRIVILEGED) < 0) { // normal user
					Client target = context.getClients().getClient(commands[1]);
					if (target == null) {
						return false;
					}

					if (client.getBattleID() == Battle.NO_BATTLE_ID) {
						client.sendLine("SERVERMSG RING command failed: You can only ring players participating in your own battle!");
						return false;
					}

					Battle bat = context.getBattles().getBattleByID(client.getBattleID());
					context.getBattles().verify(bat);

					if (!bat.isClientInBattle(commands[1])) {
						client.sendLine("SERVERMSG RING command failed: You don't have permission to ring players other than those participating in your battle!");
						return false;
					}

					// only host can ring players participating in his own battle, unless target is host himself:
					if ((client != bat.getFounder()) && (target != bat.getFounder())) {
						client.sendLine("SERVERMSG RING command failed: You can ring only battle host, or if you are the battle host, only players participating in your own battle!");
						return false;
					}

					target.sendLine(new StringBuilder("RING ").append(client.getAccount().getName()).toString());
				} else { // privileged user
					Client target = context.getClients().getClient(commands[1]);
					if (target == null) {
						return false;
					}

					target.sendLine(new StringBuilder("RING ").append(client.getAccount().getName()).toString());
				}
			} else if (commands[0].equals("ADDSTARTRECT")) {
				if (commands.length != 6) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				if (bat.getFounder() != client) {
					return false;
				}

				int allyno, left, top, right, bottom;
				try {
					allyno = Integer.parseInt(commands[1]);
					left = Integer.parseInt(commands[2]);
					top = Integer.parseInt(commands[3]);
					right = Integer.parseInt(commands[4]);
					bottom = Integer.parseInt(commands[5]);
				} catch (NumberFormatException e) {
					client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
							.append(commands[0]).append(" command). You will now be disconnected ...").toString());
					context.getClients().killClient(client, "Quit: inconsistent data");
					return false;
				}

				StartRect startRect = bat.getStartRects().get(allyno);
				if (startRect.isEnabled()) {
					client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
							.append(commands[0]).append(" command). You will now be disconnected ...").toString());
					context.getClients().killClient(client, "Quit: inconsistent data");
					return false;
				}

				startRect.setEnabled(true);
				startRect.setLeft(left);
				startRect.setTop(top);
				startRect.setRight(right);
				startRect.setBottom(bottom);

				bat.sendToAllExceptFounder(new StringBuilder("ADDSTARTRECT ")
						.append(allyno).append(" ")
						.append(left).append(" ")
						.append(top).append(" ")
						.append(right).append(" ")
						.append(bottom).toString());
			} else if (commands[0].equals("REMOVESTARTRECT")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				if (bat.getFounder() != client) {
					return false;
				}

				int allyno;
				try {
					allyno = Integer.parseInt(commands[1]);
				} catch (NumberFormatException e) {
					client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
							.append(commands[0]).append(" command). You will now be disconnected ...").toString());
					context.getClients().killClient(client, "Quit: inconsistent data");
					return false;
				}

				StartRect startRect = bat.getStartRects().get(allyno);
				if (!startRect.isEnabled()) {
					client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
							.append(commands[0]).append(" command). You will now be disconnected ...").toString());
					context.getClients().killClient(client, "Quit: inconsistent data");
					return false;
				}

				startRect.setEnabled(false);

				bat.sendToAllExceptFounder(new StringBuilder("REMOVESTARTRECT ").append(allyno).toString());
			} else if (commands[0].equals("SCRIPTSTART")) {
				if (commands.length != 1) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				bat.getTempReplayScript().clear();
			} else if (commands[0].equals("SCRIPT")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				bat.getTempReplayScript().add(Misc.makeSentence(commands, 1));
			} else if (commands[0].equals("SCRIPTEND")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				// copy temp script to active script:
				bat.ratifyTempScript();

				bat.sendScriptToAllExceptFounder();
			} else if (commands[0].equals("SETSCRIPTTAGS")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				if (bat.getFounder() != client) {
					return false;
				}

				if (commands.length < 2) {
					// kill client since it is not using this command correctly
					client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
							.append(commands[0]).append(" command). You will now be disconnected ...").toString());
					context.getClients().killClient(client, "Quit: inconsistent data");
					return false;
				}

				int pairsStart = command.indexOf(' ');
				if (pairsStart < 0) {
					return false;
				}
				String[] pairs = command.substring(pairsStart + 1).split("\t");
				StringBuilder validPairs = new StringBuilder();

				for (int i = 0; i < pairs.length; i++) {

					String s = pairs[i];

					int equalPos = s.indexOf('=');
					if (equalPos < 1) {
						continue;
					}

					// parse the key
					String key = s.substring(0, equalPos).toLowerCase();
					if (key.length() <= 0) {
						continue;
					}
					if (key.indexOf(' ') >= 0) {
						continue;
					}
					if (key.indexOf('=') >= 0) {
						continue;
					}
					if (key.indexOf(';') >= 0) {
						continue;
					}
					if (key.indexOf('{') >= 0) {
						continue;
					}
					if (key.indexOf('}') >= 0) {
						continue;
					}
					if (key.indexOf('[') >= 0) {
						continue;
					}
					if (key.indexOf(']') >= 0) {
						continue;
					}
					if (key.indexOf('\n') >= 0) {
						continue;
					}
					if (key.indexOf('\r') >= 0) {
						continue;
					}

					// parse the value
					String value = s.substring(equalPos + 1);
					if (value.equals(value.trim())) {
						continue;
					} // forbid trailing/leading spaces
					if (value.indexOf(';') >= 0) {
						continue;
					}
					if (value.indexOf('}') >= 0) {
						continue;
					}
					if (value.indexOf('[') >= 0) {
						continue;
					}
					if (value.indexOf('\n') >= 0) {
						continue;
					}
					if (value.indexOf('\r') >= 0) {
						continue;
					}

					// insert the tag data into the map
					bat.getScriptTags().put(key, value);

					// add to the validPairs string
					if (validPairs.length() > 0) {
						validPairs.append("\t");
					}
					validPairs.append(key).append("=").append(value);
				}

				// relay the valid pairs
				if (validPairs.length() > 0) {
					bat.sendToAllClients(new StringBuilder("SETSCRIPTTAGS ").append(validPairs).toString());
				}
			} else if (commands[0].equals("REMOVESCRIPTTAGS")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				context.getBattles().verify(bat);

				if (bat.getFounder() != client) {
					return false;
				}

				if (commands.length < 2) {
					// kill client since it is not using this command correctly
					client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
							.append(commands[0]).append(" command). You will now be disconnected ...").toString());
					context.getClients().killClient(client, "Quit: inconsistent data");
					return false;
				}

				StringBuilder loweyKeyCommand = new StringBuilder("REMOVESCRIPTTAGS");
				for (int i = 1; i < commands.length; i++) {
					String lowerKey = commands[i].toLowerCase();
					loweyKeyCommand.append(" ").append(lowerKey);
					bat.getScriptTags().remove(lowerKey);
				}

				// relay the command
				bat.sendToAllClients(loweyKeyCommand.toString());
			} else {
				// unknown command!
				return false;
			}
		} finally {
			client.setSendMsgId(Client.NO_MSG_ID);
		}


		return true;
	} // tryToExecCommand()

	/**
	 * Adds a default administrator account, if not running in LAN mode,
	 * and if there are no accounts in the active accounts service.
	 */
	private void createAdminIfNoUsers() {

		if (!context.getServer().isLanMode()) {
			AccountsService accountsService = context.getAccountsService();
			if (accountsService.getAccountsSize() == 0) {
				String username = "admin";
				String password = "admin";
				s_log.info("As there are no accounts yet, we are creating an admin account: username=\"" + username + "\", password=\"" + password + "\"");
				Account admin = createAdmin(username, password);
				accountsService.addAccount(admin);
				accountsService.saveAccountsIfNeeded();
			}
		}
	}

	/**
	 * Creates a simple account with administrator rights.
	 */
	private static Account createAdmin(String username, String password) {

		Account admin = new Account(username, Misc.encodePassword(password), Account.NO_ACCOUNT_LAST_IP, Account.NO_ACCOUNT_LAST_COUNTRY);
		admin.setAccess(Account.Access.ADMIN);
		return admin;
	}

	public TASServer(Context context) {

		this.context = context;
		this.commandProcessors = new CommandProcessors();

		context.addLiveStateListener(this);
		context.addContextReceiver(commandProcessors);

		AccountsService accountsService = null;
		if (!context.getServer().isLanMode() && context.getServer().isUseUserDB()) {
			accountsService = new JPAAccountsService();
		} else {
			accountsService = new FSAccountsService();
		}
		context.setAccountsService(accountsService);

		BanService banService = null;
		if (!context.getServer().isLanMode()) {
			try {
				banService = new JPABanService();
			} catch (Exception pex) {
				banService = new DummyBanService();
				s_log.warn("Failed to access database for ban entries, bans are not supported!", pex);
			}
		} else {
			banService = new DummyBanService();
		}
		context.setBanService(banService);

		context.push();

		commandProcessors.init();

		// switch to LAN mode if user accounts information is not present:
		if (!context.getServer().isLanMode()) {
			if (!context.getAccountsService().isReadyToOperate()) {
				s_log.warn("Accounts service not ready, switching to \"LAN mode\" ...");
				context.getServer().setLanMode(true);
			}
		}

		if (!context.getServer().isLanMode()) {
			context.getAccountsService().loadAccounts();
			context.getBanService();
			context.getAgreement().read();
		} else {
			s_log.info("LAN mode enabled");
		}

		context.getMessageOfTheDay().read();
		context.getServer().setStartTime(System.currentTimeMillis());

		if (readUpdateProperties(UPDATE_PROPERTIES_FILENAME)) {
			s_log.info(new StringBuilder("\"Update properties\" read from ").append(UPDATE_PROPERTIES_FILENAME).toString());
		}

		long tempTime = System.currentTimeMillis();
		if (IP2Country.getInstance().initializeAll()) {
			tempTime = System.currentTimeMillis() - tempTime;
			s_log.info(new StringBuilder("<IP2Country> loaded in ")
					.append(tempTime).append(" ms.").toString());
		}

		// start "help UDP" server:
		context.getNatHelpServer().startServer();

		// start server:
		if (!startServer()) {
			context.getServer().closeServerAndExit();
		}

		// add server notification:
		ServerNotification sn = new ServerNotification("Server started");
		sn.addLine(new StringBuilder("Server has been started on port ")
				.append(context.getServer().getPort()).append(". There are ")
				.append(context.getAccountsService().getAccountsSize())
				.append(" accounts currently loaded. See server log for more info.").toString());
		context.getServerNotifications().addNotification(sn);

		running = true;
		while (running) { // main loop

			// check for new client connections
			acceptNewConnections();

			// check for incoming messages
			readIncomingMessages();

			// flush any data that is waiting to be sent
			context.getClients().flushData();

			// reset received bytes count every n seconds
			if (context.getServer().getFloodProtection().hasFloodCheckPeriodPassed()) {
				for (int i = 0; i < context.getClients().getClientsSize(); i++) {
					context.getClients().getClient(i).setDataOverLastTimePeriod(0);
				}
			}

			// check for timeouts:
			Collection<Client> timedOutClients = context.getServer().getTimedOutClients();
			for (Client client : timedOutClients) {
				if (client.isHalfDead()) {
					continue; // already scheduled for kill
				}
				s_log.warn(new StringBuilder("Timeout detected from ")
						.append(client.getAccount().getName()).append(" (")
						.append(client.getIp()).append("). Client has been scheduled for kill ...").toString());
				context.getClients().killClientDelayed(client, "Quit: timeout");
			}

			// kill all clients scheduled to be killed:
			context.getClients().processKillList();

			context.getStatistics().update();

			// check UDP server for any new packets:
			DatagramPacket packet;
			while ((packet = context.getNatHelpServer().fetchNextPackage()) != null) {
				InetAddress address = packet.getAddress();
				int p = packet.getPort();
				String data = new String(packet.getData(), packet.getOffset(), packet.getLength());
				if (s_log.isDebugEnabled()) {
					s_log.debug(new StringBuilder("*** UDP packet received from ")
							.append(address.getHostAddress()).append(" from port ")
							.append(p).toString());
				}
				Client client = context.getClients().getClient(data);
				if (client == null) {
					continue;
				}
				client.setUdpSourcePort(p);
				client.sendLine(new StringBuilder("UDPSOURCEPORT ").append(p).toString());
			}

			// save accounts info to disk on regular intervals:
			context.getAccountsService().saveAccountsIfNeeded();

			// purge mute lists of all channels on regular intervals:
			if (System.currentTimeMillis() - lastMutesPurgeTime > purgeMutesInterval) {
				lastMutesPurgeTime = System.currentTimeMillis();
				for (int i = 0; i < context.getChannels().getChannelsSize(); i++) {
					context.getChannels().getChannel(i).getMuteList().clearExpiredOnes();
				}
			}

			// sleep a bit
			try {
				Thread.sleep(MAIN_LOOP_SLEEP);
			} catch (InterruptedException ie) {
			}
		}

		// close everything:
		if (!context.getServer().isLanMode()) {
			context.getAccountsService().saveAccounts(true);
		}
		if (context.getNatHelpServer().isRunning()) {
			context.getNatHelpServer().stopServer();
		}

		// add server notification:
		sn = new ServerNotification("Server stopped");
		sn.addLine("Server has just been stopped gracefully. See server log for more info.");
		context.getServerNotifications().addNotification(sn);

		s_log.info("Server closed gracefully!");
	}
}
