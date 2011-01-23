/*
	Copyright (c) 2010 Robin Vobruba <robin.vobruba@derisk.ch>

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
import com.springrts.tasserver.Battle;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Clients;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("OPENBATTLE")
public class OpenBattleCommandProcessor extends AbstractCommandProcessor {

	public OpenBattleCommandProcessor() {
		super(Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (client.getBattleID() != Battle.NO_BATTLE_ID) {
			client.sendLine("OPENBATTLEFAILED You are already hosting a battle!"
					);
			return false;
		}
		Battle battle = getContext().getBattles().createBattleFromString(args,
				client);
		if (battle == null) {
			client.sendLine("OPENBATTLEFAILED Invalid command format or bad"
					+ " arguments");
			return false;
		}
		getContext().getBattles().addBattle(battle);
		client.setDefaultBattleStatus();
		client.setBattleID(battle.getId());
		client.setRequestedBattleID(Battle.NO_BATTLE_ID);

		boolean local;
		Clients clients = getContext().getClients();
		for (int i = 0; i < clients.getClientsSize(); i++) {
			Client c = clients.getClient(i);
			if (c.getAccount().getAccess().isLessThen(Account.Access.NORMAL)) {
				continue;
			}
			// make sure that the clients behind NAT get local IPs and not
			// external ones:
			local = client.getIp().equals(c.getIp());
			c.sendLine(battle.createBattleOpenedCommandEx(local));
		}

		// notify client that he successfully opened a new battle
		client.sendLine("OPENBATTLE " + battle.getId());
		client.sendLine("REQUESTBATTLESTATUS");
		return true;
	}
}
