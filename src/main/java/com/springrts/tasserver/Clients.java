/*
 * Created on 2006.11.2
 */

package com.springrts.tasserver;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 */
public class Clients implements ContextReceiver, Updateable {

	private static final Logger LOG  = LoggerFactory.getLogger(Clients.class);

	private static class KillJob {

		private Client client;
		private String reason;

		KillJob(Client client, String reason) {

			this.client = client;
			this.reason = reason;
		}

		public Client getClient() {
			return client;
		}

		public String getReason() {
			return reason;
		}
	}

	private List<Client> clients = new ArrayList<Client>();
	/**
	 * A list of clients waiting to be killed/disconnected.
	 * This is used when we want to kill a client but not immediately,
	 * within a loop, for example.
	 * Clients on the list will get killed at the start of a main server loop
	 * iteration.
	 * Any redundant entries will be removed, so a client will be killed only
	 * once; no additional logic for consistency is required.
	 * @see killClientDelayed(Client)
	 */
	private List<KillJob> delayedKills = new ArrayList<KillJob>();

	/**
	 * Here we keep a list of clients who have their send queues not empty.
	 * This collection is not synchronized!
	 * Use <code>Collections.synchronizedList(List)</code> to wrap it,
	 * if synchronized access is needed.
	 * @see http://java.sun.com/j2se/1.5.0/docs/api/java/util/Collections.html#synchronizedList(java.util.List))
	 */
	private Queue<Client> sendQueue = new LinkedList<Client>();

	/** in milli-seconds */
	private static final int TIMEOUT_CHECK = 5000;
	/**
	 * Time ({@link java.lang.System#currentTimeMillis()}) when we last checked
	 * for timeouts from clients.
	 */
	private long lastTimeoutCheck;

	private Context context = null;


	public Clients() {
		lastTimeoutCheck = System.currentTimeMillis();
	}

	@Override
	public void receiveContext(Context context) {

		this.context = context;
		for (Client client : clients) {
			client.receiveContext(context);
		}
	}
	private Context getContext() {
		return context;
	}

	@Override
	public void update() {

		flushData();

		resetReceivedBytesCounts();

		checkForTimeouts();

		processKillList();
	}

	private void resetReceivedBytesCounts() {

		if (getContext().getFloodProtection().hasFloodCheckPeriodPassed()) {
			for (int i = 0; i < getClientsSize(); i++) {
				getClient(i).resetDataOverLastTimePeriod();
			}
		}
	}

	private void checkForTimeouts() {

		for (Client client : getTimedOutClients()) {
			if (client.isHalfDead()) {
				continue; // already scheduled for kill
			}
			LOG.warn("Timeout detected from {} ({}). "
					+ "Client has been scheduled for kill ...",
					client.getAccount().getName(),
					client.getIp());
			getContext().getClients().killClientDelayed(client, "Quit: timeout");
		}
	}

	/**
	 * Checks if the time-out check period has passed already,
	 * and if so, resets the last-check-time.
	 * @return true if the time-out check period has passed since
	 *         the last successful call to this method
	 */
	public Collection<Client> getTimedOutClients() {

		Collection<Client> timedOutClients = new LinkedList<Client>();

		boolean timeOut = ((System.currentTimeMillis() - lastTimeoutCheck) > TIMEOUT_CHECK);

		if (timeOut) {
			lastTimeoutCheck = System.currentTimeMillis();
			long now = System.currentTimeMillis();
			long timeoutLength = getContext().getServer().getTimeoutLength();
			for (int i = 0; i < getClientsSize(); i++) {
				Client client = getClient(i);
				if ((now - client.getTimeOfLastReceive()) > timeoutLength) {
					timedOutClients.add(client);
				}
			}
		}

		return timedOutClients;
	}

	/**
	 * Will create new <code>Client</code> object, add it to the 'clients' list
	 * and register its socket channel with 'readSelector'.
	 * @param sendBufferSize specifies the sockets send buffer size.
	 */
	public Client addNewClient(SocketChannel chan, Selector readSelector, int sendBufferSize) {

		Client client = new Client(chan);
		client.receiveContext(context);
		clients.add(client);

		// register the channel with the selector
		// store a new Client as the Key's attachment
		try {
			chan.configureBlocking(false);
			chan.socket().setSendBufferSize(sendBufferSize);
			// TODO this doesn't seem to have an effect with java.nio
			//chan.socket().setSoTimeout(TIMEOUT_LENGTH);
			client.setSelKey(chan.register(readSelector, SelectionKey.OP_READ, client));
		} catch (IOException ioex) {
			LOG.warn("Failed to establish a connection with a client", ioex);
			killClient(client, "Failed to establish a connection");
			return null;
		}

		return client;
	}

