/*
 * Created on 4.3.2006
 */

package com.springrts.chanserv;


import java.io.*;
import java.net.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.xpath.*;

import org.xml.sax.SAXException;

import org.w3c.dom.*;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Betalord
 */
class KeepAliveTask extends TimerTask {
	public void run() {
		try {
			ChanServ.configLock.acquire();

			ChanServ.sendLine("PING");
			// also save config on regular intervals:
			ChanServ.saveConfig(ChanServ.CONFIG_FILENAME);
		} catch (InterruptedException e) {
			ChanServ.forceDisconnect();
			return ;
		} finally {
			ChanServ.configLock.release();
		}
	}
}

/**
 * For the list of commands, see commands.html!
 *
 * *** LINKS ****
 * * http://java.sun.com/docs/books/tutorial/extra/regex/test_harness.html
 *   (how to use regex in java to match a pattern)
 *
 * * http://www.regular-expressions.info/floatingpoint.html
 *   (how to properly match a floating value in regex)
 *
 * *** NOTES ***
 * * ChanServ MUST use account with admin privileges (not just moderator privileges)
 *   or else it won't be able to join locked channels and won't work correct!
 * * Use Vector for thread-safe list (ArrayList and similar classes aren't thread-safe!)
 *
 * *** TODO QUICK NOTES ***
 * * diff between synchronized(object) {...} and using a Semaphore... ?
 *
 * @author Betalord
 */
public class ChanServ {

	private static final Logger logger = LoggerFactory.getLogger(ChanServ.class);

	static final String VERSION = "0.1";
	static final String CONFIG_FILENAME = "settings.xml";
	static final boolean DEBUG = false;
	private static boolean connected = false; // are we connected to the TASServer?
	static Document config;
	static String serverAddress = "";
	static int serverPort;
	static String username = "";
	static String password = "";
	static Socket socket = null;
	static PrintWriter sockout = null;
	static BufferedReader sockin = null;
	static Timer keepAliveTimer;
	static Timer logCleanerTimer;
	static boolean timersStarted = false;

	static Semaphore configLock = new Semaphore(1, true); // we use it when there is a danger of config object being used by main and TaskTimer threads simultaneously

	static RemoteAccessServer remoteAccessServer;
	static int remoteAccessPort;

	static List<Client> clients = new Vector<Client>();
	static List<Channel> channels = new Vector<Channel>();

	private static List<String> lastMuteList = new Vector<String>(); // list of mute entries for a specified channel (see lastMuteListChannel)
	private static String lastMuteListChannel; // name of channel for which we are currently receiving (or we already did receive) mute list from the server
	private static List<MuteListRequest> forwardMuteList = new Vector<MuteListRequest>(); // list of current requests for mute lists.

	// database related:
	public static DBInterface database;
	private static String DB_URL = "jdbc:mysql://127.0.0.1/ChanServLogs";
	private static String DB_username = "";
	private static String DB_password = "";

	private ChanServ() {}

	public static void closeAndExit() {
		closeAndExit(0);
	}

	public static void closeAndExit(int returncode) {
		AntiSpamSystem.uninitialize();
		if (timersStarted) {
			try {
				stopTimers();
			} catch (Exception e) {
				// ignore
			}
		}
		Log.log("Program stopped.");
		System.exit(returncode);
	}

	public static void forceDisconnect() {
		try {
			socket.close();
		} catch (IOException e) {
			//
		}
	}

	public static boolean isConnected() {
		return connected;
	}

	private static void loadConfig(String fname)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			config = builder.parse(new File(fname));

			XPath xpath = XPathFactory.newInstance().newXPath();
			Node node, node2;

			//node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
			serverAddress = (String)xpath.evaluate("config/account/serveraddress/text()", config, XPathConstants.STRING);
			serverPort = Integer.parseInt((String)xpath.evaluate("config/account/serverport/text()", config, XPathConstants.STRING));
			remoteAccessPort = Integer.parseInt((String)xpath.evaluate("config/account/remoteaccessport/text()", config, XPathConstants.STRING));
			username = (String)xpath.evaluate("config/account/username/text()", config, XPathConstants.STRING);
			password = (String)xpath.evaluate("config/account/password/text()", config, XPathConstants.STRING);

			//node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
			//node.setTextContent("this is a test!");

			// read database info:
			DB_URL = (String)xpath.evaluate("config/database/url/text()", config, XPathConstants.STRING);
			DB_username = (String)xpath.evaluate("config/database/username/text()", config, XPathConstants.STRING);
			DB_password = (String)xpath.evaluate("config/database/password/text()", config, XPathConstants.STRING);

