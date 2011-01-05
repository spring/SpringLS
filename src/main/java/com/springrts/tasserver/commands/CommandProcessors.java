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

package com.springrts.tasserver.commands;


import com.springrts.tasserver.Context;
import com.springrts.tasserver.ContextReceiver;
import com.springrts.tasserver.commands.impl.AdminBroadcastCommandProcessor;
import com.springrts.tasserver.commands.impl.BroadcastCommandProcessor;
import com.springrts.tasserver.commands.impl.BroadcastExtendedCommandProcessor;
import com.springrts.tasserver.commands.impl.ChangeAccountAccessCommandProcessor;
import com.springrts.tasserver.commands.impl.ChangeAccountPasswordCommandProcessor;
import com.springrts.tasserver.commands.impl.ChangeCharsetCommandProcessor;
import com.springrts.tasserver.commands.impl.ChannelMessageCommandProcessor;
import com.springrts.tasserver.commands.impl.CreateAccountCommandProcessor;
import com.springrts.tasserver.commands.impl.EnableLoginCommandProcessor;
import com.springrts.tasserver.commands.impl.EnableRegisterCommandProcessor;
import com.springrts.tasserver.commands.impl.FindIpCommandProcessor;
import com.springrts.tasserver.commands.impl.FloodLevelCommandProcessor;
import com.springrts.tasserver.commands.impl.ForceCloseBattleCommandProcessor;
import com.springrts.tasserver.commands.impl.ForceLeaveChannelCommandProcessor;
import com.springrts.tasserver.commands.impl.ForceStopServerCommandProcessor;
import com.springrts.tasserver.commands.impl.ForgeMessageCommandProcessor;
import com.springrts.tasserver.commands.impl.GarbageCollectorCommandProcessor;
import com.springrts.tasserver.commands.impl.GetAccountAccessCommandProcessor;
import com.springrts.tasserver.commands.impl.GetAccountCountCommandProcessor;
import com.springrts.tasserver.commands.impl.GetAccountInfoCommandProcessor;
import com.springrts.tasserver.commands.impl.GetInGameTimeCommandProcessor;
import com.springrts.tasserver.commands.impl.GetIpCommandProcessor;
import com.springrts.tasserver.commands.impl.GetLastIpCommandProcessor;
import com.springrts.tasserver.commands.impl.GetLastLoginTimeCommandProcessor;
import com.springrts.tasserver.commands.impl.GetLobbyVersionCommandProcessor;
import com.springrts.tasserver.commands.impl.GetSendBufferSizeCommandProcessor;
import com.springrts.tasserver.commands.impl.Ip2CountryCommandProcessor;
import com.springrts.tasserver.commands.impl.KickUserCommandProcessor;
import com.springrts.tasserver.commands.impl.KillCommandProcessor;
import com.springrts.tasserver.commands.impl.KillIpCommandProcessor;
import com.springrts.tasserver.commands.impl.LongTimeToDateCommandProcessor;
import com.springrts.tasserver.commands.impl.MemoryAvailableCommandProcessor;
import com.springrts.tasserver.commands.impl.MuteCommandProcessor;
import com.springrts.tasserver.commands.impl.MuteListCommandProcessor;
import com.springrts.tasserver.commands.impl.OpenBattleCommandProcessor;
import com.springrts.tasserver.commands.impl.PingCommandProcessor;
import com.springrts.tasserver.commands.impl.ReInitializeIp2CountryCommandProcessor;
import com.springrts.tasserver.commands.impl.RedirectCommandProcessor;
import com.springrts.tasserver.commands.impl.RedirectOffCommandProcessor;
import com.springrts.tasserver.commands.impl.RegisterCommandProcessor;
import com.springrts.tasserver.commands.impl.RemoveAccountCommandProcessor;
import com.springrts.tasserver.commands.impl.SaveAccountsServerCommandProcessor;
import com.springrts.tasserver.commands.impl.SetChannelKeyCommandProcessor;
import com.springrts.tasserver.commands.impl.SetTimeOutCommandProcessor;
import com.springrts.tasserver.commands.impl.UnmuteCommandProcessor;
import com.springrts.tasserver.commands.impl.UpTimeCommandProcessor;
import com.springrts.tasserver.commands.impl.UpdateIp2CountryCommandProcessor;
import com.springrts.tasserver.commands.impl.UpdateMotdCommandProcessor;
import com.springrts.tasserver.commands.impl.UpdateStatisticsCommandProcessor;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages all the command processors for a server instance.
 * @see CommandProcessor}
 * @author hoijui
 */
