/*
	Copyright (c) 2011 Robin Vobruba <robin.vobruba@derisk.ch>

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
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InsufficientAccessCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("GETINGAMETIME")
public class GetInGameTimeCommandProcessor extends AbstractCommandProcessor {

	public GetInGameTimeCommandProcessor() {
		super(0, 1, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (args.isEmpty()) {
			client.sendLine(new StringBuilder("SERVERMSG Your in-game time is ")
					.append(client.getAccount().getInGameTimeInMins()).append(" minutes.").toString());
		} else {
			if (client.getAccount().getAccess().isLessThen(Account.Access.PRIVILEGED)) {
				client.sendLine("SERVERMSG You have no access to see other player's in-game time!");
				throw new InsufficientAccessCommandProcessingException(getCommandName(), Account.Access.PRIVILEGED, client.getAccount().getAccess());
			}

			String username = args.get(0);

			Account acc = getContext().getAccountsService().getAccount(username);
			if (acc == null) {
				client.sendLine(new StringBuilder("SERVERMSG GETINGAMETIME failed: user ")
						.append(username).append(" not found!").toString());
				return false;
			}

			client.sendLine(new StringBuilder("SERVERMSG ")
					.append(acc.getName()).append("'s in-game time is ")
					.append(acc.getInGameTimeInMins()).append(" minutes.").toString());
		}

		return true;
	}
}
