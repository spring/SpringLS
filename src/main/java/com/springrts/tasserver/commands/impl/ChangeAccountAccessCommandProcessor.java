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
import com.springrts.tasserver.ServerNotification;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("CHANGEACCOUNTACCESS")
public class ChangeAccountAccessCommandProcessor extends AbstractCommandProcessor {

	public ChangeAccountAccessCommandProcessor() {
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

		String username = args.get(0);
		String accessBitsString = args.get(1);

		int newAccessBifField = -1;
		try {
			newAccessBifField = Integer.parseInt(accessBitsString);
		} catch (NumberFormatException e) {
			return false;
		}

		Account acc = getContext().getAccountsService().getAccount(username);
		if (acc == null) {
			return false;
		}

		int oldAccessBitField = acc.getAccessBitField();
		Account accountNew = acc.clone();
		accountNew.setAccess(Account.extractAccess(newAccessBifField));
		accountNew.setBot(Account.extractBot(newAccessBifField));
		accountNew.setInGameTime(Account.extractInGameTime(newAccessBifField));
		accountNew.setAgreementAccepted(Account.extractAgreementAccepted(newAccessBifField));
		final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges(accountNew, accountNew.getName());
		if (mergeOk) {
			acc = accountNew;
		} else {
			client.sendLine(String.format(
					"SERVERMSG Changing ACCESS for account <%s> failed.",
					acc.getName()));
			return false;
		}

		getContext().getAccountsService().saveAccounts(false); // save changes
		// FIXME Do this just in case if rank got changed?
		//Client target=context.getClients().getClient(commands[1]);
		//target.setRank(client.account.getRank().ordinal());
		//if(target.alive)
		//	context.getClients().notifyClientsOfNewClientStatus(target);

		client.sendLine(String.format(
				"SERVERMSG You have changed ACCESS for <%s> successfully.",
				acc.getName()));

		// add server notification:
		ServerNotification sn = new ServerNotification(
				"Account access changed by admin");
		sn.addLine(String.format(
				"Admin <%s> has changed access/status bits for account <%s>.",
				client.getAccount().getName(), acc.getName()));
		sn.addLine(String.format("Old access code: %d. New code: %d",
				oldAccessBitField, newAccessBifField));
		getContext().getServerNotifications().addNotification(sn);

		return true;
	}
}
