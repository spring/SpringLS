
package com.springrts.chanserv;


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
public class LegacyConfigStorage implements ConfigStorage {

	private static final Logger logger = LoggerFactory.getLogger(LegacyConfigStorage.class);

	private Document xmlConfig;

	private final Context context;

	public LegacyConfigStorage(Context context) {
		this.context = context;
	}

	@Override
	public void loadConfig(String fileName) {

		ChanServ chanServ = context.getChanServ();
		Configuration config = context.getConfiguration();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			xmlConfig = builder.parse(new File(fileName));

			XPath xpath = XPathFactory.newInstance().newXPath();
			Node node, node2;

			//node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
			config.setServerAddress((String) xpath.evaluate("config/account/serveraddress/text()", xmlConfig, XPathConstants.STRING));
			config.setServerPort(Integer.parseInt((String) xpath.evaluate("config/account/serverport/text()", xmlConfig, XPathConstants.STRING)));
			config.setRemoteAccessPort(Integer.parseInt((String) xpath.evaluate("config/account/remoteaccessport/text()", xmlConfig, XPathConstants.STRING)));
			config.setUsername((String) xpath.evaluate("config/account/username/text()", xmlConfig, XPathConstants.STRING));
			config.setPassword((String) xpath.evaluate("config/account/password/text()", xmlConfig, XPathConstants.STRING));

			//node = (Node)xpath.evaluate("config/account/username", config, XPathConstants.NODE);
			//node.setTextContent("this is a test!");

			// DEPRECATED - read database info:
			//config.DB_URL = (String)xpath.evaluate("config/database/url/text()", xmlConfig, XPathConstants.STRING);
			//config.DB_username = (String)xpath.evaluate("config/database/username/text()", xmlConfig, XPathConstants.STRING);
			//config.DB_password = (String)xpath.evaluate("config/database/password/text()", xmlConfig, XPathConstants.STRING);

			// load remote access accounts:
			node = (Node)xpath.evaluate("config/remoteaccessaccounts", xmlConfig, XPathConstants.NODE);
			if (node == null) {
				logger.error("Bad XML document. Path config/remoteaccessaccounts does not exist. Exiting ...");
				chanServ.closeAndExit(1);
				return;
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					context.getRemoteAccessServer().getRemoteAccounts().add(((Element)node).getAttribute("key"));
				}
				node = node.getNextSibling();
			}

