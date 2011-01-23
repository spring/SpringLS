/*
 * Created on 2005.6.19
 */

package com.springrts.tasserver;


import com.springrts.tasserver.util.Processor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 */
public class Battle implements ContextReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(Battle.class);

	/** the id that the next battle will have */
	private static int nextId;

	/**
	 * This is used as a null kind of value,
	 * whenever we want to refer to no-battle.
	 */
	public static final int NO_BATTLE_ID = -1;
	/** unique id */
	private int id;
	/** 0 = normal battle, 1 = battle replay */
	private int type;
	/** NAT traversal technique used by the host. Use 0 for none. */
	private int natType;
	/** description set by the founder of the battle */
	private String title;
	/** founder (host) of the battle */
	private Client founder;
	/** clients without the founder (host) */
	private List<Client> clients;
	/** bots added by clients participating in this battle */
	private List<Bot> bots;
	/** see protocol description for details */
	private String modName;
	/** name of the map currently selected for this battle */
	private String mapName;
	/** see protocol description for details */
	private int mapHash;
	/** maximum number of players (including bots) that can participate */
	private int maxPlayers;
	/** use restricted() method to find out if battle is password-protected */
	private String password;
	/** IP port number this battle will be hosted on (default is 8452). */
	private int port;
	/** see notes for description */
	private int hashCode;
	/**
	 * If 0, no rank limit is set.
	 * If 1 or higher, only players with this rank (or higher)
	 * can join the battle
	 * (Note: rank index 1 means seconds rank, not the first one,
	 * since you can not limit the game to players of the first rank,
	 * because that means the game is open to all players
	 * and you do not have to limit it in that case)
	 */
	private int rank;
	/** list of unit-definition names which are not allowed to be built */
	private List<String> disabledUnits;
	/** list of start rectangles */
	private List<StartRect> startRects;
	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 */
	private boolean locked;
	/**  */
	private Map<String, String> scriptTags;
	/**
	 * contains lines of the script file.
	 * This is only used if type == 1.
	 */
	private List<String> replayScript;
	/**
	 * Stores script lines until we receive the SCRIPTEND command.
	 * Then we copy it to the "replayScript" object,
	 * and notify all clients about it.
	 * This is only used if type == 1.
	 */
	private List<String> tempReplayScript;

	private Context context = null;


	@Override
	public void receiveContext(Context context) {

		this.context = context;
		initStartRects();
	}


	public Battle(int type, int natType, Client founder, String password,
			int port, int maxPlayers, int hashCode, int rank, int mapHash,
			String mapName, String title, String modName)
	{
		this.id = nextId++;
		this.type = type;
		this.natType = natType;
		this.title = title;
		this.founder = founder;
		this.clients = new ArrayList<Client>();
		this.bots = new ArrayList<Bot>();
		this.mapName = mapName;
		this.maxPlayers = maxPlayers;
		this.password = password;
		this.port = port;
		this.hashCode = hashCode;
		this.rank = rank;
		this.mapHash = mapHash;
		this.modName = modName;
		this.disabledUnits = new ArrayList<String>();
		// we assume this by default. Client must make sure it is unlocked.
		this.locked = false;
		this.scriptTags = new HashMap<String, String>();
		this.replayScript = new ArrayList<String>();
		this.tempReplayScript = new ArrayList<String>();
	}

	private void initStartRects() {

		int maxAllyTeams = context.getEngine().getMaxAllyTeams();
		this.startRects = new ArrayList<StartRect>(maxAllyTeams);
		for (int at = 0; at < maxAllyTeams; at++) {
			this.getStartRects().add(new StartRect());
		}
	}

	/**
	 * Creates BATTLEOPENED command from this battle and returns it as a result
	 * using the external IP.
	 */
	public String createBattleOpenedCommand() {
		return createBattleOpenedCommandEx(false);
	}

	/**
	 * Same as createBattleOpenedCommand() but requires sender to specify
	 * what IP to use (local or external).
	 * @see #createBattleOpenedCommand()
	 */
	public String createBattleOpenedCommandEx(boolean local) {

		InetAddress addr;
		if (local) {
			addr = getFounder().getLocalIp();
		} else {
			addr = getFounder().getIp();
		}

		return new StringBuilder("BATTLEOPENED ")
				.append(getId()).append(" ")
				.append(getType()).append(" ")
				.append(getNatType()).append(" ")
				.append(getFounder().getAccount().getName()).append(" ")
				.append(addr.getHostAddress()).append(" ")
				.append(getPort()).append(" ")
				.append(getMaxPlayers()).append(" ")
				.append(Misc.boolToStr(restricted())).append(" ")
				.append(getRank()).append(" ")
				.append(getMapHash()).append(" ")
				.append(getMapName()).append("\t")
				.append(getTitle()).append("\t")
				.append(getModName()).toString();
	}

	private static class BattleStatusNotifyer implements Processor<Client> {

		private final Client client;

		BattleStatusNotifyer(Client client) {
			this.client = client;
		}

		@Override
		public void process(Client curClient) {
			if (curClient != client) {
				client.sendLine(String.format("CLIENTBATTLESTATUS %s %d %d",
						curClient.getAccount().getName(),
						curClient.getBattleStatus(),
						Misc.colorJavaToSpring(curClient.getTeamColor())));
			}
		}
	}

	/**
	 * Sends series of CLIENTBATTLESTATUS command to client telling him
	 * about the battle stati of all clients in this battle EXCEPT for himself!
	 */
	public void notifyOfBattleStatuses(final Client client) {
		applyToClientsAndFounder(new BattleStatusNotifyer(client));
	}

	/**
	 * Notifies all clients in the battle (including the client)
	 * about the new battle status of the client.
	 */
	public void notifyClientsOfBattleStatus(Client client) {

		sendToAllClients(String.format("CLIENTBATTLESTATUS %s %d %d",
				client.getAccount().getName(),
				client.getBattleStatus(),
				Misc.colorJavaToSpring(client.getTeamColor())));
	}

	public void notifyClientJoined(Client client) {

		// do the actuall joining and notifying
		client.setDefaultBattleStatus();
		client.setBattleID(getId());
		client.setRequestedBattleID(Battle.NO_BATTLE_ID);
		addClient(client);
	 	// notify client that he has successfully joined the battle
		client.sendLine("JOINBATTLE " + getId() + " " + getHashCode());
		context.getClients().notifyClientsOfNewClientInBattle(this, client);
		notifyOfBattleStatuses(client);
		sendBotListToClient(client);
		// tell host about this client's IP and UDP source port
		// if battle is hosted using one of the NAT traversal techniques
		if ((getNatType() == 1) || (getNatType() == 2)) {
			// make sure that clients behind NAT get local IPs and not external
			// ones
			InetAddress ip = (getFounder().getIp().equals(client.getIp())
					? client.getLocalIp() : client.getIp());
			getFounder().sendLine(String.format("CLIENTIPPORT %s %s %d",
					client.getAccount().getName(),
					ip.getHostAddress(),
					client.getUdpSourcePort()));
		}

		client.sendLine("REQUESTBATTLESTATUS");
		sendDisabledUnitsListToClient(client);
		sendStartRectsListToClient(client);
		sendScriptTagsToClient(client);

		if (getType() == 1) {
			sendScriptToClient(client);
		}
	}


	private static class MessageSender implements Processor<Client> {

		private final String message;

		MessageSender(String message) {
			this.message = message;
		}

		@Override
		public void process(Client curClient) {
			curClient.sendLine(message);
		}
	}
	/**
	 * Sends <code>message</code> to all clients participating in this battle.
	 */
	public void sendToAllClients(final String message) {
		applyToClientsAndFounder(new MessageSender(message));
	}

	/**
	 * Sends <code>message</code> to all clients participating in this battle
	 * except for the founder.
	 */
	public void sendToAllExceptFounder(final String message) {
		applyToClients(new MessageSender(message));
	}

	public String clientsToString() {

		String clientsString = null;

		if (!clients.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Client curClient : clients) {
				sb.append(" ").append(curClient.getAccount().getName());
			}
			// delete the initial " "
			sb.deleteCharAt(0);
			clientsString = sb.toString();
		} else {
			clientsString = "";
		}

		return clientsString;
	}


	public void applyToClients(Processor<? super Client> clientsProcessor) {

		for (int c = 0; c < clients.size(); c++) {
			clientsProcessor.process(clients.get(c));
		}
	}

	public void applyToClientsAndFounder(
			Processor<? super Client> clientsProcessor)
	{
		applyToClients(clientsProcessor);
		clientsProcessor.process(founder);
	}

	private void applyToBots(Processor<? super Bot> botsProcessor) {

		for (int b = 0; b < bots.size(); b++) {
			botsProcessor.process(bots.get(b));
		}
	}

	public void applyToTeamControllers(
			Processor<? super TeamController> teamControllerProcessor)
	{
		applyToClients(teamControllerProcessor);
		teamControllerProcessor.process(founder);
		applyToBots(teamControllerProcessor);
	}

	/**
	 * Returns the number of clients participating in this battle.
	 * @return number of clients participating in this battle
	 */
	public int getClientsSize() {
		return clients.size();
	}

	public boolean addClient(Client client) {
		return this.clients.add(client);
	}

	public boolean removeClient(Client client) {
		return this.clients.remove(client);
	}

	public boolean restricted() {
		return !password.equals("*");
	}

	public boolean inGame() {
		return getFounder().isInGame();
	}

	private static class SpectatorCounter implements Processor<Client> {

		private int count = 0;

		@Override
		public void process(Client curClient) {
			if (curClient.isSpectator()) {
				count++;
			}
		}

		public int getCount() {
			return count;
		}
	}

	/**
	 * Returns number of spectators in this battle. Note that this operation is
	 * not very fast - we have to go through the entire list of clients in this
	 * battle to figure out spectator count.
	 * @return number of spectators in this battle
	 */
	public int spectatorCount() {

		SpectatorCounter spectatorCounter = new SpectatorCounter();
		applyToClientsAndFounder(spectatorCounter);

		return spectatorCounter.getCount();
	}

	public int nonSpectatorCount() {
		return getClientsSize() + 1 - spectatorCount();
	}

	public boolean isClientInBattle(Client client) {
		return (client.equals(getFounder()) || clients.contains(client));
	}

	/** @deprecated */
	private boolean isClientInBattle(String username) {
		return (getClient(username) != null);
	}

	/** @deprecated */
	private Client getClient(String username) {

		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getAccount().getName().equals(username)) {
				return clients.get(i);
			}
		}
		if (getFounder().getAccount().getName().equals(username)) {
			return getFounder();
		}

		return null;
	}

	private void sendDisabledUnitsListToClient(Client client) {

		if (getDisabledUnits().isEmpty()) {
			// nothing to send
			return;
		}

		StringBuilder line = new StringBuilder("DISABLEUNITS ");
		line.append(getDisabledUnits().get(0));
		for (int i = 1; i < getDisabledUnits().size(); i++) {
			line.append(" ").append(getDisabledUnits().get(i));
		}

		client.sendLine(line.toString());
	}

	/** Returns number of bots in this battle (size of the bot list) */
	public int getBotsSize() {
		return bots.size();
	}

	/**
	 * Returns Bot object of the specified bot, or <code>null</code>
	 * if the bot does not exist
	 */
	public Bot getBot(String name) {

		for (int i = 0; i < bots.size(); i++) {
			Bot bot = bots.get(i);
			if (bot.getName().equals(name)) {
				return bot;
			}
		}
		return null;
	}

	/** Returns <code>null</code> if index is out of bounds */
	public Bot getBot(int index) {

		Bot bot = null;

		try {
			bot = bots.get(index);
		} catch (IndexOutOfBoundsException ex) {
			LOG.error("Failed fetching a bot by index", ex);
			bot = null;
		}

		return bot;
	}

	/** Adds the specified bot to the bot list, and informs other clients */
	public void addBot(Bot bot) {

		bots.add(bot);

		sendToAllClients(new StringBuilder("ADDBOT ")
				.append(getId()).append(" ")
				.append(bot.getName()).append(" ")
				.append(bot.getOwnerName()).append(" ")
				.append(bot.getBattleStatus()).append(" ")
				.append(bot.getTeamColor()).append(" ")
				.append(bot.getSpecifier()).toString());
	}

	/**
	 * Removes the specified bot from the bot list if present, and informs other
	 * clients.
	 */
	public boolean removeBot(Bot bot) {

		boolean containedBot = bots.remove(bot);

		if (containedBot) {
			sendToAllClients(new StringBuilder("REMOVEBOT ")
					.append(getId()).append(" ")
					.append(bot.getName()).toString());
		}

		return containedBot;
	}

	/* Removes all bots owned by client */
	public void removeClientBots(Client client) {

		Set<Bot> clientsBots = new HashSet<Bot>();
		for (int i = 0; i < bots.size(); i++) {
			Bot bot = bots.get(i);
			if (bot.getOwnerName().equals(client.getAccount().getName())) {
				clientsBots.add(bot);
			}
		}

		for (Bot bot : clientsBots) {
			removeBot(bot);
		}
	}

	private void sendBotListToClient(Client client) {

		for (int i = 0; i < bots.size(); i++) {
			Bot bot = bots.get(i);
			client.sendLine(String.format("ADDBOT %d %s %s %d %d %s",
					getId(),
					bot.getName(),
					bot.getOwnerName(),
					bot.getBattleStatus(),
					Misc.colorJavaToSpring(bot.getTeamColor()),
					bot.getSpecifier()));
		}
	}

	private void sendStartRectsListToClient(Client client) {

		for (int i = 0; i < getStartRects().size(); i++) {
			StartRect curStartRect = getStartRects().get(i);
			if (curStartRect.isEnabled()) {
				client.sendLine(String.format("ADDSTARTRECT %d %d %d %d %d",
						i,
						curStartRect.getLeft(),
						curStartRect.getTop(),
						curStartRect.getRight(),
						curStartRect.getBottom()));
			}
		}
	}

	private void sendScriptToClient(Client client) {

		client.beginFastWrite();
		client.sendLine("SCRIPTSTART");
		for (int i = 0; i < getReplayScript().size(); i++) {
			client.sendLine("SCRIPT " + getReplayScript().get(i));
		}
		client.sendLine("SCRIPTEND");
		client.endFastWrite();
	}

	public void sendScriptToAllExceptFounder() {

		for (int i = 0; i < clients.size(); i++) {
			sendScriptToClient(clients.get(i));
		}
	}

	/**
	 * Will put together all script tags in a single line
	 * to be taken as an argument to the SETSCRIPTTAGS command.
	 */
	private String joinScriptTags() {

		StringBuilder joined = new StringBuilder();

		for (Map.Entry<String, String> e : getScriptTags().entrySet()) {
			joined.append("\t");
			joined.append(e.getKey()).append("=").append(e.getValue());
		}
		// delete the initial "\t"
		if (joined.length() > 0) {
			joined.deleteCharAt(0);
		}

		return joined.toString();
	}

	private void sendScriptTagsToClient(Client client) {

		if (getScriptTags().isEmpty()) {
			// nothing to send
			return;
		}
		client.sendLine("SETSCRIPTTAGS " + joinScriptTags());
	}

	/** @deprecated */
	private void sendScriptTagsToAll() {

		for (int i = 0; i < clients.size(); i++) {
			sendScriptTagsToClient(clients.get(i));
		}
	}

	/**
	 * Will copy tempReplayScript to replayScript.
	 * This method is called when SCRIPTEND command is received.
	 */
	public void ratifyTempScript() {

		replayScript = new ArrayList<String>(getTempReplayScript().size());
		Collections.copy(getReplayScript(), getTempReplayScript());
	}

	/**
	 * Returns the unique id of this battle.
	 * @see #NO_BATTLE_ID
	 * @return the unique id of this battle
	 */
	public int getId() {
		return id;
	}

	/**
	 * 0 = normal battle, 1 = battle replay
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * NAT traversal technique used by the host. Use 0 for none.
	 * @return the natType
	 */
	public int getNatType() {
		return natType;
	}

	/**
	 * description set by the founder of the battle
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * founder (host) of the battle
	 * @return the founder
	 */
	public Client getFounder() {
		return founder;
	}

	/**
	 * name of the map currently selected for this battle
	 * @return the mapName
	 */
	public String getMapName() {
		return mapName;
	}

	/**
	 * name of the map currently selected for this battle
	 * @param mapName the mapName to set
	 */
	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	/**
	 * maximum number of players (including bots) that can participate
	 * @return the maxPlayers
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * use restricted() method to find out if battle is password-protected
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * IP port number this battle will be hosted on (default is 8452).
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * see notes for description
	 * @return the hashCode
	 */
	public int getHashCode() {
		return hashCode;
	}

	/**
	 * If 0, no rank limit is set.
	 * If 1 or higher, only players with this rank (or higher)
	 * can join the battle
	 * (Note: rank index 1 means seconds rank, not the first one,
	 * since you can not limit the game to players of the first rank,
	 * because that means the game is open to all players
	 * and you do not have to limit it in that case)
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * see protocol description for details
	 * @return the mapHash
	 */
	public int getMapHash() {
		return mapHash;
	}

	/**
	 * see protocol description for details
	 * @param mapHash the mapHash to set
	 */
	public void setMapHash(int mapHash) {
		this.mapHash = mapHash;
	}

	/**
	 * see protocol description for details
	 * @return the modName
	 */
	public String getModName() {
		return modName;
	}

	/**
	 * list of unit-definition names which are not allowed to be built
	 * @return the disabledUnits
	 */
	public List<String> getDisabledUnits() {
		return disabledUnits;
	}

	/**
	 * list of start rectangles
	 * @return the startRects
	 */
	public List<StartRect> getStartRects() {
		return startRects;
	}

	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 * @return the locked
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 * @param locked the locked to set
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 *
	 * @return the scriptTags
	 */
	public Map<String, String> getScriptTags() {
		return scriptTags;
	}

	/**
	 * contains lines of the script file.
	 * This is only used if type == 1.
	 * @return the replayScript
	 */
	public List<String> getReplayScript() {
		return replayScript;
	}

	/**
	 * Stores script lines until we receive the SCRIPTEND command.
	 * Then we copy it to the "replayScript" object,
	 * and notify all clients about it.
	 * This is only used if type == 1.
	 * @return the tempReplayScript
	 */
	public List<String> getTempReplayScript() {
		return tempReplayScript;
	}
}
