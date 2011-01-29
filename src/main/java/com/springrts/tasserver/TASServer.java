/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public class TASServer {

	private static final Logger LOG = LoggerFactory.getLogger(TASServer.class);


	private TASServer() {}


	public static void startServerInstance(Context context) {

		context.setAccountsService(createAccountsService(context));

		context.setBanService(createBanService(context));

		context.push();

		// switch to LAN mode if user accounts information is not present
		if (!context.getAccountsService().isReadyToOperate()) {
			assert(context.getServer().isLanMode());
			LOG.warn("Accounts service not ready, switching to \"LAN mode\" ...");
			context.getServer().setLanMode(true);
			context.setAccountsService(createAccountsService(context));
		}

		context.getAccountsService().loadAccounts();

		// TODO needs adjusting due to new LAN mode accounts service
		if (!context.getServer().isLanMode()) {
			context.getAgreement().read();
		} else {
			LOG.info("LAN mode enabled");
		}

		context.getMessageOfTheDay().read();
		context.getServer().setStartTime(System.currentTimeMillis());

		if (context.getUpdateProperties().read(UpdateProperties.DEFAULT_FILENAME)) {
			LOG.info("\"Update properties\" read from {}", UpdateProperties.DEFAULT_FILENAME);
		}

		long tempTime = System.currentTimeMillis();
		if (IP2Country.getInstance().initializeAll()) {
			tempTime = System.currentTimeMillis() - tempTime;
			LOG.info("<IP2Country> loaded in {} ms.", tempTime);
		}

		// start "help UDP" server:
		context.getNatHelpServer().startServer();

		// start server:
		if (!context.getServerThread().startServer()) {
			context.getServerThread().closeServerAndExit();
		}

		context.getCommandProcessors().init();

		context.getServerThread().run();
	}

	private static AccountsService createAccountsService(Context context) {

		AccountsService accountsService = null;

		if (context.getServer().isLanMode()) {
			accountsService = new LanAccountsService();
		} else if(context.getServer().isUseUserDB()) {
			accountsService = new JPAAccountsService();
		} else {
			accountsService = new FSAccountsService();
		}

		return accountsService;
	}

	private static BanService createBanService(Context context) {

		BanService banService = null;

		if (!context.getServer().isLanMode()) {
			try {
				banService = new JPABanService();
			} catch (Exception pex) {
				LOG.warn("Failed to access database for ban entries, bans are not supported!", pex);
			}
		}

		if (banService == null) {
			banService = new DummyBanService();
		}

		return banService;
	}
}