			// load remote access accounts:
			node = (Node)xpath.evaluate("config/remoteaccessaccounts", config, XPathConstants.NODE);
			if (node == null) {
				Log.error("Bad XML document. Path config/remoteaccessaccounts does not exist. Exiting ...");
				closeAndExit(1);
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					RemoteAccessServer.remoteAccounts.add(((Element)node).getAttribute("key"));
				}
				node = node.getNextSibling();
			}

			// load static channel list:
			Channel chan;
			node = (Node)xpath.evaluate("config/channels/static", config, XPathConstants.NODE);
			if (node == null) {
					Log.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
				closeAndExit(1);
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					chan = new Channel(((Element)node).getAttribute("name"));
					chan.setAntiSpam(((Element) node).getAttribute("antispam").equals("yes") ? true : false);
					chan.setAntiSpamSettings(((Element) node).getAttribute("antispamsettings"));
					if (!SpamSettings.validateSpamSettingsString(chan.getAntiSpamSettings())) {
						Log.log("Fixing invalid spam settings for #" + chan.getName() + " ...");
						chan.setAntiSpamSettings(SpamSettings.spamSettingsToString(SpamSettings.DEFAULT_SETTINGS));
					}
					channels.add(chan);
					// apply anti-spam settings:
					AntiSpamSystem.setSpamSettingsForChannel(chan.getName(), chan.getAntiSpamSettings());
				}
				node = node.getNextSibling();
			}

			// load registered channel list:
			node = (Node)xpath.evaluate("config/channels/registered", config, XPathConstants.NODE);
			if (node == null) {
			Log.error("Bad XML document. Path config/channels/registered does not exist. Exiting ...");
			closeAndExit(1);
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					// this is "channel" element
					chan = new Channel(((Element)node).getAttribute("name"));
					chan.setIsStatic(false);
					chan.setTopic(((Element) node).getAttribute("topic"));
					chan.setKey(((Element) node).getAttribute("key"));
					chan.setFounder(((Element) node).getAttribute("founder"));
					chan.setAntiSpam(((Element) node).getAttribute("antispam").equals("yes") ? true : false);
					chan.setAntiSpamSettings(((Element) node).getAttribute("antispamsettings"));
					if (!SpamSettings.validateSpamSettingsString(chan.getAntiSpamSettings())) {
						Log.log("Fixing invalid spam settings for #" + chan.getName() + " ...");
						chan.setAntiSpamSettings(SpamSettings.spamSettingsToString(SpamSettings.DEFAULT_SETTINGS));
					}
					channels.add(chan);
					// load this channel's operator list:
					node2 = node.getFirstChild();
					while (node2 != null) {
						if (node2.getNodeType() == Node.ELEMENT_NODE) {
							// this is "operator" element
							chan.addOperator(((Element)node2).getAttribute("name"));
							//***Log.debug("OPERATOR: " + ((Element)node2).getAttribute("name") + "  (chan " + ((Element)node).getAttribute("name") + ")");
						}
						node2 = node2.getNextSibling();
					}
					// apply anti-spam settings:
					AntiSpamSystem.setSpamSettingsForChannel(chan.getName(), chan.getAntiSpamSettings());
				}
				node = node.getNextSibling();
			}


			Log.log("Config file read.");
		} catch (SAXException sxe) {
			// Error generated during parsing
			Log.error("Error during parsing xml document: " + fname);
			closeAndExit(1);
			/*
			Exception  x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			x.printStackTrace();
			*/
		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			Log.error("Unable to build specified xml parser");
			closeAndExit(1);
			//pce.printStackTrace();

		} catch (IOException ioe) {
			// I/O error
			Log.error("I/O error while accessing " + fname);
			closeAndExit(1);
			//ioe.printStackTrace();
		} catch (XPathExpressionException e) {
			Log.error("Error: XPath expression exception - XML document is malformed.");
			e.printStackTrace();
			closeAndExit(1);
		} catch (Exception e) {
			Log.error("Unknown exception while reading config file: " + fname);
			e.printStackTrace();
			closeAndExit(1);
			//e.printStackTrace();
		}

	}

	public static void saveConfig(String fname) {
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			Node root;
			Node node;
			Node temp;
			Element elem, elem2;
			//Text text; // text node
			Channel chan;

			try {
				// remove all static channels from config and replace it with current static channel list:

				root = (Node)xpath.evaluate("config/channels/static", config, XPathConstants.NODE);
				if (root == null) {
					Log.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
					closeAndExit(1);
				}

				// delete all static channels:
				root.getChildNodes();
				node = root.getFirstChild();
				while (node != null) {
					//if (node.getNodeType() == Node.ELEMENT_NODE) {
						temp = node;
						node = node.getNextSibling();
						root.removeChild(temp);
					//}
				}

				// add new static channels:
				for (int i = 0; i < channels.size(); i++) {
					chan = channels.get(i);
					if (!chan.isIsStatic()) {
						continue;
					}
					root.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					elem = config.createElement("channel");
					elem.setAttribute("name", chan.getName());
					elem.setAttribute("antispam", chan.isAntiSpam() ? "yes" : "no");
					elem.setAttribute("antispamsettings", chan.getAntiSpamSettings());
					//elem.setTextContent(chan.name);
					root.appendChild(elem);
				}
				root.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(4)));

				// remove all registered channels from config and replace it with current registered channel list:

				root = (Node)xpath.evaluate("config/channels/registered", config, XPathConstants.NODE);
				if (root == null) {
					Log.error("Bad XML document. Path config/channels/registered does not exist. Exiting ...");
					closeAndExit(1);
				}

				// delete all channels:
				root.getChildNodes();
				node = root.getFirstChild();
				while (node != null) {
					temp = node;
					node = node.getNextSibling();
					root.removeChild(temp);
				}

				// add new channels:
				for (int i = 0; i < channels.size(); i++) {
					chan = channels.get(i);
					if (chan.isIsStatic()) {
						continue;
					}
					root.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					elem = config.createElement("channel");
					elem.setAttribute("name", chan.getName());
					elem.setAttribute("topic", chan.getTopic());
					elem.setAttribute("key", chan.getKey());
					elem.setAttribute("founder", chan.getFounder());
					elem.setAttribute("antispam", chan.isAntiSpam() ? "yes" : "no");
					elem.setAttribute("antispamsettings", chan.getAntiSpamSettings());

					// write operator list:
					if (chan.getOperatorList().size() > 0) {
						elem.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(8)));
						for (int j = 0; j < chan.getOperatorList().size(); j++) {
							elem2 = config.createElement("operator");
							elem2.setAttribute("name", chan.getOperatorList().get(j));
							elem.appendChild(elem2);
							if (j != chan.getOperatorList().size()-1) {
								elem.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(8)));
							}
						}
						elem.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					}

					root.appendChild(elem);
				}
				root.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(4)));

			} catch (XPathExpressionException e) {
				e.printStackTrace();
				closeAndExit(1);
			}

			// ok save it now:
			config.normalize(); //*** is this needed?
			DOMSource source = new DOMSource(config);
			StreamResult result = new StreamResult(new FileOutputStream(fname));

			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();

			transformer.transform(source, result);

			if (DEBUG) {
				Log.log("Config file saved to " + fname);
			}
		} catch (Exception e) {
			Log.error("Unable to save config file to " + fname + "! Ignoring ...");
		}
	}

	/** multiple threads may call this method */
	public static synchronized void sendLine(String s) {

		if (DEBUG) {
			Log.log("Client: \"" + s + "\"");
		}
		sockout.println(s);
	}

	private static boolean tryToConnect() {
		try {
			Log.log("Connecting to " + serverAddress + ":" + serverPort + " ...");
			socket = new Socket(serverAddress, serverPort);
			sockout = new PrintWriter(socket.getOutputStream(), true);
			sockin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (UnknownHostException e) {
			Log.error("Unknown host error: " + serverAddress);
			return false;
		} catch (IOException e) {
			Log.error("Couldn't get I/O for the connection to: " + serverAddress);
			return false;
		}

		Log.log("Now connected to " + serverAddress);
		return true;
	}

	public static void messageLoop() {
		String line;
		while (true) {
			try {
				line = sockin.readLine();
			} catch (IOException e) {
				Log.error("Connection with server closed with exception.");
				break;
			}
			if (line == null) {
				break;
			}
			if (DEBUG) {
				Log.log("Server: \"" + line + "\"");
			}

			// parse command and respond to it:
			try {
				configLock.acquire();
				execRemoteCommand(line);
			} catch (InterruptedException e) {
				//return ;
			} finally {
				configLock.release();
			}

		}

		try {
			sockout.close();
			sockin.close();
			socket.close();
		} catch (IOException e) {
			// do nothing
		}
		Log.log("Connection with server closed.");
	}

	// processes messages that were only sent to server admins. "message" parameter must be a
	// message string withouth the "[broadcast to all admins]: " part.
	public static void processAdminBroadcast(String message) {
		Log.debug("admin broadcast: '" + message + "'");

		// let's check if some channel founder/operator has just renamed his account:
		if (message.matches("User <[^>]{1,}> has just renamed his account to <[^>]{1,}>")) {
			String oldNick = message.substring(message.indexOf('<')+1, message.indexOf('>'));
			String newNick = message.substring(message.indexOf('<', message.indexOf('>'))+1, message.indexOf('>', message.indexOf('>')+1));

			// lets rename all founder/operator entries for this user:
			for (int i = 0; i < channels.size(); i++) {
				Channel chan = channels.get(i);
				if (chan.isFounder(oldNick)) {
					chan.renameFounder(newNick);
					Log.log("Founder <" + oldNick + "> of #" + chan.getName() + " renamed to <" + newNick + ">");
				}
				if (chan.isOperator(oldNick)) {
					chan.renameOperator(oldNick, newNick);
					Log.log("Operator <" + oldNick + "> of #" + chan.getName() + " renamed to <" + newNick + ">");
				}
			}
		}
	}

	public static boolean execRemoteCommand(String command) {
		if (command.trim().equals("")) {
			return false;
		}

		// try to extract message ID if present:
		if (command.charAt(0) == '#') {
			try {
				if (!command.matches("^#\\d+\\s[\\s\\S]*")) {
					// malformed command
					return false;
				}
				int ID = Integer.parseInt(command.substring(1).split("\\s")[0]);
				// remove ID field from the rest of command:
				command = command.replaceFirst("#\\d+\\s", "");
				// forward the command to the waiting thread:
				synchronized (remoteAccessServer.threads) {
					for (RemoteClientThread rct : remoteAccessServer.threads) {
						if (rct.ID == ID) {
							try {
								rct.replyQueue.put(command);
							} catch (InterruptedException ex) {
								ex.printStackTrace();
								return false;
							}
							return true;
						}
					}
				}
				return false; // no suitable thread found! Perhaps thread already finished before it could read the response (not a problem)
			} catch (NumberFormatException e) {
				return false; // this means that the command is malformed
			} catch (PatternSyntaxException e) {
				return false; // this means that the command is malformed
			}
		}

		String[] commands = command.split(" ");
		commands[0] = commands[0].toUpperCase();

		if (commands[0].equals("TASSERVER")) {
			sendLine("LOGIN " + username + " " + password + " 0 * ChanServ " + VERSION);
		} else if (commands[0].equals("ACCEPTED")) {
			Log.log("Login accepted.");
			// join registered and static channels:
			for (Channel channel : channels) {
				sendLine("JOIN " + channel.getName());
			}
		} else if (commands[0].equals("DENIED")) {
			Log.log("Login denied. Reason: " + Misc.makeSentence(commands, 1));
			closeAndExit();
		} else if (commands[0].equals("AGREEMENT")) {
			// not done yet. Should respond with CONFIRMAGREEMENT and then resend LOGIN command.
			Log.log("Server is requesting agreement confirmation. Cancelling ...");
			closeAndExit();
		} else if (commands[0].equals("ADDUSER")) {
			clients.add(new Client(commands[1]));
		} else if (commands[0].equals("REMOVEUSER")) {
			for (Client client : clients) {
				if (client.getName().equals(commands[1])) {
					clients.remove(client);
					break;
				}
			}
		} else if (commands[0].equals("CLIENTSTATUS")) {
			for (Client client : clients) {
				if (client.getName().equals(commands[1])) {
					client.setStatus(Integer.parseInt(commands[2]));
					AntiSpamSystem.processClientStatusChange(client);
					break;
				}
			}
		} else if (commands[0].equals("JOIN")) {
			Log.log("Joined #" + commands[1]);
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			chan.setJoined(true);
			chan.clearClients();
			// set topic, lock channel, ... :
			if (!chan.isIsStatic()) {
				if (!chan.getKey().equals("")) {
					sendLine("SETCHANNELKEY " + chan.getName() + " " + chan.getKey());
				}
				if (chan.getTopic().equals("")) {
					sendLine("CHANNELTOPIC " + chan.getName() + " *");
				} else {
					sendLine("CHANNELTOPIC " + chan.getName() + " " + chan.getTopic());
				}
			}
		} else if (commands[0].equals("CLIENTS")) {
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			for (int i = 2; i < commands.length; i++) {
				chan.addClient(commands[i]);
			}
		} else if (commands[0].equals("JOINED")) {
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			chan.addClient(commands[2]);
			Log.toFile(chan.getLogFileName(), "* " + commands[2] + " has joined " + "#" + chan.getName());
		} else if (commands[0].equals("LEFT")) {
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			chan.removeClient(commands[2]);
			String out = "* " + commands[2] + " has left " + "#" + chan.getName();
			if (commands.length > 3) {
				out = out + " (" + Misc.makeSentence(commands, 3) + ")";
			}
			Log.toFile(chan.getLogFileName(), out);
		} else if (commands[0].equals("JOINFAILED")) {
			channels.add(new Channel(commands[1]));
			Log.log("Failed to join #" + commands[1] + ". Reason: " + Misc.makeSentence(commands, 2));
		} else if (commands[0].equals("CHANNELTOPIC")) {
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			chan.setTopic(Misc.makeSentence(commands, 4));
			Log.toFile(chan.getLogFileName(), "* Channel topic is '" + chan.getTopic() + "' set by " + commands[2]);
		} else if (commands[0].equals("SAID")) {
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			String user = commands[2];
			String msg = Misc.makeSentence(commands, 3);
			if (chan.isAntiSpam()) {
				AntiSpamSystem.processUserMsg(chan.getName(), user, msg);
			}
			Log.toFile(chan.getLogFileName(), "<" + user + "> " + msg);
			if ((msg.length() > 0) && (msg.charAt(0) == '!')) {
				processUserCommand(msg.substring(1, msg.length()), getClient(user), chan);
			}
		} else if (commands[0].equals("SAIDEX")) {
			Channel chan = getChannel(commands[1]);
			if (chan == null) {
				// this could happen just after we unregistered the channel,
				// since there is always some lag between us and the server
				return false;
			}
			String user = commands[2];
			String msg = Misc.makeSentence(commands, 3);
			if (chan.isAntiSpam()) {
				AntiSpamSystem.processUserMsg(chan.getName(), user, msg);
			}
			Log.toFile(chan.getLogFileName(), "* " + user + " " + msg);
		} else if (commands[0].equals("SAIDPRIVATE")) {

			String user = commands[1];
			String msg = Misc.makeSentence(commands, 2);

			Log.toFile(user + ".log", "<" + user + "> " + msg);
			if ((msg.length() > 0) && (msg.charAt(0)) == '!') {
				processUserCommand(msg.substring(1, msg.length()), getClient(user), null);
			}
		} else if (commands[0].equals("SERVERMSG")) {
			Log.log("Message from server: " + Misc.makeSentence(commands, 1));
			if (Misc.makeSentence(commands, 1).startsWith("[broadcast to all admins]")) {
				processAdminBroadcast(Misc.makeSentence(commands, 1).substring("[broadcast to all admins]: ".length(), Misc.makeSentence(commands, 1).length()));
			}
		} else if (commands[0].equals("SERVERMSGBOX")) {
			Log.log("MsgBox from server: " + Misc.makeSentence(commands, 1));
		} else if (commands[0].equals("CHANNELMESSAGE")) {
			Channel chan = getChannel(commands[1]);
			if (chan != null) {
				String out = "* Channel message: " + Misc.makeSentence(commands, 2);
				Log.toFile(chan.getLogFileName(), out);
			}
		} else if (commands[0].equals("BROADCAST")) {
			Log.log("*** Broadcast from server: " + Misc.makeSentence(commands, 1));
		} else if (commands[0].equals("MUTELISTBEGIN")) {
			lastMuteList.clear();
			lastMuteListChannel = commands[1];
		} else if (commands[0].equals("MUTELIST")) {
			lastMuteList.add(Misc.makeSentence(commands, 1));
		} else if (commands[0].equals("MUTELISTEND")) {
			int i = 0;
			while (i < forwardMuteList.size()) {
				MuteListRequest request = forwardMuteList.get(i);
				if (!request.getChannelName().equals(lastMuteListChannel)) {
					i++;
					continue;
				}
				Client target = getClient(request.getSendTo());
				if (target == null) { // user who made the request has already gone offline!
					forwardMuteList.remove(i);
					continue;
				}
				if (System.currentTimeMillis() - request.getRequestTime() > 10000) { // this request has already expired
					forwardMuteList.remove(i);
					continue;
				}
				// forward the mute list to the one who requested it:
				if (lastMuteList.isEmpty()) {
					sendMessage(target, getChannel(request.getReplyToChan()), "Mute list for #" + request.getChannelName() + " is empty!");
				}
				else {
					sendMessage(target, getChannel(request.getReplyToChan()), "Mute list for #" + request.getChannelName()+ " (" + lastMuteList.size() + " entries):");
					for (String lastMute : lastMuteList) {
						sendMessage(target, getChannel(request.getReplyToChan()), lastMute);
					}
				}
				forwardMuteList.remove(i);
			}
		}


		return true;
	}

	/**
	 * If the command was issued from a private chat,
	 * then the "channel" parameter should be <code>null</code>.
	 */
	public static void processUserCommand(String command, Client client, Channel channel) {

		command = command.trim();
		if (command.equals("")) {
			return;
		}
		String[] splitParams = command.split("[\\s]+");
		List<String> params = new LinkedList<String>();
		params.addAll(Arrays.asList(splitParams));
		// convert the base command
		String commandName = params.get(0).toUpperCase();
		params.remove(0);

		if (commandName.equals("HELP")) {
			// force the message to be sent to private chat rather than
			// to the channel (to avoid unneccessary bloating the channel):
			sendMessage(client, null, "Hello, " + client.getName() + "!");
			sendMessage(client, null, "I am an automated channel service bot,");
			sendMessage(client, null, "for the full list of commands, see http://spring.clan-sy.com/dl/ChanServCommands.html");
			sendMessage(client, null, "If you want to go ahead and register a new channel, please contact one of the server moderators!");
		} else if (commandName.equals("INFO")) {
			// if the command was issued from a channel:
			if (channel != null) {
				// insert <channame> parameter so we do not have to handle
				// two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if (chan == null) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (chan.isIsStatic()) {
				sendMessage(client, channel, "Channel #" + chanName + " is registered as a static channel, no further info available!");
				return ;
			}

			StringBuilder respond = new StringBuilder("Channel #");
			respond.append(chan.getName()).append(" info: Anti-spam protection is ");
			respond.append(chan.isAntiSpam() ? "on" : "off").append(". Founder is <").append(chan.getFounder()).append(">, ");
			List<String> ops = chan.getOperatorList();
			if (ops.isEmpty()) {
				respond.append("no operators are registered.");
			} else if (ops.size() == 1) {
				respond.append("1 registered operator is <").append(ops.get(0)).append(">.");
			} else {
				respond.append(ops.size()).append(" registered operators are ");
				for (int i = 0; i < ops.size()-1; i++) {
					respond.append("<").append(ops.get(i)).append(">, ");
				}
				respond.append("<").append(ops.get(ops.size() - 1)).append(">.");
			}

			sendMessage(client, channel, respond.toString());
		} else if (commandName.equals("REGISTER")) {
			if (!client.isModerator()) {
				sendMessage(client, channel, "Sorry, you'll have to contact one of the server moderators to register a channel for you!");
				return ;
			}

			if (params.size() != 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1, params.get(0).length());
			String valid = Channel.isChanNameValid(chanName);
			if (valid != null) {
				sendMessage(client, channel, "Error: Bad channel name (" + valid + ")");
				return ;
			}

			for (Channel chan : channels) {
				if (chan.getName().equals(chanName)) {
					if (chan.isIsStatic()) {
						sendMessage(client, channel, "Error: channel #" + chanName + " is a static channel (cannot register it)!");
					} else {
						sendMessage(client, channel, "Error: channel #" + chanName + " is already registered!");
					}
					return;
				}
			}

			// ok register the channel now:
			Channel chan = new Channel(chanName);
			channels.add(chan);
			chan.setFounder(params.get(1));
			chan.setIsStatic(false);
			chan.setAntiSpam(false);
			chan.setAntiSpamSettings(SpamSettings.spamSettingsToString(SpamSettings.DEFAULT_SETTINGS));
			sendLine("JOIN " + chan.getName());
			sendMessage(client, channel, "Channel #" + chanName + " successfully registered to " + chan.getFounder());
		} else if (commandName.equals("CHANGEFOUNDER")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			// just to protect from flooding the bot with long usernames:
			if (params.get(1).length() > 30) {
				sendMessage(client, channel, "Error: Too long username!");
				return ;
			}

			// set founder:
			chan.setFounder(params.get(1));

			sendMessage(client, channel, "You've successfully set founder of #" + chanName + " to <" + chan.getFounder() + ">");
			sendLine("CHANNELMESSAGE " + chan.getName() + " <" + chan.getFounder() + "> has just been set as this channel's founder");
		} else if (commandName.equals("UNREGISTER")) {
			if (params.size() != 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			// ok unregister the channel now:
			channels.remove(chan);
			sendLine("CHANNELMESSAGE " + chan.getName() + " " + "This channel has just been unregistered from <" + username + "> by <" + client.getName() + ">");
			sendMessage(client, channel, "Channel #" + chanName + " successfully unregistered!");
			sendLine("LEAVE " + chan.getName());
		} else if (commandName.equals("ADDSTATIC")) {
			if (!client.isModerator()) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			if (params.size() != 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);

			for (Channel chan : channels) {
				if (chan.getName().equals(chanName)) {
					if (chan.isIsStatic()) {
						sendMessage(client, channel, "Error: channel #" + chanName + " is already static!");
					} else {
						sendMessage(client, channel, "Error: channel #" + chanName + " is already registered! (unregister it first and then add it to static list)");
					}
					return;
				}
			}

			// ok add the channel to static list:
			Channel chan = new Channel(chanName);
			channels.add(chan);
			chan.setIsStatic(true);
			chan.setAntiSpam(false);
			chan.setAntiSpamSettings(SpamSettings.spamSettingsToString(SpamSettings.DEFAULT_SETTINGS));
			sendLine("JOIN " + chan.getName());
			sendMessage(client, channel, "Channel #" + chanName + " successfully added to static list.");
		} else if (commandName.equals("REMOVESTATIC")) {
			if (params.size() != 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (!chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not in the static channel list!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			// ok remove the channel from static channel list now:
			channels.remove(chan);
			sendMessage(client, channel, "Channel #" + chanName + " successfully removed from static channel list!");
			sendLine("LEAVE " + chan.getName());
		} else if (commandName.equals("OP")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			if (chan.isOperator(params.get(1))) {
				sendMessage(client, channel, "Error: User is already in this channel's operator list!");
				return ;
			}

			// just to protect from flooding the bot with long usernames:
			if (params.get(1).length() > 30) {
				sendMessage(client, channel, "Error: Too long username!");
				return ;
			}

			if (chan.getOperatorList().size() > 100) {
				sendMessage(client, channel, "Error: Too many operators (100) registered. This is part of a bot-side protection against flooding, if you think you really need more operators assigned, please contact bot maintainer.");
				return ;
			}

			// ok add user to channel's operator list:
			chan.addOperator(params.get(1));
			sendLine("CHANNELMESSAGE " + chan.getName() + " <" + params.get(1) + "> has just been added to this channel's operator list by <" + client.getName() + ">");
		} else if (commandName.equals("DEOP")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			if (!chan.isOperator(params.get(1))) {
				sendMessage(client, channel, "Error: User <" + params.get(1) + "> is not in this channel's operator list!");
				return ;
			}

			// ok remove user from channel's operator list:
			chan.removeOperator(params.get(1));
			sendLine("CHANNELMESSAGE " + chan.getName() + " <" + params.get(1) + "> has just been removed from this channel's operator list by <" + client.getName() + ">");
		} else if (commandName.equals("SPAMPROTECTION")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if ((params.size() != 2) && (params.size() != 1)) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if (chan == null) {
				sendMessage(client, channel, "Channel #" + chanName + " does not exist!");
				return ;
			}

			if (params.size() == 1) {
				sendMessage(client, channel, "Anti-spam protection for channel #" + chan.getName() + " is " + (chan.isAntiSpam() ? "on (settings: " + chan.getAntiSpamSettings() + ")" : "off"));
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			if (params.get(1).toUpperCase().equals("ON")) {
				sendMessage(client, channel, "Anti-spam protection has been enabled for #" + chan.getName());
				sendLine("CHANNELMESSAGE " + chan.getName() + " Anti-spam protection for channel #" + chan.getName() + " has been enabled");
				chan.setAntiSpam(true);
				return ;
			} else if (params.get(1).toUpperCase().equals("OFF")) {
				sendMessage(client, channel, "Anti-spam protection has been disabled for #" + chan.getName());
				sendLine("CHANNELMESSAGE " + chan.getName() + " Anti-spam protection for channel #" + chan.getName() + " has been disabled");
				chan.setAntiSpam(false);
				return ;
			} else {
				sendMessage(client, channel, "Error: Invalid parameter (\"" + params.get(1) + "\"). Valid is \"on|off\"");
				return ;
			}
		} else if (commandName.equals("SPAMSETTINGS")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 6) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if (chan == null) {
				sendMessage(client, channel, "Channel #" + chanName + " does not exist!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			String settings = Misc.makeSentence(params, 1);

			if (!SpamSettings.validateSpamSettingsString(settings)) {
				sendMessage(client, channel, "Invalid 'settings' parameter!");
				return ;
			}

			chan.setAntiSpamSettings(settings);
			AntiSpamSystem.setSpamSettingsForChannel(chan.getName(), chan.getAntiSpamSettings());
			sendMessage(client, channel, "Anti-spam settings successfully updated (" + chan.getAntiSpamSettings() + ")");
		} else if (commandName.equals("TOPIC")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() < 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String topic;
			if (params.size() == 1) {
				topic = "*";
			} else {
				topic = Misc.makeSentence(params, 1);
			}
			if (topic.trim().equals("")) {
				topic = "*";
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			// ok set the topic:
			sendLine("CHANNELTOPIC " + chan.getName() + " " + topic);
		} else if (commandName.equals("CHANMSG")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() < 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String msg = Misc.makeSentence(params, 1);
			if (msg.trim().equals("")) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			// ok send the channel message:
			sendLine("CHANNELMESSAGE " + chan.getName() + " issued by <" + client.getName() + ">: " + msg);
		} else if (commandName.equals("LOCK")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			if (!Misc.isValidName(params.get(1))) {
				sendMessage(client, channel, "Error: key contains some invalid characters!");
				return ;
			}

			// ok lock the channel:
			sendLine("SETCHANNELKEY " + chan.getName() + " " + params.get(1));
			if (params.get(1).equals("*")) {
				chan.setKey("");
			} else {
				chan.setKey(params.get(1));
			}
		} else if (commandName.equals("UNLOCK")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			// ok unlock the channel:
			sendLine("SETCHANNELKEY " + chan.getName() + " *");
			chan.setKey("");
		} else if (commandName.equals("KICK")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() < 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			String target = params.get(1);
			if (!chan.clientExists(target)) {
				sendMessage(client, channel, "Error: <" + target + "> not found in #" + chanName + "!");
				return ;
			}

			if (target.equals(username)) {
				// not funny!
				sendMessage(client, channel, "You are not allowed to issue this command!");
				return ;
			}

			String reason = "";
			if (params.size() > 2) {
				reason = " " + Misc.makeSentence(params, 2);
			}

			// ok kick the user:
			sendLine("FORCELEAVECHANNEL " + chan.getName() + " " + target + reason);
		} else if (commandName.equals("MUTE")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() < 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			String target = params.get(1);
			if (getClient(target) == null) {
				sendMessage(client, channel, "Error: Invalid username - <" + target + "> does not exist or is not online. Command dropped.");
				return ;
			}

			if (target.equals(username)) {
				// not funny!
				sendMessage(client, channel, "You are not allowed to issue this command!");
				return ;
			}

			int duration = 0;
			if (params.size() == 3) {
				try {
					duration = Integer.parseInt(params.get(2));
				} catch (Exception e) {
					sendMessage(client, channel, "Error: <duration> argument should be an integer!");
					return;
				}
			}

			// ok mute the user:
			sendLine("MUTE " + chan.getName() + " " + target + " " + duration);
		} else if (commandName.equals("UNMUTE")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 2) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			String target = params.get(1);

			// ok try to unmute the user:
			sendLine("UNMUTE " + chan.getName() + " " + target);
		} else if (commandName.equals("MUTELIST")) {
			// if the command was issued from a channel:
			if (channel != null) { // insert <channame> parameter so we don't have to handle two different situations for each command
				params.add(0, "#" + channel.getName());
			}

			if (params.size() != 1) {
				sendMessage(client, channel, "Error: Invalid params!");
				return ;
			}

			if (params.get(0).charAt(0) != '#') {
				sendMessage(client, channel, "Error: Bad channel name (forgot #?)");
				return ;
			}

			String chanName = params.get(0).substring(1);
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isIsStatic())) {
				sendMessage(client, channel, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.getName().equals(chan.getFounder()) || chan.isOperator(client.getName()))) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			forwardMuteList.add(new MuteListRequest(chanName, client.getName(), System.currentTimeMillis(), (channel != null) ? channel.getName() : ""));
			sendLine("MUTELIST " + chan.getName());
		} else if (commandName.equals("SHUTDOWN")) {
			if (!client.isModerator()) {
				sendMessage(client, channel, "Insufficient access to execute " + commandName + " command!");
				return ;
			}

			String reason = "restarting ..."; // default reason text

			if (params.size() > 0) {
				reason = Misc.makeSentence(params, 0);
			}

			for (Channel chan : channels) {
				// skip static channels
				if (!chan.isIsStatic()) {
					sendLine("SAYEX " + chan.getName() + " is quitting. Reason: " + reason);
				}
			}

			// stop the program:
			stopTimers();
			saveConfig(CONFIG_FILENAME);
			closeAndExit();
		}
	}

	public static void sendPrivateMsg(Client client, String msg) {

		sendLine("SAYPRIVATE " + client.getName() + " " + msg);
		Log.toFile(client.getName() + ".log", "<" + username + "> " + msg);
	}

	/**
	 * This method will send a message either to a client or a channel.
	 * This method decides what to do, by examining the 'client' and 'chan'
	 * parameters (null / not null):
	 *
	 *  1) client  && chan  - will send a msg to a channel with client's username in front of it
	 *  2) client  && !chan - will send a private msg to the client
	 *  3) !client && chan  - will send a msg to a channel (general message withouth any prefix)
	 *  4) !client && !chan - invalid parameters
	 */
	public static void sendMessage(Client client, Channel chan, String msg) {

		if ((client == null) && (chan == null)) {
			// this should not happen!
			logger.warn("sendMessage() called, with neither a client nor a channel specified");
			return;
		} else if ((client == null) && (chan != null)) {
			chan.sendMessage(msg);
		} else if ((client != null) && (chan != null)) {
			chan.sendMessage(client.getName() + ": " + msg);
		} else if ((client != null) && (chan == null)) {
			sendPrivateMsg(client, msg);
		}
	}

	public static Client getClient(String username) {

		for (Client client : clients) {
			if (client.getName().equals(username)) {
				return client;
			}
		}

		return null;
	}

	/** Returns <code>null</code> if the channel is not found */
	public static Channel getChannel(String name) {

		for (Channel channel : channels) {
			if (channel.getName().equals(name)) {
				return channel;
			}
		}

		return null;
	}

	public static void startTimers() {

		keepAliveTimer = new Timer();
		keepAliveTimer.schedule(new KeepAliveTask(),
				1000,		//initial delay
				15*1000);  //subsequent rate

		logCleanerTimer = new Timer();
		logCleanerTimer.schedule(new LogCleaner(),
				5000,		//initial delay
				20*1000);  //subsequent rate

		timersStarted = true;
	}

	public static void stopTimers() {
		try {
			keepAliveTimer.cancel();
			logCleanerTimer.cancel();
		} catch (Exception e) {
			//
		}
	}

	public static void main(String[] args) {

		Log.externalLogFileName = "$main.log";
		Log.useExternalLogging = true;

		// check if LOG_FOLDER folder exists, if not then create it:
		File file = new File(Log.LOG_FOLDER);
		if (!file.exists()) {
			if (!file.mkdir()) {
				Log.error("unable to create " + Log.LOG_FOLDER + " folder! Exiting ...");
				closeAndExit(1);
			} else {
				Log.log("Folder " + Log.LOG_FOLDER + " has been created");
			}
		}

		Log.log("ChanServ started on " + Misc.easyDateFormat("dd/MM/yy"));
		Log.log("");

		// it is vital that we initialize AntiSpamSystem before loading the configuration file (since we will configure AntiSpamSystem too)
		AntiSpamSystem.initialize();

		loadConfig(CONFIG_FILENAME);
		saveConfig(CONFIG_FILENAME); //*** debug

		// run remote access server:
		Log.log("Trying to run remote access server on port " + remoteAccessPort + " ...");
		remoteAccessServer = new RemoteAccessServer(remoteAccessPort);
		remoteAccessServer.start();

		// establish connection with database:
		database = new DBInterface();
		if (!database.loadJDBCDriver()) {
			closeAndExit(1);
		}
		if (!database.connectToDatabase(DB_URL, DB_username, DB_password)) {
			closeAndExit(1);
		}

		if (!tryToConnect()) {
			closeAndExit(1);
		} else {
			startTimers();
			connected = true;
			messageLoop();
			connected = false;
			stopTimers();
		}

		// we are out of the main loop (due to an error, for example), lets reconnect:
		while (true) {
			try {
				Thread.sleep(10000); // wait for 10 secs before trying to reconnect
			} catch (InterruptedException e) {
			}

			Log.log("Trying to reconnect to the server ...");
			if (!tryToConnect()) {
				continue;
			}
			startTimers();
			connected = true;
			messageLoop();
			connected = false;
			stopTimers();
		}

		// AntiSpamSystem.uninitialize(); -> this code is unreachable. We call it in closeAndExit() method!

	}
}
