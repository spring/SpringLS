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
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Allows a user to change his password.
 * @author hoijui
 */
@SupportedCommand("CHANGEPASSWORD")
public class ChangePasswordCommandProcessor extends AbstractCommandProcessor {

	public ChangePasswordCommandProcessor() {
		super(2, 2, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine(String.format(
					"SERVERMSG Bad %s command:"
					+ " Too many or too few parameters have been supplied",
					getCommandName()));
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String oldPassword = args.get(0);
		String newPassword = args.get(1);

		if (getContext().getServer().isLanMode()) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: You can not change your password"
					+ " while the server is running in LAN mode!",
					getCommandName()));
			return false;
		}

		if (!oldPassword.equals(client.getAccount().getPassword())) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: The old password is incorrect!",
					getCommandName()));
			return false;
		}

		// validate password:
		String valid = Account.isPasswordValid(newPassword);
		if (valid != null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Invalid password (reason: %s)",
					getCommandName(), valid));
			return false;
		}

		final String oldPasswd = client.getAccount().getPassword();
		client.getAccount().setPassword(newPassword);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(client.getAccount(),
				client.getAccount().getName());
		if (!mergeOk) {
			client.getAccount().setPassword(oldPasswd);
			client.sendLine(String.format(
					"SERVERMSG %s failed: Failed saving to persistent storage.",
					getCommandName()));
			return false;
		}

		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);
		client.sendLine("SERVERMSG Your password has been successfully updated!"
				);

		return true;
	}
}
