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

/*
 * Copied from an example from the Apache Software Foundation (ASF).
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.springrts.springls.commands.impl;


import com.springrts.springls.Context;
import com.springrts.springls.commands.CommandProcessor;
import com.springrts.springls.commands.CommandProcessors;
import java.util.Collection;
import java.util.LinkedList;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple bundle activator for the default
 * <tt>CommandProcessor</tt> service. This activator simply creates instances
 * of the command-processor service object and registers them with the service
 * registry along with the service properties indicating the services name.
 */
public class Activator implements BundleActivator {

	private final Logger log = LoggerFactory.getLogger(Activator.class);
	private BundleContext context = null;

	/**
	 * Implements the <tt>BundleActivator.start()</tt> method, which
	 * registers the standard <tt>CommandProcessor</tt> services.
	 * @param context The context for the bundle.
	 */
	@Override
	public void start(BundleContext context) {
		this.context = context;

		try {
			load(getCommandProcessorClasses());
			// FIXME This should probably be done in the ForceJoinBattleCommandProcessor directly.
			//   That would first need some start(bundle) and stop(bundle) equivalent there, though.
			Context.getService(context, Context.class).getServer().getSupportedCompFlags().add("m");
		} catch (Exception ex) {
			log.error("Failed to load Command Processors", ex);
			Context.getService(context, Context.class).getServerThread().closeServerAndExit();
		}
	}

	/**
	 * Implements the <tt>BundleActivator.start()</tt> method, which
	 * does nothing.
	 * @param context The context for the bundle.
	 */
	@Override
	public void stop(BundleContext context) {
		Context.getService(context, Context.class).getServer().getSupportedCompFlags().remove("m");
	}

	/**
	 * Loads instances for all supplied command processor classes.
	 * @param cpcs classes of the command processors to be load
	 * @return true, if the command was valid and successfully executed
	 * @throws Exception if loading of any of the command processors failed,
	 *   for whatever reason
	 */
	private void load(Collection<Class<? extends CommandProcessor>> cpcs)
			throws Exception
	{
		for (Class<? extends CommandProcessor> cpc : cpcs) {
			CommandProcessors.add(context, CommandProcessors.load(cpc));
		}
	}

