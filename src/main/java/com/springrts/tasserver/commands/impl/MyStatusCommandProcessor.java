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
import com.springrts.tasserver.Battle;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sent by client to server telling him his status changed.
 * To figure out if battle is "in-game", client must check in-game status of the
 * host.
 * @author hoijui
 */
@SupportedCommand("MYSTATUS")
public class MyStatusCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG
			= LoggerFactory.getLogger(MyStatusCommandProcessor.class);

	public MyStatusCommandProcessor() {
		super(1, 1, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String newStatusStr = args.get(0);

		int newStatus;
		try {
			newStatus = Integer.parseInt(newStatusStr);
		} catch (NumberFormatException e) {
			return false;
		}

		boolean oldInGame = client.isInGame();

		client.setStatus(newStatus, false);

		if (client.isInGame() != oldInGame) {
			// user changed his in-game status.
			if (!oldInGame) { // client just entered game
				Battle battle = getBattle(client);
				if ((battle != null) && (battle.getClientsSize() > 0)) {
					client.setInGameTime(System.currentTimeMillis());
				} else {
					// we will not update clients that play by themselves
					// (or with bots), since some try to exploit the system by
					// leaving thier computer alone in-battle for hours, to
					// increase their ranks
					client.setInGameTime(0);
				}
				if ((battle != null) && (battle.getFounder() == client)
						&& (battle.getNatType() == 1))
				{
					// the client is a battle host using the "hole punching"
					// technique

					// tell clients to replace the battle port with the
					// founder's public UDP source port
					battle.sendToAllExceptFounder(String.format("HOSTPORT %d",
							client.getUdpSourcePort()));
				}
			} else { // back from game
				if (client.getInGameTime() != 0) {
					// We will not update clients that play
					// by themselves (or with bots only),
					// since some try to exploit the system
					// by leaving their computer alone in-battle
					// for hours, to increase their ranks.
					long diffMins = (System.currentTimeMillis()
							- client.getInGameTime()) / 60000;
					boolean rankChanged = client.getAccount()
							.addMinsToInGameTime(diffMins);
					if (rankChanged) {
						client.setRank(client.getAccount().getRank());
					}
					final boolean mergeOk = getContext().getAccountsService()
							.mergeAccountChanges(client.getAccount(),
							client.getAccount().getName());
					if (!mergeOk) {
						// as this is no serious problem, only log a message
						LOG.warn("Failed updating users in-game-time in"
								+ " persistent storage: {}",
								client.getAccount().getName());
						return false;
					}
				}
			}
		}
		getContext().getClients().notifyClientsOfNewClientStatus(client);

		return true;
	}
}
