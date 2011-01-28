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
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;


/**
 * Sent by any client when he is trying to say something in "/me" IRC style.
 * @author hoijui
 */
@SupportedCommand("SAYEX")
public class SayExCommandProcessor extends AbstractSayCommandProcessor {

	public SayExCommandProcessor() {
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

		Channel channel = client.getChannel(channelName);
		if (channel == null) {
			return false;
		}

		checkMuted(client, channel);

		checkFlooding(client, message);

		channel.sendLineToClients(String.format("SAIDEX %s %s %s",
				channel.getName(),
				client.getAccount().getName(),
				message));

		return true;
	}
}
