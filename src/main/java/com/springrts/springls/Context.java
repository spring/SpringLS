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

package com.springrts.springls;


import com.springrts.springls.accounts.AccountsService;
import com.springrts.springls.bans.BanService;
import com.springrts.springls.commands.CommandProcessors;
import java.util.LinkedList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

/**
 * Contains global instances, unique for per server instance.
 * @author hoijui
 */
public class Context implements LiveStateListener {

	private final List<ContextReceiver> contextReceivers;
	private final List<LiveStateListener> liveStateListeners;

	private Framework framework;
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
	private Agreement agreement = null;
	private CommandProcessors commandProcessors = null;
	private ServerThread serverThread = null;
	private FloodProtection floodProtection = null;


	public Context() {

		this.contextReceivers = new LinkedList<ContextReceiver>();
		this.liveStateListeners = new LinkedList<LiveStateListener>();

		this.framework = null;
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
		this.messageOfTheDay = null;
		this.agreement = null;
		this.commandProcessors = null;
		this.serverThread = null;
		this.floodProtection = null;
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
		setAgreement(new Agreement());
		setCommandProcessors(new CommandProcessors());
		setServerThread(new ServerThread());
		setFloodProtection(new FloodProtection());
	}


	public static <T> T getService(BundleContext bundleContext, Class<T> serviceClass) {

		T service = null;

		ServiceReference serviceReference
				= bundleContext.getServiceReference(serviceClass.getName());

		if (serviceReference != null) {
			service = (T) bundleContext.getService(serviceReference);
		}

		return service;
	}

	public <T> T getService(Class<T> serviceClass) {
		return getService(getFramework().getBundleContext(), serviceClass);
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

	public Framework getFramework() {
		return framework;
	}

	public void setFramework(Framework framework) {
		this.framework = framework;
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

	public void setServerNotifications(
			ServerNotifications serverNotifications)
	{
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
		addContextReceiver(natHelpServer);
		addLiveStateListener(natHelpServer);
	}

	public MessageOfTheDay getMessageOfTheDay() {
		return messageOfTheDay;
	}

	public void setMessageOfTheDay(MessageOfTheDay messageOfTheDay) {

		this.messageOfTheDay = messageOfTheDay;
		addContextReceiver(messageOfTheDay);
	}

	public Agreement getAgreement() {
		return agreement;
	}

	public void setAgreement(Agreement agreement) {

		this.agreement = agreement;
		addContextReceiver(agreement);
	}

	public CommandProcessors getCommandProcessors() {
		return commandProcessors;
	}

	public void setCommandProcessors(CommandProcessors commandProcessors) {

		this.commandProcessors = commandProcessors;
		addContextReceiver(commandProcessors);
	}

	public ServerThread getServerThread() {
		return serverThread;
	}

	public void setServerThread(ServerThread serverThread) {

		this.serverThread = serverThread;
		addContextReceiver(serverThread);
		addLiveStateListener(serverThread);
	}

	/**
	 * Returns the flood-protection system.
	 */
	public FloodProtection getFloodProtection() {
		return floodProtection;
	}

	public void setFloodProtection(FloodProtection floodProtection) {

		this.floodProtection = floodProtection;
	}
}
