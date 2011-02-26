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
import com.springrts.tasserver.Channel;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Lets a moderator kick a client from a channel.
 * @author hoijui
 */
@SupportedCommand("FORCELEAVECHANNEL")
public class ForceLeaveChannelCommandProcessor
		extends AbstractCommandProcessor
{
	public ForceLeaveChannelCommandProcessor() {
		super(2, ARGS_MAX_NOCHECK, Account.Access.PRIVILEGED);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine(String.format(
					"SERVERMSG Bad arguments (command %s)", getCommandName()));
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String channelName = args.get(0);
		String username = args.get(1);

		Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			client.sendLine(String.format(
					"SERVERMSG Error: Channel does not exist: %s",
					channelName));
			return false;
		}

		Client target = getContext().getClients().getClient(username);
		if (target == null) {
			client.sendLine(String.format(
					"SERVERMSG Error: <%s> not found!", username));
			return false;
		}

		if (!chan.isClientInThisChannel(target)) {
			client.sendLine(String.format(
					"SERVERMSG Error: <%s> is not in the channel #%s!",
					username, chan.getName()));
			return false;
		}

		String reason = null;
		if (args.size() > 2) {
			reason = Misc.makeSentence(args, 2);
		}

		chan.broadcast(String.format("<%s> has kicked <%s> from the channel%s",
				client.getAccount().getName(),
				target.getAccount().getName(),
				(reason == null) ? "" : " (reason: " + reason + ")"));
		target.sendLine(String.format("FORCELEAVECHANNEL %s %s%s",
				chan.getName(),
				client.getAccount().getName(),
				(reason == null) ? "" : " " + reason));
		target.leaveChannel(chan, "kicked from channel");

		return true;
	}
}
