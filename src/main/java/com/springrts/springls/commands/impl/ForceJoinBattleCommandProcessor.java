/*
	Copyright (c) 2012 Robin Vobruba <hoijui.quaero@gmail.com>

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
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by a client that is battle host or lobby moderator,
 * to request a user being moved to an other host.
 * @author cheesecan
 */
@SupportedCommand("FORCEJOINBATTLE")
public class ForceJoinBattleCommandProcessor extends AbstractCommandProcessor {

	public ForceJoinBattleCommandProcessor() {
		super(2, 3, Account.Access.NORMAL, true);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		int battleID = client.getBattleID();
		Battle battle = getContext().getBattles().getBattleByID(battleID);
		if (!battle.getFounder().equals(client)
				&& !client.getAccount().getAccess().isAtLeast(Account.Access.PRIVILEGED))
		{
			client.sendLine("FORCEJOINBATTLE Failed, source client must be battle host or lobby moderator.");
			return false;
		}

		String username = args.get(0);
		Client affectedClient = getContext().getClients().getClient(username);
		if (affectedClient == null) {
			client.sendLine("FORCEJOINBATTLE Failed, must specify valid user.");
			return false;
		}

		String battleId = args.get(1);
		Battle destinationBattle = getContext().getBattles().getBattleByID(battleID);
		if (destinationBattle == null) {
			client.sendLine("FORCEJOINBATTLE Failed, must specify valid battle.");
			return false;
		}

		String battlePassword = null;
		if (args.size() > 2) { // if optional battlePassword was set
			battlePassword = args.get(2);
		}

		String successResponseMessage = (battlePassword == null)
				? String.format("FORCEJOINBATTLE %s", battleId)
				: String.format("FORCEJOINBATTLE %s %s", battleId, battlePassword);

		// Issue response command to notify affected client
		affectedClient.sendLine(successResponseMessage);

		return true;
	}
}
