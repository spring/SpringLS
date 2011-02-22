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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Battle;
import com.springrts.springls.Bot;
import com.springrts.springls.Client;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import com.springrts.springls.util.ProtocolUtil;
import java.awt.Color;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("ADDBOT")
public class AddBotCommandProcessor extends AbstractCommandProcessor {

	public AddBotCommandProcessor() {
		super(4, ARGS_MAX_NOCHECK, Account.Access.NORMAL, true);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		Battle battle = getBattle(client);

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

		Color teamColor = ProtocolUtil.colorSpringStringToJava(teamColorStr);
		if (teamColor == null) {
			return false;
		}

		if (!Bot.isValidName(botName)) {
			client.sendLine("SERVERMSGBOX Bad bot name. Try another!");
			return false;
		}

		if (battle.getBot(botName) != null) {
			client.sendLine("SERVERMSGBOX Bot name already assigned."
					+ " Choose another!");
			return false;
		}

		Bot bot = new Bot(botName, client.getAccount().getName(), specifier,
				battleStatus, teamColor);
		battle.addBot(bot);

		return true;
	}
}
