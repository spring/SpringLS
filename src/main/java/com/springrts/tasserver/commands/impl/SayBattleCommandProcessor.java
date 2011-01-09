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
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sent by client who is participating in a battle to server, who forwards this
 * message to all other clients in the battle. BATTLE_ID is not required since
 * every user can participate in only one battle at the time. If user is not
 * participating in the battle, this command is ignored and is considered
 * invalid.
 * @author hoijui
 */
@SupportedCommand("SAYBATTLE")
public class SayBattleCommandProcessor extends AbstractCommandProcessor {

	private static final Log s_log  = LogFactory.getLog(SayBattleCommandProcessor.class);

	public SayBattleCommandProcessor() {
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

		String message = Misc.makeSentence(args, 0);

		if (client.getBattleID() == Battle.NO_BATTLE_ID) {
			return false;
		}
		Battle bat = getContext().getBattles().getBattleByID(client.getBattleID());
		if (bat == null) {
			return false;
		}

		// check for flooding:
		if ((message.length() > getContext().getServer().getMaxChatMessageLength()) && (client.getAccount().getAccess().compareTo(Account.Access.ADMIN) < 0)) {
			s_log.warn(new StringBuilder("Flooding detected from ")
					.append(client.getIp()).append(" (")
					.append(client.getAccount().getName())
					.append(") [exceeded max. chat message size]").toString());
			client.sendLine(new StringBuilder("SERVERMSG Flooding detected - you have exceeded maximum allowed chat message size (")
					.append(getContext().getServer().getMaxChatMessageLength()).append(" bytes). Your message has been ignored.").toString());
			getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Flooding has been detected from ")
					.append(client.getIp()).append(" (")
					.append(client.getAccount().getName())
					.append(") - exceeded maximum chat message size. Ignoring ...").toString());
			return false;
		}

		bat.sendToAllClients(new StringBuilder("SAIDBATTLE ")
				.append(client.getAccount().getName()).append(" ")
				.append(message).toString());

		return true;
	}
}
