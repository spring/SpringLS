
package com.springrts.chanserv;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see RemoteAccessServer
 * @author hoijui
 */
public class RemoteClientThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(RemoteClientThread.class);

	/** in milliseconds */
	private static final int TIMEOUT = 30000;

	/**
	 * A unique ID which we will use as a message ID when sending commands to
	 * the lobby server.
	 */
	public final int ID = (int)((Math.random() * 65535));

	/**
	 * Reply queue which gets filled by context.getChanServ() automatically
	 * (needs to be thread-save)
	 */
	private final BlockingQueue<String> replyQueue;
	//Queue replyQueue = new Queue();

	/* socket for the client which we are handling */
	private Socket socket;
	private String IP;
	/** whether the remote client has already identified */
	private boolean identified = false;

	private PrintWriter out;
	private BufferedReader in;

	/** the object that spawned this thread */
	private RemoteAccessServer parent;

	private Context context;

	RemoteClientThread(Context context, RemoteAccessServer parent, Socket s) throws IOException {

		this.context = context;
		// LinkedBlockingQueue is thread-save
		this.replyQueue = new LinkedBlockingQueue<String>();
		this.socket = s;
		this.parent = parent;
		try {
			socket.setSoTimeout(TIMEOUT);
			socket.setTcpNoDelay(true);
		} catch (SocketException ex) {
			logger.error("Serious error in RemoteClient constructor (SocketException)", ex);
		}
		IP = socket.getInetAddress().getHostAddress();

		OutputStream rawOut = null;
		out = null;
		InputStream rawIn = null;
		in = null;
		try {
			rawOut = socket.getOutputStream();
			out = new PrintWriter(rawOut, true);

			rawIn = socket.getInputStream();
			in = new BufferedReader(new InputStreamReader(rawIn));
		} catch (IOException ex) {
			if (out != null) {
				out.close();
			} else if (rawOut != null) {
				rawOut.close();
			}
			if (in != null) {
				in.close();
			} else if (rawIn != null) {
				rawIn.close();
			}
			throw new IOException("Failed to associate input/output with the client socket; connection terminated.", ex);
		}
	}

	public String readLine() throws IOException {
		return in.readLine();
	}

	public void sendLine(String text) {

		logger.debug("RAS: \"{}\"", text);
		out.println(text);
	}

	private void disconnect() {
		try {
			if (!socket.isClosed()) {
				out.close();
				in.close();
				socket.close();
			}
		} catch (IOException ex) {
			logger.warn("Failed to propperly close remote client connection with " + IP, ex);
		}
	}

	private void kill() {
		disconnect();
		parent.removeRemoteClientThread(this);
	}

	@Override
	public void run() {
		String input;

		try {
			while (true) {
				input = readLine();
				if (input == null) {
					throw new IOException();
				}
				logger.debug("{}: \"{}\"", IP, input);
				processCommand(input);
			}
		} catch (InterruptedIOException ex) {
			kill();
			return;
		} catch (IOException ex) {
			kill();
			return;
		}
	}

	private void queryTASServer(String command) {
		context.getChanServ().sendLine("#" + ID + " " + command);
	}

	/** Will wait until queue doesn't contain a response */
	private String waitForReply() {

		try {
			return replyQueue.take();
		} catch (InterruptedException ex) {
			return null;
		}
	}

	/**
	 * Lines-up a command in the queue for processing.
	 */
	public void putCommand(String command) throws InterruptedException {
		replyQueue.put(command);
	}

	private void processCommand(String command) {

		String cleanCommand = command.trim();
		if (cleanCommand.equals("")) {
			return;
		}

		String[] params = cleanCommand.split(" ");
		params[0] = params[0].toUpperCase();

		if (params[0].equals("IDENTIFY")) {
			if (params.length != 2) {
				logger.trace("Malformed command: {}", cleanCommand);
				return;
			}
			if (!context.getChanServ().isConnected()) {
				sendLine("FAILED");
				return ;
			}
			for (int i = 0; i < parent.getRemoteAccounts().size(); i++) {
				if (parent.getRemoteAccounts().get(i).equals(params[1])) {
					sendLine("PROCEED");
					identified = true; // client has successfully identified
					return ;
				}
			}
			sendLine("FAILED");
		} else if (params[0].equals("TESTLOGIN")) {
			if (!identified) {
				return;
			}
			if (params.length != 3) {
				logger.trace("Malformed command: {}", cleanCommand);
				return;
			}
			if (!context.getChanServ().isConnected()) {
				sendLine("LOGINBAD");
				return ;
			}
			queryTASServer("TESTLOGIN " + params[1] + " " + params[2]);
			String reply = waitForReply().toUpperCase();
			if (reply.equals("TESTLOGINACCEPT")) {
				sendLine("LOGINOK");
			} else {
				sendLine("LOGINBAD");
			}
		} else if (params[0].equals("GETACCESS")) {
			if (!identified) {
				return;
			}
			if (params.length != 2) {
				logger.trace("Malformed command: {}", cleanCommand);
				return;
			}
			if (!context.getChanServ().isConnected()) {
				kill();
				return ;
			}
			queryTASServer("GETACCOUNTACCESS " + params[1]);
			String reply = waitForReply();

			// if user not found:
			if (reply.equals("User <" + params[1] + "> not found!")) {
				sendLine("0");
				return ;
			}

			String[] tmp = reply.split(" ");
			int access = 0;
			try {
				access = Integer.parseInt(tmp[tmp.length-1]);
			} catch (NumberFormatException e) { // should not happen
				kill();
				return ;
			}
			sendLine("" + (access & 0x7));
		} else if (params[0].equals("GENERATEUSERID")) {
			if (!identified) {
				return;
			}
			if (params.length != 2) {
				logger.trace("Malformed command: {}", cleanCommand);
				return;
			}
			if (!context.getChanServ().isConnected()) {
				kill();
				return ;
			}
			queryTASServer("FORGEMSG " + params[1] + " ACQUIREUSERID");
			sendLine("OK");
		} else if (params[0].equals("ISONLINE")) {
			if (!identified) {
				return;
			}
			if (params.length != 2) {
				logger.trace("Malformed command: {}", cleanCommand);
				return;
			}
			if (!context.getChanServ().isConnected()) {
				kill();
				return ;
			}

			boolean success = false;
			synchronized(context.getChanServ().clients) {
				for (Client client : context.getChanServ().clients) {
					if (client.getName().equals(params[1])) {
						success = true;
						break;
					}
				}
			} // end of synchronized
			sendLine(success ? "OK" : "NOTOK");
		} else if (params[0].equals("QUERYSERVER")) {
			if (!identified) {
				return;
			}
			if (params.length != 2) {
				logger.trace("Malformed command: {}", cleanCommand);
				return;
			}
			if (!context.getChanServ().isConnected()) {
				kill();
				return ;
			}
			boolean allow = RemoteAccessServer.getAllowedQueryCommands().contains(params[1]);

			if (!allow) {
				// client is trying to execute a command that is not allowed!
				kill();
			}

			queryTASServer(Misc.makeSentence(params, 1));
			String reply = waitForReply();
			sendLine(reply);

			// quick fix for context.getChanServ() crash on adding ban entry in
			// the web interface
			if (Misc.makeSentence(params, 1).equalsIgnoreCase("RETRIEVELATESTBANLIST")) {
				reply = waitForReply(); // wait for the second line of reply
			}

		} else {
			// unknown command!
		}
	}
}
