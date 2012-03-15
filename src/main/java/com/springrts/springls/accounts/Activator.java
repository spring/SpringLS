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

package com.springrts.springls.accounts;


import com.springrts.springls.Context;
import com.springrts.springls.ServerConfiguration;
import com.springrts.springls.Updateable;
import org.apache.commons.configuration.Configuration;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple bundle activator for the
 * <tt>AccountsService</tt> and its default implementations.
 */
public class Activator implements BundleActivator {

	private final Logger log = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(BundleContext context) {

		Context springLsContext = Context.getService(context, Context.class);

		AccountsService accounts = createAccountsService(springLsContext);

		// switch to LAN mode if user accounts information is not present
		if (!accounts.isReadyToOperate()) {
			Configuration conf = springLsContext.getService(Configuration.class);
			assert(conf.getBoolean(ServerConfiguration.LAN_MODE));
			log.error("Accounts service is not ready, shutting down the server");
			springLsContext.getServerThread().closeServerAndExit();
		}

		accounts.receiveContext(springLsContext);
		accounts.loadAccounts();

		springLsContext.setAccountsService(accounts);
		context.registerService(new String[] {
				AccountsService.class.getName(),
				Updateable.class.getName()
				},
				accounts, null);
	}

	private static AccountsService createAccountsService(Context context) {

		AccountsService accountsService = null;

		Configuration conf = context.getService(Configuration.class);
		boolean lanMode = conf.getBoolean(ServerConfiguration.LAN_MODE);
		boolean useDatabase = conf.getBoolean(ServerConfiguration.USE_DATABASE);

		if (lanMode) {
			accountsService = new LanAccountsService();
		} else if (useDatabase) {
			accountsService = new JPAAccountsService();
		} else {
			accountsService = new FSAccountsService();
		}

		return accountsService;
	}

	@Override
	public void stop(BundleContext context) {
	}
}
