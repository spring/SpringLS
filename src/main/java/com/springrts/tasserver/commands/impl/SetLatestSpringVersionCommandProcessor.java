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
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Engine;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * Allows an administrator to change the latest (stable) engine version,
 * which is generally required to play online.
 * @author hoijui
 */
@SupportedCommand("SETLATESTSPRINGVERSION")
public class SetLatestSpringVersionCommandProcessor extends AbstractCommandProcessor {

	public SetLatestSpringVersionCommandProcessor() {
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
			client.sendLine("SERVERMSG Bad arguments to SETLATESTSPRINGVERSION command!");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		String engineVersion = args.get(0);

		getContext().setEngine(new Engine(engineVersion));

		client.sendLine("SERVERMSG Latest spring version has been set to "
				+ getContext().getEngine().getVersion());

		return true;
	}
}
