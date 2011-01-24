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
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Allows an administrator to check a pair of login credentials for validity.
 * @author hoijui
 */
@SupportedCommand("TESTLOGIN")
public class TestLoginCommandProcessor extends AbstractCommandProcessor {

	public TestLoginCommandProcessor() {
		super(2, 2, Account.Access.ADMIN);
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
		String password = args.get(1);

		if (getContext().getAccountsService().verifyLogin(userName, password) == null) {
			client.sendLine("TESTLOGINDENY");
			return false;
		}

		// We don't check here if agreement bit is set yet,
		// or if user is banned.
		// We only verify if login info is correct
		client.sendLine("TESTLOGINACCEPT");

		return true;
	}
}
