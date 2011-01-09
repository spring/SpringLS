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
 * Allows a moderator to change the topic of a channel.
 * @author hoijui
 */
@SupportedCommand("CHANNELTOPIC")
public class ChannelTopicCommandProcessor extends AbstractCommandProcessor {

	public ChannelTopicCommandProcessor() {
		super(2, ARGS_MAX_NOCHECK, Account.Access.PRIVILEGED);
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
		String channelTopic = Misc.makeSentence(args, 1);

		Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			client.sendLine("SERVERMSG Error: Channel does not exist: " + channelName);
			return false;
		}

		if (!chan.setTopic(channelTopic, client.getAccount().getName())) {
			client.sendLine("SERVERMSG You've just disabled the topic for channel #" + chan.getName());
			chan.broadcast(new StringBuilder("<")
					.append(client.getAccount().getName()).append("> has just disabled topic for #")
					.append(chan.getName()).toString());
		} else {
			client.sendLine("SERVERMSG You've just changed the topic for channel #" + chan.getName());
			chan.broadcast(new StringBuilder("<")
					.append(client.getAccount().getName()).append("> has just changed topic for #")
					.append(chan.getName()).toString());
			chan.sendLineToClients(new StringBuilder("CHANNELTOPIC ")
					.append(chan.getName()).append(" ")
					.append(chan.getTopicAuthor()).append(" ")
					.append(chan.getTopicChangedTime()).append(" ")
					.append(chan.getTopic()).toString());
		}

		return true;
	}
}
