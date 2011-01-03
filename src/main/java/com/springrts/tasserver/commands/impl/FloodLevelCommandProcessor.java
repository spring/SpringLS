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

package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("FLOODLEVEL")
public class FloodLevelCommandProcessor extends AbstractCommandProcessor {

	public FloodLevelCommandProcessor() {
		super(2, 2, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String type = args.get(0).toUpperCase();

		if (type.equals("PERIOD")) {
			int seconds = Integer.parseInt(args.get(1));
			getContext().getServer().getFloodProtection().setReceivedRecordPeriod(seconds);
			client.sendLine(new StringBuilder("SERVERMSG The antiflood period is now ")
					.append(seconds).append(" seconds.").toString());
		} else if (type.equals("USER")) {
			int bytes = Integer.parseInt(args.get(1));
			getContext().getServer().getFloodProtection().setMaxBytesAlert(bytes);
			client.sendLine(new StringBuilder("SERVERMSG The antiflood amount for a normal user is now ")
					.append(bytes).append(" bytes.").toString());
		} else if (type.equals("BOT")) {
			int bytes = Integer.parseInt(args.get(1));
			getContext().getServer().getFloodProtection().setMaxBytesAlertForBot(bytes);
			client.sendLine(new StringBuilder("SERVERMSG The antiflood amount for a bot is now ")
					.append(bytes).append(" bytes.").toString());
		}

		return true;
	}
}
