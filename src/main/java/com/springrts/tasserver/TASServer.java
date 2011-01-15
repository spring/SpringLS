/*
 * Created on 2005.6.16
 */

package com.springrts.tasserver;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


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
	private static final Log s_log  = LogFactory.getLog(TASServer.class);

	public TASServer(Context context) {

		AccountsService accountsService = null;
		if (!context.getServer().isLanMode() && context.getServer().isUseUserDB()) {
			accountsService = new JPAAccountsService();
		} else {
			accountsService = new FSAccountsService();
		}
		context.setAccountsService(accountsService);

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
		context.setBanService(banService);

		context.push();

		context.getCommandProcessors().init();

		// switch to LAN mode if user accounts information is not present:
		if (!context.getServer().isLanMode()) {
			if (!context.getAccountsService().isReadyToOperate()) {
				s_log.warn("Accounts service not ready, switching to \"LAN mode\" ...");
				context.getServer().setLanMode(true);
			}
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
			s_log.info(new StringBuilder("\"Update properties\" read from ").append(UpdateProperties.DEFAULT_FILENAME).toString());
		}

		long tempTime = System.currentTimeMillis();
		if (IP2Country.getInstance().initializeAll()) {
			tempTime = System.currentTimeMillis() - tempTime;
			s_log.info(new StringBuilder("<IP2Country> loaded in ")
					.append(tempTime).append(" ms.").toString());
		}

		// start "help UDP" server:
		context.getNatHelpServer().startServer();

		// start server:
		if (!context.getServerThread().startServer()) {
			context.getServerThread().closeServerAndExit();
		}

		// add server notification:
		ServerNotification sn = new ServerNotification("Server started");
		sn.addLine(new StringBuilder("Server has been started on port ")
				.append(context.getServer().getPort()).append(". There are ")
				.append(context.getAccountsService().getAccountsSize())
				.append(" accounts currently loaded. See server log for more info.").toString());
		context.getServerNotifications().addNotification(sn);

		context.getServerThread().run();
	}
}
