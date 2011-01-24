/*
	Copyright (c) 2011 Robin Vobruba <robin.vobruba@derisk.ch>

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
 * Sent by client who is hosting a battle replay game indicating he is now
 * sending us the game script used in the original replay.
 * The server will then forward this script to all other participants in his
 * battle. Correct sequence of commands when sending the script file is this:
 * <ul>
 *   <li>1) SCRIPTSTART command</li>
 *   <li>2) multiple SCRIPT commands</li>
 *   <li>3) SCRIPTEND command</li>
 * </ul>
 * @author hoijui
 */
@SupportedCommand("SCRIPTSTART")
public class ScriptStartCommandProcessor extends AbstractCommandProcessor {

	public ScriptStartCommandProcessor() {
		super(0, 0, Account.Access.NORMAL, true);
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

		battle.getTempReplayScript().clear();

		return true;
	}
}
