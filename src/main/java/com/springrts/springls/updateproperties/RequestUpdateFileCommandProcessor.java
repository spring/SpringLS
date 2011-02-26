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


import com.springrts.springls.Client;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * The client sends his current engine version, to request a suitable update
 * file.
 * If no update could be found, the client will be killed.
 * @author hoijui
 */
@SupportedCommand("REQUESTUPDATEFILE")
public class RequestUpdateFileCommandProcessor extends AbstractCommandProcessor {

	public RequestUpdateFileCommandProcessor() {
		super(1, ARGS_MAX_NOCHECK);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		String version = Misc.makeSentence(args, 0);
		UpdateProperties updateProperties = getService(UpdateProperties.class);
		String response = updateProperties.getResponse(version);

		// send a response to the client:
		client.sendLine(response);

		// kill client if no update has been found for him:
		if (response.substring(0, 12).toUpperCase().equals("SERVERMSGBOX")) {
			getContext().getClients().killClient(client);
		}

		return true;
	}
}
