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
import com.springrts.tasserver.Channel;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("MUTELIST")
public class MuteListCommandProcessor extends AbstractCommandProcessor {

	public MuteListCommandProcessor() {
		super(1, 1, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("SERVERMSG MUTELIST failed: Invalid arguments!");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String chanelName = args.get(0);

		Channel chan = getContext().getChannels().getChannel(chanelName);
		if (chan == null) {
			client.sendLine(new StringBuilder("SERVERMSG MUTELIST failed: Channel #")
					.append(chanelName).append(" does not exist!").toString());
			return false;
		}

		client.sendLine(new StringBuilder("MUTELISTBEGIN ").append(chan.getName()).toString());

		int size = chan.getMuteList().size(); // we mustn't call muteList.size() in for loop since it will purge expired records each time and so we could have ArrayOutOfBounds exception
		for (int i = 0; i < size; i++) {
			if (chan.getMuteList().getRemainingSeconds(i) == 0) {
				client.sendLine(new StringBuilder("MUTELIST ")
						.append(chan.getMuteList().getUsername(i))
						.append(", indefinite time remaining").toString());
			} else {
				client.sendLine(new StringBuilder("MUTELIST ")
						.append(chan.getMuteList().getUsername(i)).append(", ")
						.append(chan.getMuteList().getRemainingSeconds(i))
						.append(" seconds remaining").toString());
			}
		}

		client.sendLine("MUTELISTEND");

		return true;
	}
}
