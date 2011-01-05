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
public class ForceLeaveChannelCommandProcessor extends AbstractCommandProcessor {

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
			client.sendLine("SERVERMSG Bad arguments (command FORCELEAVECHANNEL)");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String channelName = args.get(0);
		String username = args.get(1);

		Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			client.sendLine(new StringBuilder("SERVERMSG Error: Channel does not exist: ").append(channelName).toString());
			return false;
		}

		Client target = getContext().getClients().getClient(username);
		if (target == null) {
			client.sendLine(new StringBuilder("SERVERMSG Error: <").append(username).append("> not found!").toString());
			return false;
		}

		if (!chan.isClientInThisChannel(target)) {
			client.sendLine(new StringBuilder("SERVERMSG Error: <")
					.append(username).append("> is not in the channel #")
					.append(chan.getName()).append("!").toString());
			return false;
		}

		String reason = null;
		if (args.size() > 2) {
			reason = Misc.makeSentence(args, 2);
		}

		chan.broadcast(new StringBuilder("<")
				.append(client.getAccount().getName()).append("> has kicked <")
				.append(target.getAccount().getName()).append("> from the channel")
				.append((reason == null) ? "" : " (reason: ").append(reason).append(")").toString());
		target.sendLine(new StringBuilder("FORCELEAVECHANNEL ")
				.append(chan.getName()).append(" ")
				.append(client.getAccount().getName()).append(reason).toString());
		target.leaveChannel(chan, "kicked from channel");

		return true;
	}
}
