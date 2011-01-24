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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.ServerNotification;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Allows an administrator to add a server-notification.
 * @author hoijui
 */
@SupportedCommand("ADDNOTIFICATION")
public class AddNotificationCommandProcessor extends AbstractCommandProcessor {

	public AddNotificationCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("SERVERMSG Error: arguments missing (ADDNOTIFICATION command)");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String message = Misc.makeSentence(args, 0);

		if (getContext().getServerNotifications().addNotification(
				new ServerNotification("Admin notification",
					client.getAccount().getName(), message)))
		{
			client.sendLine("SERVERMSG Notification added.");
		} else {
			client.sendLine("SERVERMSG Error while adding notification! Notification not added.");
		}

		return true;
	}
}
