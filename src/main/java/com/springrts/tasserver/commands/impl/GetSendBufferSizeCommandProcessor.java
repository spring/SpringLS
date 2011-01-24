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
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Lets an administrator fetch the size of the send-buffer for a specific
 * client.
 * @author hoijui
 */
@SupportedCommand("GETSENDBUFFERSIZE")
public class GetSendBufferSizeCommandProcessor extends AbstractCommandProcessor {

	public GetSendBufferSizeCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("SERVERMSG Error: this method requires exactly 2 arguments!");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String username = args.get(0);

		Client c = getContext().getClients().getClient(username);
		if (c == null) {
			client.sendLine(new StringBuilder("SERVERMSG Error: user <")
					.append(username).append("> not found online!").toString());
			return false;
		}

		int size;
		try {
			size = c.getSockChan().socket().getSendBufferSize();
		} catch (Exception e) {
			// this could perhaps happen if user just disconnected or something
			client.sendLine(new StringBuilder("SERVERMSG Error: exception raised while trying to get send buffer size for <")
					.append(username).append(">!").toString());
			return false;
		}

		client.sendLine(new StringBuilder("SERVERMSG Send buffer size for <")
				.append(c.getAccount().getName()).append("> is set to ")
				.append(size).append(" bytes.").toString());

		return true;
	}
}
