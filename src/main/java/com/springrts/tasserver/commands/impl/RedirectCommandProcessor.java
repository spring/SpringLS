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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hoijui
 */
@SupportedCommand("REDIRECT")
public class RedirectCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(RedirectCommandProcessor.class);

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
		try {
			getContext().getServer().setRedirectAddress(InetAddress.getByName(redirectIpStr));
		} catch (UnknownHostException ex) {
			LOG.debug("Invalid redirect IP supplied", ex);
			return false;
		}
		getContext().getClients().sendToAllRegisteredUsers("BROADCAST Server has entered redirection mode");

		// add server notification:
		ServerNotification sn = new ServerNotification("Entered redirection mode");
		sn.addLine(new StringBuilder("Admin <").append(client.getAccount().getName()).append("> has enabled redirection mode. New address: ").append(redirectIpStr).toString());
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
