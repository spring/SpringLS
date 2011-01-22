/*
 * Created on 3.12.2005
 */

package com.springrts.tasserver;


import java.net.InetAddress;
import java.util.ArrayList;

/**
 * @author Betalord
 */
public class MuteList {

	private ArrayList<String> usernames;
	/**
	 * Time (in milliseconds) when it will expire.
	 * Expired records are automatically removed in certain methods.
	 * Use 0 to mute user for indefinite time.
	 * @see System.currentTimeMillis()
	 */
	private ArrayList<Long> mutedUntil;
	/**
	 * IPs of the muted users.
	 * If user is not muted by IP (but only by username), the corresponding
	 * IP in this list is set to 'null'.
	 */
	private ArrayList<InetAddress> ips;
	private Channel channel;

	public MuteList(Channel channel) {

		this.usernames = new ArrayList<String>();
		this.mutedUntil = new ArrayList<Long>();
		this.ips = new ArrayList<InetAddress>();
		this.channel = channel;
	}

	/** Purges expired entries */
	public void clearExpiredOnes() {
		// remove any expired records
		// (those with expire time 0 are persistent, we won't remove them):
		for (int i = 0; i < usernames.size(); i++) {
			if ((mutedUntil.get(i).longValue() <= System.currentTimeMillis()) && (mutedUntil.get(i).longValue() != 0)) {
				channel.broadcast("<" + usernames.get(i) + "> has been unmuted (mute expired)");
				usernames.remove(i);
				mutedUntil.remove(i);
				ips.remove(i);
				i--;
			}
		}
	}

	public boolean isMuted(String username) {
		clearExpiredOnes();

		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(username)) {
				return true;
			}
		}

		return false;
	}

	public boolean isMuted(String username, InetAddress ip) {
		clearExpiredOnes();

		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(username) || ((ips.get(i) != null) && (ips.get(i).equals(ip)))) {
				return true;
			}
		}

		return false;
	}

	public boolean isIpMuted(InetAddress ip) {
		clearExpiredOnes();

		for (int i = 0; i < ips.size(); i++) {
			if ((ips.get(i) != null) && (ips.get(i).equals(ip))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Mutes a user.
	 * @param username name of hte user to mute.
	 * @param seconds use to specify for how long he user should be muted.
	 * @param ip set to 'null' if you don't want to mute this user by the IP.
	 * @return false if already muted
	 */
	public boolean mute(String username, long seconds, InetAddress ip) {

		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(username)) {
				return false;
			}
		}

		usernames.add(username);
		Long until = 0L;
		if (seconds != 0) {
			until = System.currentTimeMillis() + (seconds * 1000);
		}
		mutedUntil.add(until);
		ips.add(ip);
		return true;
	}

	/**
	 * @return false if the user is not on the list
	 */
	public boolean unmute(String username) {

		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(username)) {
				usernames.remove(i);
				mutedUntil.remove(i);
				ips.remove(i);
				return true;
			}
		}

		return false;
	}

	public int size() {
		clearExpiredOnes();
		return usernames.size();
	}

	public String getUsername(int index) {
		if (index > usernames.size()-1) {
			return "";
		} else {
			return usernames.get(index);
		}
	}

	public long getRemainingSeconds(int index) {
		// note: you shouldn't call clearExpiredOnes() here! (see "MUTELIST" command to see why)
		if (index > mutedUntil.size()-1) {
			return -1;
		} else if (mutedUntil.get(index).longValue() == 0) {
			return 0;
		} else {
			return (mutedUntil.get(index).longValue() - System.currentTimeMillis()) / 1000;
		}
	}

	/**
	 * @return 'null' if no IP is set for the user
	 */
	public InetAddress getIp(int index) {
		return ips.get(index);
	}

	public boolean rename(String oldUsername, String newUsername) {
		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(oldUsername)) {
				usernames.set(i, newUsername);
				return true;
			}
		}
		return false;
	}
}
