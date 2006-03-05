/*
 * Created on 4.3.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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

import java.util.Timer;
import java.util.TimerTask;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


class KeepAliveTask extends TimerTask {
	public void run() {
		ChanServ.sendLine("PING");
	}
}

public class ChanServ {

	static final String VERSION = "0.1";
	static final String CONFIG_FILENAME = "settings.xml";
	static final String SERVER_ADDRESS = "taspringmaster.clan-sy.com";
	static final int SERVER_PORT = 8200;
	static Document config; 
	static String username = "";
	static String password = "";
	static Socket socket = null;
    static PrintWriter sockout = null;
    static BufferedReader sockin = null;
	
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
           Node node;
           
           //node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
           username = (String)xpath.evaluate("config/account/username/text()", config, XPathConstants.STRING);
           password = (String)xpath.evaluate("config/account/password/text()", config, XPathConstants.STRING);
           
           node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
           node.setTextContent("to je kr en username!");
           
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
        } catch (Exception e) {
        	Log.error("Unknown exception while reading config file: " + fname);
        	closeAndExit(1);
        	//e.printStackTrace();
        }
		
	}
	
	public static void saveConfig(String fname) {
		try {
			config.normalize();
	        DOMSource source = new DOMSource(config);
	        StreamResult result = new StreamResult(new FileOutputStream(fname));

	        TransformerFactory transFactory = TransformerFactory.newInstance();
	        Transformer transformer = transFactory.newTransformer();

	        transformer.transform(source, result);
	        
	        Log.log("Config file saved to " + fname);
		} catch (Exception e) {
			Log.error("Unable to save config file to " + fname + "! Ignoring ...");
		}
	}
	
	public static void sendLine(String s) {
		Log.log("Client: \"" + s + "\"");
		sockout.println(s);
	}
	
	private static boolean tryToConnect() {
        try {
        	Log.log("Connecting to " + SERVER_ADDRESS + ":" + SERVER_PORT + " ...");
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            sockout = new PrintWriter(socket.getOutputStream(), true);
            sockin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException e) {
            Log.error("Unknown host error: " + SERVER_ADDRESS);
            closeAndExit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + SERVER_ADDRESS);
            closeAndExit(1);
        }
		
        Log.log("Now connected to " + SERVER_ADDRESS);
        return true;
	}
	
	public static void serverLoop() {
		String line;
        while (true) {
        	try {
            	line = sockin.readLine();
        	} catch (IOException e) {
        		Log.error("Connection with server closed with exception.");
        		break;
        	}
        	if (line == null) break;
            Log.log("Server: \"" + line + "\"");
            
            // parse command and respond to it:
            execRemoteCommand(line);
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
		} else if (commands[0].equals("DENIED")) {
			Log.log("Login denied. Reason: " + Misc.makeSentence(commands, 1));
			closeAndExit();
		} else if (commands[0].equals("AGREEMENT")) {
			// not done yet. Should respond with CONFIRMAGREEMENT and then resend LOGIN command.
			Log.log("Server is requesting agreement confirmation. Cancelling ...");
			closeAndExit();
		}
			
		return true;
	}
	
	public static void main(String[] args) {
		loadConfig(CONFIG_FILENAME);
		tryToConnect();
		
		new Timer().schedule(new KeepAliveTask(),
                1000,        //initial delay
                15*1000);  //subsequent rate
		
		serverLoop();
	}
}
