/*
 * Created on 2006.11.2
 */

package com.springrts.tasserver;


import java.util.ArrayList;
import java.util.List;

/**
 * @author Betalord
 */
public class Channels implements ContextReceiver, LiveStateListener, Updateable
{
	private List<Channel> channels;
	private Context context;
	public static final String MATCH_NO_CHANNEL = "^%%%%%%%$";
	private String channelsToLogRegex;

	/**
	 * In this interval, all channel mute lists will be checked
	 * for expirations and purged accordingly. In milliseconds.
	 */
	private long purgeMutesInterval = 1000 * 3;
	/**
	 * Time when we last purged mute lists of all channels.
	 * @see System.currentTimeMillis()
	 */
	private long lastMutesPurgeTime = System.currentTimeMillis();


	public Channels() {

		channels = new ArrayList<Channel>();
		context = null;
		channelsToLogRegex = MATCH_NO_CHANNEL;
	}


	@Override
	public void update() {
		purgeMuteLists();
	}

	/**
	 * Purges the mute-lists of all channels
	 */
	private void purgeMuteLists() {

		if ((System.currentTimeMillis() - lastMutesPurgeTime)
				> purgeMutesInterval)
		{
			lastMutesPurgeTime = System.currentTimeMillis();
			for (Channel channel : channels) {
				channel.getMuteList().clearExpiredOnes();
			}
		}
	}

	public String getChannelsToLogRegex() {
		return channelsToLogRegex;
	}

	public void setChannelsToLogRegex(String channelsToLogRegex) {
		this.channelsToLogRegex = channelsToLogRegex;
	}

	@Override
	public void receiveContext(Context context) {

		this.context = context;
		for (Channel channel : channels) {
			channel.receiveContext(context);
		}
	}

	@Override
	public void starting() {

		for (Channel channel : channels) {
			channel.starting();
		}
	}
	@Override
	public void started() {

		for (Channel channel : channels) {
			channel.started();
		}
	}

	@Override
	public void stopping() {

		for (Channel channel : channels) {
			channel.stopping();
		}
	}
	@Override
	public void stopped() {

		for (Channel channel : channels) {
			channel.stopped();
		}
	}

	public int getChannelsSize() {
		return channels.size();
	}

	/**
	 * Returns <code>null</code> if channel does not exist (is not open)
	 */
	public Channel getChannel(String chanName) {

		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).getName().equals(chanName)) {
				return channels.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns <code>null</code> if index is out of bounds
	 */
	public Channel getChannel(int index) {
		try {
			return channels.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public boolean addChannel(Channel channel) {

		if (getChannel(channel.getName()) != null) {
			// channel already exists!
			return false;
		}
		channel.receiveContext(context);
		channels.add(channel);
		return true;
	}

	/**
	 * Removes channel from channel list.
	 * @return <code>true</code> if channel was found
	 */
	public boolean removeChannel(Channel chan) {

		boolean removed = channels.remove(chan);

		if (removed) {
			chan.shutdown();
		}

		return removed;
	}

	/**
	 * Sends information about all clients in a channel to  a specific client.
	 * Also sets the topic of the channel for that client.
	 */
	public boolean sendChannelInfoToClient(Channel chan, Client client) {
		client.beginFastWrite();
		// it always sends info about at least one client;
		// the one to whom this list must be sent
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENTS ").append(chan.getName());
		int c = 0;

		for (int i = 0; i < chan.getClientsSize(); i++) {
			sb.append(' ').append(chan.getClient(i).getAccount().getName());
			c++;
			// 10 is the maximum number of users in a single line
			// (we would like to avoid too long lines, but it is not vital)
			if (c > 10) {
				client.sendLine(sb.toString());
				sb = new StringBuilder();
				sb.append("CLIENTS ").append(chan.getName());
				c = 0;
			}
		}
		if (c > 0) {
			client.sendLine(sb.toString());
		}

		// send the topic:
		if (chan.isTopicSet()) {
			client.sendLine(String.format("CHANNELTOPIC %s %s %d %s",
					chan.getName(),
					chan.getTopicAuthor(),
					chan.getTopicChangedTime(),
					chan.getTopic()));
		}

		client.endFastWrite();
		return true;
	}

	/**
	 * Sends a list of all open channels to a client
	 */
	public void sendChannelListToClient(Client client) {

		if (channels.isEmpty()) {
			// nothing to send
			return;
		}

		client.beginFastWrite();
		for (int i = 0; i < channels.size(); i++) {
			Channel chan = channels.get(i);
			client.sendLine(String.format("CHANNEL %s %d%s",
					chan.getName(),
					chan.getClientsSize(),
					(chan.isTopicSet() ? (" " + chan.getTopic()) : "")));
		}
		client.sendLine("ENDOFCHANNELS");
		client.endFastWrite();
	}

	public void notifyClientsOfNewClientInChannel(Channel chan, Client client) {

		for (int i = 0; i < chan.getClientsSize(); i++) {
			Client toBeNotified = chan.getClient(i);
			if (toBeNotified != client) {
				toBeNotified.sendLine("JOINED " + chan.getName() + " "
						+ client.getAccount().getName());
			}
		}
	}

	/**
	 * Returns <code>null</code> if the channel name is valid,
	 * an error description otherwise.
	 */
	public String isChanNameValid(String channame) {

		if (channame.length() > 20) {
			return "Channel name too long";
		}
		if (channame.length() < 1) {
			return "Channel name too short";
		}
		if (!channame.matches("^[A-Za-z0-9_\\[\\]]+$")) {
			return "Channel name contains invalid characters";
		}
		// everything is OK:
		return null;
	}
}
