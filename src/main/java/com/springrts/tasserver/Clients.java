/*
 * Created on 2006.11.2
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Betalord
 */
public class Clients implements ContextReceiver, Updateable {

	private final Log s_log  = LogFactory.getLog(Clients.class);

	private List<Client> clients = new ArrayList<Client>();
	/** A list of clients waiting to be killed (disconnected) */
	private List<Client> killList = new ArrayList<Client>();
	/**
	 * KillList is used when we want to kill a client but not immediately
	 * (within a loop, for example).
	 * Client on the list will get killed after main server loop reaches its end.
	 * Also see killClientDelayed() method. Any redundant entries will be
	 * removed (client will be killed only once), so no additional logic for
	 * consistency is required.
	 * @see killList gives a reason for each scheduled kill
	 */
	private List<String> reasonList = new ArrayList<String>();

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
			s_log.warn(new StringBuilder("Timeout detected from ")
					.append(client.getAccount().getName()).append(" (")
					.append(client.getIp()).append("). Client has been scheduled for kill ...").toString());
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
			//***chan.socket().setSoTimeout(TIMEOUT_LENGTH); -> this doesn't seem to have an effect with java.nio
			client.setSelKey(chan.register(readSelector, SelectionKey.OP_READ, client));
		} catch (ClosedChannelException cce) {
			killClient(client);
			return null;
		} catch (IOException ioe) {
			killClient(client);
			return null;
		} catch (Exception e) {
			killClient(client);
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

		boolean isLoggedIn = false;

		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getAccount().getName().equals(acc.getName())) {
				isLoggedIn = true;
				break;
			}
		}

		return isLoggedIn;
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
				client.sendLine(new StringBuilder("CLIENTSTATUS ")
						.append(toBeNotified.getAccount().getName()).append(" ")
						.append(toBeNotified.getStatus()).toString());
			}
		}
		client.endFastWrite();
	}

	/**
	 * Notifies all logged-in clients (including this client)
	 * of the client's new status
	 */
	public void notifyClientsOfNewClientStatus(Client client) {

		sendToAllRegisteredUsers(new StringBuilder("CLIENTSTATUS ")
				.append(client.getAccount().getName()).append(" ")
				.append(client.getStatus()).toString());
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

		for (int i = 0; i < clients.size(); i++) {
			Client toBeNotified = clients.get(i);
			if ((toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL))
					&& (toBeNotified != client))
			{
				if (toBeNotified.isAcceptAccountIDs()) {
					toBeNotified.sendLine(new StringBuilder("ADDUSER ")
							.append(client.getAccount().getName()).append(" ")
							.append(client.getCountry()).append(" ")
							.append(client.getCpu()).append(" ")
							.append(client.getAccount().getId()).toString());
				} else {
					toBeNotified.sendLine(new StringBuilder("ADDUSER ")
							.append(client.getAccount().getName()).append(" ")
							.append(client.getCountry()).append(" ")
							.append(client.getCpu()).toString());
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

		String cmdBase = "JOINEDBATTLE " + battle.getId() + " " + client.getAccount().getName();
		for (Client toBeNotified : clients)  {
			if (toBeNotified.getAccount().getAccess().isAtLeast(Account.Access.NORMAL)) {
				StringBuilder cmd = new StringBuilder(cmdBase);
				if ((toBeNotified.equals(battle.getFounder()) || toBeNotified.equals(client))
						&& client.isScriptPassordSupported()
						&& (!client.getScriptPassword().equals(Client.NO_SCRIPT_PASSWORD)))
				{
					cmd.append(" ").append(client.getScriptPassword());
				}
				toBeNotified.sendLine(cmd.toString());
			}
		}
	}

	/** @see #killClient(Client client, String reason) */
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
		if ((reason == null) || reason.trim().equals("")) {
			reason = "Quit";
		}

		// let's remove client from all channels he is participating in:
		client.leaveAllChannels(reason);

		if (client.getBattleID() != Battle.NO_BATTLE_ID) {
			Battle bat = context.getBattles().getBattleByID(client.getBattleID());
			if (bat == null) {
				s_log.fatal("Invalid battle ID. Server will now exit!");
				context.getServerThread().closeServerAndExit();
			}
			context.getBattles().leaveBattle(client, bat); // automatically checks if client is founder and closes the battle
		}

		if (client.getAccount().getAccess() != Account.Access.NONE) {
			sendToAllRegisteredUsers("REMOVEUSER " + client.getAccount().getName());
			if (s_log.isDebugEnabled()) {
				s_log.debug("Registered user killed: " + client.getAccount().getName());
			}
		} else {
			if (s_log.isDebugEnabled()) {
				s_log.debug("Unregistered user killed");
			}
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
		killList.add(client);
		reasonList.add(reason);
		client.setHalfDead(true);
	}

	/**
	 * This will kill all clients in the current kill list and empty it.
	 * Must only be called from the main server loop (at the end of it)!
	 * Any redundant entries are ignored (cleared).
	 */
	public void processKillList() {
		while (!killList.isEmpty()) {
			killClient(killList.get(0), reasonList.get(0));
			killList.remove(0);
			reasonList.remove(0);
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
				sendQueue.add(client); // add client to the tail of the queue
				break;
			}
		}
	}

	/** Adds client to the queue of clients who have more data to be sent */
	public void enqueueDelayedData(Client client) {
		sendQueue.add(client);
	}
}
