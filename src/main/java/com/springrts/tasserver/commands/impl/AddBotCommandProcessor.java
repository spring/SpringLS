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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.awt.Color;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("ADDBOT")
public class AddBotCommandProcessor extends AbstractCommandProcessor {

	public AddBotCommandProcessor() {
		super(4, ARGS_MAX_NOCHECK, Account.Access.NORMAL);
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
		String specifier = Misc.makeSentence(args, 3);

		int battleStatus;
		try {
			battleStatus = Integer.parseInt(battleStatusStr);
		} catch (NumberFormatException e) {
			return false;
		}

		Color teamColor = Misc.colorSpringStringToJava(teamColorStr);
		if (teamColor == null) {
			return false;
		}

		if (!Bot.isValidName(botName)) {
			client.sendLine("SERVERMSGBOX Bad bot name. Try another!");
			return false;
		}

		if (bat.getBot(botName) != null) {
			client.sendLine("SERVERMSGBOX Bot name already assigned. Choose another!");
			return false;
		}

		Bot bot = new Bot(botName, client.getAccount().getName(), specifier, battleStatus, teamColor);
		bat.addBot(bot);

		bat.sendToAllClients(new StringBuilder("ADDBOT ")
				.append(bat.getId()).append(" ")
				.append(bot.getName()).append(" ")
				.append(bot.getOwnerName()).append(" ")
				.append(bot.getBattleStatus()).append(" ")
				.append(bot.getTeamColor()).append(" ")
				.append(bot.getSpecifier()).toString());

		return true;
	}
}
