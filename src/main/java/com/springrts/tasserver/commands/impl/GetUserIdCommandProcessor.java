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
 * Allows an administrator to fetch the unique name ID associated with an
 * account.
 * This is useful for smurf detection.
 * @author hoijui
 */
@SupportedCommand("GETUSERID")
public class GetUserIdCommandProcessor extends AbstractCommandProcessor {

	public GetUserIdCommandProcessor() {
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

		Account acc = getContext().getAccountsService().getAccount(username);
		if (acc == null) {
			client.sendLine(new StringBuilder("SERVERMSG User <")
					.append(username).append("> not found!").toString());
			return false;
		}

		client.sendLine(new StringBuilder("SERVERMSG Last user ID for <")
				.append(username).append("> was ")
				.append(acc.getLastUserId()).toString());

		return true;
	}
}
