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

/**
 * Sent by client to server when trying to play a "ring" sound to user
 * 'username'.
 * Only privileged users can ring anyone, although "normal" clients can ring
 * only when they are hosting and only players participating in their battle.
 * @author hoijui
 */
@SupportedCommand("RING")
public class RingCommandProcessor extends AbstractCommandProcessor {

	public RingCommandProcessor() {
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

		String username = args.get(0);

		if (client.getAccount().getAccess().compareTo(Account.Access.PRIVILEGED) < 0) {
			// normal users can ring only when they are hosting
			// and only clients who are participating in their battle
			Client target = getContext().getClients().getClient(username);
			if (target == null) {
				return false;
			}

			if (client.getBattleID() == Battle.NO_BATTLE_ID) {
				client.sendLine("SERVERMSG RING command failed: You can only ring players participating in your own battle!");
				return false;
			}

			Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
			getContext().getBattles().verify(bat);

			if (!bat.isClientInBattle(username)) {
				client.sendLine("SERVERMSG RING command failed: You don't have permission to ring players other than those participating in your battle!");
				return false;
			}

			// only host can ring players participating in his own battle, unless target is host himself:
			if ((client != bat.getFounder()) && (target != bat.getFounder())) {
				client.sendLine("SERVERMSG RING command failed: You can ring only battle host, or if you are the battle host, only players participating in your own battle!");
				return false;
			}

			target.sendLine(new StringBuilder("RING ").append(client.getAccount().getName()).toString());
		} else {
			// privileged users can ring anyone
			Client target = getContext().getClients().getClient(username);
			if (target == null) {
				return false;
			}

			target.sendLine(new StringBuilder("RING ").append(client.getAccount().getName()).toString());
		}

		return true;
	}
}