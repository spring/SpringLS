/*
 * Created on 6.3.2006
 */

package com.springrts.chanserv;


import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static channels are those for which we don't want ChanServ to moderate them,
 * only idle there so it logs all chats (for example, #main).
 *
 * @author Betalord
 */
public class Channel {

	private static final Logger logger = LoggerFactory.getLogger(ChanServ.class);

	public String name;
	public String topic = ""; // "" means topic is disabled
	public String logFileName;
	public boolean joined = false; // are we in this channel right now?
	public boolean isStatic = true; // if true, then this channel is a static one and not registered one (we can't register this channel at all)
	public String key = ""; // if "" then no key is set (channel is unlocked)
	public String founder; // username of the founder of this channel. Founder is the "owner" of the channel, he can assign operators etc.
	private List<String> operators = new ArrayList<String>();
	private List<String> clients = new ArrayList<String>();
	public boolean antispam; // if true, users will be automatically muted if spamming is detected from them
	public String antispamSettings; // anti-spam settings for this channel, used with AntiSpamSystem

	public Channel(String name) {
		this.name = name;
		logFileName = "#" + name + ".log";
	}

	public boolean isFounder(String name) {
		return name.equals(founder);
	}

	public boolean isOperator(String name) {
		return (operators.indexOf(name) != -1);
	}

	public boolean addOperator(String name) {

		if (isOperator(name)) {
			return false;
		}
		operators.add(name);
		return true;
	}

	public boolean removeOperator(String name) {

		if (!isOperator(name)) {
			return false;
		}
		operators.remove(name);
		return true;
	}

	public void renameFounder(String newFounder) {
		founder = newFounder;
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
		ChanServ.sendLine("SAY " + name + " " + msg);
	}

	public int clientCount() {
		return clients.size();
	}

	public void clearClients() {
		clients.clear();
	}

	public String getClient(int index) {

		try{
			return clients.get(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	public void addClient(String client) {
		clients.add(client);
	}

	public void removeClient(String client) {

		int i = clients.indexOf(client);
		if (i == -1) {
			// should not happen!
			logger.warn("Tried to remove a client that does not exist");
			return;
		}
		clients.remove(i);
	}

	public boolean clientExists(String client) {
		return clients.indexOf(client) != -1;
	}

	/** Returns 'null' if channel name is valid, or error description otherwise */
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
}
