/*
	Copyright (c) 2010 Robin Vobruba <robin.vobruba@derisk.ch>

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

package com.springrts.tasserver;


import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and processes the "message of the day" (MOTD).
 * It can be set by an administrator, and is sent to clients when they log in.
 * @author hoijui
 */
public class MessageOfTheDay implements ContextReceiver {

	private static final Logger LOG
			= LoggerFactory.getLogger(MessageOfTheDay.class);

	private static final String DEFAULT_TEXT = "Enjoy your stay :-)";
	private static final String DEFAULT_FILENAME = "motd.txt";


	private Context context = null;

	private String message;

	public MessageOfTheDay() {
		message = DEFAULT_TEXT;
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}
	protected Context getContext() {
		return context;
	}

	/**
	 * Reads MOTD from the default file from disk.
	 * @see #read(String fileName)
	 */
	public boolean read() {
		return read(DEFAULT_FILENAME);
	}
	/**
	 * Reads MOTD from disk (if file is found).
	 * @return true if read was successful
	 */
	public boolean read(String fileName) {

		boolean success = false;

		try {
			message = Misc.readTextFile(new File(fileName));
			success = true;
			LOG.info("Using MOTD from file '{}'.", fileName);
		} catch (IOException ex) {
			LOG.warn("Could not find or read from file '{}'."
					+ " Using the default MOTD.", fileName);
			LOG.debug("... reason:", ex);
			success = false;
		}

		return success;
	}

	/**
	 * Sends MOTD to a client.
	 * @return true if sent successfully
	 */
	public boolean sendTo(Client client) {

		client.beginFastWrite();
		client.sendLine(new StringBuilder("MOTD Welcome, ").append(client.getAccount().getName()).append("!").toString());
		client.sendLine(new StringBuilder("MOTD There are currently ").append((getContext().getClients().getClientsSize() - 1)).append(" clients connected").toString()); // -1 is because we should not count the client to which we are sending MOTD
		client.sendLine(new StringBuilder("MOTD to server talking in ").append(getContext().getChannels().getChannelsSize()).append(" open channels and").toString());
		client.sendLine(new StringBuilder("MOTD participating in ").append(getContext().getBattles().getBattlesSize()).append(" battles.").toString());
		client.sendLine(new StringBuilder("MOTD Server's uptime is ").append(Misc.timeToDHM(System.currentTimeMillis() - context.getServer().getStartTime())).append(".").toString());
		client.sendLine("MOTD");
		String[] sl = message.split(Misc.EOL);
		for (int i = 0; i < sl.length; i++) {
			client.sendLine(new StringBuilder("MOTD ").append(sl[i]).toString());
		}
		client.endFastWrite();

		return true;
	}
}
