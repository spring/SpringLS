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

package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.IP2Country;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Lets an administrator re-initialize the internal IP2Country database from a
 * specified data-source file.
 * @author hoijui
 */
@SupportedCommand("UPDATEIP2COUNTRY")
public class UpdateIp2CountryCommandProcessor extends AbstractCommandProcessor {

	public UpdateIp2CountryCommandProcessor() {
		super(0, 0, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (IP2Country.getInstance().updateInProgress()) {
			client.sendLine("SERVERMSG IP2Country database update is already in progress, try again later.");
			return false;
		}

		client.sendLine("SERVERMSG Updating IP2country database ... Server will notify of success via server notification system.");
		IP2Country.getInstance().updateDatabase();

		return true;
	}
}
