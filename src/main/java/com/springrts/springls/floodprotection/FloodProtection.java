/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.floodprotection;


import com.springrts.springls.Account;
import com.springrts.springls.Client;

/**
 *
 * @author hoijui
 */
public class FloodProtection {

	/**
	 * Length of time period for which we keep record of bytes received from
	 * the client, in seconds.
	 */
	private int receivedRecordPeriod;
	/**
	 * Maximum number of bytes received in the last
	 * {@link #receivedRecordPeriod} seconds from a single client before we
	 * raise "flood alert".
	 */
	private int maxBytesAlert;
	/**
	 * Same as {@link #maxBytesAlert} but is used for clients in "bot mode"
	 * only.
	 * @see Client#isBot()
	 */
	private int maxBytesAlertForBot;
	/**
	 * Time when we last updated the flood-check.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long lastFloodCheckedTime;

	public FloodProtection() {

		this.receivedRecordPeriod = 10;
		this.maxBytesAlert = 20000;
		this.maxBytesAlertForBot = 50000;
		this.lastFloodCheckedTime = System.currentTimeMillis();
	}

	/**
	 * Length of time period for which we keep record of bytes received from
	 * the client, in seconds.
	 * @return the receivedRecordPeriod
	 */
	public int getReceivedRecordPeriod() {
		return receivedRecordPeriod;
	}

	/**
	 * Length of time period for which we keep record of bytes received from
	 * the client, in seconds.
	 * @param receivedRecordPeriod the receivedRecordPeriod to set
	 */
	public void setReceivedRecordPeriod(int receivedRecordPeriod) {
		this.receivedRecordPeriod = receivedRecordPeriod;
	}

	/**
	 * Maximum number of bytes received in the last
	 * {@link #receivedRecordPeriod} seconds from a single client before we
	 * raise "flood alert".
	 * @return the maxBytesAlert
	 */
	public int getMaxBytesAlert() {
		return maxBytesAlert;
	}

	/**
	 * Maximum number of bytes received in the last
	 * {@link #receivedRecordPeriod} seconds from a single client before we
	 * raise "flood alert".
	 * @param maxBytesAlert the maxBytesAlert to set
	 */
	public void setMaxBytesAlert(int maxBytesAlert) {
		this.maxBytesAlert = maxBytesAlert;
	}

	/**
	 * Same as {@link #maxBytesAlert} but is used for clients in "bot mode"
	 * only.
	 * @see Account#isBot()
	 * @return the maxBytesAlertForBot
	 */
	public int getMaxBytesAlertForBot() {
		return maxBytesAlertForBot;
	}

	/**
	 * Same as {@link #maxBytesAlert} but is used for clients in "bot mode"
	 * only.
	 * @see Account#isBot()
	 * @param maxBytesAlertForBot the maxBytesAlertForBot to set
	 */
	public void setMaxBytesAlertForBot(int maxBytesAlertForBot) {
		this.maxBytesAlertForBot = maxBytesAlertForBot;
	}

	/**
	 * Time when we last updated the flood-check.
	 * @see java.lang.System#currentTimeMillis()
	 * @return the lastFloodCheckedTime
	 */
	public long getLastFloodCheckedTime() {
		return lastFloodCheckedTime;
	}

	/**
	 * Time when we last updated the flood-check.
	 * @see java.lang.System#currentTimeMillis()
	 * @param lastFloodCheckedTime the lastFloodCheckedTime to set
	 */
//	public void setLastFloodCheckedTime(long lastFloodCheckedTime) {
//		this.lastFloodCheckedTime = lastFloodCheckedTime;
//	}

	/**
	 * Returns true if the supplied client is flooding
	 * @param client the client to check for flooding
	 * @return true if the supplied client is flooding
	 */
	public boolean isFlooding(Client client) {

		int maxBytes = client.getAccount().isBot() ? getMaxBytesAlertForBot()
				: getMaxBytesAlert();
		return client.getAccount().getAccess().isAtLeast(Account.Access.ADMIN)
				&& (client.getDataOverLastTimePeriod() > maxBytes);
	}

	/**
	 * Checks if the the flood-protection time period has passed already,
	 * and if so, resets the last-check-time.
	 * @return true if the flood-protection time period has passed since
	 *   the last successful call to this method
	 */
	public boolean hasFloodCheckPeriodPassed() {

		boolean passed = (System.currentTimeMillis()
				- getLastFloodCheckedTime())
				> (getReceivedRecordPeriod() * 1000L);

		if (passed) {
			lastFloodCheckedTime = System.currentTimeMillis();
		}

		return passed;
	}
}
