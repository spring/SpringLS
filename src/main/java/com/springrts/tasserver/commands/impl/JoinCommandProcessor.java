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
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Allows a user to join a channel.
 * @author hoijui
 */
@SupportedCommand("JOIN")
public class JoinCommandProcessor extends AbstractCommandProcessor {

	public JoinCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.NORMAL);
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
		String channelKey = Misc.makeSentence(args, 1);

		// check if channel name is OK:
		String valid = getContext().getChannels().isChanNameValid(channelName);
		if (valid != null) {
			client.sendLine(new StringBuilder("JOINFAILED Bad channel name (\"#")
					.append(channelName).append("\"). Reason: ")
					.append(valid).toString());
			return false;
		}

		// check if key is correct (if channel is locked):
		Channel chan = getContext().getChannels().getChannel(channelName);
		if ((chan != null) && (chan.isLocked()) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0 /* we will allow admins to join locked channels */)) {
			if (!channelKey.equals(chan.getKey())) {
				client.sendLine(new StringBuilder("JOINFAILED ").append(channelName).append(" Wrong key (this channel is locked)!").toString());
				return false;
			}
		}

		chan = client.joinChannel(channelName);
		if (chan == null) {
			client.sendLine(new StringBuilder("JOINFAILED ").append(channelName).append(" Already in the channel!").toString());
			return false;
		}
		client.sendLine(new StringBuilder("JOIN ").append(channelName).toString());
		getContext().getChannels().sendChannelInfoToClient(chan, client);
		getContext().getChannels().notifyClientsOfNewClientInChannel(chan, client);

		return true;
	}
}
