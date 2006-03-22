/*
 * Created on 4.3.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 *
 * For the list of commands, see commands.html!
 * 
 * *** LINKS ****
 * * http://java.sun.com/docs/books/tutorial/extra/regex/test_harness.html
 *   (how to use regex in java to match a pattern)
 *   
 * *** NOTES ***
 * * ChanServ MUST use account with admin privileges (not just moderator privileges)
 *   or else it won't be able to join locked channels and won't work correct!
 * * Use Vector for thread-safe list (ArrayList and similar classes aren't thread-safe!)    
 * 
 * *** TODO QUICK NOTES ***
 * * diff between synchronized(object) {...} and using a Semaphore... ?
 * * currently the "channel" parameter is not used in processUserCommand()
 *   (channel name should be passed as well to make it useful) 
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.io.*;
import java.net.*;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.xpath.*;
 
import org.xml.sax.SAXException;  
import org.xml.sax.SAXParseException;  

import org.w3c.dom.*;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


class KeepAliveTask extends TimerTask {
	public void run() {
		try {
			ChanServ.configLock.acquire();

			ChanServ.sendLine("PING");
			// also save config on regular intervals:
			ChanServ.saveConfig(ChanServ.CONFIG_FILENAME);
		} catch (InterruptedException e) {
			return ;
		} finally {
			ChanServ.configLock.release();
		}
	}
}

public class ChanServ {

	static final String VERSION = "0.1";
	static final String CONFIG_FILENAME = "settings.xml";
	static final boolean DEBUG = false;
	static Document config; 
	static String serverAddress = "";
	static int serverPort;
	static String username = "";
	static String password = "";
	static Socket socket = null;
    static PrintWriter sockout = null;
    static BufferedReader sockin = null;
    
    static Semaphore configLock = new Semaphore(1, true); // we use it when there is a danger of config object being used by main and TaskTimer threads simultaneously
    
    static Vector/*Client*/ clients = new Vector();
    static Vector/*Channel*/ channels = new Vector();
	
    public static void closeAndExit() {
    	closeAndExit(0);
    }
    	
	public static void closeAndExit(int returncode) {
		Log.log("Program stopped.");
		System.exit(returncode);
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
           username = (String)xpath.evaluate("config/account/username/text()", config, XPathConstants.STRING);
           password = (String)xpath.evaluate("config/account/password/text()", config, XPathConstants.STRING);
           
           //node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
           //node.setTextContent("this is a test!");

           // load static channel list:
           node = (Node)xpath.evaluate("config/channels/static", config, XPathConstants.NODE);
           if (node == null) {
           		Log.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
				closeAndExit(1);
           }
           node = node.getFirstChild();
           while (node != null) {
           	if (node.getNodeType() == Node.ELEMENT_NODE) {
           		channels.add(new Channel(((Element)node).getAttribute("name")));
			}
			node = node.getNextSibling();
           }
           
           // load registered channel list:
           Channel chan;
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
					chan.isStatic = false;
					chan.topic = ((Element)node).getAttribute("topic");
					chan.key = ((Element)node).getAttribute("key");
					chan.founder = ((Element)node).getAttribute("founder");
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
	        Text text; // text node
	        Channel chan;
	        
	        try {
				// remove all static channels from config and replace it with current static channel list:
	        	
				root = (Node)xpath.evaluate("config/channels/static", config, XPathConstants.NODE);
				if (root == null) {
					Log.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
					closeAndExit(1);
				}
				
				// delete all channels:
				root.getChildNodes();
				node = root.getFirstChild();
				while (node != null) {
					//if (node.getNodeType() == Node.ELEMENT_NODE) {
						temp = node;
						node = node.getNextSibling();
						root.removeChild(temp);
					//}
				}
				
				// add new channels:
				for (int i = 0; i < channels.size(); i++) {
					chan = (Channel)channels.get(i);
					if (!chan.isStatic) continue;
					root.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					elem = config.createElement("channel");
					elem.setAttribute("name", chan.name);
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
					chan = (Channel)channels.get(i);
					if (chan.isStatic) continue;
					root.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					elem = config.createElement("channel");
					elem.setAttribute("name", chan.name);
					elem.setAttribute("topic", chan.topic);
					elem.setAttribute("key", chan.key);
					elem.setAttribute("founder", chan.founder);

					// write operator list:
					if (chan.getOperatorList().size() > 0) {
						elem.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(8)));
						for (int j = 0; j < chan.getOperatorList().size(); j++) {
							elem2 = config.createElement("operator");
							elem2.setAttribute("name", (String)chan.getOperatorList().get(j));
							elem.appendChild(elem2);
							if (j != chan.getOperatorList().size()-1)
								elem.appendChild(config.createTextNode(Misc.EOL + Misc.enumSpaces(8)));
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
	        
	        if (DEBUG) Log.log("Config file saved to " + fname);
		} catch (Exception e) {
			Log.error("Unable to save config file to " + fname + "! Ignoring ...");
		}
	}
	
	public static void sendLine(String s) {
		if (DEBUG) Log.log("Client: \"" + s + "\"");
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
            closeAndExit(1);
        } catch (IOException e) {
            Log.error("Couldn't get I/O for the connection to: " + serverAddress);
            closeAndExit(1);
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
        	if (line == null) break;
            if (DEBUG) Log.log("Server: \"" + line + "\"");
            
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
	}
	
	public static boolean execRemoteCommand(String command) {
		if (command.trim().equals("")) return false;
		String[] commands = command.split(" ");
		commands[0] = commands[0].toUpperCase();

		if (commands[0].equals("TASSERVER")) {
			sendLine("LOGIN " + username + " " + password + " 0 * ChanServ " + VERSION);
		} else if (commands[0].equals("ACCEPTED")) {
			Log.log("Login accepted.");
			// join registered and static channels:
			for (int i = 0; i < channels.size(); i++)
				sendLine("JOIN " + ((Channel)channels.get(i)).name);
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
			for (int i = 0; i < clients.size(); i++) if (((Client)clients.get(i)).name.equals(commands[1])) {
				clients.remove(i);
				break;
			}
		} else if (commands[0].equals("CLIENTSTATUS")) {
			for (int i = 0; i < clients.size(); i++) if (((Client)clients.get(i)).name.equals(commands[1])) {
				((Client)clients.get(i)).setStatus(Integer.parseInt(commands[2]));
				break;
			}
		} else if (commands[0].equals("JOIN")) {
			Log.log("Joined #" + commands[1]);
			Channel chan = getChannel(commands[1]);
			chan.joined = true;
			chan.clients.clear();
			// put "logging started" header in the log file:
			Misc.outputLog(chan.logFileName, "");
			Misc.outputLog(chan.logFileName, "Log started on " + Misc.easyDateFormat("dd/MM/yy"));
			// set topic, lock channel, ... :
			if (!chan.isStatic) {
				if (!chan.key.equals(""))
					sendLine("SETCHANNELKEY " + chan.name + " " + chan.key);
				if (chan.topic.equals(""))
					sendLine("CHANNELTOPIC " + chan.name + " *");
				else
					sendLine("CHANNELTOPIC " + chan.name + " " + chan.topic);
			}
		} else if (commands[0].equals("CLIENTS")) { 
			Channel chan = getChannel(commands[1]);
			for (int i = 2; i < commands.length; i++) {
				chan.clients.add(commands[i]);
			}
		} else if (commands[0].equals("JOINED")) {
			Channel chan = getChannel(commands[1]);
			chan.clients.add(commands[2]);
			Misc.outputLog(chan.logFileName, Misc.easyDateFormat("[HH:mm:ss]") + " * " + commands[2] + " has joined " + "#" + chan.name);
		} else if (commands[0].equals("LEFT")) { 
			Channel chan = getChannel(commands[1]);
			chan.clients.remove(commands[2]);
			String out = Misc.easyDateFormat("[HH:mm:ss]") + " * " + commands[2] + " has left " + "#" + chan.name;
			if (commands.length > 3)
				out = out + " (" + Misc.makeSentence(commands, 3) + ")";
			Misc.outputLog(chan.logFileName, out);
		} else if (commands[0].equals("JOINFAILED")) {
			channels.add(new Channel(commands[1]));
			Log.log("Failed to join #" + commands[1] + ". Reason: " + Misc.makeSentence(commands, 2));
		} else if (commands[0].equals("CHANNELTOPIC")) {
			Channel chan = getChannel(commands[1]);
			chan.topic = Misc.makeSentence(commands, 4);
			Misc.outputLog(chan.logFileName, Misc.easyDateFormat("[HH:mm:ss]") + " * Channel topic is '" + chan.topic + "' set by " + commands[2]);
		} else if (commands[0].equals("SAID")) {
			Channel chan = getChannel(commands[1]);
			String user = commands[2];
			String msg = Misc.makeSentence(commands, 3);
			
			Misc.outputLog(chan.logFileName, Misc.easyDateFormat("[HH:mm:ss]") + " <" + user + "> " + msg);
			if (msg.charAt(0) == '!') processUserCommand(msg.substring(1, msg.length()), getClient(user), true);
		} else if (commands[0].equals("SAIDEX")) {
			Channel chan = getChannel(commands[1]);
			String user = commands[2];
			String msg = Misc.makeSentence(commands, 3);
			Misc.outputLog(chan.logFileName, Misc.easyDateFormat("[HH:mm:ss]") + " * " + user + " " + msg);
		} else if (commands[0].equals("SAIDPRIVATE")) {
			
			String user = commands[1];
			String msg = Misc.makeSentence(commands, 2);
			
			Misc.outputLog(user + ".log", Misc.easyDateFormat("[dd/MM/yy HH:mm:ss]") + " <" + user + "> " + msg);
			if (msg.charAt(0) == '!') processUserCommand(msg.substring(1, msg.length()), getClient(user), false);
		} else if (commands[0].equals("SERVERMSG")) {
			Log.log("Message from server: " + Misc.makeSentence(commands, 1));
		} else if (commands[0].equals("SERVERMSGBOX")) {
			Log.log("MsgBox from server: " + Misc.makeSentence(commands, 1));
		} else if (commands[0].equals("CHANNELMESSAGE")) {
			Channel chan = getChannel(commands[1]);
			String out = Misc.easyDateFormat("[HH:mm:ss]") + " * Channel message: " + Misc.makeSentence(commands, 2);
			Misc.outputLog(chan.logFileName, out);
		} else if (commands[0].equals("BROADCAST")) {
			Log.log("*** Broadcast from server: " + Misc.makeSentence(commands, 1));
		}


			
		return true;
	}
	
	/* channel - true if command was issued in the channel (and not in private chat) */
	public static void processUserCommand(String command, Client client, boolean channel) {
		if (command.trim().equals("")) return ;
		String[] params = command.split(" ");
		params[0] = params[0].toUpperCase(); // params[0] is the base command

		if (params[0].equals("HELP")) {
			sendPrivateMsg(client, "Hello, " + client.name + "!");
			sendPrivateMsg(client, "I am an automated channel service bot,");
			sendPrivateMsg(client, "for the full list of commands, see http://taspring.clan-sy.com/dl/ChanServCommands.html");
			sendPrivateMsg(client, "If you want to go ahead and register a new channel, please contact one of the server moderators!");
		} else if (params[0].equals("REGISTER")) {
			if (!client.isModerator()) {
				sendPrivateMsg(client, "Sorry, you'll have to contact one of the server moderators to register a channel for you!");
				return ;
			}
			
			if (params.length != 3) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());

			for (int i = 0; i < channels.size(); i++) {
				if (((Channel)channels.get(i)).name.equals(chanName)) {
					if (((Channel)channels.get(i)).isStatic)
						sendPrivateMsg(client, "Error: channel #" + chanName + " is a static channel (cannot register it)!");
					else
						sendPrivateMsg(client, "Error: channel #" + chanName + " is already registered!");	
					return ;
				}
			}
			
			// ok register the channel now:
			Channel chan = new Channel(chanName);
			channels.add(chan);
			chan.founder = params[2];
			chan.isStatic = false;
			sendLine("JOIN " + chan.name);
			sendPrivateMsg(client, "Channel #" + chanName + " successfully registered to " + params[2]);	
		} else if (params[0].equals("UNREGISTER")) {
			if (params.length != 3) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			// ok unregister the channel now:
			channels.remove(chan);
			sendLine("CHANNELMESSAGE " + chan.name + " " + "This channel has just been unregistered from <" + username + "> by <" + client.name + ">");
			sendLine("LEAVE " + chan.name);
			sendPrivateMsg(client, "Channel #" + chanName + " successfully unregistered!");
		} else if (params[0].equals("ADDSTATIC")) {
			if (!client.isModerator()) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			if (params.length != 2) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());

			for (int i = 0; i < channels.size(); i++) {
				if (((Channel)channels.get(i)).name.equals(chanName)) {
					if (((Channel)channels.get(i)).isStatic)
						sendPrivateMsg(client, "Error: channel #" + chanName + " is already static!");
					else
						sendPrivateMsg(client, "Error: channel #" + chanName + " is already registered! (unregister it first and then add it to static list)");	
					return ;
				}
			}
			
			// ok add the channel to static list:
			Channel chan = new Channel(chanName);
			channels.add(chan);
			chan.isStatic = true;
			sendLine("JOIN " + chan.name);
			sendPrivateMsg(client, "Channel #" + chanName + " successfully added to static list.");
		} else if (params[0].equals("REMOVESTATIC")) {
			if (params.length != 2) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (!chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not in the static channel list!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			// ok remove the channel from static channel list now:
			channels.remove(chan);
			sendLine("LEAVE " + chan.name);
			sendPrivateMsg(client, "Channel #" + chanName + " successfully removed from static channel list!");
		} else if (params[0].equals("OP")) {
			if (params.length != 3) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			if (chan.getOperatorList().indexOf(params[2]) != -1) {
				sendPrivateMsg(client, "Error: User is already in this channel's operator list!");
				return ;
			}
			
			// ok add user to channel's operator list:
			chan.getOperatorList().add(params[2]);
			sendLine("CHANNELMESSAGE " + chan.name + " <" + params[2] + "> has just been added to this channel's operator list by <" + client.name + ">");
		} else if (params[0].equals("DEOP")) {
			if (params.length != 3) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			if (chan.getOperatorList().indexOf(params[2]) == -1) {
				sendPrivateMsg(client, "Error: User is not in this channel's operator list!");
				return ;
			}
			
			// ok remove user from channel's operator list:
			chan.getOperatorList().remove(params[2]);
			sendLine("CHANNELMESSAGE " + chan.name + " <" + params[2] + "> has just been removed from this channel's operator list by <" + client.name + ">");
		} else if (params[0].equals("TOPIC")) {
			
			if (params.length < 2) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String topic;
			if (params.length == 2) topic = "*";
			else topic = Misc.makeSentence(params, 2);
			if (topic.trim().equals("")) topic = "*";
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder) || chan.isOperator(client.name))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			// ok set the topic:
			sendLine("CHANNELTOPIC " + chan.name + " " + topic);
		} else if (params[0].equals("LOCK")) {
			
			if (params.length != 3) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder) || chan.isOperator(client.name))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			if (!Misc.isValidName(params[2])) {
				sendPrivateMsg(client, "Error: key contains some invalid characters!");
				return ;
			}
			
			// ok lock the channel:
			sendLine("SETCHANNELKEY " + chan.name + " " + params[2]);
			if (params[2].equals("*")) chan.key = ""; 
			else chan.key = params[2];
		} else if (params[0].equals("UNLOCK")) {
			
			if (params.length != 2) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}
			
			if (!(client.isModerator() || client.name.equals(chan.founder) || chan.isOperator(client.name))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			// ok unlock the channel:
			sendLine("SETCHANNELKEY " + chan.name + " *");
			chan.key = "";
		} else if (params[0].equals("KICK")) {
			
			if (params.length < 3) {
				sendPrivateMsg(client, "Error: Invalid params!");
				return ;
			}
			
			if (params[1].charAt(0) != '#') {
				sendPrivateMsg(client, "Error: Bad channel name (forgot #?)");
				return ;
			}
			
			String chanName = params[1].substring(1, params[1].length());
			Channel chan = getChannel(chanName);
			if ((chan == null) || (chan.isStatic)) {
				sendPrivateMsg(client, "Channel #" + chanName + " is not registered!");
				return ;
			}

			if (!(client.isModerator() || client.name.equals(chan.founder) || chan.isOperator(client.name))) {
				sendPrivateMsg(client, "Insufficient access to execute " + params[0] + " command!");
				return ;
			}
			
			String target = params[2];
			if (chan.clients.indexOf(target) == -1) {
				sendPrivateMsg(client, "Error: <" + target + "> not found in #" + chanName + "!");
				return ;
			}
			
			if (target.equals(username)) {
				// not funny!
				sendPrivateMsg(client, "You are not allowed to issue this command!");
				return ;
			}
			
			String reason = "";
			if (params.length > 3) reason = " " + Misc.makeSentence(params, 3);
			
			// ok kick the user:
			sendLine("FORCELEAVECHANNEL " + chan.name + " " + target + reason);
		} 
 
	}
	
	public static void sendPrivateMsg(Client client, String msg) {
		sendLine("SAYPRIVATE " + client.name + " " + msg);
		Misc.outputLog(client.name + ".log", Misc.easyDateFormat("[dd/MM/yy HH:mm:ss]") + " <" + username + "> " + msg);
	}
	
	public static Client getClient(String username) {
		for (int i = 0; i < clients.size(); i++) {
			if (((Client)clients.get(i)).name.equals(username)) {
				return ((Client)clients.get(i)); 
			}
		}
		
		return null;
	}
	
    /* returns null if channel is not found */
	public static Channel getChannel(String name) {
		for (int i = 0; i < channels.size(); i++)
			if (((Channel)channels.get(i)).name.equals(name)) return ((Channel)channels.get(i));
		return null;
	}
	
	public static void main(String[] args) {

		Log.externalLogFileName = "$main.log";
		Log.useExternalLogging = true;

		// check if "./logs" folder exists, if not then create it:
		File file = new File("./logs");
		if (!file.exists()) {
			if (!file.mkdir()) {
				Log.error("unable to create ./logs folder! Exiting ...");
				closeAndExit(1);
			} else {
				Log.log("Folder ./logs has been created");
			}
		}

		Log.log("ChanServ started on " + Misc.easyDateFormat("dd/MM/yy"));
		Log.log("");
		
		loadConfig(CONFIG_FILENAME);
		saveConfig(CONFIG_FILENAME); //*** debug
		tryToConnect();
		
		new Timer().schedule(new KeepAliveTask(),
                1000,        //initial delay
                15*1000);  //subsequent rate
		
		messageLoop();
	}
}
