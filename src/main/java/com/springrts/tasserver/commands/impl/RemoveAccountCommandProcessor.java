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
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("REMOVEACCOUNT")
public class RemoveAccountCommandProcessor extends AbstractCommandProcessor {

	public RemoveAccountCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
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

		if (!getContext().getAccountsService().removeAccount(username)) {
			return false;
		}

		// if any user is connected to this account, kick him:
		for (int j = 0; j < getContext().getClients().getClientsSize(); j++) {
			if (getContext().getClients().getClient(j).getAccount().getName().equals(username)) {
				getContext().getClients().killClient(getContext().getClients().getClient(j));
				j--;
			}
		}

		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);
		client.sendLine(new StringBuilder("SERVERMSG You have successfully removed <")
				.append(username).append("> account!").toString());

		return true;
	}
}
