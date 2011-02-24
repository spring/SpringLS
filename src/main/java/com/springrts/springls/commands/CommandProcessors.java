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

package com.springrts.springls.commands;


import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.commands.impl.Activator;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all the command processors for a server instance.
 * @see CommandProcessor
 * @author hoijui
 */
public class CommandProcessors implements ContextReceiver {

	private final Logger log  = LoggerFactory.getLogger(CommandProcessors.class);
	private Map<String, CommandProcessor> cmdNameToProcessor;
	private Context context;
	private CommandProcessorTracker commandProcessorTracker;
	private Activator commandImplActivator;

	/**
	 * Extracts the name of the command supported by a command processor
	 * from its {@link SupportedCommand} annotation.
	 */
	public static String extractCommandName(Class<? extends CommandProcessor> cmdProcCls) {

		String name = null;

		SupportedCommand supCmd = cmdProcCls.getAnnotation(SupportedCommand.class);
		if (supCmd == null) {
			throw new IllegalArgumentException(cmdProcCls.getCanonicalName()
					+ " is not a valid "
					+ CommandProcessor.class.getCanonicalName()
					+ "; @" + SupportedCommand.class.getCanonicalName()
					+ " annotation is missing.");
		}
		if (!supCmd.value().equals(supCmd.value().toUpperCase())) {
			throw new IllegalArgumentException(cmdProcCls.getCanonicalName()
					+ " is not a valid "
					+ CommandProcessor.class.getCanonicalName()
					+ "; The command name has to be upper-case only.");
		}
		name = supCmd.value();

		return name;
	}


	public CommandProcessors() {

		cmdNameToProcessor = new HashMap<String, CommandProcessor>();
		context = null;
		commandProcessorTracker = null;
		commandImplActivator = null;
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	/**
	 * Loads the default command processors.
	 */
	public void init() {

		// TODO move this into a BundleActivator.start() method
		commandProcessorTracker = new CommandProcessorTracker(context.getFramework().getBundleContext());
		commandProcessorTracker.open();
		// TODO move this into a BundleActivator.stop() method
//		commandProcessorTracker.close();

		commandImplActivator = new Activator();
		commandImplActivator.start(context.getFramework().getBundleContext());
	}

	/**
	 * Adds a command processor, responsible for handling the given
	 * command-name.
	 * @param commandName the name of the command to handle with the given
	 *   processor
	 * @param commandProcessor the processor to handle the given command-name
	 */
	public void add(String commandName, CommandProcessor commandProcessor) {
		cmdNameToProcessor.put(commandName, commandProcessor);
	}

	/**
	 * Removes a command processor, responsible for handling the given
	 * command-name.
	 * @param commandName the name for which to remove the command processor
	 */
	public void remove(String commandName) {
		cmdNameToProcessor.remove(commandName);
	}

	/**
	 * Returns the command processor responsible for handling the given command.
	 * @param commandName the name of the command to search a processor for
	 * @return the command processor responsible for handling the given command,
	 *         or <code>null</code>, if no suitable one was found
	 */
	public CommandProcessor get(String commandName) {
		return cmdNameToProcessor.get(commandName);
	}
}
