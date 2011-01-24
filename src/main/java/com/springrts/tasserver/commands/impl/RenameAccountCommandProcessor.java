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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.ServerNotification;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
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
			client.sendLine("SERVERMSG Bad RENAMEACCOUNT command - too many or too few parameters");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String newUsername = Misc.makeSentence(args, 0);

		if (getContext().getServer().isLanMode()) {
			client.sendLine("SERVERMSG RENAMEACCOUNT failed: You cannot rename your account while server is running in LAN mode since you have no account!");
			return false;
		}

		// validate new userName:
		String valid = Account.isOldUsernameValid(newUsername);
		if (valid != null) {
			client.sendLine(new StringBuilder("SERVERMSG RENAMEACCOUNT failed: Invalid username (reason: ").append(valid).append(")").toString());
			return false;
		}

		Account acc = getContext().getAccountsService().findAccountNoCase(newUsername);
		if (acc != null && acc != client.getAccount()) {
			client.sendLine("SERVERMSG RENAMEACCOUNT failed: Account with same username already exists!");
			return false;
		}

		// make sure all mutes are accordingly adjusted to new userName:
		for (int i = 0; i < getContext().getChannels().getChannelsSize(); i++) {
			getContext().getChannels().getChannel(i).getMuteList().rename(client.getAccount().getName(), newUsername);
		}

		final String oldName = client.getAccount().getName();
		Account accountNew = client.getAccount().clone();
		accountNew.setName(newUsername);
		accountNew.setLastLogin(System.currentTimeMillis());
		accountNew.setLastIp(client.getIp());
		final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges(accountNew, client.getAccount().getName());
		if (mergeOk) {
			client.setAccount(accountNew);
		} else {
			client.sendLine("SERVERMSG Your account renaming failed.");
			return false;
		}

		client.sendLine(new StringBuilder("SERVERMSG Your account has been renamed to <")
				.append(accountNew.getName())
				.append(">. Reconnect with new account (you will now be automatically disconnected)!").toString());
		getContext().getClients().killClient(client, "Quit: renaming account");
		getContext().getAccountsService().saveAccounts(false); // let's save new accounts info to disk
		getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: User <")
				.append(oldName).append("> has just renamed his account to <")
				.append(client.getAccount().getName()).append(">").toString());

		// add server notification:
		ServerNotification sn = new ServerNotification("Account renamed");
		sn.addLine(new StringBuilder("User <")
				.append(oldName).append("> has renamed his account to <")
				.append(client.getAccount().getName()).append(">").toString());
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
