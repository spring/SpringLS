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
			client.sendLine(String.format(
					"JOINFAILED Bad channel name (\"#%s\"). Reason: %s",
					channelName, valid));
			return false;
		}

		// check if key is correct (if channel is locked):
		Channel chan = getContext().getChannels().getChannel(channelName);
		if ((chan != null) && (chan.isLocked())
				// we allow admins to join locked channels
				&& client.getAccount().getAccess().isLessThen(Account.Access.ADMIN)
				&& !channelKey.equals(chan.getKey()))
		{
			client.sendLine(String.format(
					"JOINFAILED %s Wrong key (this channel is locked)!",
					channelName));
			return false;
		}

		chan = client.joinChannel(channelName);
		if (chan == null) {
			client.sendLine(String.format(
					"JOINFAILED %s Already in the channel!", channelName));
			return false;
		}
		client.sendLine(String.format("JOIN %s", channelName));
		getContext().getChannels().sendChannelInfoToClient(chan, client);
		getContext().getChannels().notifyClientsOfNewClientInChannel(chan, client);

		return true;
	}
}
