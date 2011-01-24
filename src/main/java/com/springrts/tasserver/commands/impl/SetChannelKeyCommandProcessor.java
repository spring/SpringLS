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
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Lets a moderator set the key of a channel.
 * @see Channel#getKey()
 * @author hoijui
 */
@SupportedCommand("SETCHANNELKEY")
public class SetChannelKeyCommandProcessor extends AbstractCommandProcessor {

	public SetChannelKeyCommandProcessor() {
		super(2, 2, Account.Access.PRIVILEGED);
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
		String key = args.get(1);

		Channel chan = getContext().getChannels().getChannel(channelName);
		if (chan == null) {
			client.sendLine(new StringBuilder("SERVERMSG Error: Channel does not exist: ").append(channelName).toString());
			return false;
		}

		if (key.equals("*")) {
			if (!chan.isLocked()) {
				client.sendLine("SERVERMSG Error: Unable to unlock channel - channel is not locked!");
				return false;
			}
			chan.setKey(Channel.KEY_NONE);
			chan.broadcast(new StringBuilder("<")
					.append(client.getAccount().getName()).append("> has just unlocked #")
					.append(chan.getName()).toString());
		} else {
			if (!key.matches("^[A-Za-z0-9_]+$")) {
				client.sendLine(new StringBuilder("SERVERMSG Error: Invalid key: ").append(key).toString());
				return false;
			}
			chan.setKey(key);
			chan.broadcast(new StringBuilder("<")
					.append(client.getAccount().getName()).append("> has just locked #")
					.append(chan.getName()).append(" with private key").toString());
		}

		return true;
	}
}
