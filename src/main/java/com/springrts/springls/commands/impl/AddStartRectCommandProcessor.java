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
 * Sent by host of the battle adding a start rectangle for 'allyno' ally team.
 * See lobby client implementation and Spring docs for more info on this one.
 * "left", "top", "right" and "bottom" refer to a virtual rectangle that is
 * 200x200 in size, where coordinates should be in interval [0, 200].
 * @author hoijui
 */
@SupportedCommand("ADDSTARTRECT")
public class AddStartRectCommandProcessor extends AbstractCommandProcessor {

	public AddStartRectCommandProcessor() {
		super(5, 5, Account.Access.NORMAL, true, true);
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
		int left;
		int top;
		int right;
		int bottom;
		try {
			allyno = Integer.parseInt(args.get(0));
			left = Integer.parseInt(args.get(1));
			top = Integer.parseInt(args.get(2));
			right = Integer.parseInt(args.get(3));
			bottom = Integer.parseInt(args.get(4));
		} catch (NumberFormatException ex) {
			client.sendLine(String.format(
					"SERVERMSG Error: inconsistent data (%s command)."
					+ " You will now be disconnected ...", getCommandName()));
			getContext().getClients().killClient(client,
					"Quit: inconsistent data");
			return false;
		}

		StartRect startRect = battle.getStartRects().get(allyno);
		if (startRect.isEnabled()) {
			client.sendLine(String.format(
					"SERVERMSG Error: inconsistent data (%s command)."
					+ " You will now be disconnected ...", getCommandName()));
			getContext().getClients().killClient(client,
					"Quit: inconsistent data");
			return false;
		}

		startRect.setEnabled(true);
		startRect.setLeft(left);
		startRect.setTop(top);
		startRect.setRight(right);
		startRect.setBottom(bottom);

		battle.sendToAllExceptFounder(String.format(
				"ADDSTARTRECT %d %d %d %d %d",
				allyno, left, top, right, bottom));

		return true;
	}
}
