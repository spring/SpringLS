package com.springrts.tasserver;

/**
 * Contains global instances, unique for per server instance.
 */
public class Context {

	private AccountsService accountsService = null;
	private BanService banService = null;
	private Battles battles = null;
	private Channels channels = null;
	private Clients clients = null;
	private ServerNotifications serverNotifications = null;
	private Statistics statistics = null;


	public AccountsService getAccountsService() {
		return accountsService;
	}

	public void setAccountsService(AccountsService accountsService) {
		this.accountsService = accountsService;
	}

	public BanService getBanService() {
		return banService;
	}

	public void setBanService(BanService banService) {
		this.banService = banService;
	}

	public Battles getBattles() {
		return battles;
	}

	public void setBattles(Battles battles) {
		this.battles = battles;
	}

	public Channels getChannels() {
		return channels;
	}

	public void setChannels(Channels channels) {
		this.channels = channels;
	}

	public Clients getClients() {
		return clients;
	}

	public void setClients(Clients clients) {
		this.clients = clients;
	}

	public ServerNotifications getServerNotifications() {
		return serverNotifications;
	}

	public void setServerNotifications(ServerNotifications serverNotifications) {
		this.serverNotifications = serverNotifications;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public void setStatistics(Statistics statistics) {
		this.statistics = statistics;
	}
}
