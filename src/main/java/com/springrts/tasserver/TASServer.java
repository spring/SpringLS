/*
 * Created on 2005.6.16
 */

package com.springrts.tasserver;


import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.CommandProcessor;
import com.springrts.tasserver.commands.CommandProcessors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
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

	/**
	 * Agreement which is sent to user upon first login.
	 * User must send CONFIRMAGREEMENT command to confirm the agreement
	 * before the server allows him to log in.
	 * See LOGIN command implementation for more details.
	 */
	private String agreement = "";
	private final String AGREEMENT_FILENAME = "agreement.rtf";
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
	/**
	 * Here we store information on latest failed login attempts.
	 * We use it to block users from brute-forcing other accounts.
	 */
	private List<FailedLoginAttempt> failedLoginAttempts = new ArrayList<FailedLoginAttempt>();
	/** Time when we last purged list of failed login attempts */
	private long lastFailedLoginsPurgeTime = System.currentTimeMillis();
	private static final Log s_log  = LogFactory.getLog(TASServer.class);

	private final int READ_BUFFER_SIZE = 256; // size of the ByteBuffer used to read data from the socket channel. This size doesn't really matter - server will work with any size (tested with READ_BUFFER_SIZE==1), but too small buffer size may impact the performance.
	private final int SEND_BUFFER_SIZE = 8192 * 2; // socket's send buffer size
	private final long MAIN_LOOP_SLEEP = 10L;
	private long maxChatMessageLength = 1024; // used with basic anti-flood protection. Any chat messages (channel or private chat messages) longer than this are considered flooding. Used with following commands: SAY, SAYEX, SAYPRIVATE, SAYBATTLE, SAYBATTLEEX
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

	/** Reads agreement from disk (if file is found) */
	private boolean readAgreement() {

		boolean success = false;

		String newAgreement = null;
		try {
			newAgreement = Misc.readTextFile(new File(AGREEMENT_FILENAME));
			if (newAgreement.length() > 2) {
				agreement = newAgreement;
				success = true;
				s_log.info("Using agreement from file '" + AGREEMENT_FILENAME + "'.");
			} else {
				s_log.warn("Agreement in file '" + AGREEMENT_FILENAME + "' is too short.");
			}
		} catch (IOException e) {
			s_log.warn("Could not find or read from file '" + AGREEMENT_FILENAME + "'. Using no agreement.");
			s_log.debug("... reason:", e);
		}

		return success;
	}

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

	private void verifyBattle(Battle battle) {

		if (battle == null) {
			s_log.fatal("Invalid battle ID. Server will now exit!");
			context.getServer().closeServerAndExit();
		}
	}

	private Account verifyLogin(String user, String pass) {
		Account acc = context.getAccountsService().getAccount(user);
		if (acc == null) {
			return null;
		}
		if (acc.getPassword().equals(pass)) {
			return acc;
		} else {
			return null;
		}
	}

	private void recordFailedLoginAttempt(String username) {
		FailedLoginAttempt attempt = findFailedLoginAttempt(username);
		if (attempt == null) {
			attempt = new FailedLoginAttempt(username, 0, 0);
			failedLoginAttempts.add(attempt);
		}
		attempt.addFailedAttempt();
	}

	/** @return 'null' if no record found */
	private FailedLoginAttempt findFailedLoginAttempt(String username) {
		for (int i = 0; i < failedLoginAttempts.size(); i++) {
			if (failedLoginAttempts.get(i).getUserName().equals(username)) {
				return failedLoginAttempts.get(i);
			}
		}
		return null;
	}

	private void sendAgreementToClient(Client client) {
		client.beginFastWrite();
		String[] sl = agreement.split("\n");
		for (int i = 0; i < sl.length; i++) {
			client.sendLine(new StringBuilder("AGREEMENT ").append(sl[i]).toString());
		}
		client.sendLine("AGREEMENTEND");
		client.endFastWrite();
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

	public void notifyClientJoinedBattle(Client client, Battle bat) {
		// This non-object oriented function is ugly, but Client and Battle classes are made in such a way that
		// they do not handle players notifications, which is made in TASServer class...

		// do the actually joining and notifying:
		client.setBattleStatus(0); // reset client's battle status
		client.setBattleID(bat.getId());
		client.setRequestedBattleID(Battle.NO_BATTLE_ID);
		bat.addClient(client);
	 	// notify client that he has successfully joined the battle
		client.sendLine("JOINBATTLE " + bat.getId() + " " + bat.getHashCode());
		context.getClients().notifyClientsOfNewClientInBattle(bat, client);
		bat.notifyOfBattleStatuses(client);
		bat.sendBotListToClient(client);
		// tell host about this client's ip and UDP source port (if battle is hosted using one of the NAT traversal techniques):
		if ((bat.getNatType() == 1) || (bat.getNatType() == 2)) {
			// make sure that clients behind NAT get local IPs and not external ones:
			bat.getFounder().sendLine("CLIENTIPPORT " + client.getAccount().getName() + " " + (bat.getFounder().getIp().equals(client.getIp()) ? client.getLocalIP() : client.getIp()) + " " + client.getUdpSourcePort());
		}

		client.sendLine("REQUESTBATTLESTATUS");
		bat.sendDisabledUnitsListToClient(client);
		bat.sendStartRectsListToClient(client);
		bat.sendScriptTagsToClient(client);

		if (bat.getType() == 1) {
			bat.sendScriptToClient(client);
		}
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
			} else if (commands[0].equals("TESTLOGIN")) {
				if (commands.length != 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				String userName = commands[1];
				String password = commands[2];

				if (verifyLogin(userName, password) == null) {
					client.sendLine("TESTLOGINDENY");
					return false;
				}

				// We don't check here if agreement bit is set yet,
				// or if user is banned.
				// We only verify if login info is correct
				client.sendLine("TESTLOGINACCEPT");
			} else if (commands[0].equals("SETBOTMODE")) {
				if (commands.length != 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

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

				String userName = commands[1];

				Account acc = context.getAccountsService().getAccount(userName);
				if (acc == null) {
					client.sendLine(new StringBuilder("SERVERMSG User <").append(userName).append("> not found!").toString());
					return false;
				}

				final boolean wasBot = acc.isBot();
				acc.setBot((mode == 0) ? false : true);
				final boolean mergeOk = context.getAccountsService().mergeAccountChanges(acc, acc.getName());
				if (!mergeOk) {
					acc.setBot(wasBot);
					client.sendLine("SERVERMSG SETBOTMODE failed: Failed saving to persistent storage.");
					return false;
				}

				client.sendLine(new StringBuilder("SERVERMSG Bot mode set to ")
						.append(mode).append(" for user <")
						.append(acc.getName()).append(">").toString());
			} else if (commands[0].equals("GETREGISTRATIONDATE")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				String userName = commands[1];

				Account acc = context.getAccountsService().getAccount(userName);
				if (acc == null) {
					client.sendLine(new StringBuilder("SERVERMSG User <").append(userName).append("> not found!").toString());
					return false;
				}

				// As DateFormats are generally not-thread save,
				// we always create a new one.
				DateFormat dateTimeFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");
				client.sendLine(new StringBuilder("SERVERMSG Registration timestamp for <")
						.append(userName).append("> is ")
						.append(acc.getRegistrationDate()).append(" (")
						.append(dateTimeFormat.format(new Date(acc.getRegistrationDate())))
						.append(")").toString());
			} else if (commands[0].equals("SETLATESTSPRINGVERSION")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}
				if (commands.length != 2) {
					client.sendLine("SERVERMSG Bad arguments to SETLATESTSPRINGVERSION command!");
					return false;
				}

				String engineVersion = commands[1];

				context.setEngine(new Engine(engineVersion));

				client.sendLine(new StringBuilder("SERVERMSG Latest spring version has been set to ").append(context.getEngine().getVersion()).toString());
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
			} else if (commands[0].equals("GETUSERID")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				Account acc = context.getAccountsService().getAccount(commands[1]);
				if (acc == null) {
					client.sendLine(new StringBuilder("SERVERMSG User <").append(commands[1]).append("> not found!").toString());
					return false;
				}

				client.sendLine(new StringBuilder("SERVERMSG Last user ID for <")
						.append(commands[1]).append("> was ")
						.append(acc.getLastUserId()).toString());
			} else if (commands[0].equals("GENERATEUSERID")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				Client targetClient = context.getClients().getClient(commands[1]);
				if (targetClient == null) {
					client.sendLine(new StringBuilder("SERVERMSG <").append(commands[1]).append("> not found or is not currently online!").toString());
					return false;
				}
				targetClient.sendLine("ACQUIREUSERID");

				client.sendLine("SERVERMSG ACQUIREUSERID command was dispatched. Server will notify of response via notification system.");
			} else if (commands[0].equals("KILLALL")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				StringBuilder reason = new StringBuilder();
				if (commands.length > 1) {
					reason.append(" (reason: ").append(Misc.makeSentence(commands, 1)).append(")");
				}

				while (context.getClients().getClientsSize() > 0) {
					context.getClients().killClient(context.getClients().getClient(0), (reason.length() == 0 ? "Disconnected by server" : "Disconnected by server: " + reason));
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
			} else if (commands[0].equals("LOGIN")) {
				if (client.getAccount().getAccess() != Account.Access.NONE) {
					client.sendLine("DENIED Already logged in");
					return false; // user with accessLevel > 0 cannot re-login
				}

				if (!context.getServer().isLoginEnabled() && context.getAccountsService().getAccount(commands[1]).getAccess().compareTo(Account.Access.PRIVILEGED) < 0) {
					client.sendLine("DENIED Sorry, logging in is currently disabled");
					return false;
				}

				if (commands.length < 6) {
					client.sendLine("DENIED Bad command arguments");
					return false;
				}

				String[] args2 = Misc.makeSentence(commands, 5).split("\t");
				String lobbyVersion = args2[0];
				int userID = Account.NO_USER_ID;
				if (args2.length > 1) {
					try {
						long temp = Long.parseLong(args2[1], 16);
						userID = (int) temp; // we transform unsigned 32 bit integer to a signed one
					} catch (NumberFormatException e) {
						client.sendLine("DENIED <userID> field should be an integer");
						return false;
					}
				}
				if (args2.length > 2) {
					// prepare the compatibility flags (space separated)
					String compatFlags_str = Misc.makeSentence(args2, 2);
					String[] compatFlags_split = compatFlags_str.split(" ");
					ArrayList<String> compatFlags = new ArrayList<String>(compatFlags_split.length + 1);
					compatFlags.addAll(Arrays.asList(compatFlags_split));
					// split old flags for backwards compatibility,
					// as there were no spaces in the past
					if (compatFlags.remove("ab") || compatFlags.remove("ba")) {
						compatFlags.add("a");
						compatFlags.add("b");
					}

					// handle flags ...
					client.setAcceptAccountIDs(compatFlags.contains("a"));
					client.setHandleBattleJoinAuthorization(compatFlags.contains("b"));
					client.setScriptPassordSupported(compatFlags.contains("sp"));
				}

				int cpu;
				try {
					cpu = Integer.parseInt(commands[3]);
				} catch (NumberFormatException e) {
					client.sendLine("DENIED <cpu> field should be an integer");
					return false;
				}

				if (!context.getServer().isLanMode()) { // "normal", non-LAN mode
					String username = commands[1];
					String password = commands[2];

					// protection from brute-forcing the account:
					FailedLoginAttempt attempt = findFailedLoginAttempt(username);
					if ((attempt != null) && (attempt.getFailedAttempts() >= 3)) {
						client.sendLine("DENIED Too many failed login attempts. Wait for 30 seconds before trying again!");
						recordFailedLoginAttempt(username);
						if (!attempt.isLogged()) {
							attempt.setLogged(true);
							context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Too many failed login attempts for <")
									.append(username).append("> from ")
									.append(client.getIp()).append(". Blocking for 30 seconds. Will not notify any longer.").toString());
							// add server notification:
							ServerNotification sn = new ServerNotification("Excessive failed login attempts");
							sn.addLine(new StringBuilder("Too many failed login attempts for <")
									.append(username).append("> from ")
									.append(client.getIp()).append(". Blocking for 30 seconds.").toString());
							context.getServerNotifications().addNotification(sn);
						}
						return false;
					}

					Account acc = verifyLogin(username, password);
					if (acc == null) {
						client.sendLine("DENIED Bad username/password");
						recordFailedLoginAttempt(username);
						return false;
					}
					if (context.getClients().isUserLoggedIn(acc)) {
						client.sendLine("DENIED Already logged in");
						return false;
					}
					BanEntry ban = context.getBanService().getBanEntry(username, Misc.ip2Long(client.getIp()), userID);
					if (ban != null && ban.isActive()) {
						client.sendLine(new StringBuilder("DENIED You are banned from this server! (Reason: ")
								.append(ban.getPublicReason()).append("). Please contact server administrator.").toString());
						recordFailedLoginAttempt(username);
						return false;
					}
					if ((!acc.isAgreementAccepted()) && (!client.getAccount().isAgreementAccepted()) && (!agreement.equals(""))) {
						sendAgreementToClient(client);
						return false;
					}
					// everything is OK so far!
					if (!acc.isAgreementAccepted()) {
						// user has obviously accepted the agreement... Let's update it
						acc.setAgreementAccepted(true);
						final boolean mergeOk = context.getAccountsService().mergeAccountChanges(acc, acc.getName());
						if (!mergeOk) {
							acc.setAgreementAccepted(false);
							client.sendLine("DENIED Failed saving 'agreement accepted' to persistent storage.");
							return false;
						}
						context.getAccountsService().saveAccounts(false);
					}
					if (acc.getLastLogin() + 5000 > System.currentTimeMillis()) {
						client.sendLine("DENIED This account has already connected in the last 5 seconds");
						return false;
					}
					client.setAccount(acc);
				} else { // lanMode == true
					if (commands[1].equals("")) {
						client.sendLine("DENIED Cannot login with null username");
					}
					Account acc = context.getAccountsService().getAccount(commands[1]);
					if (acc != null) {
						client.sendLine("DENIED Player with same name already logged in");
						return false;
					}
					Account.Access accessLvl = Account.Access.NORMAL;
					if ((commands[1].equals(context.getServer().getLanAdminUsername())) && (commands[2].equals(context.getServer().getLanAdminPassword()))) {
						accessLvl = Account.Access.ADMIN;
					}
					acc = new Account(commands[1], commands[2], "?", "XX");
					acc.setAccess(accessLvl);
					context.getAccountsService().addAccount(acc);
					client.setAccount(acc);
				}

				// set client's status:
				client.setRankToStatus(client.getAccount().getRank().ordinal());
				client.setBotModeToStatus(client.getAccount().isBot());
				client.setAccessToStatus((((client.getAccount().getAccess().compareTo(Account.Access.PRIVILEGED) >= 0) && (!context.getServer().isLanMode())) ? true : false));

				client.setCpu(cpu);
				client.getAccount().setLastLogin(System.currentTimeMillis());
				client.getAccount().setLastCountry(client.getCountry());
				client.getAccount().setLastIP(client.getIp());
				if (commands[4].equals("*")) {
					client.setLocalIP(client.getIp());
				} else {
					client.setLocalIP(commands[4]);
				}
				client.setLobbyVersion(lobbyVersion);
				client.getAccount().setLastUserId(userID);
				final boolean mergeOk = context.getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
				if (!mergeOk) {
					s_log.info(new StringBuilder("Failed saving login info to persistent storage for user: ").append(client.getAccount().getName()).toString());
					return false;
				}

				// do the notifying and all:
				client.sendLine(new StringBuilder("ACCEPTED ").append(client.getAccount().getName()).toString());
				context.getMessageOfTheDay().sendTo(client);
				context.getClients().sendListOfAllUsersToClient(client);
				context.getBattles().sendInfoOnBattlesToClient(client);
				context.getClients().sendInfoOnStatusesToClient(client);
				// notify client that we've finished sending login info:
				client.sendLine("LOGININFOEND");

				// notify everyone about new client:
				context.getClients().notifyClientsOfNewClientOnServer(client);
				context.getClients().notifyClientsOfNewClientStatus(client);

				if (s_log.isDebugEnabled()) {
					s_log.debug(new StringBuilder("User just logged in: ").append(client.getAccount().getName()).toString());
				}
			} else if (commands[0].equals("CONFIRMAGREEMENT")) {
				// update client's temp account (he is not logged in yet since he needs to confirm the agreement before server will allow him to log in):
				client.getAccount().setAgreementAccepted(true);
				final boolean mergeOk = context.getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
				if (!mergeOk) {
					s_log.debug(new StringBuilder("Failed saving 'agreement accepted' state to persistent storage for user: ").append(client.getAccount().getName()).toString());
					return false;
				}
			} else if (commands[0].equals("USERID")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (commands.length != 2) {
					client.sendLine("SERVERMSG Bad USERID command - too many or too few parameters");
					return false;
				}

				int userID = Account.NO_USER_ID;
				try {
					long temp = Long.parseLong(commands[1], 16);
					userID = (int) temp; // we transform unsigned 32 bit integer to a signed one
				} catch (NumberFormatException e) {
					client.sendLine("SERVERMSG Bad USERID command - userID field should be an integer");
					return false;
				}

				client.getAccount().setLastUserId(userID);
				final boolean mergeOk = context.getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
				if (!mergeOk) {
					client.sendLine("SERVERMSG Failed saving last userid to persistent storage");
					return false;
				}

				// add server notification:
				ServerNotification sn = new ServerNotification("User ID received");
				sn.addLine(new StringBuilder("<")
						.append(client.getAccount().getName()).append("> has generated a new user ID: ")
						.append(commands[1]).append("(")
						.append(userID).append(")").toString());
				context.getServerNotifications().addNotification(sn);
			} else if (commands[0].equals("RENAMEACCOUNT")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (commands.length != 2) {
					client.sendLine("SERVERMSG Bad RENAMEACCOUNT command - too many or too few parameters");
					return false;
				}

				if (context.getServer().isLanMode()) {
					client.sendLine("SERVERMSG RENAMEACCOUNT failed: You cannot rename your account while server is running in LAN mode since you have no account!");
					return false;
				}

				// validate new userName:
				String valid = Account.isOldUsernameValid(commands[1]);
				if (valid != null) {
					client.sendLine(new StringBuilder("SERVERMSG RENAMEACCOUNT failed: Invalid username (reason: ").append(valid).append(")").toString());
					return false;
				}

				Account acc = context.getAccountsService().findAccountNoCase(commands[1]);
				if (acc != null && acc != client.getAccount()) {
					client.sendLine("SERVERMSG RENAMEACCOUNT failed: Account with same username already exists!");
					return false;
				}

				// make sure all mutes are accordingly adjusted to new userName:
				for (int i = 0; i < context.getChannels().getChannelsSize(); i++) {
					context.getChannels().getChannel(i).getMuteList().rename(client.getAccount().getName(), commands[1]);
				}

				final String oldName = client.getAccount().getName();
				Account account_new = client.getAccount().clone();
				account_new.setName(commands[1]);
				account_new.setLastLogin(System.currentTimeMillis());
				account_new.setLastIP(client.getIp());
				final boolean mergeOk = context.getAccountsService().mergeAccountChanges(account_new, client.getAccount().getName());
				if (mergeOk) {
					client.setAccount(account_new);
				} else {
					client.sendLine("SERVERMSG Your account renaming failed.");
					return false;
				}

				client.sendLine(new StringBuilder("SERVERMSG Your account has been renamed to <")
						.append(account_new.getName())
						.append(">. Reconnect with new account (you will now be automatically disconnected)!").toString());
				context.getClients().killClient(client, "Quit: renaming account");
				context.getAccountsService().saveAccounts(false); // let's save new accounts info to disk
				context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: User <")
						.append(oldName).append("> has just renamed his account to <")
						.append(client.getAccount().getName()).append(">").toString());

				// add server notification:
				ServerNotification sn = new ServerNotification("Account renamed");
				sn.addLine(new StringBuilder("User <")
						.append(oldName).append("> has renamed his account to <")
						.append(client.getAccount().getName()).append(">").toString());
				context.getServerNotifications().addNotification(sn);
			} else if (commands[0].equals("CHANGEPASSWORD")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (commands.length != 3) {
					client.sendLine("SERVERMSG Bad CHANGEPASSWORD command - too many or too few parameters");
					return false;
				}

				if (context.getServer().isLanMode()) {
					client.sendLine("SERVERMSG CHANGEPASSWORD failed: You cannot change your password while server is running in LAN mode!");
					return false;
				}

				if (!(commands[1].equals(client.getAccount().getPassword()))) {
					client.sendLine("SERVERMSG CHANGEPASSWORD failed: Old password is incorrect!");
					return false;
				}

				// validate password:
				String valid = Account.isPasswordValid(commands[2]);
				if (valid != null) {
					client.sendLine(new StringBuilder("SERVERMSG CHANGEPASSWORD failed: Invalid password (reason: ").append(valid).append(")").toString());
					return false;
				}

				final String oldPasswd = client.getAccount().getPassword();
				client.getAccount().setPassword(commands[2]);
				final boolean mergeOk = context.getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
				if (!mergeOk) {
					client.getAccount().setPassword(oldPasswd);
					client.sendLine("SERVERMSG CHANGEPASSWORD failed: Failed saving to persistent storage.");
					return false;
				}

				context.getAccountsService().saveAccounts(false); // let's save new accounts info to disk
				client.sendLine("SERVERMSG Your password has been successfully updated!");
			} else if (commands[0].equals("JOIN")) {
				if (commands.length < 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				// check if channel name is OK:
				String valid = context.getChannels().isChanNameValid(commands[1]);
				if (valid != null) {
					client.sendLine(new StringBuilder("JOINFAILED Bad channel name (\"#")
							.append(commands[1]).append("\"). Reason: ")
							.append(valid).toString());
					return false;
				}

				// check if key is correct (if channel is locked):
				Channel chan = context.getChannels().getChannel(commands[1]);
				if ((chan != null) && (chan.isLocked()) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0 /* we will allow admins to join locked channels */)) {
					if (!Misc.makeSentence(commands, 2).equals(chan.getKey())) {
						client.sendLine(new StringBuilder("JOINFAILED ").append(commands[1]).append(" Wrong key (this channel is locked)!").toString());
						return false;
					}
				}

				chan = client.joinChannel(commands[1]);
				if (chan == null) {
					client.sendLine(new StringBuilder("JOINFAILED ").append(commands[1]).append(" Already in the channel!").toString());
					return false;
				}
				client.sendLine(new StringBuilder("JOIN ").append(commands[1]).toString());
				context.getChannels().sendChannelInfoToClient(chan, client);
				context.getChannels().notifyClientsOfNewClientInChannel(chan, client);
			} else if (commands[0].equals("LEAVE")) {
				if (commands.length < 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				Channel chan = context.getChannels().getChannel(commands[1]);
				if (chan == null) {
					return false;
				}

				client.leaveChannel(chan, "");
			} else if (commands[0].equals("CHANNELTOPIC")) {
				if (commands.length < 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.PRIVILEGED) < 0) {
					return false;
				}

				Channel chan = context.getChannels().getChannel(commands[1]);
				if (chan == null) {
					client.sendLine(new StringBuilder("SERVERMSG Error: Channel does not exist: ").append(commands[1]).toString());
					return false;
				}

				if (!chan.setTopic(Misc.makeSentence(commands, 2), client.getAccount().getName())) {
					client.sendLine(new StringBuilder("SERVERMSG You've just disabled the topic for channel #").append(chan.getName()).toString());
					chan.broadcast(new StringBuilder("<")
							.append(client.getAccount().getName()).append("> has just disabled topic for #")
							.append(chan.getName()).toString());
				} else {
					client.sendLine(new StringBuilder("SERVERMSG You've just changed the topic for channel #").append(chan.getName()).toString());
					chan.broadcast(new StringBuilder("<")
							.append(client.getAccount().getName()).append("> has just changed topic for #")
							.append(chan.getName()).toString());
					chan.sendLineToClients(new StringBuilder("CHANNELTOPIC ")
							.append(chan.getName()).append(" ")
							.append(chan.getTopicAuthor()).append(" ")
							.append(chan.getTopicChangedTime()).append(" ")
							.append(chan.getTopic()).toString());
				}
			} else if (commands[0].equals("SAY")) {
				if (commands.length < 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				Channel chan = client.getChannel(commands[1]);
				if (chan == null) {
					return false;
				}

				if (chan.getMuteList().isMuted(client.getAccount().getName())) {
					client.sendLine(new StringBuilder("SERVERMSG Message dropped. You are not allowed to talk in #")
							.append(chan.getName()).append("! Please contact one of the moderators.").toString());
					return false;
				} else if (chan.getMuteList().isIPMuted(client.getIp())) {
					client.sendLine(new StringBuilder("SERVERMSG Message dropped. You are not allowed to talk in #")
							.append(chan.getName()).append(" (muted by IP address)! If you believe this is an error, contact one of the moderators.").toString());
					return false;
				}


				String s = Misc.makeSentence(commands, 2);
				// check for flooding:
				if ((s.length() > maxChatMessageLength) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(") [exceeded max. chat message size]").toString());
					client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
							.append(maxChatMessageLength).append(" bytes). Your message has been ignored.").toString());
					context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(") - exceeded maximum chat message size. Ignoring ...").toString());
					return false;
				}
				chan.sendLineToClients(new StringBuilder("SAID ").append(chan.getName()).append(" ").append(client.getAccount().getName()).append(" ").append(s).toString());
			} else if (commands[0].equals("SAYEX")) {
				if (commands.length < 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				Channel chan = client.getChannel(commands[1]);
				if (chan == null) {
					return false;
				}

				if (chan.getMuteList().isMuted(client.getAccount().getName())) {
					client.sendLine(new StringBuilder("SERVERMSG Message dropped. You are not allowed to talk in #")
							.append(chan.getName())
							.append("! Please contact one of the moderators.").toString());
					return false;
				} else if (chan.getMuteList().isIPMuted(client.getIp())) {
					client.sendLine(new StringBuilder("SERVERMSG Message dropped. You are not allowed to talk in #")
							.append(chan.getName())
							.append(" (muted by IP address)! If you believe this is an error, contact one of the moderators.").toString());
					return false;
				}

				String s = Misc.makeSentence(commands, 2);
				// check for flooding:
				if ((s.length() > maxChatMessageLength) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") [exceeded max. chat message size]").toString());
					client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
							.append(maxChatMessageLength).append(" bytes). Your message has been ignored.").toString());
					context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") - exceeded maximum chat message size. Ignoring ...").toString());
					return false;
				}

				chan.sendLineToClients(new StringBuilder("SAIDEX ")
						.append(chan.getName()).append(" ")
						.append(client.getAccount().getName()).append(" ")
						.append(s).toString());
			} else if (commands[0].equals("SAYPRIVATE")) {
				if (commands.length < 3) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				Client target = context.getClients().getClient(commands[1]);
				if (target == null) {
					return false;
				}

				String s = Misc.makeSentence(commands, 2);
				// check for flooding:
				if ((s.length() > maxChatMessageLength) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(") [exceeded max. chat message size]").toString());
					client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
							.append(maxChatMessageLength).append(" bytes). Your message has been ignored.").toString());
					context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") - exceeded maximum chat message size. Ignoring ...").toString());
					return false;
				}

				target.sendLine(new StringBuilder("SAIDPRIVATE ")
						.append(client.getAccount().getName()).append(" ")
						.append(s).toString());
				client.sendLine(command); // echo the command. See protocol description!
			} else if (commands[0].equals("JOINBATTLE")) {
				if (commands.length < 2) {
					return false; // requires 1 or 2 arguments (password is optional)
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				int battleID;

				try {
					battleID = Integer.parseInt(commands[1]);
				} catch (NumberFormatException e) {
					client.sendLine("JOINBATTLEFAILED No battle ID!");
					return false;
				}

				if (client.getBattleID() != Battle.NO_BATTLE_ID) { // can't join a battle if already participating in another battle
					client.sendLine("JOINBATTLEFAILED Cannot participate in multiple battles at the same time!");
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(battleID);

				if (bat == null) {
					client.sendLine("JOINBATTLEFAILED Invalid battle ID!");
					return false;
				}

				if (bat.restricted()) {
					if (commands.length < 3) {
						client.sendLine("JOINBATTLEFAILED Password required");
						return false;
					}

					if (!bat.getPassword().equals(commands[2])) {
						client.sendLine("JOINBATTLEFAILED Invalid password");
						return false;
					}
				}

				if (bat.isLocked()) {
					client.sendLine("JOINBATTLEFAILED You cannot join locked battles!");
					return false;
				}

				if (commands.length > 3) {
					client.setScriptPassword(commands[3]);
				}

				if (bat.getFounder().isHandleBattleJoinAuthorization()) {
					client.setRequestedBattleID(battleID);
					bat.getFounder().sendLine("JOINBATTLEREQUEST " + client.getAccount().getName() + " " + (bat.getFounder().getIp().equals(client.getIp()) ? client.getLocalIP() : client.getIp()));
				} else {
					notifyClientJoinedBattle(client,bat);
				}

			} else if (commands[0].equals("JOINBATTLEACCEPT")) {
				if (commands.length != 2) {
					return false;
				} else if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				} else if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				// check battle
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				if (bat == null) {
					return false;
				} else if (bat.getFounder() != client) {
					// only founder can accept battle join
					return false;
				}

				// check client
				Client joiningClient = context.getClients().getClient(commands[1]);
				if (joiningClient == null) {
					return false;
				} else if (joiningClient.getRequestedBattleID() !=  client.getBattleID()) {
					return false;
				}

				notifyClientJoinedBattle(joiningClient,bat);
			} else if (commands[0].equals("JOINBATTLEDENY")) {
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
					return false;
				} // only founder can deny battle join
				Client joiningClient = context.getClients().getClient(commands[1]);
				if (joiningClient == null) {
					return false;
				}
				if (joiningClient.getRequestedBattleID() !=  client.getBattleID()) {
					return false;
				}
				joiningClient.setRequestedBattleID(Battle.NO_BATTLE_ID);
				if(commands.length > 2) {
				    joiningClient.sendLine("JOINBATTLEFAILED Denied by battle founder - " + Misc.makeSentence(commands, 2));
				} else {
				    joiningClient.sendLine("JOINBATTLEFAILED Denied by battle founder");
				}
			} else if (commands[0].equals("LEAVEBATTLE")) {
				if (commands.length != 1) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false; // this may happen when client sent LEAVEBATTLE command right after he was kicked from the battle, for example.
				}
				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				verifyBattle(bat);
				context.getBattles().leaveBattle(client, bat); // automatically checks if client is a founder and closes battle
			} else if (commands[0].equals("MYBATTLESTATUS")) {
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

				int newTeamColor;
				try {
					newTeamColor = Integer.parseInt(commands[2]);
				} catch (NumberFormatException e) {
					return false;
				}
				client.setTeamColor(newTeamColor);

				int newStatus;
				try {
					newStatus = Integer.parseInt(commands[1]);
				} catch (NumberFormatException e) {
					return false;
				}
				// update new battle status. Note: we ignore handicap value as it can be changed only by founder with HANDICAP command!
				client.setBattleStatus(Misc.setHandicapOfBattleStatus(newStatus, Misc.getHandicapFromBattleStatus(client.getBattleStatus())));

				// if game is full or game type is "battle replay", force player's mode to spectator:
				if ((bat.getClientsSize() + 1 - bat.spectatorCount() > bat.getMaxPlayers()) || (bat.getType() == 1)) {
					client.setBattleStatus(Misc.setModeOfBattleStatus(client.getBattleStatus(), 0));
				}
				// if player has chosen team number which is already used by some other player/bot,
				// force his ally number and team color to be the same as of that player/bot:
				if (bat.getFounder() != client) {
					if ((Misc.getTeamNoFromBattleStatus(bat.getFounder().getBattleStatus()) == Misc.getTeamNoFromBattleStatus(client.getBattleStatus())) && (Misc.getModeFromBattleStatus(bat.getFounder().getBattleStatus()) != 0)) {
						client.setBattleStatus(Misc.setAllyNoOfBattleStatus(client.getBattleStatus(), Misc.getAllyNoFromBattleStatus(bat.getFounder().getBattleStatus())));
						client.setTeamColor(bat.getFounder().getTeamColor());
					}
				}
				for (int i = 0; i < bat.getClientsSize(); i++) {
					if (bat.getClient(i) != client) {
						if ((Misc.getTeamNoFromBattleStatus(bat.getClient(i).getBattleStatus()) == Misc.getTeamNoFromBattleStatus(client.getBattleStatus())) && (Misc.getModeFromBattleStatus(bat.getClient(i).getBattleStatus()) != 0)) {
							client.setBattleStatus(Misc.setAllyNoOfBattleStatus(client.getBattleStatus(), Misc.getAllyNoFromBattleStatus(bat.getClient(i).getBattleStatus())));
							client.setTeamColor(bat.getClient(i).getTeamColor());
							break;
						}
					}
				}
				for (int i = 0; i < bat.getBotsSize(); i++) {
					if (Misc.getTeamNoFromBattleStatus(bat.getBot(i).getBattleStatus()) == Misc.getTeamNoFromBattleStatus(client.getBattleStatus())) {
						client.setBattleStatus(Misc.setAllyNoOfBattleStatus(client.getBattleStatus(), Misc.getAllyNoFromBattleStatus(bat.getBot(i).getBattleStatus())));
						client.setTeamColor(bat.getBot(i).getTeamColor());
						break;
					}
				}

				bat.notifyClientsOfBattleStatus(client);
			} else if (commands[0].equals("MYSTATUS")) {
				if (commands.length != 2) {
					return false;
				}
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

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

				client.setStatus(newStatus);

				client.setRankToStatus(tmp);
				client.setAccessToStatus(tmp3);
				client.setBotModeToStatus(tmp4);

				if (client.getInGameFromStatus() != tmp2) {
					// user changed his in-game status.
					if (tmp2 == false) { // client just entered game
						Battle bat = context.getBattles().getBattleByID(client.getBattleID());
						if ((bat != null) && (bat.getClientsSize() > 0)) {
							client.setInGameTime(System.currentTimeMillis());
						} else {
							client.setInGameTime(0); // we won't update clients who play by themselves (or with bots), since some try to exploit the system by leaving computer alone in-battle for hours to increase their ranks
						}						// check if client is a battle host using "hole punching" technique:
						if ((bat != null) && (bat.getFounder() == client) && (bat.getNatType() == 1)) {
							// tell clients to replace battle port with founder's public UDP source port:
							bat.sendToAllExceptFounder(new StringBuilder("HOSTPORT ").append(client.getUdpSourcePort()).toString());
						}
					} else { // back from game
						if (client.getInGameTime() != 0) {
							// We will not update clients that play
							// by themselves (or with bots only),
							// since some try to exploit the system
							// by leaving their computer alone in-battle
							// for hours, to increase their ranks.
							long diffMins = (System.currentTimeMillis() - client.getInGameTime()) / 60000;
							boolean rankChanged = client.getAccount().addMinsToInGameTime(diffMins);
							if (rankChanged) {
								client.setRankToStatus(client.getAccount().getRank().ordinal());
							}
							final boolean mergeOk = context.getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
							if (!mergeOk) {
								// as this is no serious problem, only log a message
								s_log.warn(new StringBuilder("Failed updating users in-game-time in persistent storage: ")
										.append(client.getAccount().getName()).toString());
								return false;
							}
						}
					}
				}
				context.getClients().notifyClientsOfNewClientStatus(client);
			} else if (commands[0].equals("SAYBATTLE")) {
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

				String s = Misc.makeSentence(commands, 1);
				// check for flooding:
				if ((s.length() > maxChatMessageLength) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") [exceeded max. chat message size]").toString());
					client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
							.append(maxChatMessageLength).append(" bytes). Your message has been ignored.").toString());
					context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") - exceeded maximum chat message size. Ignoring ...").toString());
					return false;
				}

				bat.sendToAllClients(new StringBuilder("SAIDBATTLE ")
						.append(client.getAccount().getName()).append(" ")
						.append(s).toString());
			} else if (commands[0].equals("SAYBATTLEEX")) {
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

				String s = Misc.makeSentence(commands, 1);
				// check for flooding:
				if ((s.length() > maxChatMessageLength) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") [exceeded max. chat message size]").toString());
					client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
							.append(maxChatMessageLength).append(" bytes). Your message has been ignored.").toString());
					context.getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName())
							.append(") - exceeded maximum chat message size. Ignoring ...").toString());
					return false;
				}

				bat.sendToAllClients(new StringBuilder("SAIDBATTLEEX ")
						.append(client.getAccount().getName()).append(" ")
						.append(s).toString());
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
				verifyBattle(bat);

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
				verifyBattle(bat);

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
				verifyBattle(bat);

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
					verifyBattle(bat);

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
				verifyBattle(bat);

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
				verifyBattle(bat);

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
				verifyBattle(bat);

				bat.getTempReplayScript().clear();
			} else if (commands[0].equals("SCRIPT")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				verifyBattle(bat);

				bat.getTempReplayScript().add(Misc.makeSentence(commands, 1));
			} else if (commands[0].equals("SCRIPTEND")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
					return false;
				}

				if (client.getBattleID() == Battle.NO_BATTLE_ID) {
					return false;
				}

				Battle bat = context.getBattles().getBattleByID(client.getBattleID());
				verifyBattle(bat);

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
				verifyBattle(bat);

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
				verifyBattle(bat);

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
			readAgreement();
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

			// purge list of failed login attempts:
			if (System.currentTimeMillis() - lastFailedLoginsPurgeTime > 1000) {
				lastFailedLoginsPurgeTime = System.currentTimeMillis();
				for (int i = 0; i < failedLoginAttempts.size(); i++) {
					FailedLoginAttempt attempt = failedLoginAttempts.get(i);
					if (System.currentTimeMillis() - attempt.getTimeOfLastFailedAttempt() > 30000) {
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
