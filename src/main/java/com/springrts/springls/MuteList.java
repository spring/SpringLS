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


import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Betalord
 * @author hoijui
 */
public class MuteList {

	private static class MuteEntry {

		private String username;
		private long expireTime;
		private InetAddress ip;

		MuteEntry(String username, long expireTime, InetAddress ip) {

			this.username = username;
			this.expireTime = expireTime;
			this.ip = ip;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * Time (in milliseconds) when it will expire.
		 * Expired records are automatically removed in certain methods.
		 * Use 0 to mute user for indefinite time.
		 * @see java.lang.System#currentTimeMillis()
		 */
		public long getExpireTime() {
			return expireTime;
		}

		/**
		 * IP of the muted users.
		 * If the user is not muted by IP (but only by username), the IP is
		 * <code>null</code>.
		 */
		public InetAddress getIp() {
			return ip;
		}

		public boolean isPersistent() {
			return (expireTime == 0);
		}
		public boolean isExpired() {
			return (!isPersistent()
					&& getExpireTime() <= System.currentTimeMillis());
		}
	}

	private List<MuteEntry> mutes;
	private Channel channel;

	public MuteList(Channel channel) {

		this.mutes = new ArrayList<MuteEntry>();
		this.channel = channel;
	}

	/** Purges expired entries */
	public void clearExpiredOnes() {

		ListIterator<MuteEntry> m = mutes.listIterator();
		while (m.hasNext()) {
			MuteEntry mute = m.next();
			if (mute.isExpired()) {
				m.remove();
				channel.broadcast(String.format(
						"<%s> has been unmuted (mute expired)",
						mute.getUsername()));
			}
		}
	}

	public boolean isMuted(String username) {

		clearExpiredOnes();
		return isMutedFast(username);
	}

	private boolean isMutedFast(String username) {

		boolean muted = false;

		for (MuteEntry mute : mutes) {
			if (mute.getUsername().equals(username)) {
				muted = true;
				break;
			}
		}

		return muted;
	}

	public boolean isMuted(String username, InetAddress ip) {

		boolean muted = false;

		clearExpiredOnes();

		for (MuteEntry mute : mutes) {
			if (mute.getUsername().equals(username)
					|| ((mute.getIp() != null) && (mute.getIp().equals(ip))))
			{
				muted = true;
				break;
			}
		}

		return muted;
	}

	public boolean isIpMuted(InetAddress ip) {

		boolean muted = false;

		clearExpiredOnes();

		for (MuteEntry mute : mutes) {
			if ((mute.getIp() != null) && (mute.getIp().equals(ip))) {
				muted = true;
				break;
			}
		}

		return muted;
	}

	/**
	 * Mutes a user.
	 * @param username name of the user to mute.
	 * @param seconds use to specify for how long he user should be muted.
	 * @param ip set to 'null' if you don't want to mute this user by the IP.
	 * @return false if already muted
	 */
	public boolean mute(String username, long seconds, InetAddress ip) {

		if (isMutedFast(username)) {
			return false;
		}

		long until = 0L;
		if (seconds != 0L) {
			until = System.currentTimeMillis() + (seconds * 1000L);
		}

		mutes.add(new MuteEntry(username, until, ip));

		return true;
	}

	/**
	 * @return false if the user is not on the list
	 */
	public boolean unmute(String username) {

		boolean unmuted = false;

		for (MuteEntry mute : mutes) {
			if (mute.getUsername().equals(username)) {
				mutes.remove(mute);
				unmuted = true;
				break;
			}
		}

		return unmuted;
	}

	public int size() {

		clearExpiredOnes();
		return mutes.size();
	}

	public String getUsername(int index) {

		if (index >= mutes.size()) {
			return "";
		} else {
			return mutes.get(index).getUsername();
		}
	}

	public long getRemainingSeconds(int index) {

		// note: you shouldn't call clearExpiredOnes() here!
		// (see "MUTELIST" command to see why)
		if (index >= mutes.size()) {
			return -1;
		} else if (mutes.get(index).isPersistent()) {
			return 0;
		} else {
			return (mutes.get(index).getExpireTime()
					- System.currentTimeMillis()) / 1000L;
		}
	}

	/**
	 * @return 'null' if no IP is set for the user
	 */
	public InetAddress getIp(int index) {
		return mutes.get(index).getIp();
	}

	public boolean rename(String oldUsername, String newUsername) {

		for (MuteEntry mute : mutes) {
			if (mute.getUsername().equals(oldUsername)) {
				mute.setUsername(newUsername);
				return true;
			}
		}
		return false;
	}
}
