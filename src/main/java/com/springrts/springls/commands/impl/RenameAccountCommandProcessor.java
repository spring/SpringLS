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
import com.springrts.springls.Misc;
import com.springrts.springls.ServerNotification;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Allows a user to change his username.
 * @author hoijui
 */
@SupportedCommand("RENAMEACCOUNT")
public class RenameAccountCommandProcessor extends AbstractCommandProcessor {

	public RenameAccountCommandProcessor() {
		super(1, 1, Account.Access.NORMAL);
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
					"SERVERMSG Bad %s command - too many or too few parameters",
					getCommandName()));
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String newUsername = Misc.makeSentence(args, 0);

		if (getContext().getServer().isLanMode()) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: You cannot rename your account while"
					+ " the server is running in LAN mode, since you have no"
					+ " persistent account!", getCommandName()));
			return false;
		}

		// validate new user name
		String valid = Account.isOldUsernameValid(newUsername);
		if (valid != null) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Invalid username (reason: %s)",
					getCommandName(), valid));
			return false;
		}

		Account account = getContext().getAccountsService().findAccountNoCase(newUsername);
		if ((account != null) && (account != client.getAccount())) {
			client.sendLine(String.format(
					"SERVERMSG %s failed: Account with same username already exists!",
					getCommandName()));
			return false;
		}

		final String oldName = client.getAccount().getName();
		Account accountNew = client.getAccount().clone();
		accountNew.setName(newUsername);
		accountNew.setLastLogin(System.currentTimeMillis());
		accountNew.setLastIp(client.getIp());
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(accountNew, client.getAccount().getName());
		if (mergeOk) {
			client.setAccount(accountNew);
		} else {
			client.sendLine("SERVERMSG Your account renaming failed.");
			return false;
		}

		// make sure all mutes are accordingly adjusted to the new userName:
		for (int i = 0; i < getContext().getChannels().getChannelsSize(); i++) {
			getContext().getChannels().getChannel(i).getMuteList().rename(
					client.getAccount().getName(), newUsername);
		}

		client.sendLine(String.format(
				"SERVERMSG Your account has been renamed to <%s>."
				+ " Reconnect with the new account."
				+ " You will now be automatically disconnected!",
				accountNew.getName()));
		getContext().getClients().killClient(client, "Quit: renaming account");
		// let's save new accounts info to disc
		getContext().getAccountsService().saveAccounts(false);
		getContext().getClients().sendToAllAdministrators(String.format(
				"SERVERMSG [broadcast to all admins]:"
				+ " User <%s> has just renamed his account to <%s>",
				oldName, client.getAccount().getName()));

		// add server notification:
		ServerNotification sn = new ServerNotification("Account renamed");
		sn.addLine(String.format("User <%s> has renamed his account to <%s>",
				oldName, client.getAccount().getName()));
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
