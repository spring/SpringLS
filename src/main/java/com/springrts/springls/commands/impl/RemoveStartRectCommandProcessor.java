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
import com.springrts.springls.Battle;
import com.springrts.springls.Client;
import com.springrts.springls.StartRect;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by host of the battle removing a start rectangle for 'allyno' ally team.
 * See client implementation and Spring docs for more info on this one.
 * @author hoijui
 */
@SupportedCommand("REMOVESTARTRECT")
public class RemoveStartRectCommandProcessor extends AbstractCommandProcessor {

	public RemoveStartRectCommandProcessor() {
		super(1, 1, Account.Access.NORMAL, true, true);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		Battle battle = getBattle(client);

		int allyno;
		try {
			allyno = Integer.parseInt(args.get(0));
		} catch (NumberFormatException ex) {
			client.sendLine(String.format(
					"SERVERMSG Serious error: inconsistent data (%s command)."
					+ " You will now be disconnected ...", getCommandName()));
			getContext().getClients().killClient(client,
					"Quit: inconsistent data");
			return false;
		}

		StartRect startRect = battle.getStartRects().get(allyno);
		if (!startRect.isEnabled()) {
			client.sendLine(String.format(
					"SERVERMSG Serious error: inconsistent data (%s command)."
					+ " You will now be disconnected ...", getCommandName()));
			getContext().getClients().killClient(client,
					"Quit: inconsistent data");
			return false;
		}

		startRect.setEnabled(false);

		battle.sendToAllExceptFounder(String.format("REMOVESTARTRECT %d",
				allyno));

		return true;
	}
}
