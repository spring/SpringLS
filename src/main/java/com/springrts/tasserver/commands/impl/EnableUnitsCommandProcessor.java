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
 * Sent by founder of the battle to server telling him he enabled one or more
 * previous disabled units.
 * At least one unit name must be passed as an argument.
 * @author hoijui
 */
@SupportedCommand("ENABLEUNITS")
public class EnableUnitsCommandProcessor extends AbstractCommandProcessor {

	public EnableUnitsCommandProcessor() {
		// only the founder can disable/enable units
		super(1, ARGS_MAX_NOCHECK, Account.Access.NORMAL, true, true);
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

		for (String unit : args) {
			// will ignore it if string is not found in the list
			battle.getDisabledUnits().remove(unit);
		}

		battle.sendToAllExceptFounder(reconstructFullCommand(args));

		return true;
	}
}
