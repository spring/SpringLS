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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by a client to the server telling him his status in the battle changed.
 * @author hoijui
 */
@SupportedCommand("MYBATTLESTATUS")
public class MyBattleStatusCommandProcessor extends AbstractCommandProcessor {

	public MyBattleStatusCommandProcessor() {
		super(2, 2, Account.Access.NORMAL);
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
			return false;
		}

		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		if (bat == null) {
			return false;
		}

		String newStatusStr = args.get(0);
		String teamColorStr = args.get(1);

		int newStatus;
		try {
			newStatus = Integer.parseInt(newStatusStr);
		} catch (NumberFormatException ex) {
			return false;
		}
		// update new battle status. Note: we ignore handicap value as it can be changed only by founder with HANDICAP command!
		client.setBattleStatus(Misc.setHandicapOfBattleStatus(newStatus, Misc.getHandicapFromBattleStatus(client.getBattleStatus())));

		int newTeamColor;
		try {
			newTeamColor = Integer.parseInt(teamColorStr);
		} catch (NumberFormatException e) {
			return false;
		}
		client.setTeamColor(newTeamColor);

		// if game is full or game type is "battle replay", force player's mode to spectator:
		if ((bat.getClientsSize() + 1 - bat.spectatorCount() > bat.getMaxPlayers()) || (bat.getType() == 1)) {
			client.setBattleStatus(Misc.setModeOfBattleStatus(client.getBattleStatus(), 0));
		}
		// if player has chosen team number which is already used by some other player/bot,
		// force his ally number and team color to be the same as of that player/bot:
		if (bat.getFounder() != client) {
			if ((Misc.getTeamNoFromBattleStatus(bat.getFounder().getBattleStatus()) == Misc.getTeamNoFromBattleStatus(client.getBattleStatus())) && (Misc.getModeFromBattleStatus(bat.getFounder().getBattleStatus()) != 0)) {
				client.setBattleStatus(Misc.setAllyNoOfBattleStatus(client.getBattleStatus(), Misc.getAllyNoFromBattleStatus(bat.getFounder().getBattleStatus())));
				client.setTeamColor(bat.getFounder().getTeamColor());
			}
		}
		for (int i = 0; i < bat.getClientsSize(); i++) {
			if (bat.getClient(i) != client) {
				if ((Misc.getTeamNoFromBattleStatus(bat.getClient(i).getBattleStatus()) == Misc.getTeamNoFromBattleStatus(client.getBattleStatus())) && (Misc.getModeFromBattleStatus(bat.getClient(i).getBattleStatus()) != 0)) {
					client.setBattleStatus(Misc.setAllyNoOfBattleStatus(client.getBattleStatus(), Misc.getAllyNoFromBattleStatus(bat.getClient(i).getBattleStatus())));
					client.setTeamColor(bat.getClient(i).getTeamColor());
					break;
				}
			}
		}
		for (int i = 0; i < bat.getBotsSize(); i++) {
			if (Misc.getTeamNoFromBattleStatus(bat.getBot(i).getBattleStatus()) == Misc.getTeamNoFromBattleStatus(client.getBattleStatus())) {
				client.setBattleStatus(Misc.setAllyNoOfBattleStatus(client.getBattleStatus(), Misc.getAllyNoFromBattleStatus(bat.getBot(i).getBattleStatus())));
				client.setTeamColor(bat.getBot(i).getTeamColor());
				break;
			}
		}

		bat.notifyClientsOfBattleStatus(client);

		return true;
	}
}
