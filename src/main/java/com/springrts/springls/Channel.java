/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public class Channel implements ContextReceiver, LiveStateListener {

	private static final Logger LOG = LoggerFactory.getLogger(Channel.class);

	private static final String LOG_FILES_DIR  = "./";
	/**
	 * A channel with this topic -> topic is disabled for this channel.
	 * @see #getTopic()
	 */
	public static final String TOPIC_NONE  = "";
	/**
	 * Has equal meaning like TOPIC_NONE.
	 * @see #TOPIC_NONE
	 */
	public static final String TOPIC_NONE_2  = "*";
	/**
	 * A channel with this key -> channel is not locked, so anyone can join.
	 * @see #getKey()
	 */
	public static final String KEY_NONE  = "";
	/**
	 * Has equal meaning like KEY_NONE.
	 * @see #KEY_NONE
	 */
	public static final String KEY_NONE_2  = "*";

	private String name;
	/**
	 * @see #TOPIC_NONE
	 */
	private String topic;
	private String topicAuthor;
	/** Time when topic was last changed (in ms since Jan 1, 1970 UTC) */
	private long topicChangedTime;
	/**
	 * The client must supply this key to join the channel, except it is
	 * KEY_NONE.
	 * @see #KEY_NONE
	 */
	private String key;
	/** The clients participating in this channel */
	private List<Client> clients;
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


	public Channel(String name) {

		this.name = name;
		topic = TOPIC_NONE;
		topicAuthor = "";
		key = KEY_NONE;
		clients = new ArrayList<Client>();
		logFile = createDefaultActivityLogFilePath(name);
		logging = false;
		fileLog = null;
	}


	/**
	 * Creates the path to the log file of a channel,
	 * where its activity may be logged.
	 * @param channelName the name of the channel to query the log file for
	 * @return local file-system path to a channels activity log file
	 */
	public static File createDefaultActivityLogFilePath(String channelName) {
		return new File(LOG_FILES_DIR, "channel_" + channelName + ".log");
	}

	public static boolean isTopicNone(String topic) {
		return (topic.equals(TOPIC_NONE) || topic.equals(TOPIC_NONE_2));
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

		Configuration configuration =
				context.getService(Configuration.class);
		String channelsToLogRegex =
				configuration.getString(ServerConfiguration.CHANNELS_LOG_REGEX);
		setLogging(name.matches(channelsToLogRegex));
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

	/**
	 * Sets a new topic or disables topic for this channel.
	 * @param newTopic content of the new topic, or TOPIC_NONE or TOPIC_NONE_2
	 *   to disable the topic.
	 * @param author username of the author of the new topic
	 * @return returns true if the new topic has been set, false if the topic
	 *   has been disabled.
	 */
	public boolean setTopic(String newTopic, String author) {

		topicAuthor = author;
		topicChangedTime = System.currentTimeMillis();
		if (isTopicNone(newTopic.trim())) {
			topic = TOPIC_NONE;
			LOG.debug("* Topic for #{} disabled (by <{}>)", name, author);
			return false;
		} else {
			topic = newTopic.trim();
			if (LOG.isDebugEnabled()) {
				LOG.debug("* Topic for #{} changed to '{}' (set by <{}>)",
						new Object[] {name, topic, author});
			}
			return true;
		}
	}

	public boolean isTopicSet() {
		return !isTopicNone(topic);
	}

	/** Sends 'msg' as a channel message to all clients on this channel */
	public void broadcast(String msg) {

		if (msg.trim().isEmpty()) {
			// do not send empty messages
			return;
		}

		sendLineToClients(String.format("CHANNELMESSAGE %s %s", name, msg));
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
		} catch (IndexOutOfBoundsException ex) {
			return null;
		}
	}

	/** Sends a text to all clients in this channel */
	public void sendLineToClients(String msg) {

		if (fileLog != null) {
			// As DateFormats are generally not-thread save,
			// we always create a new one.
			DateFormat timeFormat = new SimpleDateFormat("<HH:mm:ss> ");
			fileLog.println(timeFormat.format(new Date()));
			fileLog.println(msg);
		}
		for (int i = 0; i < clients.size(); i++) {
			clients.get(i).sendLine(msg);
		}
	}

	public boolean isLocked() {
		return !(key.equals(KEY_NONE) || key.equals(KEY_NONE_2));
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

	// TODO use slf4j too?
	public boolean setLogging(boolean enabled) {

		// only change if change is needed
		if (enabled != isLogging()) {
			if (enabled) {
				OutputStream fOut = null;
				OutputStream bOut = null;
				try {
					fOut = new FileOutputStream(logFile, true);
					bOut = new BufferedOutputStream(fOut);
					fileLog = new PrintStream(bOut, true);
					fileLog.println();
					fileLog.print("Log started on ");
					fileLog.println(new SimpleDateFormat("dd/MM/yy")
							.format(new Date()));
					logging = true;
				} catch (IOException ex) {
					try {
						if (fileLog != null) {
							fileLog.close();
						} else if (bOut != null) {
							bOut.close();
						} else if (fOut != null) {
							fOut.close();
						}
					} catch (IOException ex2) {
						LOG.warn("Failed to close buffered stream to channel"
								+ " log-file " + logFile.getAbsolutePath(),
								ex2);
					}
					fileLog = null;
					LOG.error("Unable to open channel log file for channel "
							+ name + ": " + logFile.getAbsolutePath(), ex);
					logFile = null;
				}
			} else {
				try {
					fileLog.close();
				} catch (Exception ex) {
					LOG.warn("Failed to close buffered stream to channel"
							+ " log-file " + logFile.getAbsolutePath(), ex);
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