			// load static channel list:
			Channel chan;
			node = (Node)xpath.evaluate("config/channels/static", xmlConfig, XPathConstants.NODE);
			if (node == null) {
				logger.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
				chanServ.closeAndExit(1);
				return;
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					chan = new Channel(context, ((Element)node).getAttribute("name"));
					chan.setAntiSpam(((Element) node).getAttribute("antispam").equals("yes") ? true : false);
					String spamSettingsString = ((Element) node).getAttribute("antispamsettings");
					SpamSettings spamSettings = null;
					try {
						spamSettings = SpamSettings.fromProtocolString(spamSettingsString);
					} catch (Exception ex) {
						logger.warn("Fixing invalid spam settings for #" + chan.getName() + " ...", ex);
						spamSettings = SpamSettings.DEFAULT_SETTINGS;
					}
					chan.setAntiSpamSettings(spamSettings);
					config.getChannels().add(chan);
					// apply anti-spam settings:
					context.getAntiSpamSystem().setSpamSettingsForChannel(chan.getName(), chan.getAntiSpamSettings());
				}
				node = node.getNextSibling();
			}

			// load registered channel list:
			node = (Node)xpath.evaluate("config/channels/registered", xmlConfig, XPathConstants.NODE);
			if (node == null) {
				logger.error("Bad XML document. Path config/channels/registered does not exist. Exiting ...");
				chanServ.closeAndExit(1);
				return;
			}
			node = node.getFirstChild();
			while (node != null) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					// this is "channel" element
					chan = new Channel(context, ((Element)node).getAttribute("name"));
					chan.setStatic(false);
					chan.setTopic(((Element) node).getAttribute("topic"));
					chan.setKey(((Element) node).getAttribute("key"));
					chan.setFounder(((Element) node).getAttribute("founder"));
					chan.setAntiSpam(((Element) node).getAttribute("antispam").equals("yes") ? true : false);
					String spamSettingsString = ((Element) node).getAttribute("antispamsettings");
					SpamSettings spamSettings = null;
					try {
						spamSettings = SpamSettings.fromProtocolString(spamSettingsString);
					} catch (Exception ex) {
						logger.warn("Fixing invalid spam settings for #" + chan.getName() + " ...", ex);
						spamSettings = SpamSettings.DEFAULT_SETTINGS;
					}
					chan.setAntiSpamSettings(spamSettings);
					config.getChannels().add(chan);
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
					context.getAntiSpamSystem().setSpamSettingsForChannel(chan.getName(), chan.getAntiSpamSettings());
				}
				node = node.getNextSibling();
			}


			logger.info("Config file read.");
		} catch (SAXException sxex) {
			// Error generated during parsing
			logger.error("Error during parsing xml document: " + fileName, sxex);
			chanServ.closeAndExit(1);
			return;
		} catch (ParserConfigurationException pcex) {
			// Parser with specified options ca not be built
			logger.error("Unable to build specified xml parser", pcex);
			chanServ.closeAndExit(1);
			return;
		} catch (IOException ioex) {
			// I/O error
			logger.error("I/O error while accessing " + fileName, ioex);
			chanServ.closeAndExit(1);
			return;
		} catch (XPathExpressionException ex) {
			logger.error("Error: XPath expression exception - XML document is malformed.", ex);
			chanServ.closeAndExit(1);
			return;
		} catch (Exception ex) {
			logger.error("Unknown exception while reading config file: " + fileName, ex);
			chanServ.closeAndExit(1);
			return;
		}

	}

	@Override
	public void saveConfig(String fileName) {

		ChanServ chanServ = context.getChanServ();
		Configuration config = context.getConfiguration();

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

				root = (Node)xpath.evaluate("config/channels/static", xmlConfig, XPathConstants.NODE);
				if (root == null) {
					logger.error("Bad XML document. Path config/channels/static does not exist. Exiting ...");
					chanServ.closeAndExit(1);
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
				for (int i = 0; i < config.getChannels().size(); i++) {
					chan = config.getChannels().get(i);
					if (!chan.isStatic()) {
						continue;
					}
					root.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					elem = xmlConfig.createElement("channel");
					elem.setAttribute("name", chan.getName());
					elem.setAttribute("antispam", chan.isAntiSpam() ? "yes" : "no");
					elem.setAttribute("antispamsettings", chan.getAntiSpamSettings().toProtocolString());
					//elem.setTextContent(chan.name);
					root.appendChild(elem);
				}
				root.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(4)));

				// remove all registered channels from config and replace it with current registered channel list:

				root = (Node)xpath.evaluate("config/channels/registered", xmlConfig, XPathConstants.NODE);
				if (root == null) {
					logger.error("Bad XML document. Path config/channels/registered does not exist. Exiting ...");
					chanServ.closeAndExit(1);
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
				for (int i = 0; i < config.getChannels().size(); i++) {
					chan = config.getChannels().get(i);
					if (chan.isStatic()) {
						continue;
					}
					root.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					elem = xmlConfig.createElement("channel");
					elem.setAttribute("name", chan.getName());
					elem.setAttribute("topic", chan.getTopic());
					elem.setAttribute("key", chan.getKey());
					elem.setAttribute("founder", chan.getFounder());
					elem.setAttribute("antispam", chan.isAntiSpam() ? "yes" : "no");
					elem.setAttribute("antispamsettings", chan.getAntiSpamSettings().toProtocolString());

					// write operator list:
					if (chan.getOperatorList().size() > 0) {
						elem.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(8)));
						for (int j = 0; j < chan.getOperatorList().size(); j++) {
							elem2 = xmlConfig.createElement("operator");
							elem2.setAttribute("name", chan.getOperatorList().get(j));
							elem.appendChild(elem2);
							if (j != chan.getOperatorList().size()-1) {
								elem.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(8)));
							}
						}
						elem.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(6)));
					}

					root.appendChild(elem);
				}
				root.appendChild(xmlConfig.createTextNode(Misc.EOL + Misc.enumSpaces(4)));

			} catch (XPathExpressionException ex) {
				logger.error("Failed parsing legacy config file", ex);
				chanServ.closeAndExit(1);
			}

			// ok save it now:
			xmlConfig.normalize(); //*** is this needed?
			DOMSource source = new DOMSource(xmlConfig);
			StreamResult result = new StreamResult(new FileOutputStream(fileName));

			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();

			transformer.transform(source, result);

			logger.debug("Config file saved to {}", fileName);
		} catch (Exception ex) {
			logger.error("Unable to save config file to " + fileName + "! Ignoring ...", ex);
		}
	}
}
