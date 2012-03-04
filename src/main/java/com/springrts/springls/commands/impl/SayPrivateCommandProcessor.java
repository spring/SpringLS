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
import com.springrts.springls.Client;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;


/**
 * Sent by client when he is trying to send a private message to some other
 * client.
 * @author hoijui
 */
@SupportedCommand("SAYPRIVATE")
public class SayPrivateCommandProcessor extends AbstractSayCommandProcessor {

	public SayPrivateCommandProcessor() {
		super(2, ARGS_MAX_NOCHECK, Account.Access.NORMAL);
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

		checkFlooding(client, message);

		target.sendLine(String.format("SAIDPRIVATE %s %s",
				client.getAccount().getName(), message));
		// echo the command. See protocol description!
		client.sendLine(getCommandName() + " " + Misc.makeSentence(args, 0));

		return true;
	}
}
