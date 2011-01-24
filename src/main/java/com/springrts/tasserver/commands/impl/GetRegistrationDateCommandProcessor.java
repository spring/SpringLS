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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Allows an administrator fetch the date of registration of an account.
 * @author hoijui
 */
@SupportedCommand("GETREGISTRATIONDATE")
public class GetRegistrationDateCommandProcessor extends AbstractCommandProcessor {

	public GetRegistrationDateCommandProcessor() {
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

		String userName = args.get(0);

		Account acc = getContext().getAccountsService().getAccount(userName);
		if (acc == null) {
			client.sendLine(new StringBuilder("SERVERMSG User <").append(userName).append("> not found!").toString());
			return false;
		}

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		DateFormat dateTimeFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");
		client.sendLine(new StringBuilder("SERVERMSG Registration timestamp for <")
				.append(userName).append("> is ")
				.append(acc.getRegistrationDate()).append(" (")
				.append(dateTimeFormat.format(new Date(acc.getRegistrationDate())))
				.append(")").toString());

		return true;
	}
}
