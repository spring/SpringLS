/*
 * Created on 2006.11.17
 */

package com.springrts.chanserv;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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
public class RemoteAccessServer extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(ChanServ.class);

	/** used with QUERYSERVER command */
	private static final List<String> ALLOWED_QUERY_COMMANDS;
	static {
		List<String> allowedQueryCommands = new ArrayList<String>();
		allowedQueryCommands.add("GETREGISTRATIONDATE");
		allowedQueryCommands.add("GETINGAMETIME");
		allowedQueryCommands.add("GETLASTIP");
		allowedQueryCommands.add("GETLASTLOGINTIME");
		allowedQueryCommands.add("RELOADUPDATEPROPERTIES");
		allowedQueryCommands.add("GETLOBBYVERSION");
		allowedQueryCommands.add("UPDATEMOTD");
		allowedQueryCommands.add("RETRIEVELATESTBANLIST");
		allowedQueryCommands.add("GETUSERID");
		ALLOWED_QUERY_COMMANDS = Collections.unmodifiableList(allowedQueryCommands);
	}
	public static List<String> getAllowedQueryCommands() {
		return ALLOWED_QUERY_COMMANDS;
	}

	/** Keys for remote server access (needs to be thread-save) */
	private final List<String> remoteAccounts;

	/** A list of all currently running client threads (needs to be thread-save) */
	private final Map<Integer, RemoteClientThread> threads;
	private final int port;
	private ServerSocket serverSocket;
	private boolean running;

	private Context context;

	public RemoteAccessServer(Context context, int port) {

		this.context = context;
		this.port = port;
		this.serverSocket = null;
		this.running = false;
		this.remoteAccounts = java.util.Collections.synchronizedList(new LinkedList<String>());
		this.threads = java.util.Collections.synchronizedMap(new HashMap<Integer, RemoteClientThread>());
	}

	@Override
	public void run() {

		logger.info("Trying to run remote access server on port {} ...", port);

		try {
			serverSocket = new ServerSocket(port);
			running = true;
		} catch (IOException ex) {
			logger.error("Failed to start the remote access server", ex);
		}

		while (running) {
			try {
				Socket cs = serverSocket.accept();
				RemoteClientThread thread = new RemoteClientThread(context, this, cs);
				threads.put(thread.getClientId(), thread);
				thread.start();
			} catch (SocketException sex) {
				// this is triggered when the server-socket was closed while we
				// were blocked in accept()
				// -> nothing bad, server is beeing closed
			} catch (IOException ex) {
				logger.warn("Failed to handle a remote client connect operation", ex);
			}
		}
	}

	/**
	 * Keys for remote server access (needs to be thread-save)
	 * @return the remoteAccounts
	 */
	public List<String> getRemoteAccounts() {
		return remoteAccounts;
	}

	/**
	 * Forward a command to the waiting thread.
	 * @return true if forwarded successfully
	 */
	public boolean forwardCommand(int threadId, String command) {

		synchronized (threads) {
			RemoteClientThread rct = threads.get(threadId);
			if (rct != null) {
				try {
					rct.putCommand(command);
					return true;
				} catch (InterruptedException ex) {
					logger.warn("Failed forwarding a command", ex);
					return false;
				}
			}
		}
		// No suitable thread could be found!
		// Perhaps the thread has already finished
		// before it could read the response (not a problem).
		return false;
	}

	public RemoteClientThread removeRemoteClientThread(RemoteClientThread thread) {
		return threads.remove(thread.getClientId());
	}
}
