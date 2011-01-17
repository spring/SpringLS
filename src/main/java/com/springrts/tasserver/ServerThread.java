/*
	Copyright (c) 2010 Robin Vobruba <robin.vobruba@derisk.ch>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.tasserver;


import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.CommandProcessor;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Runs the thread that handles connections by and messages sent from clients.
 * @author hoijui
 */
public class ServerThread implements ContextReceiver, LiveStateListener {

	private static final Log s_log  = LogFactory.getLog(ServerThread.class);

	private static class DeprecatedCommand {

		private String name;
		private String message;

		DeprecatedCommand(String name, String message) {

			this.name = name;
			this.message = message;
		}

		public String getName() {
			return name;
		}

		public String getMessage() {
			return message;
		}
	}

	private Context context;

	/**
	 * Contains a list of deprecated commands, for example:
	 * "WHITELIST" -> "deprecated feature: white-listing"
	 */
	private static Map<String, DeprecatedCommand> DEPRECATED_COMMANDS = null;

	/**
	 * The size of the ByteBuffer used to read data from the socket channel.
	 * This size does not really matter, as the server will work with any size
	 * (tested with READ_BUFFER_SIZE==1), but too small buffer size may impact
	 * the performance.
	 */
	private static final int READ_BUFFER_SIZE = 256;
	/**
	 * The socket's send buffer size.
	 */
	private static final int SEND_BUFFER_SIZE = 8192 * 2;
	private static final long MAIN_LOOP_SLEEP = 10L;
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


	public ServerThread() {

		this.context = null;
		initDeprecatedCommands();
	}

