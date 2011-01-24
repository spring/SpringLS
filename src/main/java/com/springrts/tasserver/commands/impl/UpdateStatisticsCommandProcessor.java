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
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Lets an administrator save the statistics from memory to persistent storage.
 * @author hoijui
 */
@SupportedCommand("UPDATESTATISTICS")
public class UpdateStatisticsCommandProcessor extends AbstractCommandProcessor {

	public UpdateStatisticsCommandProcessor() {
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

		int taken = getContext().getStatistics().saveStatisticsToDisk();
		if (taken == -1) {
			client.sendLine("SERVERMSG Unable to update statistics!");
		} else {
			client.sendLine(new StringBuilder("SERVERMSG Statistics have been updated. Time taken to calculate: ")
					.append(taken).append(" ms.").toString());
		}

		return true;
	}
}