public class CommandProcessors implements ContextReceiver {

	private final Log log  = LogFactory.getLog(CommandProcessors.class);

	private Map<String, CommandProcessor> cmdName_processor;
	private Context context = null;


	/**
	 * Extracts the name of the command supported by a command processor
	 * from its {@link SupportedCommand} annotation.
	 */
	public static String extractCommandName(Class<? extends CommandProcessor> cmdProcCls) {

		String name = null;

		SupportedCommand supCmd = cmdProcCls.getAnnotation(SupportedCommand.class);
		if (supCmd == null) {
			throw new IllegalArgumentException(cmdProcCls.getCanonicalName() +
					" is not a valid " +
					CommandProcessor.class.getCanonicalName() + "; " +
					"@" + SupportedCommand.class.getCanonicalName() +
					" annotation is missing.");
		}
		if (!supCmd.value().equals(supCmd.value().toUpperCase())) {
			throw new IllegalArgumentException(cmdProcCls.getCanonicalName() +
					" is not a valid " +
					CommandProcessor.class.getCanonicalName() + "; " +
					"The command name has to be upper-case only.");
		}
		name = supCmd.value();

		return name;
	}


	public CommandProcessors() {
		cmdName_processor = new HashMap<String, CommandProcessor>();
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	/**
	 * Loads the default command processors.
	 */
	public void init() {

		Collection<Class<? extends CommandProcessor>> commandProcessorClasses =
				new LinkedList<Class<? extends CommandProcessor>>();
		commandProcessorClasses.add(PingCommandProcessor.class);
		commandProcessorClasses.add(OpenBattleCommandProcessor.class);
		commandProcessorClasses.add(CreateAccountCommandProcessor.class);
		commandProcessorClasses.add(RegisterCommandProcessor.class);
		commandProcessorClasses.add(KickUserCommandProcessor.class);
		commandProcessorClasses.add(UpTimeCommandProcessor.class);
		commandProcessorClasses.add(FloodLevelCommandProcessor.class);
		commandProcessorClasses.add(KillCommandProcessor.class);
		commandProcessorClasses.add(KillIpCommandProcessor.class);
		commandProcessorClasses.add(EnableLoginCommandProcessor.class);
		commandProcessorClasses.add(EnableRegisterCommandProcessor.class);
		commandProcessorClasses.add(SetTimeOutCommandProcessor.class);
		commandProcessorClasses.add(RemoveAccountCommandProcessor.class);
		commandProcessorClasses.add(ForceStopServerCommandProcessor.class);
		commandProcessorClasses.add(SaveAccountsServerCommandProcessor.class);
		commandProcessorClasses.add(ChangeAccountPasswordCommandProcessor.class);
		commandProcessorClasses.add(ChangeAccountAccessCommandProcessor.class);
		commandProcessorClasses.add(GetAccountAccessCommandProcessor.class);
		commandProcessorClasses.add(RedirectCommandProcessor.class);
		commandProcessorClasses.add(RedirectOffCommandProcessor.class);
		commandProcessorClasses.add(BroadcastCommandProcessor.class);
		commandProcessorClasses.add(BroadcastExtendedCommandProcessor.class);
		commandProcessorClasses.add(AdminBroadcastCommandProcessor.class);
		commandProcessorClasses.add(GetAccountCountCommandProcessor.class);
		commandProcessorClasses.add(FindIpCommandProcessor.class);
		commandProcessorClasses.add(GetLastIpCommandProcessor.class);
		commandProcessorClasses.add(GetAccountInfoCommandProcessor.class);
		commandProcessorClasses.add(ForgeMessageCommandProcessor.class);
		commandProcessorClasses.add(GetIpCommandProcessor.class);
		commandProcessorClasses.add(GetInGameTimeCommandProcessor.class);
		commandProcessorClasses.add(ForceCloseBattleCommandProcessor.class);
		commandProcessorClasses.add(MuteCommandProcessor.class);
		commandProcessorClasses.add(UnmuteCommandProcessor.class);
		commandProcessorClasses.add(MuteListCommandProcessor.class);
		commandProcessorClasses.add(ChannelMessageCommandProcessor.class);
		commandProcessorClasses.add(Ip2CountryCommandProcessor.class);
		commandProcessorClasses.add(ReInitializeIp2CountryCommandProcessor.class);
		commandProcessorClasses.add(UpdateIp2CountryCommandProcessor.class);
		commandProcessorClasses.add(ChangeCharsetCommandProcessor.class);
		commandProcessorClasses.add(GetLobbyVersionCommandProcessor.class);
		commandProcessorClasses.add(UpdateStatisticsCommandProcessor.class);
		commandProcessorClasses.add(UpdateMotdCommandProcessor.class);
		commandProcessorClasses.add(LongTimeToDateCommandProcessor.class);
		commandProcessorClasses.add(GetLastLoginTimeCommandProcessor.class);
		commandProcessorClasses.add(SetChannelKeyCommandProcessor.class);
		commandProcessorClasses.add(ForceLeaveChannelCommandProcessor.class);
		commandProcessorClasses.add(GetSendBufferSizeCommandProcessor.class);
		commandProcessorClasses.add(MemoryAvailableCommandProcessor.class);
		commandProcessorClasses.add(GarbageCollectorCommandProcessor.class);

		try {
			load(commandProcessorClasses);
		} catch (Exception ex) {
			log.fatal("Failed to load Command Processors", ex);
			context.getServer().closeServerAndExit();
		}
	}

	/**
	 * Adds a command processor.
	 * @param cp to be added
	 * @throws Exception if name extraction fails
	 */
	private void add(CommandProcessor cp) throws Exception {

		String cmdName = null;
		try {
			cmdName = extractCommandName(cp.getClass());
		} catch (Exception ex) {
			throw new RuntimeException("Failed extracting command name", ex);
		}

		cp.receiveContext(context);

		cmdName_processor.put(cmdName, cp);
	}

	/**
	 * Instantiates a single CommandProcessor.
	 * @param cmdName_processor where to store it
	 * @param cpc the class to instantiate
	 * @throws Exception if loading failed, for whatever reason
	 */
	private static CommandProcessor load(Class<? extends CommandProcessor> cpc) throws Exception {

		CommandProcessor cp = null;

		Constructor<? extends CommandProcessor> noArgsCtor = null;
		try {
			noArgsCtor = cpc.getConstructor();
		} catch (NoSuchMethodException ex) {
			throw new RuntimeException(cpc.getCanonicalName() +
				" is not a valid CommandProcessor; " +
				"No-args constructor is missing.", ex);
		}
		try {
			cp = noArgsCtor.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to instantiate " +
					cpc.getCanonicalName(), ex);
		}

		return cp;
	}

	/**
	 * Loads instances for all supplied command processor classes.
	 * @param cpcs classes of the command processors to be load
	 * @return true, if the command was valid and successfully executed
	 * @throws Exception if loading of any of the command processors failed,
	 *                   for whatever reason
	 */
	private void load(Collection<Class<? extends CommandProcessor>> cpcs)
			throws Exception {

		for (Class<? extends CommandProcessor> cpc : cpcs) {
			add(load(cpc));
		}
	}

	/**
	 * Returns the command processor responsible for handling the given command.
	 * @param commandName the name of the command to search a processor for
	 * @return the command processor responsible for handling the given command,
	 *         or <code>null</code>, if no suitable one was found
	 */
	public CommandProcessor get(String commandName) {
		return cmdName_processor.get(commandName);
	}
}
