/*
	Copyright (c) 2012 Robin Vobruba <hoijui.quaero@gmail.com>

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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.DataConfiguration;

/**
 * Handles the arguments given on the command line.
 * @author hoijui
 */
public final class CmdLineArgs {

	private Options options;
	private CommandLineParser parser;

	public CmdLineArgs() {

		options = createOptions();
		parser = new GnuParser();
	}

	private static Options createOptions() {

		Configuration defaults = ServerConfiguration.getDefaults();

		Options options = new Options();

		Option help = new Option(null, "help", false,
				"Print this help message.");
		options.addOption(help);

		Option port = new Option("p", "port", true,
				String.format(
				"The main (TCP) port number to host on [1, 65535]."
				+ " The default is %d.",
				defaults.getInt(ServerConfiguration.PORT)));
		// possible types:
		// * File.class
		// * Number.class
		// * Class.class
		// * Object.class
		// * Url.class
		port.setType(Number.class);
		port.setArgName("port-number");
		options.addOption(port);

		Option statistics = new Option(null, "statistics", false,
				"Whether to create and save statistics to disc on predefined"
				+ " intervals.");
		options.addOption(statistics);

		Option natPort = new Option("n", "nat-port", true,
				String.format(
				"The (UDP) port number to host the NAT traversal techniques"
				+ " help service on [1, 65535], which lets clients detect their"
				+ " source port, for example when using \"hole punching\"."
				+ " The default is %d.",
				defaults.getInt(ServerConfiguration.NAT_PORT)));
		port.setType(Number.class);
		natPort.setArgName("NAT-port-number");
		options.addOption(natPort);

		Option logMain = new Option(null, "log-main", false,
				String.format(
				"Whether to log all conversations from channel #main to \"%s\"",
				Channel.createDefaultActivityLogFilePath("main").getPath()));
		options.addOption(logMain);

		Option lanAdmin = new Option(null, "lan-admin", true,
				String.format(
				"The LAN mode admin account. Use this account to administer"
				+ " your LAN server. The default is \"%s\", with password \"%s\".",
				defaults.getString(ServerConfiguration.LAN_ADMIN_USERNAME),
				defaults.getString(ServerConfiguration.LAN_ADMIN_PASSWORD)));
		lanAdmin.setArgName("username");
		options.addOption(lanAdmin);

		Option loadArgs = new Option(null, "load-args", true,
				"Will read command-line arguments from the specified file."
				+ " You can freely combine actual command-line arguments with"
				+ " the ones from the file. If duplicate args are specified,"
				+ " the last one will prevail.");
		loadArgs.setArgName("filename");
		port.setType(File.class);
		options.addOption(loadArgs);

		Option springVersion = new Option(null, "spring-version", true,
				"Will set the latest Spring version to this string."
				+ " The default is \"*\". This is used to tell clients which"
				+ " version is the latest one, so that they know when to"
				+ " update.");
		springVersion.setArgName("version");
		options.addOption(springVersion);

		Option useStorageDb = new Option(null, "database", false,
				"Use a DB for user accounts and ban entries."
				+ " This disables \"LAN mode\".");
		options.addOption(useStorageDb);

		Option useStorageFile = new Option(null, "file-storage", false,
				"Use the (deprecated) accounts.txt for user accounts."
				+ " This disables \"LAN mode\".");
		options.addOption(useStorageFile);

		OptionGroup storageOG = new OptionGroup();
		storageOG.addOption(useStorageDb);
		storageOG.addOption(useStorageFile);
		options.addOptionGroup(storageOG);

		return options;
	}

	/**
	 * Processes all command line arguments in 'args'.
	 * Raises an exception in case of errors.
	 * @return whether to exit the application after this method
	 */
	private static boolean apply(Configuration configuration,
			CommandLineParser parser, Options options, String[] args)
			throws ParseException
	{
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(Server.getApplicationName(), options);
			return true;
		}

		if (cmd.hasOption("port")) {
			String portStr = cmd.getOptionValue("port");
			int port = Integer.parseInt(portStr);
			if ((port < 1) || (port > 65535)) {
				throw new ParseException("Invalid port specified: "
						+ portStr);
			}
			configuration.setProperty(ServerConfiguration.PORT, port);
		}
		if (cmd.hasOption("database")) {
			configuration.setProperty(ServerConfiguration.USE_DATABASE, true);
		} else if (cmd.hasOption("file-storage")) {
			configuration.setProperty(ServerConfiguration.USE_DATABASE, false);
		} else {
			configuration.setProperty(ServerConfiguration.LAN_MODE, true);
		}
		if (cmd.hasOption("statistics")) {
			configuration.setProperty(ServerConfiguration.STATISTICS_STORE, true);
		}
		if (cmd.hasOption("nat-port")) {
			String portStr = cmd.getOptionValue("port");
			int port = Integer.parseInt(portStr);
			if ((port < 1) || (port > 65535)) {
				throw new ParseException("Invalid NAT traversal port"
						+ " specified: " + portStr);
			}
			configuration.setProperty(ServerConfiguration.NAT_PORT, port);
		}
		if (cmd.hasOption("log-main")) {
			configuration.setProperty(ServerConfiguration.CHANNELS_LOG_REGEX, "^main$");
		}
		if (cmd.hasOption("lan-admin")) {
			String[] usernamePassword = cmd.getOptionValues("lan-admin");

			if (usernamePassword.length < 1) {
				throw new MissingArgumentException(
						"LAN admin user name is missing");
			}
			String username = usernamePassword[0];
			String password = (usernamePassword.length > 1)
					? usernamePassword[0]
					: ServerConfiguration.getDefaults().getString(
							ServerConfiguration.LAN_ADMIN_PASSWORD);

			String error = Account.isOldUsernameValid(username);
			if (error != null) {
				throw new ParseException(
						"LAN admin user name is not valid: " + error);
			}
			error = Account.isPasswordValid(password);
			if (error != null) {
				throw new ParseException(
						"LAN admin password is not valid: " + error);
			}
			configuration.setProperty(ServerConfiguration.LAN_ADMIN_USERNAME, username);
			configuration.setProperty(ServerConfiguration.LAN_ADMIN_PASSWORD, password);
		}
		if (cmd.hasOption("load-args")) {
			File argsFile = new File(cmd.getOptionValue("load-args"));
			Reader inF = null;
			BufferedReader in = null;
			try {
				try {
					inF = new FileReader(argsFile);
					in = new BufferedReader(inF);
					String line;
					List<String> argsList = new LinkedList<String>();
					while ((line = in.readLine()) != null) {
						String[] argsLine = line.split("[ \t]+");
						argsList.addAll(Arrays.asList(argsLine));
					}
					String[] args2 = argsList.toArray(new String[argsList.size()]);
					apply(configuration, parser, options, args2);
				} finally {
					if (in != null) {
						in.close();
					} else if (inF != null) {
						inF.close();
					}
				}
			} catch (Exception ex) {
				throw new ParseException("invalid load-args argument: "
						+ ex.getMessage());
			}
		}
		if (cmd.hasOption("spring-version")) {
			String version = cmd.getOptionValue("spring-version");
			configuration.setProperty(ServerConfiguration.ENGINE_VERSION, version);
		}

		return false;
	}

	private static void printHelp(Options options) {

		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(Server.getApplicationName(), options);
	}

	public boolean apply(DataConfiguration configuration, String[] args)
			throws Exception
	{
		return apply(configuration, parser, options, args);
	}

	public void printHelp() {
		printHelp(options);
	}
}
