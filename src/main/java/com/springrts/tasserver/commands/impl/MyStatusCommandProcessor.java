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
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sent by client to server telling him his status changed.
 * To figure out if battle is "in-game", client must check in-game status of the
 * host.
 * @author hoijui
 */
@SupportedCommand("MYSTATUS")
public class MyStatusCommandProcessor extends AbstractCommandProcessor {

	private static final Log s_log  = LogFactory.getLog(MyStatusCommandProcessor.class);

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

		// we must preserve rank bits, access bit and bot mode bit (client is not allowed to change them himself):
		int tmp = client.getRankFromStatus();
		boolean tmp2 = client.getInGameFromStatus();
		boolean tmp3 = client.getAccessFromStatus();
		boolean tmp4 = client.getBotModeFromStatus();

		client.setStatus(newStatus);

		client.setRankToStatus(tmp);
		client.setAccessToStatus(tmp3);
		client.setBotModeToStatus(tmp4);

		if (client.getInGameFromStatus() != tmp2) {
			// user changed his in-game status.
			if (tmp2 == false) { // client just entered game
				Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
				if ((bat != null) && (bat.getClientsSize() > 0)) {
					client.setInGameTime(System.currentTimeMillis());
				} else {
					client.setInGameTime(0); // we won't update clients who play by themselves (or with bots), since some try to exploit the system by leaving computer alone in-battle for hours to increase their ranks
				}						// check if client is a battle host using "hole punching" technique:
				if ((bat != null) && (bat.getFounder() == client) && (bat.getNatType() == 1)) {
					// tell clients to replace battle port with founder's public UDP source port:
					bat.sendToAllExceptFounder(new StringBuilder("HOSTPORT ").append(client.getUdpSourcePort()).toString());
				}
			} else { // back from game
				if (client.getInGameTime() != 0) {
					// We will not update clients that play
					// by themselves (or with bots only),
					// since some try to exploit the system
					// by leaving their computer alone in-battle
					// for hours, to increase their ranks.
					long diffMins = (System.currentTimeMillis() - client.getInGameTime()) / 60000;
					boolean rankChanged = client.getAccount().addMinsToInGameTime(diffMins);
					if (rankChanged) {
						client.setRankToStatus(client.getAccount().getRank().ordinal());
					}
					final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges( client.getAccount(), client.getAccount().getName());
					if (!mergeOk) {
						// as this is no serious problem, only log a message
						s_log.warn(new StringBuilder("Failed updating users in-game-time in persistent storage: ")
								.append(client.getAccount().getName()).toString());
						return false;
					}
				}
			}
		}
		getContext().getClients().notifyClientsOfNewClientStatus(client);

		return true;
	}
}
