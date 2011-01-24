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

package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.InvalidNumberOfArgumentsCommandProcessingException;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("REGISTER")
public class RegisterCommandProcessor extends AbstractCommandProcessor {

	public RegisterCommandProcessor() {
		super(2, 2, Account.Access.NONE);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = false;
		try {
			checksOk = super.process(client, args);
		} catch (InvalidNumberOfArgumentsCommandProcessingException ex) {
			client.sendLine("REGISTRATIONDENIED Bad command arguments");
			throw ex;
		}
		if (!checksOk) {
			return false;
		}

		if (!getContext().getAccountsService().isRegistrationEnabled()) {
			client.sendLine("REGISTRATIONDENIED Sorry, account registration is currently disabled");
			return false;
		}

		if (client.getAccount().getAccess() != Account.Access.NONE) { // only clients which aren't logged-in can register
			client.sendLine("REGISTRATIONDENIED You are already logged-in, no need to register new account");
			return false;
		}

		if (getContext().getServer().isLanMode()) { // no need to register account in LAN mode since it accepts any userName
			client.sendLine("REGISTRATIONDENIED Can't register in LAN-mode. Login with any username and password to proceed");
			return false;
		}

		String username = args.get(0);
		String password = args.get(1);

		// validate userName:
		String valid = Account.isOldUsernameValid(username);
		if (valid != null) {
			client.sendLine(new StringBuilder("REGISTRATIONDENIED Invalid username (reason: ")
					.append(valid).append(")").toString());
			return false;
		}

		// validate password:
		valid = Account.isPasswordValid(password);
		if (valid != null) {
			client.sendLine(new StringBuilder("REGISTRATIONDENIED Invalid password (reason: ")
					.append(valid).append(")").toString());
			return false;
		}
		Account acc = getContext().getAccountsService().findAccountNoCase(username);
		if (acc != null) {
			client.sendLine("REGISTRATIONDENIED Account already exists");
			return false;
		}

		// check for reserved names:
		if (Account.RESERVED_NAMES.contains(username)) {
			client.sendLine("REGISTRATIONDENIED Invalid account name - you are trying to register a reserved account name");
				return false;
		}
		/*if (!getContext().whiteList.contains(client.getIp())) {
			if (registrationTimes.containsKey(client.ip)
			&& (int)(registrationTimes.get(client.ip)) + 3600 > (System.currentTimeMillis()/1000)) {
			client.sendLine("REGISTRATIONDENIED This ip has already registered an account recently");
			context.getClients().sendToAllAdministrators("SERVERMSG Client at " + client.ip + "'s registration of " + username + " was blocked due to register spam");
			return false;
			}
			registrationTimes.put(client.ip, (int)(System.currentTimeMillis()/1000));*/
			/*String proxyDNS = "dnsbl.dronebl.org"; //Bot checks this with the broadcast, no waiting for a response
			String[] ipChunks = client.ip.split("\\.");
			for (int i = 0; i < 4; i++) {
			proxyDNS = ipChunks[i] + "." + proxyDNS;
			}
			try {
			InetAddress.getByName(proxyDNS);
			client.sendLine("REGISTRATIONDENIED Using a known proxy ip");
			context.getClients().sendToAllAdministrators("SERVERMSG Client at " + client.ip + "'s registration of " + username + " was blocked as it is a proxy ip");
			return false;
			} catch (UnknownHostException e) {
			}
		}*/
		getContext().getClients().sendToAllAdministrators(new StringBuilder("SERVERMSG New registration of <")
				.append(username).append("> at ")
				.append(client.getIp().getHostAddress()).toString());
		acc = new Account(username, password, client.getIp(),
				client.getCountry());
		getContext().getAccountsService().addAccount(acc);

		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);

		client.sendLine("REGISTRATIONACCEPTED");
		return true;
	}
}
