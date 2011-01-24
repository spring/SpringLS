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
import com.springrts.tasserver.ServerNotification;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("REDIRECTOFF")
public class RedirectOffCommandProcessor extends AbstractCommandProcessor {

	public RedirectOffCommandProcessor() {
		super(Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		getContext().getServer().disableRedirect();
		getContext().getClients().sendToAllRegisteredUsers("BROADCAST Server has left redirection mode");

		// add server notification:
		ServerNotification sn = new ServerNotification("Redirection mode disabled");
		sn.addLine(new StringBuilder("Admin <").append(client.getAccount().getName())
				.append("> has disabled redirection mode.").toString());
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
