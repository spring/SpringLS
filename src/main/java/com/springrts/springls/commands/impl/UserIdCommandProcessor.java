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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.ServerNotification;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Allows a user to set a new user ID for himself.
 * @author hoijui
 */
@SupportedCommand("USERID")
public class UserIdCommandProcessor extends AbstractCommandProcessor {

	public UserIdCommandProcessor() {
		super(1, 1, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("SERVERMSG Bad USERID command - too many or too few parameters");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String userIdStr = args.get(0);

		int userId = Account.NO_USER_ID;
		try {
			long tempUserId = Long.parseLong(userIdStr, 16);
			// we transform an unsigned 32 bit integer to a signed one
			userId = (int) tempUserId;
		} catch (NumberFormatException e) {
			client.sendLine("SERVERMSG Bad USERID command - userID field should be an integer");
			return false;
		}

		client.getAccount().setLastUserId(userId);
		final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges(client.getAccount(), client.getAccount().getName());
		if (!mergeOk) {
			// FIXME set back?
			client.sendLine("SERVERMSG Failed saving last userid to persistent storage");
			return false;
		}

		// add server notification:
		ServerNotification sn = new ServerNotification("User ID received");
		sn.addLine(String.format("<%s> has generated a new user ID: %s(%d)",
				client.getAccount().getName(), userIdStr, userId));
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
