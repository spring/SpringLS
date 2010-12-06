/*
 * Created on 6.3.2006
 */

package com.springrts.chanserv;


import com.springrts.chanserv.antispam.SpamSettings;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static channels are those for which we don't want ChanServ to moderate them,
 * only idle there so it logs all chats (for example, #main).
 *
 * @author Betalord
 */
public class Channel implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(ChanServ.class);

	@XmlElement
	private final String name;
	/** "" means topic is disabled */
	private String topic;
	private final String logFileName;
	/** are we in this channel right now? */
	private boolean joined;
	/**
	 * Whether this channel is a static one and not registered.
	 * We can not register this channel at all.
	 */
	private boolean isStatic;
	/** if "" then no key is set (channel is unlocked) */
	private String key;
	/**
	 * The user-name of the founder of this channel.
	 * The founder is the "owner" of the channel, he can assign operators etc.
	 */
	private String founder;
	@XmlElementWrapper()
	@XmlElement(name = "name")
	private final List<String> operators;
	private final List<String> clients;
	/**
	 * Whether users will be automatically muted if spamming is detected
	 * from them
	 */
	private boolean antiSpam;
	/** anti-spam settings for this channel, used with AntiSpamSystem */
	private SpamSettings antiSpamSettings;

	private final Context context;

	/** Required fro JAXB serialization/marshaling */
	private Channel() {
		this(null, null);
	}

	public Channel(Context context, String name) {

		this.context = context;
		this.name = name;
		this.topic = "";
		this.logFileName = "#" + name + ".log";
		this.joined = false;
		this.isStatic = true;
		this.key = "";
		this.founder = null;
		this.operators = new ArrayList<String>();
		this.clients = new ArrayList<String>();
		this.antiSpam = false;
		this.antiSpamSettings = null;
	}

	public boolean isOperator(String name) {
		return operators.contains(name);
	}

	public boolean addOperator(String name) {
		return operators.add(name);
	}

	public boolean removeOperator(String name) {
		return operators.remove(name);
	}

	public void renameFounder(String newFounder) {
		setFounder(newFounder);
	}

	public boolean renameOperator(String oldOp, String newOp) {

		int index = operators.indexOf(oldOp);
		if (index == -1) {
			// operator does not exist!
			return false;
		}
		operators.set(index, newOp);
		return true;
	}

	public List<String> getOperatorList() {
		return operators;
	}

	public void sendMessage(String msg) {
		context.getChanServ().sendLine(String.format("SAY %s %s", getName(), msg));
	}

	public int clientCount() {
		return clients.size();
	}

	public void clearClients() {
		clients.clear();
	}

	public String getClient(int index) {

		try {
			return clients.get(index);
		} catch (IndexOutOfBoundsException ex) {
			return null;
		}
	}

	public void addClient(String client) {
		clients.add(client);
	}

	public void removeClient(String client) {

		if (!clients.remove(client)) {
			// should not happen!
			logger.warn("Tried to remove a client that does not exist");
			return;
		}
	}

	public boolean isClient(String client) {
		return clients.contains(client);
	}

	/** Returns 'null' if channel name is valid, an error description otherwise */
	public static String isChanNameValid(String channame) {

		if (channame.length() > 20) {
			return "Channel name too long";
		} else if (channame.length() < 1) {
			return "Channel name too short";
		} else if (!channame.matches("^[A-Za-z0-9_\\[\\]]+$")) {
			return "Channel name contains invalid characters";
		} else {
			// everything is OK:
			return null;
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * "" means topic is disabled
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * "" means topic is disabled
	 * @param topic the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getLogFileName() {
		return logFileName;
	}

	/**
	 * are we in this channel right now?
	 * @return the joined
	 */
	public boolean isJoined() {
		return joined;
	}

	/**
	 * are we in this channel right now?
	 * @param joined the joined to set
	 */
	@XmlTransient
	public void setJoined(boolean joined) {
		this.joined = joined;
	}

	/**
	 * Whether this channel is a static one and not registered.
	 * We can not register this channel at all.
	 * @return the isStatic
	 */
	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * Whether this channel is a static one and not registered.
	 * We can not register this channel at all.
	 * @param isStatic the isStatic to set
	 */
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	/**
	 * if "" then no key is set (channel is unlocked)
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * if "" then no key is set (channel is unlocked)
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * The user-name of the founder of this channel.
	 * The founder is the "owner" of the channel, he can assign operators etc.
	 * @return the founder
	 */
	public String getFounder() {
		return founder;
	}

	/**
	 * The user-name of the founder of this channel.
	 * The founder is the "owner" of the channel, he can assign operators etc.
	 * @param founder the founder to set
	 */
	public void setFounder(String founder) {
		this.founder = founder;
	}

	/**
	 * Whether users will be automatically muted if spamming is detected
	 * from them
	 * @return the antiSpam
	 */
	public boolean isAntiSpam() {
		return antiSpam;
	}

	/**
	 * Whether users will be automatically muted if spamming is detected
	 * from them
	 * @param antiSpam the antiSpam to set
	 */
	public void setAntiSpam(boolean antiSpam) {
		this.antiSpam = antiSpam;
	}

	/**
	 * anti-spam settings for this channel, used with AntiSpamSystem
	 * @return the antiSpamSettings
	 */
	public SpamSettings getAntiSpamSettings() {
		return antiSpamSettings;
	}

	/**
	 * anti-spam settings for this channel, used with AntiSpamSystem
	 * @param antiSpamSettings the antiSpamSettings to set
	 */
	public void setAntiSpamSettings(SpamSettings antiSpamSettings) {
		this.antiSpamSettings = antiSpamSettings;
	}
}
