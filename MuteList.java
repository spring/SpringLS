/*
 * Created on 3.12.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.ArrayList;

public class MuteList {
	private ArrayList<String> usernames;
	private ArrayList<Long> mutedUntil; // time (in milliseconds, refers to System.currentTimeMillis()) when it will expire. Expired records are automatically removed in certain methods. Use 0 to mute user for indefinite time.
	private ArrayList<String> IPs; // IPs of the muted users. If user is not muted by IP (but only by username), corresponding IP in this list is set to 'null'
	private Channel channel;

	public MuteList(Channel channel) {
		usernames = new ArrayList<String>();
		mutedUntil = new ArrayList<Long>();
		IPs = new ArrayList<String>();
		this.channel = channel;
	}

	/* will purge expired entries */
	public void clearExpiredOnes() {
		// remove any expired records (those with expire time 0 are persistent, we won't remove them):
		for (int i = 0; i < usernames.size(); i++)
			if ((mutedUntil.get(i).longValue() <= System.currentTimeMillis()) && (mutedUntil.get(i).longValue() != 0)) {
				channel.broadcast("<" + usernames.get(i) + "> has been unmuted (mute expired)");
				usernames.remove(i);
				mutedUntil.remove(i);
				IPs.remove(i);
				i--;
			}
	}

	public boolean isMuted(String username) {
		clearExpiredOnes();

		for (int i = 0; i < usernames.size(); i++)
			if (usernames.get(i).equals(username)) {
				return true;
			}

		return false;
	}

	public boolean isMuted(String username, String IP) {
		clearExpiredOnes();

		for (int i = 0; i < usernames.size(); i++)
			if (usernames.get(i).equals(username) || ((IPs.get(i) != null) && (IPs.get(i).equals(IP)))) return true;

		return false;
	}

	public boolean isIPMuted(String IP) {
		clearExpiredOnes();

		for (int i = 0; i < IPs.size(); i++) {
			if ((IPs.get(i) != null) && (IPs.get(i).equals(IP))) return true;
		}

		return false;
	}

	// returns false if already muted. Use "seconds" to tell for how long should user be muted.
	// Set 'IP' to 'null' if you don't want to mute this user by the IP.
	public boolean mute(String username, int seconds, String IP) {
		for (int i = 0; i < usernames.size(); i++)
			if (usernames.get(i).equals(username)) return false;

		usernames.add(username);
		Long until = new Long(0);
		if (seconds != 0) until = new Long(System.currentTimeMillis() + seconds*1000);
		mutedUntil.add(until);
		IPs.add(IP);
		return true;
	}

	// returns false if user is not on the list
	public boolean unmute(String username) {
		for (int i = 0; i < usernames.size(); i++)
			if (usernames.get(i).equals(username)) {
				usernames.remove(i);
				mutedUntil.remove(i);
				IPs.remove(i);
				return true;
			}

		return false;
	}

	public int size() {
		clearExpiredOnes();
		return usernames.size();
	}

	public String getUsername(int index) {
		if (index > usernames.size()-1) return ""; else return usernames.get(index);
	}

	public long getRemainingSeconds(int index) {
		// note: you shouldn't call clearExpiredOnes() here! (see "MUTELIST" command to see why)
		if (index > mutedUntil.size()-1) return -1;
		if (mutedUntil.get(index).longValue() == 0) return 0;
		else return (mutedUntil.get(index).longValue() - System.currentTimeMillis()) / 1000;
	}

	// returns 'null' if no IP is set for the user
	public String getIP(int index) {
		return IPs.get(index);
	}

	public boolean rename(String oldUsername, String newUsername) {
		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(oldUsername)) {
				usernames.set(i, new String(newUsername));
				return true;
			}
		}
		return false;
	}

}
