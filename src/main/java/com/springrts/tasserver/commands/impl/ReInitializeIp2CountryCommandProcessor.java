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
import com.springrts.tasserver.Client;
import com.springrts.tasserver.IP2Country;
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.io.File;
import java.util.List;

/**
 * Lets an administrator re-initialize the internal IP2Country database from a
 * specified data-source file.
 * @author hoijui
 */
@SupportedCommand("REINITIP2COUNTRY")
public class ReInitializeIp2CountryCommandProcessor extends AbstractCommandProcessor {

	public ReInitializeIp2CountryCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String dataFileName = Misc.makeSentence(args, 0);

		IP2Country.getInstance().setDataFile(new File(dataFileName));
		if (IP2Country.getInstance().initializeAll()) {
			client.sendLine("SERVERMSG IP2COUNTRY database initialized successfully!");
		} else {
			client.sendLine("SERVERMSG Error while initializing IP2COUNTRY database!");
		}

		return true;
	}
}
