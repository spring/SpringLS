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
import com.springrts.tasserver.UpdateProperties;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reloads the locally stored list of Spring versions and server responses to
 * them.
 * @see com.springrts.tasserver.UpdateProperties
 * @author hoijui
 */
@SupportedCommand("RELOADUPDATEPROPERTIES")
public class ReloadUpdatePropertiesCommandProcessor extends AbstractCommandProcessor {

	private static final Log s_log  = LogFactory.getLog(ReloadUpdatePropertiesCommandProcessor.class);

	public ReloadUpdatePropertiesCommandProcessor() {
		super(Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (getContext().getUpdateProperties().read(UpdateProperties.DEFAULT_FILENAME)) {
			s_log.info("\"Update properties\" read from " + UpdateProperties.DEFAULT_FILENAME);
			client.sendLine("SERVERMSG \"Update properties\" have been successfully loaded from " + UpdateProperties.DEFAULT_FILENAME);
		} else {
			client.sendLine(new StringBuilder("SERVERMSG Unable to load \"Update properties\" from ")
					.append(UpdateProperties.DEFAULT_FILENAME).append("!").toString());
		}

		return true;
	}
}
