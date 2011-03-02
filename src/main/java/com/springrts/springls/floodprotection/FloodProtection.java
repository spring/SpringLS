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
import com.springrts.springls.Clients;
import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.ServerNotification;
import com.springrts.springls.Updateable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hoijui
 */
public class FloodProtection implements FloodProtectionService, Updateable,
		ContextReceiver
{
	private static final Logger LOG
			= LoggerFactory.getLogger(FloodProtection.class);

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

	/**
	 * How many bytes did this client to us since login until the last
	 * {@link #receivedRecordPeriod} seconds interval has passed.
	 * XXX if lookups on this get too expensive, add it to a property list in
	 *   Client its self: <tt>client.properties[receivedOverPeriodIndex]</tt>
	 */
	private Map<Client, Long> receivedTillLastCheck;

	private Context context = null;

	public FloodProtection() {

		this.receivedRecordPeriod = 10;
		this.maxBytesAlert = 20000;
		this.maxBytesAlertForBot = 50000;
		this.lastFloodCheckedTime = System.currentTimeMillis();
		this.receivedTillLastCheck = new HashMap<Client, Long>();
	}


	/**
	 * {@inheritDoc}
	 * This has to be called after
	 */
	@Override
	public void update() {

		memorizeReceivedBytes();
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}
	protected Context getContext() {
		return context;
	}

	private void memorizeReceivedBytes() {

		if (hasFloodCheckPeriodPassed()) {
			receivedTillLastCheck.clear();
			Clients clients = getContext().getClients();
			for (int c = 0; c < clients.getClientsSize(); c++) {
				Client client = clients.getClient(c);
				receivedTillLastCheck.put(client,
						client.getReceivedSinceLogin());
			}
		}
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

	private long getReceivedSinceLastCheck(Client client) {

		long receivedSinceLastCheck = 0L;

		if (receivedTillLastCheck.containsKey(client)) {
			receivedSinceLastCheck = client.getReceivedSinceLogin()
					- receivedTillLastCheck.get(client);
		}

		return receivedSinceLastCheck;
	}

	private boolean checkFlooding(Client client) {

		boolean flooding = false;

		if (client.getAccount().getAccess().isLessThen(Account.Access.ADMIN)) {
			int maxBytes = client.getAccount().isBot()
					? getMaxBytesAlertForBot()
					: getMaxBytesAlert();
			long receivedSinceLastCheck = getReceivedSinceLastCheck(client);
			flooding = (receivedSinceLastCheck > maxBytes);
		}

		return flooding;
	}

	@Override
	public boolean isFlooding(Client client) {

		boolean flooding = checkFlooding(client);

		// basic anti-flood protection:
		if (flooding) {
			LOG.warn("Flooding detected from {} ({})",
					client.getIp().getHostAddress(),
					client.getAccount().getName());
			getContext().getClients().sendToAllAdministrators(
					String.format(
					"SERVERMSG [broadcast to all admins]:"
					+ " Flooding has been detected from %s <%s>."
					+ " User has been kicked.",
					client.getIp().getHostAddress(),
					client.getAccount().getName()));
			getContext().getClients().killClient(client,
					"Disconnected due to excessive flooding");

			// add server notification:
			ServerNotification sn = new ServerNotification(
					"Flooding detected");
			sn.addLine(String.format(
					"Flooding detected from %s (%s).",
					client.getIp().getHostAddress(),
					client.getAccount().getName()));
			sn.addLine("User has been kicked from the server.");
			getContext().getServerNotifications().addNotification(sn);
		}

		return flooding;
	}

	/**
	 * Checks if the the flood-protection time period has passed already,
	 * and if so, resets the last-check-time.
	 * @return true if the flood-protection time period has passed since
	 *   the last successful call to this method
	 */
	private boolean hasFloodCheckPeriodPassed() {

		boolean passed = (System.currentTimeMillis()
				- getLastFloodCheckedTime())
				> (getReceivedRecordPeriod() * 1000L);

		if (passed) {
			lastFloodCheckedTime = System.currentTimeMillis();
		}

		return passed;
	}
}
