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


import com.springrts.springls.Account;
import com.springrts.springls.Battle;
import com.springrts.springls.Client;
import com.springrts.springls.Context;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.osgi.framework.BundleContext;

/**
 * Utility base class for command processors.
 * @author hoijui
 */
public abstract class AbstractCommandProcessor implements CommandProcessor {

	public static final int ARGS_MIN_NOCHECK = -1;
	public static final int ARGS_MAX_NOCHECK = -1;
	private static final Account.Access ACCESS_NOCHECK = null;
	private Context context = null;
	private final String commandName;
	private final int argsMin;
	private final int argsMax;
	private final Account.Access accessMin;
	private final boolean battleRequired;
	private final boolean battleFounderRequired;

	protected AbstractCommandProcessor(int argsMin, int argsMax,
			Account.Access accessMin, boolean battleRequired,
			boolean battleFounderRequired)
	{
		this.commandName
				= CommandProcessors.extractCommandName(this.getClass());
		this.argsMin = argsMin;
		this.argsMax = argsMax;
		this.accessMin = accessMin;
		this.battleRequired = battleRequired;
		this.battleFounderRequired = battleFounderRequired;
		if (battleFounderRequired && !battleRequired) {
			throw new IllegalArgumentException("The client can never be founder"
					+ " of a battle but not in a battle");
		}
	}
	protected AbstractCommandProcessor(int argsMin, int argsMax,
			Account.Access accessMin, boolean battleRequired)
	{
		this(argsMin, argsMax, accessMin, battleRequired, false);
	}
	protected AbstractCommandProcessor(int argsMin, int argsMax,
			Account.Access accessMin)
	{
		this(argsMin, argsMax, accessMin, false);
	}
	protected AbstractCommandProcessor(Account.Access accessMin,
			boolean battleRequired)
	{
		this(ARGS_MIN_NOCHECK, ARGS_MAX_NOCHECK, accessMin, battleRequired);
	}
	protected AbstractCommandProcessor(int argsMin, int argsMax) {
		this(argsMin, argsMax, ACCESS_NOCHECK);
	}
	protected AbstractCommandProcessor(Account.Access accessMin) {
		this(ARGS_MIN_NOCHECK, ARGS_MAX_NOCHECK, accessMin);
	}
	protected AbstractCommandProcessor() {
		this(ARGS_MIN_NOCHECK, ARGS_MAX_NOCHECK);
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}
	protected Context getContext() {
		return context;
	}
	protected Configuration getConfiguration() {
		return context.getService(Configuration.class);
	}
	protected BundleContext getBundleContext() {
		return context.getFramework().getBundleContext();
	}
	public <T> T getService(Class<T> serviceClass) {
		return Context.getService(getBundleContext(), serviceClass);
	}

	/**
	 * Returns the name of the command supported by this processor.
	 * @see SupportedCommand
	 */
	public String getCommandName() {
		return this.commandName;
	}

	/**
	 * Returns the command as it was given to the server.
	 * @return command-name + " " + arg0 + " " + arg1 ...
	 */
	public String reconstructFullCommand(List<String> args) {

		StringBuilder fullCommand = new StringBuilder(getCommandName());

		for (String arg : args) {
			fullCommand.append(" ").append(arg);
		}

		return fullCommand.toString();
	}

	/** Returns the minimum number of arguments supported by the command */
	public int getArgsMin() {
		return this.argsMin;
	}
	/** Returns the maximum number of arguments supported by the command */
	public int getArgsMax() {
		return this.argsMax;
	}

	/**
	 * Returns the minimum access right required to execute the command.
	 * @return minimum access right required to execute, or <code>null</code>.
	 *   if no check should be performed.
	 */
	public Account.Access getAccessMin() {
		return this.accessMin;
	}

	/**
	 * Returns whether this command requires the sending client to be in a
	 * battle.
	 * @return true if the sending client is required to be in a battle.
	 */
	public boolean isBattleRequired() {
		return this.battleRequired;
	}

	/**
	 * Returns whether this command requires the sending client to be the
	 * founder of a battle.
	 * @return true if the sending client is requires to be the founder of a
	 *   battle.
	 */
	public boolean isBattleFounderRequired() {
		return this.battleFounderRequired;
	}

	/**
	 * Returns whether the client is currently in a battle.
	 * @return true if the client is currently in a battle.
	 */
	protected Battle getBattle(Client client) {
		return getContext().getBattles().getBattleByID(client.getBattleID());
	}

	/**
	 * Returns whether the client is currently in a battle.
	 * @return true if the client is currently in a battle.
	 */
	protected boolean isInBattle(Client client) {
		return (getBattle(client) != null);
	}

	/**
	 * Returns whether the client is currently founder of a battle.
	 * @return true if the client is currently founder of a battle.
	 */
	protected boolean isBattleFounder(Client client) {

		Battle battle = getContext().getBattles()
					.getBattleByID(client.getBattleID());
		return ((battle != null) && (battle.getFounder() == client));
	}

	/**
	 * Perform common checks.
	 */
	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{

		if ((getAccessMin() != ACCESS_NOCHECK)
				&& client.getAccount().getAccess().isLessThen(getAccessMin()))
		{
			throw new InsufficientAccessCommandProcessingException(
					getCommandName(),
					getAccessMin(),
					client.getAccount().getAccess());
		}

		if ((getArgsMin() != ARGS_MIN_NOCHECK)
				&& (args.size() < getArgsMin()))
		{
			throw new TooFewArgumentsCommandProcessingException(
					getCommandName(),
					getArgsMin(),
					args.size());
		}
		if ((getArgsMax() != ARGS_MAX_NOCHECK)
				&& (args.size() > getArgsMax()))
		{
			throw new TooManyArgumentsCommandProcessingException(
					getCommandName(),
					getArgsMax(),
					args.size());
		}

		if (isBattleFounderRequired() && !isBattleFounder(client)) {
			throw new CommandProcessingException(getCommandName(),
					"This command requires the issuing client to be founder of"
					+ " the battle");
		} else if (isBattleRequired() && !isInBattle(client)) {
			throw new CommandProcessingException(getCommandName(),
					"This command requires the issuing client to in a battle");
		}

		return true;
	}
}
