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
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
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
					client.sendLine("SERVERMSG Bad CHANGEPASSWORD command - too many or too few parameters");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String oldPassword = args.get(0);
		String newPassword = args.get(1);

		if (getContext().getServer().isLanMode()) {
			client.sendLine("SERVERMSG CHANGEPASSWORD failed: You cannot change your password while server is running in LAN mode!");
			return false;
		}

		if (!(oldPassword.equals(client.getAccount().getPassword()))) {
			client.sendLine("SERVERMSG CHANGEPASSWORD failed: Old password is incorrect!");
			return false;
		}

		// validate password:
		String valid = Account.isPasswordValid(newPassword);
		if (valid != null) {
			client.sendLine(new StringBuilder("SERVERMSG CHANGEPASSWORD failed: Invalid password (reason: ").append(valid).append(")").toString());
			return false;
		}

		final String oldPasswd = client.getAccount().getPassword();
		client.getAccount().setPassword(newPassword);
		final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
		if (!mergeOk) {
			client.getAccount().setPassword(oldPasswd);
			client.sendLine("SERVERMSG CHANGEPASSWORD failed: Failed saving to persistent storage.");
			return false;
		}

		getContext().getAccountsService().saveAccounts(false); // let's save new accounts info to disk
		client.sendLine("SERVERMSG Your password has been successfully updated!");

		return true;
	}
}
