
package com.springrts.chanserv;


import com.springrts.chanserv.antispam.AntiSpamSystem;
import com.springrts.chanserv.antispam.SpamSettings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 */
public class ConfigStorage {

	private static final Logger logger = LoggerFactory.getLogger(ConfigStorage.class);

	private static Document config;

	static void loadConfig(String fname) {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			config = builder.parse(new File(fname));

			XPath xpath = XPathFactory.newInstance().newXPath();
			Node node, node2;

			//node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
			ChanServ.serverAddress = (String)xpath.evaluate("config/account/serveraddress/text()", config, XPathConstants.STRING);
			ChanServ.serverPort = Integer.parseInt((String)xpath.evaluate("config/account/serverport/text()", config, XPathConstants.STRING));
			ChanServ.remoteAccessPort = Integer.parseInt((String)xpath.evaluate("config/account/remoteaccessport/text()", config, XPathConstants.STRING));
			ChanServ.username = (String)xpath.evaluate("config/account/username/text()", config, XPathConstants.STRING);
			ChanServ.password = (String)xpath.evaluate("config/account/password/text()", config, XPathConstants.STRING);

			//node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
			//node.setTextContent("this is a test!");

			// read database info:
			ChanServ.DB_URL = (String)xpath.evaluate("config/database/url/text()", config, XPathConstants.STRING);
			ChanServ.DB_username = (String)xpath.evaluate("config/database/username/text()", config, XPathConstants.STRING);
			ChanServ.DB_password = (String)xpath.evaluate("config/database/password/text()", config, XPathConstants.STRING);

			// load remote access accounts:
			node = (Node)xpath.evaluate("config/remoteaccessaccounts", config, XPathConstants.NODE);
			if (node == null) {
				Log.error("Bad XML document. Path config/remoteaccessaccounts does not exist. Exiting ...");
				ChanServ.closeAndExit(1);
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					ChanServ.remoteAccessServer.getRemoteAccounts().add(((Element)node).getAttribute("key"));
				}
				node = node.getNextSibling();
			}

			// load static channel list:
			Channel chan;
			node = (Node)xpath.evaluate("config/channels/static", config, XPathConstants.NODE);
			if (node == null) {
					Log.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
				ChanServ.closeAndExit(1);
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
					ChanServ.channels.add(chan);
					// apply anti-spam settings:
					AntiSpamSystem.setSpamSettingsForChannel(chan.getName(), chan.getAntiSpamSettings());
				}
				node = node.getNextSibling();
			}

			// load registered channel list:
			node = (Node)xpath.evaluate("config/channels/registered", config, XPathConstants.NODE);
			if (node == null) {
				Log.error("Bad XML document. Path config/channels/registered does not exist. Exiting ...");
				ChanServ.closeAndExit(1);
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
					ChanServ.channels.add(chan);
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
			ChanServ.closeAndExit(1);
			/*
			Exception  x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			x.printStackTrace();
			*/
		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			Log.error("Unable to build specified xml parser");
			ChanServ.closeAndExit(1);
			//pce.printStackTrace();

		} catch (IOException ioe) {
			// I/O error
			Log.error("I/O error while accessing " + fname);
			ChanServ.closeAndExit(1);
			//ioe.printStackTrace();
		} catch (XPathExpressionException e) {
			Log.error("Error: XPath expression exception - XML document is malformed.");
			e.printStackTrace();
			ChanServ.closeAndExit(1);
		} catch (Exception e) {
			Log.error("Unknown exception while reading config file: " + fname);
			e.printStackTrace();
			ChanServ.closeAndExit(1);
			//e.printStackTrace();
		}

	}

	static void saveConfig(String fname) {
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
					ChanServ.closeAndExit(1);
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
				for (int i = 0; i < ChanServ.channels.size(); i++) {
					chan = ChanServ.channels.get(i);
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
					ChanServ.closeAndExit(1);
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
				for (int i = 0; i < ChanServ.channels.size(); i++) {
					chan = ChanServ.channels.get(i);
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
				ChanServ.closeAndExit(1);
			}

			// ok save it now:
			config.normalize(); //*** is this needed?
			DOMSource source = new DOMSource(config);
			StreamResult result = new StreamResult(new FileOutputStream(fname));

			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();

			transformer.transform(source, result);

			logger.debug("Config file saved to {}", fname);
		} catch (Exception e) {
			Log.error("Unable to save config file to " + fname + "! Ignoring ...");
		}
	}
}
