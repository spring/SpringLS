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
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("MUTE")
public class MuteCommandProcessor extends AbstractCommandProcessor {

	public MuteCommandProcessor() {
		super(3, ARGS_MAX_NOCHECK, Account.Access.PRIVILEGED);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String chanelName = args.get(0);
		String username = args.get(1);

		Channel chan = getContext().getChannels().getChannel(chanelName);
		if (chan == null) {
			client.sendLine(new StringBuilder("SERVERMSG MUTE failed: Channel #").append(chanelName).append(" does not exist!").toString());
			return false;
		}

		if (chan.getMuteList().isMuted(username)) {
			client.sendLine(new StringBuilder("SERVERMSG MUTE failed: User <").append(username).append("> is already muted. Unmute first!").toString());
			return false;
		}

		Account targetAccount = getContext().getAccountsService().getAccount(username);
		if (targetAccount == null) {
			client.sendLine(new StringBuilder("SERVERMSG MUTE failed: User <").append(username).append("> does not exist").toString());
			return false;
		}

		boolean muteByIP = false;
		if (args.size() > 3) {
			String option = args.get(3);
			if (option.toUpperCase().equals("IP")) {
				muteByIP = true;
			} else {
				client.sendLine(new StringBuilder("SERVERMSG MUTE failed: Invalid argument: ").append(option).append("\"").toString());
				return false;
			}
		}

		long minutes;
		try {
			minutes = Long.parseLong(args.get(2));
		} catch (NumberFormatException e) {
			client.sendLine("SERVERMSG MUTE failed: Invalid argument - should be an integer");
			return false;
		}

		chan.getMuteList().mute(username, minutes * 60, (muteByIP ? targetAccount.getLastIP() : null));

		client.sendLine(new StringBuilder("SERVERMSG You have muted <")
				.append(username).append("> on channel #")
				.append(chan.getName()).append(".").toString());
		chan.broadcast(new StringBuilder("<")
				.append(client.getAccount().getName()).append("> has muted <")
				.append(username).append(">").toString());

		return true;
	}
}
