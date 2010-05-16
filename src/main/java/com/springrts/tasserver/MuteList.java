/*
 * Created on 3.12.2005
 */

package com.springrts.tasserver;


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
	private ArrayList<String> IPs;
	private Channel channel;

	public MuteList(Channel channel) {
		usernames = new ArrayList<String>();
		mutedUntil = new ArrayList<Long>();
		IPs = new ArrayList<String>();
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
				IPs.remove(i);
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

	public boolean isMuted(String username, String IP) {
		clearExpiredOnes();

		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(username) || ((IPs.get(i) != null) && (IPs.get(i).equals(IP)))) {
				return true;
			}
		}

		return false;
	}

	public boolean isIPMuted(String IP) {
		clearExpiredOnes();

		for (int i = 0; i < IPs.size(); i++) {
			if ((IPs.get(i) != null) && (IPs.get(i).equals(IP))) return true;
		}

		return false;
	}

	/**
	 * Mutes a user.
	 * @param username name of hte user to mute.
	 * @param seconds use to specify for how long he user should be muted.
	 * @param IP set to 'null' if you don't want to mute this user by the IP.
	 * @return false if already muted
	 */
	public boolean mute(String username, int seconds, String IP) {

		for (int i = 0; i < usernames.size(); i++) {
			if (usernames.get(i).equals(username)) {
				return false;
			}
		}

		usernames.add(username);
		Long until = new Long(0);
		if (seconds != 0) until = new Long(System.currentTimeMillis() + seconds*1000);
		mutedUntil.add(until);
		IPs.add(IP);
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
				IPs.remove(i);
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
		if (index > usernames.size()-1) return ""; else return usernames.get(index);
	}

	public long getRemainingSeconds(int index) {
		// note: you shouldn't call clearExpiredOnes() here! (see "MUTELIST" command to see why)
		if (index > mutedUntil.size()-1) return -1;
		if (mutedUntil.get(index).longValue() == 0) return 0;
		else return (mutedUntil.get(index).longValue() - System.currentTimeMillis()) / 1000;
	}

	/**
	 * @return 'null' if no IP is set for the user
	 */
	public String getIP(int index) {
		return IPs.get(index);
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
