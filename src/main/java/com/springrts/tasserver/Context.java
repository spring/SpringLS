package com.springrts.tasserver;


import java.util.LinkedList;
import java.util.List;

/**
 * Contains global instances, unique for per server instance.
 */
public class Context implements LiveStateListener {

	private final List<ContextReceiver> contextReceivers;
	private final List<LiveStateListener> liveStateListeners;

	private AccountsService accountsService = null;
	private BanService banService = null;
	private Battles battles = null;
	private Channels channels = null;
	private Clients clients = null;
	private Engine engine = null;
	private Server server = null;
	private ServerNotifications serverNotifications = null;
	private Statistics statistics = null;
	private NatHelpServer natHelpServer = null;
	private MessageOfTheDay messageOfTheDay = null;


	public Context() {

		this.contextReceivers = new LinkedList<ContextReceiver>();
		this.liveStateListeners = new LinkedList<LiveStateListener>();

		this.accountsService = null;
		this.banService = null;
		this.accountsService = null;
		this.banService = null;
		this.battles = null;
		this.channels = null;
		this.clients = null;
		this.engine = null;
		this.server = null;
		this.serverNotifications = null;
		this.statistics = null;
		this.natHelpServer = null;
	}

	public void init() {

		this.accountsService = null;
		this.banService = null;
		setBattles(new Battles());
		setChannels(new Channels());
		setClients(new Clients());
		setEngine(new Engine());
		setServer(new Server());
		setServerNotifications(new ServerNotifications());
		setStatistics(new Statistics());
		setNatHelpServer(new NatHelpServer());
		setMessageOfTheDay(new MessageOfTheDay());
	}


	public void push() {

		for (ContextReceiver contextReceiver : contextReceivers) {
			contextReceiver.receiveContext(this);
		}
	}

	public void addContextReceiver(ContextReceiver contextReceiver) {

		if (!contextReceivers.contains(contextReceiver)) {
			contextReceivers.add(contextReceiver);
		}
	}

	public void addLiveStateListener(LiveStateListener liveStateListener) {

		if (!liveStateListeners.contains(liveStateListener)) {
			liveStateListeners.add(liveStateListener);
		}
	}

	@Override
	public void starting() {

		for (LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.starting();
		}
	}
	@Override
	public void started() {

		for (LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.started();
		}
	}

	@Override
	public void stopping() {

		for (LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.stopping();
		}
	}
	@Override
	public void stopped() {

		for (LiveStateListener liveStateListener : liveStateListeners) {
			liveStateListener.stopped();
		}
	}

	public AccountsService getAccountsService() {
		return accountsService;
	}

	public void setAccountsService(AccountsService accountsService) {

		this.accountsService = accountsService;
		addContextReceiver(accountsService);
		addLiveStateListener(accountsService);
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
		addContextReceiver(battles);
	}

	public Channels getChannels() {
		return channels;
	}

	public void setChannels(Channels channels) {

		this.channels = channels;
		addContextReceiver(channels);
		addLiveStateListener(channels);
	}

	public Clients getClients() {
		return clients;
	}

	public void setClients(Clients clients) {

		this.clients = clients;
		addContextReceiver(clients);
	}

	public Engine getEngine() {
		return engine;
	}

	public void setEngine(Engine engine) {

		this.engine = engine;
	}

	public ServerNotifications getServerNotifications() {
		return serverNotifications;
	}

	public void setServerNotifications(ServerNotifications serverNotifications) {

		this.serverNotifications = serverNotifications;
		addContextReceiver(serverNotifications);
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public void setStatistics(Statistics statistics) {

		this.statistics = statistics;
		addContextReceiver(statistics);
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {

		this.server = server;
		addContextReceiver(server);
	}

	public NatHelpServer getNatHelpServer() {
		return natHelpServer;
	}

	public void setNatHelpServer(NatHelpServer natHelpServer) {

		this.natHelpServer = natHelpServer;
		addLiveStateListener(natHelpServer);
	}

	public MessageOfTheDay getMessageOfTheDay() {
		return messageOfTheDay;
	}

	public void setMessageOfTheDay(MessageOfTheDay messageOfTheDay) {

		this.messageOfTheDay = messageOfTheDay;
		addContextReceiver(messageOfTheDay);
	}
}
