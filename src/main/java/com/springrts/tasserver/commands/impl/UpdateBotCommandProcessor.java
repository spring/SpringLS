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
import com.springrts.tasserver.Bot;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by client when he is trying to update status of one of his own bots
 * (only bot owner and battle host may update bot).
 * @author hoijui
 */
@SupportedCommand("UPDATEBOT")
public class UpdateBotCommandProcessor extends AbstractCommandProcessor {

	public UpdateBotCommandProcessor() {
		super(3, 3, Account.Access.NORMAL);
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
		getContext().getBattles().verify(bat);

		String botName = args.get(0);
		String battleStatusStr = args.get(1);
		String teamColorStr = args.get(2);
		// TODO needs protocol change
		//String specifier = Misc.makeSentence(args, 3);

		Bot bot = bat.getBot(botName);
		if (bot == null) {
			return false;
		}


		int battleStatus;
		try {
			battleStatus = Integer.parseInt(battleStatusStr);
		} catch (NumberFormatException e) {
			return false;
		}

		int teamColor;
		try {
			teamColor = Integer.parseInt(teamColorStr);
		} catch (NumberFormatException e) {
			return false;
		}

		// only bot owner and battle host are allowed to update bot:
		if (!((client.getAccount().getName().equals(bot.getOwnerName())) || (client.getAccount().getName().equals(bat.getFounder().getAccount().getName())))) {
			return false;
		}

		bot.setBattleStatus(battleStatus);
		bot.setTeamColor(teamColor);

		//*** add: force ally and color number if someone else is using his team number already

		bat.sendToAllClients(new StringBuilder("UPDATEBOT ")
				.append(bat.getId()).append(" ")
				.append(bot.getName()).append(" ")
				.append(bot.getBattleStatus()).append(" ")
				.append(bot.getTeamColor()).toString());

		return true;
	}
}
