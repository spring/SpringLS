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
import com.springrts.tasserver.Channel;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hoijui
 */
public abstract class AbstractSayCommandProcessor
		extends AbstractCommandProcessor
{
	private static final Logger LOG
			= LoggerFactory.getLogger(AbstractSayCommandProcessor.class);

	protected AbstractSayCommandProcessor(int argsMin, int argsMax,
			Account.Access accessMin, boolean battleRequired)
	{
		super(argsMin, argsMax, accessMin, battleRequired);
	}
	protected AbstractSayCommandProcessor(int argsMin, int argsMax,
			Account.Access accessMin)
	{
		super(argsMin, argsMax, accessMin);
	}

	protected void checkFlooding(Client client, String message)
			throws CommandProcessingException
	{
		if ((message.length() > getContext().getServer().getMaxChatMessageLength())
				&& client.getAccount().getAccess().isLessThen(Account.Access.ADMIN))
		{
			LOG.warn("Flooding detected from {} ({}) [exceeded max. chat message size]",
					client.getIp().getHostAddress(),
					client.getAccount().getName());
			client.sendLine(String.format(
					"SERVERMSG Flooding detected - you have exceeded the"
					+ " maximum allowed chat message size (%d bytes)."
					+ " Your message has been ignored.",
					getContext().getServer().getMaxChatMessageLength()));
			getContext().getClients().sendToAllAdministrators(String.format(
					"SERVERMSG [broadcast to all admins]: Flooding has been"
					+ " detected from %s <%s> - exceeded the maximum chat"
					+ " message size. Ignoring ...",
					client.getIp().getHostAddress(),
					client.getAccount().getName()));
			throw new CommandProcessingException(getCommandName(),
					"The client tried to flood."
					+ " The maximum chat message size was exceeded.");
		}
	}

	protected void checkMuted(Client client, Channel channel)
			throws CommandProcessingException
	{
		if (channel.getMuteList().isMuted(client.getAccount().getName())) {
			client.sendLine(String.format(
					"SERVERMSG Message dropped. You are not allowed to talk in"
					+ " #%s! Please contact one of the moderators.",
					channel.getName()));
			throw new CommandProcessingException(getCommandName(),
					"The client is muted");
		} else if (channel.getMuteList().isIpMuted(client.getIp())) {
			client.sendLine(String.format(
					"SERVERMSG Message dropped. You are not allowed to talk in"
					+ " #%s (muted by IP address)! If you believe this is an"
					+ " error, contact one of the moderators.",
					channel.getName()));
			throw new CommandProcessingException(getCommandName(),
					"The client is muted by IP");
		}
	}
}
