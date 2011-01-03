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
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("KICKUSER")
public class KickUserCommandProcessor extends AbstractCommandProcessor {

	public KickUserCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.PRIVILEGED);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String username = args.get(0);

		Client target = getContext().getClients().getClient(username);
		String reason = "";
		if (args.size() > 1) {
			reason = new StringBuilder(" (reason: ").append(Misc.makeSentence(args, 1)).append(")").toString();
		}
		if (target == null) {
			return false;
		}
		final String broadcastMsg = new StringBuilder("<")
				.append(client.getAccount().getName()).append("> has kicked <")
				.append(target.getAccount().getName()).append("> from server")
				.append(reason).toString();
		for (int i = 0; i < getContext().getChannels().getChannelsSize(); i++) {
			if (getContext().getChannels().getChannel(i).isClientInThisChannel(target)) {
				getContext().getChannels().getChannel(i).broadcast(broadcastMsg);
			}
		}
		target.sendLine(new StringBuilder("SERVERMSG You've been kicked from server by <")
				.append(client.getAccount().getName()).append(">")
				.append(reason).toString());
		getContext().getClients().killClient(target, "Quit: kicked from server");
		return true;
	}
}
