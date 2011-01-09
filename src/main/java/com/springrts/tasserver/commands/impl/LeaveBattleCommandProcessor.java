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
 * Sent by the client when he leaves a battle.
 * Also sent by a founder of the battle when he closes the battle.
 * @author hoijui
 */
@SupportedCommand("LEAVEBATTLE")
public class LeaveBattleCommandProcessor extends AbstractCommandProcessor {

	public LeaveBattleCommandProcessor() {
		super(0, 0, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (client.getBattleID() == Battle.NO_BATTLE_ID) {
			// this may happen when client sent the LEAVEBATTLE command
			// right after he was kicked from the battle, for example.
			return false;
		}
		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		getContext().getBattles().verify(bat);

		// automatically checks if the client is a founder and closes the battle
		getContext().getBattles().leaveBattle(client, bat);

		return true;
	}
}
