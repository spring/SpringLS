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
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Allows an administrator fetch the date of registration of an account.
 * @author hoijui
 */
@SupportedCommand("GETREGISTRATIONDATE")
public class GetRegistrationDateCommandProcessor
		extends AbstractCommandProcessor
{
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
			client.sendLine(String.format("SERVERMSG User <%s> not found!",
					userName));
			return false;
		}

		// As DateFormats are generally not-thread save,
		// we always create a new one.
		DateFormat dateTimeFormat = new SimpleDateFormat(
				"d MMM yyyy HH:mm:ss z");
		client.sendLine(String.format(
				"SERVERMSG Registration timestamp for <%s> is %d (%s)",
				userName, acc.getRegistrationDate(),
				dateTimeFormat.format(new Date(acc.getRegistrationDate()))));

		return true;
	}
}
