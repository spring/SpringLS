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
import java.util.List;

/**
 * Allows an administrator to set/unset the bot mode on an account.
 * @author hoijui
 */
@SupportedCommand("SETBOTMODE")
public class SetBotModeCommandProcessor extends AbstractCommandProcessor {

	public SetBotModeCommandProcessor() {
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

		int mode;
		try {
			mode = Integer.parseInt(args.get(1));
		} catch (NumberFormatException e) {
			client.sendLine("SERVERMSG Invalid 'mode' parameter (has to be 0 or 1)!");
			return false;
		}
		if ((mode != 0) && (mode != 1)) {
			client.sendLine("SERVERMSG Invalid 'mode' parameter (has to be 0 or 1)!");
			return false;
		}

		String userName = args.get(0);

		Account acc = getContext().getAccountsService().getAccount(userName);
		if (acc == null) {
			client.sendLine(String.format("SERVERMSG User <%s> not found!",
					userName));
			return false;
		}

		final boolean wasBot = acc.isBot();
		acc.setBot((mode == 0) ? false : true);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(acc, acc.getName());
		if (!mergeOk) {
			acc.setBot(wasBot);
			client.sendLine(String.format(
					"SERVERMSG %s failed: Failed saving to persistent storage.",
					getCommandName()));
			return false;
		}

		client.sendLine(String.format(
				"SERVERMSG Bot mode set to %d for user <%s>",
				mode, acc.getName()));

		return true;
	}
}