	/**
	 * Returns number of clients.
	 * Includes those who have not logged in yet.
	 */
	public int getClientsSize() {
		return clients.size();
	}

	public Client getClient(String username) {

		Client theClient = null;

		for (int i = 0; i < clients.size(); i++) {
			Client toCheck = clients.get(i);
			if (toCheck.getAccount().getName().equals(username)) {
				theClient = toCheck;
				break;
			}
		}

		return theClient;
	}
	public Client getClient(Account account) {
		return getClient(account.getName());
	}

	/** Returns null if index is out of bounds */
	public Client getClient(int index) {

		try {
			return clients.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** Returns true if user is logged in */
	public boolean isUserLoggedIn(Account acc) {
		return (getClient(acc) != null);
	}

	public void sendToAllRegisteredUsers(String s) {

		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if (toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL)) {
				toBeNotified.sendLine(s);
			}
		}
	}

	/** Sends text to all registered users except for the client */
	public void sendToAllRegisteredUsersExcept(Client client, String s) {

		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if ((toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL))
					&& (toBeNotified != client))
			{
				continue;
			}
			toBeNotified.sendLine(s);
		}
	}

	public void sendToAllAdministrators(String s) {

		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if (toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.ADMIN)) {
				toBeNotified.sendLine(s);
			}
		}
	}

	/**
	 * Notifies client of all statuses, including his own
	 * (but only if they are different from 0)
	 */
	public void sendInfoOnStatusesToClient(Client client) {

		client.beginFastWrite();
		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if ((toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL))
				&& toBeNotified.getStatus() != 0)
			{
				// only send it if not 0.
				// The user assumes that every new user's status is 0,
				// so we don't need to tell him that explicitly.
				client.sendLine(String.format("CLIENTSTATUS %s %d",
						toBeNotified.getAccount().getName(),
						toBeNotified.getStatus()));
			}
		}
		client.endFastWrite();
	}

	/**
	 * Notifies all logged-in clients (including this client)
	 * of the client's new status
	 */
	public void notifyClientsOfNewClientStatus(Client client) {

		sendToAllRegisteredUsers(String.format("CLIENTSTATUS %s %d",
				client.getAccount().getName(),
				client.getStatus()));
	}

	/**
	 * Sends a list of all users connected to the server to a client.
	 * This list includes the client itself, assuming he is already logged in
	 * and in the list.
	 */
	public void sendListOfAllUsersToClient(Client client) {

		client.beginFastWrite();
		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if (toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL)) {
				if (client.isAcceptAccountIDs()) {
					client.sendLine(new StringBuilder("ADDUSER ")
							.append(toBeNotified.getAccount().getName()).append(" ")
							.append(toBeNotified.getCountry()).append(" ")
							.append(toBeNotified.getCpu()).append(" ")
							.append(toBeNotified.getAccount().getId()).toString());
				} else {
					client.sendLine(new StringBuilder("ADDUSER ")
							.append(toBeNotified.getAccount().getName()).append(" ")
							.append(toBeNotified.getCountry()).append(" ")
							.append(toBeNotified.getCpu()).toString());
				}
			}
		}
		client.endFastWrite();
	}

	/**
	 * Notifies all registered clients of a new client who just logged in.
	 * The new client is not notified, because he is already notified
	 * by some other method.
	 */
	public void notifyClientsOfNewClientOnServer(Client client) {

		StringBuilder cmd = new StringBuilder("ADDUSER ");
		cmd.append(client.getAccount().getName());
		cmd.append(" ").append(client.getCountry());
		cmd.append(" ").append(client.getCpu());

		String cmdNoId = cmd.toString();

		cmd.append(" ").append(client.getAccount().getId());
		String cmdWithId = cmd.toString();

		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if ((toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL))
					&& (toBeNotified != client))
			{
				if (toBeNotified.isAcceptAccountIDs()) {
					toBeNotified.sendLine(cmdWithId);
				} else {
					toBeNotified.sendLine(cmdNoId);
				}
			}
		}
	}

	/**
	 * The client who just joined the battle is also notified.
	 * He should also be notified through the JOINBATTLE command.
	 * See the protocol description.
	 */
	public void notifyClientsOfNewClientInBattle(Battle battle, Client client) {

		StringBuilder cmd = new StringBuilder("JOINEDBATTLE ");
		cmd.append(battle.getId());
		cmd.append(" ").append(client.getAccount().getName());

		String cmdNoScriptPassword = cmd.toString();

		if (client.isScriptPassordSupported()
				&& (!client.getScriptPassword().equals(Client.NO_SCRIPT_PASSWORD)))
		{
			cmd.append(" ").append(client.getScriptPassword());
		}
		String cmdWithScriptPassword = cmd.toString();

		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if (toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL)) {
				if (toBeNotified.equals(battle.getFounder())
						|| toBeNotified.equals(client))
				{
					toBeNotified.sendLine(cmdWithScriptPassword);
				}
				toBeNotified.sendLine(cmdNoScriptPassword);
			}
		}
	}

	/**
	 * Kills the client and its socket channel.
	 * @see #killClient(Client client, String reason)
	 */
	public boolean killClient(Client client) {
		return killClient(client, null);
	}

	/**
	 * This method disconnects and removes a client from the clients list.
	 * Also cleans up after him (channels, battles) and notifies other
	 * users of his departure.
	 * @param reason is used with LEFT command to notify other users on same
	 *   channel of this client's departure reason. It may be left blank ("") or
	 *   be set to <code>null</code> to give no reason.
	 */
	public boolean killClient(Client client, String reason) {

		int index = clients.indexOf(client);
		if (index == -1 || !client.isAlive()) {
			return false;
		}
		client.disconnect();
		clients.remove(index);
		client.setAlive(false);
		String reasonNonNull = ((reason == null) || reason.trim().equals(""))
				? "Quit" : reason;

		// let's remove client from all channels he is participating in:
		client.leaveAllChannels(reasonNonNull);

		if (client.getBattleID() != Battle.NO_BATTLE_ID) {
			Battle bat = context.getBattles().getBattleByID(client.getBattleID());
			if (bat == null) {
				LOG.error("Invalid battle ID. Server will now exit!");
				context.getServerThread().closeServerAndExit();
			}
			// internally checks if the client is the founder and closes the
			// battle in that case
			context.getBattles().leaveBattle(client, bat);
		}

		if (client.getAccount().getAccess() != Account.Access.NONE) {
			sendToAllRegisteredUsers("REMOVEUSER " + client.getAccount().getName());
			LOG.debug("Registered user killed: {}", client.getAccount().getName());
		} else {
			LOG.debug("Unregistered user killed");
		}

		if (context.getServer().isLanMode()) {
			context.getAccountsService().removeAccount(client.getAccount());
		}

		return true;
	}

	/**
	 * This method will cause the client to be killed, but not immediately.
	 * It will do it once the main server loop reaches its end.
	 * We need this on some occasions while we iterating through
	 * the clients list. We do not want client to be removed from the list,
	 * since that would "broke" our loop, as we go from index 0 to the highest
	 * index, which would be invalid as the highest index would decrease by 1.
	 */
	public void killClientDelayed(Client client, String reason) {

		delayedKills.add(new KillJob(client, reason));
		client.setHalfDead(true);
	}

	/**
	 * This will kill all clients in the current kill list and empty it.
	 * Must only be called from the main server loop (at the end of it)!
	 * Any redundant entries are ignored (cleared).
	 */
	public void processKillList() {

		while (!delayedKills.isEmpty()) {
			KillJob killJob = delayedKills.remove(0);
			killClient(killJob.getClient(), killJob.getReason());
		}
	}

	/**
	 * This will try to go through the list of clients that still have pending
	 * data to be sent and will try to send it.
	 * When it encounters first client that can't flush data, it will add
	 * him to the queues tail and break the loop.
	 */
	public void flushData() {

		Client client;
		while ((client = sendQueue.poll()) != null) {
			if (!client.tryToFlushData()) {
				// add the client to the tail of the queue
				sendQueue.add(client);
				break;
			}
		}
	}

	/** Adds client to the queue of clients who have more data to be sent */
	public void enqueueDelayedData(Client client) {
		sendQueue.add(client);
	}
}
