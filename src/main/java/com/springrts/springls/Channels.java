/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

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


import java.util.ArrayList;
import java.util.List;

/**
 * @author Betalord
 * @author hoijui
 */
public class Channels implements ContextReceiver, LiveStateListener, Updateable
{
	private static final String MATCH_NO_CHANNEL = "^%%%%%%%$";

	private List<Channel> channels;
	private Context context;
	private String channelsToLogRegex;

	/**
	 * In this interval, all channel mute lists will be checked
	 * for expirations and purged accordingly. In milliseconds.
	 */
	private long purgeMutesInterval = 1000 * 3;
	/**
	 * Time when we last purged mute lists of all channels.
	 * @see java.lang.System#currentTimeMillis()
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

		// it always sends info about at least one client;
		// the one to whom this list must be sent
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENTS ").append(chan.getName());
		int c = 0;

		client.beginFastWrite();

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

		String cmd = String.format("JOINED %s %s", chan.getName(),
				client.getAccount().getName());
		for (int i = 0; i < chan.getClientsSize(); i++) {
			Client toBeNotified = chan.getClient(i);
			if (toBeNotified != client) {
				toBeNotified.sendLine(cmd);
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
