/*
 * Created on 2006.11.2
 *
 */

/**
 * @author Betalord
 *
 */

import java.util.ArrayList;

public class Channels {
	
	static private ArrayList<Channel> channels = new ArrayList<Channel>();

	public static int getChannelsSize() {
		return channels.size();
	}

	/* returns null if channel does not exist (is not open) */
	public static Channel getChannel(String chanName) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).name.equals(chanName)) return channels.get(i);
		}
		return null;
	}
	
	/* returns null if index is out of bounds */
	public static Channel getChannel(int index) {
		try {
			return channels.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}	

	public static boolean addChannel(Channel chan) {
		if (getChannel(chan.name) != null) return false; // channel already exists!
		channels.add(chan);
		return true;
	}
	
	/* removes channel from channel list. Returns true if channel was found. */
	public static boolean removeChannel(Channel chan) {
		return channels.remove(chan);
	}

	/* sends information on all clients in a channel (and the topic if it is set) to the client */
	public static boolean sendChannelInfoToClient(Channel chan, Client client) {
		client.beginFastWrite();
		// it always sends info about at least one client - the one to whom this list must be sent
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENTS ").append(chan.name);
		int c = 0;

		for (int i = 0; i < chan.getClientsSize(); i++) {
			sb.append(' ').append(chan.getClient(i).account.user);
			c++;
			// 10 is the maximum number of users in a single line (we would like to avoid too long lines, but it is not vital)
			if (c > 10) {
				client.sendLine(sb.toString());
				sb = new StringBuilder();
				sb.append("CLIENTS ").append(chan.name);
				c = 0;
			}
		}
		if (c > 0) {
			client.sendLine(sb.toString());
		}

		// send the topic:
		if (chan.isTopicSet()) client.sendLine("CHANNELTOPIC " + chan.name + " " + chan.getTopicAuthor() + " " + chan.getTopicChangedTime() + " " + chan.getTopic());

		client.endFastWrite();
		return true;
	}
	
	/* sends a list of all open channels to client */
	public static void sendChannelListToClient(Client client) {
		if (channels.size() == 0) return ; // nothing to send
		
		client.beginFastWrite();
		for (int i = 0; i < channels.size(); i++) {
			client.sendLine("CHANNEL " + channels.get(i).name + " " + channels.get(i).getClientsSize() + (channels.get(i).isTopicSet() ? " " + channels.get(i).getTopic() : ""));
		}
		client.sendLine("ENDOFCHANNELS");
		client.endFastWrite();
	}
	
	public static void notifyClientsOfNewClientInChannel(Channel chan, Client client) {
		for (int i = 0; i < chan.getClientsSize(); i++) {
			if (chan.getClient(i) == client) continue;
			chan.getClient(i).sendLine("JOINED " + chan.name + " " + client.account.user);
		}
	}
	
	// returns 'null' if channel name is valid, or error description otherwise
	public static String isChanNameValid(String channame) {
		if (channame.length() > 20) return "Channel name too long";
		if (channame.length() < 1) return "Channel name too short";
		if (!channame.matches("^[A-Za-z0-9_\\[\\]]+$")) return "Channel name contains invalid characters";
		// everything is OK:
		return null;
	}	

}
