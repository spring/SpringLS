/*
 * Created on 2005.6.19
 */

package com.springrts.tasserver;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Betalord
 */
public class Channel implements ContextReceiver, LiveStateListener {

	private static final Log s_log  = LogFactory.getLog(Channel.class);

	private static final String logFilesDir  = "./";

	private String name;
	private String topic; // "" represents no topic (topic is disabled for this channel)
	private String topicAuthor;
	private long topicChangedTime; // time when topic was last changed (in ms since Jan 1, 1970 UTC)
	private String key = ""; // if key is "" then this channel is not locked (anyone can join). Otherwise, user must supply correct key to join it.
	private List<Client> clients; // clients participating in this channel
	/**
	 * Contains a list of user names which are muted
	 * (not allowed to talk in the channel).
	 */
	private MuteList muteList = new MuteList(this);
	/** If not <code>null</code>, this channel gets logged to a file. */
	private File logFile;
	private boolean logging;
	/** If not <code>null</code>, this channel gets logged to a file. */
	private PrintStream fileLog;
	private Context context = null;


	public Channel(String channelName) {

		name = channelName;
		topic = "";
		topicAuthor = "";
		clients = new ArrayList<Client>();
		logFile = new File(logFilesDir, "channel_" + name + ".log");
		logging = false;
		fileLog = null;
	}


	@Override
	public void receiveContext(Context context) {

		this.context = context;
		actualiseToConfiguration();
	}

	@Override
	public void starting() {}
	@Override
	public void started() {}

	@Override
	public void stopping() {
		setLogging(false);
	}
	@Override
	public void stopped() {}

	private void actualiseToConfiguration() {
		setLogging(name.matches(context.getChannels().getChannelsToLogRegex()));
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
			// do not send empty messages
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

		if (fileLog != null) {
			// As DateFormats are generally not-thread save,
			// we always create a new one.
			DateFormat timeFormat = new SimpleDateFormat("<HH:mm:ss> ");
			fileLog.println(timeFormat.format(new Date()));
			fileLog.println(s);
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

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Contains a list of user names which are muted
	 * (not allowed to talk in the channel).
	 * @return the muteList
	 */
	public MuteList getMuteList() {
		return muteList;
	}

	public boolean isLogging() {
		return logging;
	}

	public File getLogFile() {
		return logFile;
	}

	public boolean setLogging(boolean enabled) {

		// only change if change is needed
		if (enabled != isLogging()) {
			if (enabled) {
				try {
					fileLog = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, true)), true);
					fileLog.println();
					fileLog.print("Log started on ");
					fileLog.println(new SimpleDateFormat("dd/MM/yy").format(new Date()));
					logging = true;
				} catch (Exception e) {
					if (fileLog != null) {
						fileLog.close();
					}
					fileLog = null;
					s_log.error("Unable to open channel log file for channel " +
							name + ": " + logFile.getAbsolutePath(), e);
					logFile = null;
				}
			} else {
				try {
					fileLog.close();
				} catch (Exception e) {
					// ignore
				}
				logFile = null;
				fileLog = null;
				logging = false;
			}
		}

		return (isLogging() == enabled);
	}

	/** Called when the server is shutting down */
	void shutdown() {
		setLogging(false);
	}
}
