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
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sent by client when he is trying to send a private message to some other
 * client.
 * @author hoijui
 */
@SupportedCommand("SAYPRIVATE")
public class SayPrivateCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SayPrivateCommandProcessor.class);

	public SayPrivateCommandProcessor() {
		super(2, ARGS_MAX_NOCHECK, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String channelName = args.get(0);
		String message = Misc.makeSentence(args, 1);

		Client target = getContext().getClients().getClient(channelName);
		if (target == null) {
			return false;
		}

		// check for flooding:
		if ((message.length() > getContext().getServer().getMaxChatMessageLength())
				&& client.getAccount().getAccess().isLessThen(Account.Access.ADMIN))
		{
			LOG.warn("Flooding detected from {} ({}) [exceeded max. chat message size]",
					client.getIp().getHostAddress(),
					client.getAccount().getName());
			client.sendLine(String.format(
					"SERVERMSG Flooding detected - you have exceeded the"
					+ " maximum allowed chat message size (%d bytes)."
					+ " Your message has been ignored.",
					getContext().getServer().getMaxChatMessageLength()));
			getContext().getClients().sendToAllAdministrators(String.format(
					"SERVERMSG [broadcast to all admins]: Flooding has been"
					+ " detected from %s <%s> - exceeded maximum chat message"
					+ " size. Ignoring ...",
					client.getIp().getHostAddress(),
					client.getAccount().getName()));
			return false;
		}

		target.sendLine(String.format("SAIDPRIVATE %s %s",
				client.getAccount().getName(), message));
		// echo the command. See protocol description!
		client.sendLine(getCommandName() + " " + Misc.makeSentence(args, 0));

		return true;
	}
}
