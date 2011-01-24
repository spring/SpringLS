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
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

/**
 * Lets an administrator set the server-wide char-set.
 * @author hoijui
 */
@SupportedCommand("CHANGECHARSET")
public class ChangeCharsetCommandProcessor extends AbstractCommandProcessor {

	public ChangeCharsetCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String charset = args.get(0);

		try {
			getContext().getServer().setCharset(charset);
		} catch (IllegalCharsetNameException e) {
			client.sendLine("SERVERMSG Error: Illegal charset name: " + charset);
			return false;
		} catch (UnsupportedCharsetException e) {
			client.sendLine("SERVERMSG Error: Unsupported charset: " + charset);
			return false;
		}

		client.sendLine("SERVERMSG Charset set to " + charset);

		return true;
	}
}
