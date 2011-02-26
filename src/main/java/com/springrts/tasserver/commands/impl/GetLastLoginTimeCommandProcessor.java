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

package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Lets an administrator fetch the time a specified account was last logged in.
 * @author hoijui
 */
@SupportedCommand("GETLASTLOGINTIME")
public class GetLastLoginTimeCommandProcessor extends AbstractCommandProcessor {

	public GetLastLoginTimeCommandProcessor() {
		super(1, 1, Account.Access.PRIVILEGED);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String username = args.get(0);

		Account acc = getContext().getAccountsService().getAccount(username);
		if (acc == null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: <%s> not found!",
					getCommandName(),
					username));
			return false;
		}

		if (getContext().getClients().getClient(acc.getName()) == null) {
			// As DateFormats are generally not-thread save,
			// we always create a new one.
			DateFormat dateTimeFormat = new SimpleDateFormat(
					"d MMM yyyy HH:mm:ss z");
			client.sendLine(String.format(
					"SERVERMSG <%s>'s last login was on %s",
					acc.getName(),
					dateTimeFormat.format(new Date(acc.getLastLogin()))));
		} else {
			client.sendLine(String.format("SERVERMSG <%s> is currently online",
					acc.getName()));
		}

		return true;
	}
}
