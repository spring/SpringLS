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
 * Sent by client (battle host), to remove script tags in script.txt.
 * @author hoijui
 */
@SupportedCommand("REMOVESCRIPTTAGS")
public class RemoveScriptTagsCommandProcessor extends AbstractCommandProcessor {

	public RemoveScriptTagsCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.NORMAL, true, true);
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

		Battle battle = getBattle(client);

		StringBuilder loweyKeyCommand = new StringBuilder("REMOVESCRIPTTAGS");
		for (String key : args) {
			String lowerKey = key.toLowerCase();
			loweyKeyCommand.append(" ").append(lowerKey);
			battle.getScriptTags().remove(lowerKey);
		}

		// relay the command
		battle.sendToAllClients(loweyKeyCommand.toString());

		return true;
	}
}
