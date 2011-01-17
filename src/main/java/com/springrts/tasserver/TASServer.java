/*
 * Created on 2005.6.16
 */

package com.springrts.tasserver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 */
public class TASServer {

	/**
	 * If true, the server will keep a log of all conversations from
	 * the channel #main (in file "MainChanLog.log")
	 */
	//boolean logMainChannel = false;
	//private List<String> whiteList = new LinkedList<String>();
	private static final Logger s_log  = LoggerFactory.getLogger(TASServer.class);

	public TASServer(Context context) {

		context.setAccountsService(createAccountsService(context));

		context.setBanService(createBanService(context));

		context.push();

		context.getCommandProcessors().init();

		// switch to LAN mode if user accounts information is not present:
		if (!context.getServer().isLanMode()
				&& !context.getAccountsService().isReadyToOperate())
		{
			s_log.warn("Accounts service not ready, switching to \"LAN mode\" ...");
			context.getServer().setLanMode(true);
		}

		if (!context.getServer().isLanMode()) {
			context.getAccountsService().loadAccounts();
			context.getBanService();
			context.getAgreement().read();
		} else {
			s_log.info("LAN mode enabled");
		}

		context.getMessageOfTheDay().read();
		context.getServer().setStartTime(System.currentTimeMillis());

		if (context.getUpdateProperties().read(UpdateProperties.DEFAULT_FILENAME)) {
			s_log.info("\"Update properties\" read from {}", UpdateProperties.DEFAULT_FILENAME);
		}

		long tempTime = System.currentTimeMillis();
		if (IP2Country.getInstance().initializeAll()) {
			tempTime = System.currentTimeMillis() - tempTime;
			s_log.info("<IP2Country> loaded in {} ms.", tempTime);
		}

		// start "help UDP" server:
		context.getNatHelpServer().startServer();

		// start server:
		if (!context.getServerThread().startServer()) {
			context.getServerThread().closeServerAndExit();
		}

		context.getServerThread().run();
	}

	private AccountsService createAccountsService(Context context) {

		AccountsService accountsService = null;

		if (!context.getServer().isLanMode() && context.getServer().isUseUserDB()) {
			accountsService = new JPAAccountsService();
		} else {
			accountsService = new FSAccountsService();
		}

		return accountsService;
	}

	private BanService createBanService(Context context) {

		BanService banService = null;

		if (!context.getServer().isLanMode()) {
			try {
				banService = new JPABanService();
			} catch (Exception pex) {
				banService = new DummyBanService();
				s_log.warn("Failed to access database for ban entries, bans are not supported!", pex);
			}
		} else {
			banService = new DummyBanService();
		}

		return banService;
	}
}
