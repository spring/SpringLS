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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows a user to chat.
 * @author hoijui
 */
@SupportedCommand("SAY")
public class SayCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG  = LoggerFactory.getLogger(SayCommandProcessor.class);

	public SayCommandProcessor() {
		super(2, ARGS_MAX_NOCHECK, Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String channelName = args.get(0);
		String message = Misc.makeSentence(args, 1);

		Channel chan = client.getChannel(channelName);
		if (chan == null) {
			return false;
		}

		if (chan.getMuteList().isMuted(client.getAccount().getName())) {
			client.sendLine(new StringBuilder("SERVERMSG Message dropped. You are not allowed to talk in #")
					.append(chan.getName()).append("! Please contact one of the moderators.").toString());
			return false;
		} else if (chan.getMuteList().isIPMuted(client.getIp())) {
			client.sendLine(new StringBuilder("SERVERMSG Message dropped. You are not allowed to talk in #")
					.append(chan.getName()).append(" (muted by IP address)! If you believe this is an error, contact one of the moderators.").toString());
			return false;
		}


		// check for flooding:
		if ((message.length() > getContext().getServer().getMaxChatMessageLength())
				&& client.getAccount().getAccess().isLessThen(Account.Access.ADMIN))
		{
			LOG.warn("Flooding detected from {} ({}) [exceeded max. chat message size]",
					client.getIp(),
					client.getAccount().getName());
			client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
					.append(getContext().getServer().getMaxChatMessageLength()).append(" bytes). Your message has been ignored.").toString());
			getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
					.append(client.getIp()).append(" (")
					.append(client.getAccount().getName()).append(") - exceeded maximum chat message size. Ignoring ...").toString());
			return false;
		}
		chan.sendLineToClients(new StringBuilder("SAID ").append(chan.getName()).append(" ").append(client.getAccount().getName()).append(" ").append(message).toString());

		return true;
	}
}
