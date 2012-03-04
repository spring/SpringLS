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
import java.util.ArrayList;
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

		String userName = args.get(0);
		Client affectedClient = getContext().getClients().getClient(userName);
		if (affectedClient == null) {
			client.sendLine("FORCEJOINBATTLE Failed, must specify valid user.");
			return false;
		}

		int battleId = client.getBattleID();
		Battle battle = getContext().getBattles().getBattleByID(battleId);
		if (!(battle.getFounder().equals(client) && battle.isClientInBattle(affectedClient))
				&& !client.getAccount().getAccess().isAtLeast(Account.Access.PRIVILEGED))
		{
			client.sendLine("FORCEJOINBATTLE Failed, source client must be lobby moderator or host of the affected client's current battle.");
			return false;
		}

		int destinationBattleId;
		String destinationBattleIdStr = args.get(1);
		try {
			destinationBattleId = Integer.parseInt(destinationBattleIdStr);
		} catch (NumberFormatException ex) {
			client.sendLine("FORCEJOINBATTLE Failed, invalid destination battle ID (needs to be an integer): " + destinationBattleIdStr);
			return false;
		}

		Battle destinationBattle = getContext().getBattles().getBattleByID(battleId);
		if (destinationBattle == null) {
			client.sendLine("FORCEJOINBATTLE Failed, invalid destination battle ID (battle does not exist): " + destinationBattleIdStr);
			return false;
		}

		String battlePassword = null;
		if (args.size() > 2) { // if optional battlePassword was set
			battlePassword = args.get(2);
		}

		boolean clientSupportsCmd = client.getCompatFlags().contains("m");
		if (clientSupportsCmd) {
			String successResponseMessage = (battlePassword == null)
					? String.format("FORCEJOINBATTLE %i", destinationBattleId)
					: String.format("FORCEJOINBATTLE %i %s", destinationBattleId, battlePassword);

			// Issue response command to notify affected client
			affectedClient.sendLine(successResponseMessage);
		} else {
			// Leave the current battle.
			getContext().getBattles().leaveBattle(client, battle);

			// Join the destination battle.
			// We fake a JOINBATTLE command, as if it was sent
			// by the affected client
			List<String> joinBattleArgs = new ArrayList<String>(1);
			joinBattleArgs.add(destinationBattleIdStr);
			getContext().getCommandProcessors().get("JOINBATTLE").process(affectedClient, joinBattleArgs);
		}

		return true;
	}
}
