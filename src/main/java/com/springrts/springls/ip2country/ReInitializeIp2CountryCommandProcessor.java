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

package com.springrts.springls.ip2country;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
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

		IP2Country service = getContext().getService(IP2Country.class);
		if (service == null) {
			client.sendLine("SERVERMSG IP2Country service not available!");
		} else {
			service.setDataFile(new File(dataFileName));
			if (service.initializeAll()) {
				client.sendLine("SERVERMSG IP2Country database initialized successfully!");
			} else {
				client.sendLine("SERVERMSG Error while initializing IP2Country database!");
			}
		}

		return true;
	}
}
