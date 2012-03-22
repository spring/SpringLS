/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls;


import org.apache.commons.configuration.DataConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles everything concerning starting this server as a normal
 * application, directly from the command line or the graphical interface of the
 * OS.
 * @author Betalord
 * @author hoijui
 */
public final class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	/** Make clear that this is a utility class */
	private Main() {}

	public static void main(String[] args) {

		CmdLineArgs cmdLineArgs = new CmdLineArgs();

		DataConfiguration configuration = new DataConfiguration(ServerConfiguration.getDefaults());

		boolean exit;
		try {
			exit = cmdLineArgs.apply(configuration, args);
		} catch (Exception ex) {
			LOG.warn("Bad command line arguments", ex);
			cmdLineArgs.printHelp();
			exit = true;
		}

		if (!exit) {
			TASServer.startServerInstance(configuration);
		}
	}
}
