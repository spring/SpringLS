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
import com.springrts.tasserver.BanEntry;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.FailedLoginAttempt;
import com.springrts.tasserver.Misc;
import com.springrts.tasserver.ServerNotification;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to login to the lobby.
 * @author hoijui
 */
@SupportedCommand("LOGIN")
public class LoginCommandProcessor extends AbstractCommandProcessor {

	private static final Log s_log  = LogFactory.getLog(LoginCommandProcessor.class);

	/**
	 * For how long (in milli-seconds) to keep failed login attempts recorded.
	 */
	private static final long KEEP_FAILED_LOGIN_ATTEMPT_TIME = 30000;
	/**
	 * In what interval (in milli-seconds) to check for failed login attempts
	 * for whether to purge them.
	 */
	private static final long PURGE_INTERVAL = 1000;

	private Timer failedLoginsPurger;

	/**
	 * Here we store information on latest failed login attempts.
	 * We use it to block users from brute-forcing other accounts.
	 */
	private List<FailedLoginAttempt> failedLoginAttempts = new ArrayList<FailedLoginAttempt>();

	public LoginCommandProcessor() {
		super(5, ARGS_MAX_NOCHECK);

		// TODO cleanup nicely at server shutdown?
		failedLoginsPurger = new Timer("Failed Login Purger");
		failedLoginsPurger.schedule(new PurgeFailedLogins(), PURGE_INTERVAL, PURGE_INTERVAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("DENIED Bad command arguments");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		if (client.getAccount().getAccess() != Account.Access.NONE) {
			client.sendLine("DENIED Already logged in");
			return false; // user with accessLevel > 0 can not re-login
		}

		String username = args.get(0);

		if (!getContext().getServer().isLoginEnabled()
				&& getContext().getAccountsService().getAccount(username).getAccess().isLessThen(Account.Access.PRIVILEGED))
		{
			client.sendLine("DENIED Sorry, logging in is currently disabled");
			return false;
		}

		String args2str = Misc.makeSentence(args, 4);

		String[] args2 = args2str.split("\t");
		String lobbyVersion = args2[0];
		int userID = Account.NO_USER_ID;
		if (args2.length > 1) {
			try {
				long temp = Long.parseLong(args2[1], 16);
				userID = (int) temp; // we transform unsigned 32 bit integer to a signed one
			} catch (NumberFormatException e) {
				client.sendLine("DENIED <userID> field should be an integer");
				return false;
			}
		}
		if (args2.length > 2) {
			// prepare the compatibility flags (space separated)
			String compatFlags_str = Misc.makeSentence(args2, 2);
			String[] compatFlags_split = compatFlags_str.split(" ");
			ArrayList<String> compatFlags = new ArrayList<String>(compatFlags_split.length + 1);
			compatFlags.addAll(Arrays.asList(compatFlags_split));
			// split old flags for backwards compatibility,
			// as there were no spaces in the past
			if (compatFlags.remove("ab") || compatFlags.remove("ba")) {
				compatFlags.add("a");
				compatFlags.add("b");
			}

			// handle flags ...
			client.setAcceptAccountIDs(compatFlags.contains("a"));
			client.setHandleBattleJoinAuthorization(compatFlags.contains("b"));
			client.setScriptPassordSupported(compatFlags.contains("sp"));
		}

		int cpu;
		try {
			cpu = Integer.parseInt(args.get(2));
		} catch (NumberFormatException e) {
			client.sendLine("DENIED <cpu> field should be an integer");
			return false;
		}

		String password = args.get(1);
		if (!getContext().getServer().isLanMode()) { // "normal", non-LAN mode
			// protection from brute-forcing the account:
			FailedLoginAttempt attempt = findFailedLoginAttempt(username);
			if ((attempt != null) && (attempt.getFailedAttempts() >= 3)) {
				client.sendLine("DENIED Too many failed login attempts. Wait for 30 seconds before trying again!");
				recordFailedLoginAttempt(username);
				if (!attempt.isLogged()) {
					attempt.setLogged(true);
					getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG [broadcast to all admins]: Too many failed login attempts for <")
							.append(username).append("> from ")
							.append(client.getIp()).append(". Blocking for 30 seconds. Will not notify any longer.").toString());
					// add server notification:
					ServerNotification sn = new ServerNotification("Excessive failed login attempts");
					sn.addLine(new StringBuilder("Too many failed login attempts for <")
							.append(username).append("> from ")
							.append(client.getIp()).append(". Blocking for 30 seconds.").toString());
					getContext().getServerNotifications().addNotification(sn);
				}
				return false;
			}

			Account acc = getContext().getAccountsService().verifyLogin(username, password);
			if (acc == null) {
				client.sendLine("DENIED Bad username/password");
				recordFailedLoginAttempt(username);
				return false;
			}
			if (getContext().getClients().isUserLoggedIn(acc)) {
				client.sendLine("DENIED Already logged in");
				return false;
			}
			BanEntry ban = getContext().getBanService().getBanEntry(username, Misc.ip2Long(client.getIp()), userID);
			if (ban != null && ban.isActive()) {
				client.sendLine(new StringBuilder("DENIED You are banned from this server! (Reason: ")
						.append(ban.getPublicReason()).append("). Please contact server administrator.").toString());
				recordFailedLoginAttempt(username);
				return false;
			}
			if ((!acc.isAgreementAccepted()) && (!client.getAccount().isAgreementAccepted())
					&& getContext().getAgreement().isSet()) {
				getContext().getAgreement().sendToClient(client);
				return false;
			}
			// everything is OK so far!
			if (!acc.isAgreementAccepted()) {
				// user has obviously accepted the agreement... Let's update it
				acc.setAgreementAccepted(true);
				final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges(acc, acc.getName());
				if (!mergeOk) {
					acc.setAgreementAccepted(false);
					client.sendLine("DENIED Failed saving 'agreement accepted' to persistent storage.");
					return false;
				}
				getContext().getAccountsService().saveAccounts(false);
			}
			if (acc.getLastLogin() + 5000 > System.currentTimeMillis()) {
				client.sendLine("DENIED This account has already connected in the last 5 seconds");
				return false;
			}
			client.setAccount(acc);
		} else { // lanMode == true
			if (username.equals("")) {
				client.sendLine("DENIED Cannot login with null username");
			}
			Account acc = getContext().getAccountsService().getAccount(username);
			if (acc != null) {
				client.sendLine("DENIED Player with same name already logged in");
				return false;
			}
			Account.Access accessLvl = Account.Access.NORMAL;
			if ((username.equals(getContext().getServer().getLanAdminUsername())) && (password.equals(getContext().getServer().getLanAdminPassword()))) {
				accessLvl = Account.Access.ADMIN;
			}
			acc = new Account(username, password, "?", "XX");
			acc.setAccess(accessLvl);
			getContext().getAccountsService().addAccount(acc);
			client.setAccount(acc);
		}

		// set client's status:
		client.setRankToStatus(client.getAccount().getRank().ordinal());
		client.setBotModeToStatus(client.getAccount().isBot());
		client.setAccessToStatus(((client.getAccount().getAccess().isLessThen(Account.Access.PRIVILEGED)
				&& (!getContext().getServer().isLanMode())) ? true : false));

		client.setCpu(cpu);
		client.getAccount().setLastLogin(System.currentTimeMillis());
		client.getAccount().setLastCountry(client.getCountry());
		client.getAccount().setLastIP(client.getIp());
		String ip = args.get(3);
		if (ip.equals("*")) {
			client.setLocalIP(client.getIp());
		} else {
			client.setLocalIP(ip);
		}
		client.setLobbyVersion(lobbyVersion);
		client.getAccount().setLastUserId(userID);
		final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges(client.getAccount(), client.getAccount().getName());
		if (!mergeOk) {
			s_log.info("Failed saving login info to persistent storage for user: " + client.getAccount().getName());
			return false;
		}

		// do the notifying and all:
		client.sendLine("ACCEPTED " + client.getAccount().getName());
		getContext().getMessageOfTheDay().sendTo(client);
		getContext().getClients().sendListOfAllUsersToClient(client);
		getContext().getBattles().sendInfoOnBattlesToClient(client);
		getContext().getClients().sendInfoOnStatusesToClient(client);
		// notify client that we've finished sending login info:
		client.sendLine("LOGININFOEND");

		// notify everyone about new client:
		getContext().getClients().notifyClientsOfNewClientOnServer(client);
		getContext().getClients().notifyClientsOfNewClientStatus(client);

		if (s_log.isDebugEnabled()) {
			s_log.debug("User just logged in: " + client.getAccount().getName());
		}

		return true;
	}

	private void recordFailedLoginAttempt(String username) {
		FailedLoginAttempt attempt = findFailedLoginAttempt(username);
		if (attempt == null) {
			attempt = new FailedLoginAttempt(username, 0, 0);
			failedLoginAttempts.add(attempt);
		}
		attempt.addFailedAttempt();
	}

	/** @return 'null' if no record found */
	private FailedLoginAttempt findFailedLoginAttempt(String username) {
		for (int i = 0; i < failedLoginAttempts.size(); i++) {
			if (failedLoginAttempts.get(i).getUserName().equals(username)) {
				return failedLoginAttempts.get(i);
			}
		}
		return null;
	}

	private class PurgeFailedLogins extends TimerTask {

		@Override
		public void run() {

			// purge list of failed login attempts:
			for (int i = 0; i < failedLoginAttempts.size(); i++) {
				FailedLoginAttempt attempt = failedLoginAttempts.get(i);
				if ((System.currentTimeMillis() - attempt.getTimeOfLastFailedAttempt())
						> KEEP_FAILED_LOGIN_ATTEMPT_TIME)
				{
					failedLoginAttempts.remove(i);
					i--;
				}
			}
		}
	}
}