	private static Collection<Class<? extends CommandProcessor>> getCommandProcessorClasses() {

		Collection<Class<? extends CommandProcessor>> commandProcessorClasses =
				new LinkedList<Class<? extends CommandProcessor>>();

		commandProcessorClasses.add(PingCommandProcessor.class);
		commandProcessorClasses.add(OpenBattleCommandProcessor.class);
		commandProcessorClasses.add(CreateAccountCommandProcessor.class);
		commandProcessorClasses.add(RegisterCommandProcessor.class);
		commandProcessorClasses.add(KickUserCommandProcessor.class);
		commandProcessorClasses.add(UpTimeCommandProcessor.class);
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
		commandProcessorClasses.add(ChangeCharsetCommandProcessor.class);
		commandProcessorClasses.add(GetLobbyVersionCommandProcessor.class);
		commandProcessorClasses.add(LongTimeToDateCommandProcessor.class);
		commandProcessorClasses.add(GetLastLoginTimeCommandProcessor.class);
		commandProcessorClasses.add(SetChannelKeyCommandProcessor.class);
		commandProcessorClasses.add(ForceLeaveChannelCommandProcessor.class);
		commandProcessorClasses.add(GetSendBufferSizeCommandProcessor.class);
		commandProcessorClasses.add(MemoryAvailableCommandProcessor.class);
		commandProcessorClasses.add(GarbageCollectorCommandProcessor.class);
		commandProcessorClasses.add(AddNotificationCommandProcessor.class);
		commandProcessorClasses.add(TestLoginCommandProcessor.class);
		commandProcessorClasses.add(SetBotModeCommandProcessor.class);
		commandProcessorClasses.add(GetRegistrationDateCommandProcessor.class);
		commandProcessorClasses.add(SetLatestSpringVersionCommandProcessor.class);
		commandProcessorClasses.add(GetUserIdCommandProcessor.class);
		commandProcessorClasses.add(GenerateUserIdCommandProcessor.class);
		commandProcessorClasses.add(KillAllCommandProcessor.class);
		commandProcessorClasses.add(LoginCommandProcessor.class);
		commandProcessorClasses.add(ConfirmAgreementCommandProcessor.class);
		commandProcessorClasses.add(UserIdCommandProcessor.class);
		commandProcessorClasses.add(RenameAccountCommandProcessor.class);
		commandProcessorClasses.add(ChangePasswordCommandProcessor.class);
		commandProcessorClasses.add(JoinCommandProcessor.class);
		commandProcessorClasses.add(LeaveCommandProcessor.class);
		commandProcessorClasses.add(ChannelTopicCommandProcessor.class);
		commandProcessorClasses.add(SayCommandProcessor.class);
		commandProcessorClasses.add(SayExCommandProcessor.class);
		commandProcessorClasses.add(SayPrivateCommandProcessor.class);
		commandProcessorClasses.add(SayBattleCommandProcessor.class);
		commandProcessorClasses.add(SayBattleExCommandProcessor.class);
		commandProcessorClasses.add(JoinBattleCommandProcessor.class);
		commandProcessorClasses.add(JoinBattleAcceptCommandProcessor.class);
		commandProcessorClasses.add(JoinBattleDenyCommandProcessor.class);
		commandProcessorClasses.add(LeaveBattleCommandProcessor.class);
		commandProcessorClasses.add(MyBattleStatusCommandProcessor.class);
		commandProcessorClasses.add(MyStatusCommandProcessor.class);
		commandProcessorClasses.add(UpdateBattleInfoCommandProcessor.class);
		commandProcessorClasses.add(HandicapCommandProcessor.class);
		commandProcessorClasses.add(ForceTeamNumberCommandProcessor.class);
		commandProcessorClasses.add(ForceAllyNumberCommandProcessor.class);
		commandProcessorClasses.add(ForceTeamColorCommandProcessor.class);
		commandProcessorClasses.add(ForceSpectatorModeCommandProcessor.class);
		commandProcessorClasses.add(AddBotCommandProcessor.class);
		commandProcessorClasses.add(RemoveBotCommandProcessor.class);
		commandProcessorClasses.add(UpdateBotCommandProcessor.class);
		commandProcessorClasses.add(DisableUnitsCommandProcessor.class);
		commandProcessorClasses.add(EnableUnitsCommandProcessor.class);
		commandProcessorClasses.add(EnableAllUnitsCommandProcessor.class);
		commandProcessorClasses.add(RingCommandProcessor.class);
		commandProcessorClasses.add(AddStartRectCommandProcessor.class);
		commandProcessorClasses.add(RemoveStartRectCommandProcessor.class);
		commandProcessorClasses.add(ScriptStartCommandProcessor.class);
		commandProcessorClasses.add(ScriptCommandProcessor.class);
		commandProcessorClasses.add(ScriptEndCommandProcessor.class);
		commandProcessorClasses.add(SetScriptTagsCommandProcessor.class);
		commandProcessorClasses.add(RemoveScriptTagsCommandProcessor.class);
		commandProcessorClasses.add(ChannelsCommandProcessor.class);
		commandProcessorClasses.add(ForgeReverseMessageCommandProcessor.class);
		commandProcessorClasses.add(KickFromBattleCommandProcessor.class);
		commandProcessorClasses.add(StopServerCommandProcessor.class);
		commandProcessorClasses.add(ForceJoinBattleCommandProcessor.class);
		commandProcessorClasses.add(ListCompFlagsCommandProcessor.class);
		commandProcessorClasses.add(ConnectUserCommandProcessor.class);

		return commandProcessorClasses;
	}
}
