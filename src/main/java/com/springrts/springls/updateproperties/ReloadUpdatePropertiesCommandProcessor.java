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

package com.springrts.springls.updateproperties;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reloads the locally stored list of Spring versions and server responses to
 * them.
 * @see com.springrts.springls.UpdateProperties
 * @author hoijui
 */
@SupportedCommand("RELOADUPDATEPROPERTIES")
public class ReloadUpdatePropertiesCommandProcessor extends AbstractCommandProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ReloadUpdatePropertiesCommandProcessor.class);

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

		UpdateProperties updateProperties = getService(UpdateProperties.class);
		if (updateProperties.read(UpdateProperties.DEFAULT_FILENAME))
		{
			LOG.info("\"Update properties\" read from {}",
					UpdateProperties.DEFAULT_FILENAME);
			client.sendLine(String.format(
					"SERVERMSG \"Update properties\" have been successfully loaded from %s",
					UpdateProperties.DEFAULT_FILENAME));
		} else {
			client.sendLine(String.format(
					"SERVERMSG Unable to load \"Update properties\" from %s!",
					UpdateProperties.DEFAULT_FILENAME));
		}

		return true;
	}
}
