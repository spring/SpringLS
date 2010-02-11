/*
 * Created on 2005.6.19
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Betalord
 */
public class Channel {

	private static final Log s_log  = LogFactory.getLog(Channel.class);

	public String name;
	private String topic; // "" represents no topic (topic is disabled for this channel)
	private String topicAuthor;
	private long topicChangedTime; // time when topic was last changed (in ms since Jan 1, 1970 UTC)
	private String key = ""; // if key is "" then this channel is not locked (anyone can join). Otherwise, user must supply correct key to join it.
	private List<Client> clients; // clients participating in this channel
	public MuteList muteList = new MuteList(this); // contains a list of Strings (usernames) who are muted (not allowed to talk in the channel)

	public Channel(String channelName) {
		name = new String(channelName);
		topic = "";
		topicAuthor = "";
		clients = new ArrayList<Client>();
	}

	public boolean equals(Channel chan) {
		return this.name.equals(chan.name);
	}

	public String getTopic() {
		return topic;
	}

	public String getTopicAuthor() {
		return topicAuthor;
	}

	public long getTopicChangedTime() {
		return topicChangedTime;
	}

	public boolean setTopic(String newTopic, String author) {

		if (newTopic.trim().equals("*")) {
			topic = "";
			topicAuthor = author;
			topicChangedTime = System.currentTimeMillis();
			return false;
		}
		topic = newTopic.trim();
		topicAuthor = author;
		topicChangedTime = System.currentTimeMillis();
		if (s_log.isDebugEnabled()) {
			s_log.debug("* Topic for #" + name + " changed to '" + topic + "' (set by <" + author + ">)");
		}
		return true;
	}

	/** Sends msg as a channel message to all clients on this channel */
	public void broadcast(String msg) {

		if (msg.trim().equals("")) {
			// do nto send any message
			return;
		}

		sendLineToClients("CHANNELMESSAGE " + name + " " + msg);
	}

	public boolean isTopicSet() {
		return !(topic.equals(""));
	}

	/** Adds new client to the list of clients of this channel */
	public void addClient(Client client) {

		if (isClientInThisChannel(client)) {
			// already in the channel!
			return;
		}

		clients.add(client);
	}

	public boolean removeClient(Client client) {
		return clients.remove(client);
	}

	public boolean isClientInThisChannel(Client client) {
		return (clients.indexOf(client) != -1);
	}

	/** Returns number of clients in this channel */
	public int getClientsSize() {
		return clients.size();
	}

	/** Returns null if index if out of bounds */
	public Client getClient(int index) {
		try {
			return clients.get(index);
		} catch(IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** Sends a text to all clients in this channel */
	public void sendLineToClients(String s) {
		if (name.toUpperCase().equals("MAIN") && TASServer.LOG_MAIN_CHANNEL) {
			TASServer.writeMainChanLog(s);
		}
		for (int i = 0; i < clients.size(); i++) {
			clients.get(i).sendLine(s);
		}
	}

	public boolean isLocked() {
		return !(key.equals("*") || key.equals(""));
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
