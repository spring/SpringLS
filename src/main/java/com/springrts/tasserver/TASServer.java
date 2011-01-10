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
			} else if (commands[0].equals("OUTPUTDBDRIVERSTATUS")) {
				if (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0) {
					return false;
				}

				client.sendLine("SERVERMSG This command is not supported anymore, as JPA is used for DB access for bans. Therefore, this is a no-op now.");
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

		if (context.getUpdateProperties().read(UpdateProperties.DEFAULT_FILENAME)) {
			s_log.info(new StringBuilder("\"Update properties\" read from ").append(UpdateProperties.DEFAULT_FILENAME).toString());
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
