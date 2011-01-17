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
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by client (battle host), to set script tags in script.txt.
 * The [pair] format is "key=value can have spaces".
 * Keys may not contain spaces, and are expected to use the '/' character to
 * separate tables (see example). In version 0.35 of this software, the command
 * UPDATEBATTLEDETAILS was completely replaced by this command.
 * @author hoijui
 */
@SupportedCommand("SETSCRIPTTAGS")
public class SetScriptTagsCommandProcessor extends AbstractCommandProcessor {

	public SetScriptTagsCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			// kill client since it is not using this command correctly
			client.sendLine(new StringBuilder("SERVERMSG Serious error: inconsistent data (")
					.append(getCommandName())
					.append(" command). You will now be disconnected ...").toString());
			getContext().getClients().killClient(client, "Quit: inconsistent data");
			return false;
		}
		if (!checksOk) {
			return false;
		}

		if (client.getBattleID() == Battle.NO_BATTLE_ID) {
			return false;
		}

		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		getContext().getBattles().verify(bat);

		if (bat.getFounder() != client) {
			return false;
		}

		// FIXME the whole rest surely can be done nicer
		String command = reconstructFullCommand(args);
		int pairsStart = command.indexOf(' ');
		if (pairsStart < 0) {
			return false;
		}
		String[] pairs = command.substring(pairsStart + 1).split("\t");
		StringBuilder validPairs = new StringBuilder();

		// FIXME this surely can be done nicer
		for (int i = 0; i < pairs.length; i++) {

			String s = pairs[i];

			int equalPos = s.indexOf('=');
			if (equalPos < 1) {
				continue;
			}

			// parse the key
			String key = s.substring(0, equalPos).toLowerCase();
			if (key.length() <= 0) {
				continue;
			}
			if (key.indexOf(' ') >= 0) {
				continue;
			}
			if (key.indexOf('=') >= 0) {
				continue;
			}
			if (key.indexOf(';') >= 0) {
				continue;
			}
			if (key.indexOf('{') >= 0) {
				continue;
			}
			if (key.indexOf('}') >= 0) {
				continue;
			}
			if (key.indexOf('[') >= 0) {
				continue;
			}
			if (key.indexOf(']') >= 0) {
				continue;
			}
			if (key.indexOf('\n') >= 0) {
				continue;
			}
			if (key.indexOf('\r') >= 0) {
				continue;
			}

			// parse the value
			String value = s.substring(equalPos + 1);
			if (value.equals(value.trim())) {
				continue;
			} // forbid trailing/leading spaces
			if (value.indexOf(';') >= 0) {
				continue;
			}
			if (value.indexOf('}') >= 0) {
				continue;
			}
			if (value.indexOf('[') >= 0) {
				continue;
			}
			if (value.indexOf('\n') >= 0) {
				continue;
			}
			if (value.indexOf('\r') >= 0) {
				continue;
			}

			// insert the tag data into the map
			bat.getScriptTags().put(key, value);

			// add to the validPairs string
			if (validPairs.length() > 0) {
				validPairs.append("\t");
			}
			validPairs.append(key).append("=").append(value);
		}

		// relay the valid pairs
		if (validPairs.length() > 0) {
			bat.sendToAllClients(new StringBuilder("SETSCRIPTTAGS ").append(validPairs).toString());
		}

		return true;
	}
}