	private static void add(Map<String, DeprecatedCommand> deprecatedCommands,
			DeprecatedCommand command)
	{
		deprecatedCommands.put(command.getName(), command);
	}
	private void initDeprecatedCommands() {

		if (DEPRECATED_COMMANDS == null) {
			Map<String, DeprecatedCommand> deprecatedCommands
					= new HashMap<String, DeprecatedCommand>();

			add(deprecatedCommands, new DeprecatedCommand(
					"WHITELIST",
					"IP white-listing is disabled"));
			add(deprecatedCommands, new DeprecatedCommand(
					"UNWHITELIST",
					"IP white-listing is disabled"));
			add(deprecatedCommands, new DeprecatedCommand(
					"RETRIEVELATESTBANLIST",
					"Fetching ban entries is not needed anymore."
					+ " Therefore, this is a no-op now."));
			add(deprecatedCommands, new DeprecatedCommand(
					"OUTPUTDBDRIVERSTATUS",
					"This command is not supported anymore,"
					+ " as JPA is used for DB access for bans."
					+ " Therefore, this is a no-op now."));

			DEPRECATED_COMMANDS = deprecatedCommands;
		}
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	private Context getContext() {
		return context;
	}

	private void acceptNewConnections() {

		try {
			SocketChannel clientChannel;
			// since sSockChan is non-blocking, this will return immediately
			// regardless of whether there is a connection available
			while ((clientChannel = sSockChan.accept()) != null) {
				if (getContext().getServer().isRedirectActive()) {
					if (s_log.isDebugEnabled()) {
						s_log.debug(new StringBuilder("Client redirected to ")
								.append(getContext().getServer().getRedirectAddress().getHostAddress()).append(": ")
								.append(clientChannel.socket().getInetAddress().getHostAddress()).toString());
					}
					redirectAndKill(clientChannel.socket());
					continue;
				}

				Client client = getContext().getClients().addNewClient(clientChannel, readSelector, SEND_BUFFER_SIZE);
				if (client == null) {
					continue;
				}

				// from this point on, we know that client
				// has been successfully connected
				client.sendWelcomeMessage();

				if (s_log.isDebugEnabled()) {
					s_log.debug(new StringBuilder("New client connected: ").append(client.getIp()).toString());
				}
			}
		} catch (IOException ioex) {
			s_log.error(new StringBuilder("Error during accept(): ").append(ioex.toString()).toString(), ioex);
		} catch (Exception ex) {
			s_log.error(new StringBuilder("Exception in acceptNewConnections()").append(ex.toString()).toString(), ex);
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
				if (getContext().getFloodProtection().isFlooding(client)) {
					s_log.warn(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(")").toString());
					getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append("). User has been kicked.").toString());
					getContext().getClients().killClient(client, "Disconnected due to excessive flooding");

					// add server notification:
					ServerNotification sn = new ServerNotification("Flooding detected");
					sn.addLine(new StringBuilder("Flooding detected from ")
							.append(client.getIp()).append(" (")
							.append(client.getAccount().getName()).append(").").toString());
					sn.addLine("User has been kicked from the server.");
					getContext().getServerNotifications().addNotification(sn);

					continue;
				}

				// check for end-of-stream
				if (nbytes == -1) {
					if (s_log.isDebugEnabled()) {
						s_log.debug("Socket disconnected - killing client");
					}
					channel.close();
					getContext().getClients().killClient(client); // will also close the socket channel
				} else {
					// use a CharsetDecoder to turn those bytes into a string
					// and append to client's StringBuilder
					readBuffer.flip();
					String str = getContext().getServer().getAsciiDecoder().decode(readBuffer).toString();
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
						getContext().getServerThread().executeCommand(command, client);
						time = System.currentTimeMillis() - time;
						if (time > 200) {
							getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: (DEBUG) User <")
									.append(client.getAccount().getName()).append("> caused ")
									.append(time).append(" ms load on the server. Command issued: ")
									.append(command).toString());
						}

						if (!client.isAlive()) {
							break; // in case client was killed within executeCommand() method
						}
						line = client.getRecvBuf().toString();
					}
				}
			}
		} catch (IOException ioe) {
			s_log.info("exception during select(): possibly due to force disconnect. Killing the client ...");
			try {
				if (client != null) {
					getContext().getClients().killClient(client, "Quit: connection lost");
				}
			} catch (Exception e) {
				// do nothing
			}
			s_log.debug("... the exception was:", ioe);
		} catch (Exception e) {
			s_log.info("exception in readIncomingMessages(): killing the client ... ", e);
			try {
				if (client != null) {
					getContext().getClients().killClient(client, "Quit: connection lost");
				}
			} catch (Exception ex) {
				// do nothing
			}
			s_log.debug("... the exception was:", e);
		}
	}

	/**
	 * Note: this method is not synchronized!
	 * Note2: this method may be called recursively!
	 */
	public boolean executeCommand(String command, Client client) {

		String commandClean = command.trim();
		if (commandClean.isEmpty()) {
			return false;
		}

		if (s_log.isDebugEnabled()) {
			if (client.getAccount().getAccess() != Account.Access.NONE) {
				s_log.debug(new StringBuilder("[<-")
						.append(client.getAccount().getName()).append("] \"")
						.append(commandClean).append("\"").toString());
			} else {
				s_log.debug(new StringBuilder("[<-")
						.append(client.getIp()).append("] \"")
						.append(commandClean).append("\"").toString());
			}
		}

		int msgId = Client.NO_MSG_ID;
		if (commandClean.charAt(0) == '#') {
			try {
				if (!commandClean.matches("^#\\d+\\s[\\s\\S]*")) {
					return false; // malformed command
				}
				msgId = Integer.parseInt(commandClean.substring(1).split("\\s")[0]);
				// remove id field from the rest of command:
				commandClean = commandClean.replaceFirst("#\\d+\\s", "");
			} catch (NumberFormatException ex) {
				return false; // this means that the command is malformed
			} catch (PatternSyntaxException ex) {
				return false; // this means that the command is malformed
			}
		}

		// parse command into tokens:
		String[] commands = commandClean.split(" ");
		commands[0] = commands[0].toUpperCase();

		client.setSendMsgId(msgId);

		try {
			CommandProcessor cp = getContext().getCommandProcessors().get(commands[0]);
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
			} else if (DEPRECATED_COMMANDS.containsKey(commands[0])) {
				DeprecatedCommand deprecatedCommand = DEPRECATED_COMMANDS.get(commands[0]);
				client.sendLine(String.format(
						"SERVERMSG Command %s is deprecated: %s",
						deprecatedCommand.getName(),
						deprecatedCommand.getMessage()));
			} else {
				// unknown command!
				return false;
			}
		} finally {
			client.setSendMsgId(Client.NO_MSG_ID);
		}

		return true;
	}

	public void run() {

		running = true;
		while (running) { // main loop

			// check for new client connections
			acceptNewConnections();

			// check for incoming messages
			readIncomingMessages();

			getContext().getClients().update();

			getContext().getStatistics().update();

			getContext().getNatHelpServer().update();

			getContext().getAccountsService().update();

			getContext().getChannels().update();

			// sleep a bit
			try {
				Thread.sleep(MAIN_LOOP_SLEEP);
			} catch (InterruptedException iex) {
			}
		}

		getContext().stopping();

		// close everything:
		if (!getContext().getServer().isLanMode()) {
			getContext().getAccountsService().saveAccounts(true);
		}
		if (getContext().getNatHelpServer().isRunning()) {
			getContext().getNatHelpServer().stopServer();
		}

		// add server notification:
		ServerNotification sn = new ServerNotification("Server stopped");
		sn.addLine("Server has just been stopped gracefully. See server log for more info.");
		getContext().getServerNotifications().addNotification(sn);

		s_log.info("Server closed gracefully!");

		getContext().stopped();
	}

	public boolean startServer() {

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
			s_log.error("Could not listen on port: " + port, e);
			return false;
		}

		s_log.info(new StringBuilder("Listening for connections on TCP port ")
				.append(port)
				.append(" ...").toString());

		context.started();

		return true;
	}

	/**
	 * Shuts down the server gracefully.
	 */
	public void stop() {
		running = false;
	}

	/**
	 * Shuts down the server forcefully.
	 */
	public void closeServerAndExit() {

		// FIXME these things do not get called on normal exit yet!
		//       see (end of) method run()

		//getContext().stopping();

		// add server notification:
		ServerNotification sn = new ServerNotification("Server stopped");
		sn.addLine("Server has just been stopped. See server log for more info.");
		getContext().getServerNotifications().addNotification(sn);

		//getContext().stopped();
		s_log.warn("Server stopped forcefully");

		System.exit(0);
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

		// add server notification
		ServerNotification sn = new ServerNotification("Server started");
		sn.addLine(String.format(
				"Server has been started on port %d."
				+ " There are %d accounts. "
				+ "See server log for more info.",
				context.getServer().getPort(),
				context.getAccountsService().getAccountsSize()));
		context.getServerNotifications().addNotification(sn);

		createAdminIfNoUsers();
	}

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
				s_log.info("As there are no accounts yet, we are creating an "
						+ "admin account: username=\"" + username
						+ "\", password=\"" + password + "\"");
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

		Account admin = new Account(
				username,
				Misc.encodePassword(password),
				Account.NO_ACCOUNT_LAST_IP,
				Account.NO_ACCOUNT_LAST_COUNTRY);
		admin.setAccess(Account.Access.ADMIN);
		return admin;
	}

	@Override
	public void stopping() {
		s_log.info("Server stopping ...");
	}
	@Override
	public void stopped() {

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		DateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss z");

		running = false;
		s_log.info("Server stopped on " + dateTimeFormat.format(new Date()));
	}
}
