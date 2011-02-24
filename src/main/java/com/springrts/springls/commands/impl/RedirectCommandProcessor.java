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
import com.springrts.springls.Misc;
import com.springrts.springls.ServerNotification;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.net.InetAddress;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("REDIRECT")
public class RedirectCommandProcessor extends AbstractCommandProcessor {

	public RedirectCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String redirectIpStr = args.get(0);
		InetAddress redirectIp = Misc.parseIp(redirectIpStr);
		if (redirectIp == null) {
			return false;
		}
		getContext().getServer().setRedirectAddress(redirectIp);
		getContext().getClients().sendToAllRegisteredUsers(
				"BROADCAST Server has entered redirection mode");

		// add server notification:
		ServerNotification sn = new ServerNotification(
				"Entered redirection mode");
		sn.addLine(String.format(
				"Admin <%s> has enabled redirection mode. New address: %s",
				client.getAccount().getName(), redirectIpStr));
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
