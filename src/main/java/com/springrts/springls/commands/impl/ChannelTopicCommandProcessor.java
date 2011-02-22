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
import com.springrts.springls.Channel;
import com.springrts.springls.Client;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
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
			client.sendLine("SERVERMSG Error: Channel does not exist: "
					+ channelName);
			return false;
		}

		if (!chan.setTopic(channelTopic, client.getAccount().getName())) {
			client.sendLine(
					"SERVERMSG You have just disabled the topic for channel #"
					+ chan.getName());
			chan.broadcast(String.format("<%s> has just disabled topic for #%s",
					client.getAccount().getName(), chan.getName()));
		} else {
			client.sendLine(
					"SERVERMSG You have just changed the topic for channel #"
					+ chan.getName());
			chan.broadcast(String.format("<%s> has just changed topic for #%s",
					client.getAccount().getName(), chan.getName()));
			chan.sendLineToClients(String.format("CHANNELTOPIC %s %s %d %s",
					chan.getName(),
					chan.getTopicAuthor(),
					chan.getTopicChangedTime(),
					chan.getTopic()));
		}

		return true;
	}
}
