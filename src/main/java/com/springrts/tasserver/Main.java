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

package com.springrts.tasserver;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles everything concerning starting this server as a normal
 * application, directly from the command line or the graphical interface of the
 * OS.
 * @author hoijui
 */
public class Main {

	private static final Logger LOG  = LoggerFactory.getLogger(Main.class);

	private Main() {}


	/**
	 * Processes all command line arguments in 'args'.
	 * Raises an exception in case of errors.
	 */
	public static void processCommandLineArguments(Context context, String[] args) throws IOException, Exception {

		// process command line arguments:
		String s;
		for (int i = 0; i < args.length; i++) {
			if (args[i].charAt(0) == '-') {
				s = args[i].substring(1).toUpperCase();
				if (s.equals("PORT")) {
					int p = Integer.parseInt(args[i + 1]);
					if ((p < 1) || (p > 65535)) {
						throw new IOException();
					}
					context.getServer().setPort(p);
					i++; // we must skip port number parameter in the next iteration
				} else if (s.equals("LAN")) {
					context.getServer().setLanMode(true);
				} else if (s.equals("STATISTICS")) {
					context.getStatistics().setRecording(true);
				} else if (s.equals("NATPORT")) {
					int p = Integer.parseInt(args[i + 1]);
					if ((p < 1) || (p > 65535)) {
						throw new IOException();
					}
					context.getNatHelpServer().setPort(p);
					i++; // we must skip port number parameter in the next iteration
				} else if (s.equals("LOGMAIN")) {
					context.getChannels().setChannelsToLogRegex("^main$");
				} else if (s.equals("LANADMIN")) {
					String lanAdmin_username = args[i + 1];
					String lanAdmin_password = Misc.encodePassword(args[i + 2]);

					String error;
					if ((error = Account.isOldUsernameValid(lanAdmin_username)) != null) {
						throw new IllegalArgumentException("LAN admin username is not valid: " + error);
					}
					if ((error = Account.isPasswordValid(lanAdmin_password)) != null) {
						throw new IllegalArgumentException("LAN admin password is not valid: " + error);
					}
					context.getServer().setLanAdminUsername(lanAdmin_username);
					context.getServer().setLanAdminPassword(lanAdmin_password);

					i += 2; // we must skip userName and password parameters in next iteration
				} else if (s.equals("LOADARGS")) {
					BufferedReader in = null;
					try {
						in = new BufferedReader(new FileReader(args[i + 1]));
						String line;
						while ((line = in.readLine()) != null) {
							try {
								processCommandLineArguments(context, line.split(" "));
							} catch (Exception ex) {
								LOG.error("Error in reading "+ (args[i + 1])
										+ " (invalid line)", ex);
								throw ex;
							}
						}
					} finally {
						if (in != null) {
							in.close();
						}
					}
					i++; // we must skip filename parameter in the next iteration
				} else if (s.equals("LATESTSPRINGVERSION")) {
					String latestSpringVersion = args[i + 1];
					context.setEngine(new Engine(latestSpringVersion));
					i++; // to skip Spring version argument
				} else if (s.equals("USERDB")) {
					context.getServer().setUseUserDB(true);
				} else {
					LOG.error("Invalid commandline argument");
					throw new IOException();
				}
			} else {
				LOG.error("Commandline argument does not start with a hyphen");
				throw new IOException();
			}
		}
	}


	public static void printCommandLineArgumentsHelp() {

		System.out.println("Usage:");
		System.out.println("");
		System.out.println("-PORT [number]");
		System.out.println("  Server will host on port [number]. If command is omitted,");
		System.out.println("  default port will be used.");
		System.out.println("");
		System.out.println("-LAN");
		System.out.println("  Server will run in \"LAN mode\", meaning any user can login as");
		System.out.println("  long as he uses unique username (password is ignored).");
		System.out.println("  Note: Server will accept users from outside the local network too.");
		System.out.println("");
		System.out.println("-STATISTICS");
		System.out.println("  Server will create and save statistics on disk on predefined intervals.");
		System.out.println("");
		System.out.println("-NATPORT [number]");
		System.out.println("  Server will use this port with some NAT traversal techniques. If command is omitted,");
		System.out.println("  default port will be used.");
		System.out.println("");
		System.out.println("-LOGMAIN");
		System.out.println("  Server will log all conversations from channel #main to MainChanLog.log");
		System.out.println("");
		System.out.println("-LANADMIN [username] [password]");
		System.out.println("  Will override default lan admin account. Use this account to set up your lan server");
		System.out.println("  at runtime.");
		System.out.println("");
		System.out.println("-LOADARGS [filename]");
		System.out.println("  Will read command-line arguments from the specified file. You can freely combine actual");
		System.out.println("  command-line arguments with the ones from the file (if duplicate args are specified, the last");
		System.out.println("  one will prevail).");
		System.out.println("");
		System.out.println("-LATESTSPRINGVERSION [version]");
		System.out.println("  Will set latest Spring version to this string. By default no value is set (defaults to \"*\").");
		System.out.println("  This is used to tell clients which version is the latest one so that they know when to update.");
		System.out.println("");
		System.out.println("-DBURL [url]");
		System.out.println("  Will set URL of the database (used only in \"normal mode\", not LAN mode).");
		System.out.println("");
		System.out.println("-DBUSERNAME [username]");
		System.out.println("  Will set username for the database (used only in \"normal mode\", not LAN mode).");
		System.out.println("");
		System.out.println("-DBPASSWORD [password]");
		System.out.println("  Will set password for the database (used only in \"normal mode\", not LAN mode).");
		System.out.println("");
		System.out.println("-USERDB");
		System.out.println("  Instead of accounts.txt, use the DB (used only in \"normal mode\", not LAN mode).");
		System.out.println("");
	}

	public static void main(String[] args) {

		Context context = new Context();
		context.init();

		// process command line arguments:
		try {
			processCommandLineArguments(context, args);
		} catch (Exception ex) {
			LOG.warn("Bad command line arguments", ex);
			System.out.println("Bad command line arguments.");
			System.out.println("");
			printCommandLineArgumentsHelp();
			System.exit(1);
		}

		TASServer tasServer = new TASServer(context);
	}
}
