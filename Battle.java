/*
 * Created on 2005.6.19
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
import java.util.*;

public class Battle {
	static int IDCounter; // this is the ID that the next battle will have
	
	public int ID; // each battle has it's own unique ID
	public int type; // 0 = normal battle, 1 = battle replay
	public int natType; // NAT traversal technique used by the host. Use 0 for none.
	public String title; // description of the battle set by founder of the battle
	public Client founder; // founder (host) of the battle
	private ArrayList<Client> clients; // clients without the founder (host)
	private ArrayList<Bot> bots; // bots added by clients participating in this battle
	public String mapName;
	public int maxPlayers;
	public String password; // use restricted() method to find out if battle is password-protected
	public int port;
	public int metal; // starting metal
	public int energy; // starting energy
	public int units; // max. units
	public int startPos; // 0 = fixed, 1 = random, 2 = choose in game
	public int gameEndCondition; // 0 = game continues if commander dies, 1 = game ends if commander dies, 2 = lineage mode
	public int hashCode; // see notes for description!
	public int rank; // if 0, no rank limit is set. If 1 or higher, only players with this rank (or higher) can join the battle (Note: rank index 1 means seconds rank, not the first one, since you can't limit game to players of the first rank because that means game is open to all players and you don't have to limit it in that case)
	public int mapHash; // see protocol description for details!
	public String modName;
	public ArrayList<String> disabledUnits;
	public StartRect[] startRects;
	public boolean limitDGun;
	public boolean diminishingMMs;
	public boolean ghostedBuildings;
	public boolean locked; // if true, battle is locked and noone can join it (until lock is released by founder)
	public HashMap<String, String> scriptTags;
	// following elements are used only with type=1:
	public ArrayList<String> replayScript = new ArrayList<String>(); // contains lines of the script file
	public ArrayList<String> tempReplayScript = new ArrayList<String>(); // here we save script lines until we receive SCRIPTEND command. Then we copy it to "replayScript" object and notify all clients about it. 
	

	public Battle(int type, int natType, Client founder, String password, int port, int maxPlayers, int startMetal, int startEnergy, int maxUnits, int startPos, int gameEndCondition, boolean limitDGun, boolean diminishingMMs, boolean ghostedBuildings, int hashCode, int rank, int mapHash, String mapName, String title, String modName) {
		this.ID = IDCounter++;
		this.type = type;
		this.natType = natType;
		this.title = new String(title);
		this.founder = founder;
		this.clients = new ArrayList<Client>();
		this.bots = new ArrayList<Bot>();
		this.mapName = new String(mapName);
		this.maxPlayers = maxPlayers;
		this.password = new String(password);
		this.port = port;
		this.metal = startMetal;
		this.energy = startEnergy;
		this.units = maxUnits;
		this.startPos = startPos;
		this.gameEndCondition = gameEndCondition;
		this.limitDGun = limitDGun;
		this.diminishingMMs = diminishingMMs;
		this.ghostedBuildings = ghostedBuildings;
		this.hashCode = hashCode;
		this.rank = rank;
		this.mapHash = mapHash;
		this.modName = new String(modName);
		this.disabledUnits = new ArrayList<String>();
		this.startRects = new StartRect[10];
		this.locked = false; // we assume this by default. Client must make sure it is unlocked.
		this.scriptTags = new HashMap<String, String>();
		for (int i = 0; i < startRects.length; i++) startRects[i] = new StartRect();
	}
	
	/* creates BATTLEOPENED command from this battle and returns it as a result */
	public String createBattleOpenedCommand() {
		return "BATTLEOPENED " + ID + " " + type + " " + natType + " " + founder.account.user + " " + founder.IP + " "+ port + " " + maxPlayers + " " + Misc.boolToStr(restricted()) + " " + rank + " " + mapHash + " " + mapName + "\t" + title + "\t" + modName;
	}
	
	/* same as createBattleOpenedCommand() but requires sender to tell what IP to use (local or external) */
	public String createBattleOpenedCommandEx(boolean local) {
		String ip;
		if (local) ip = founder.localIP;
		else ip = founder.IP;
		return "BATTLEOPENED " + ID + " " + type + " " + natType + " " + founder.account.user + " " + ip + " "+ port + " " + maxPlayers + " " + Misc.boolToStr(restricted()) + " " + rank + " " + mapHash + " " + mapName + "\t" + title + "\t" + modName;
	}
	
	/* sends series of CLIENTBATTLESTATUS command to client telling him about battle statuses
	 * of all clients in this battle EXCEPT for himself! */
	public void notifyOfBattleStatuses(Client client) {
		for (int i = 0; i < this.clients.size(); i++) 
			if (this.clients.get(i) == client) continue;
			else client.sendLine("CLIENTBATTLESTATUS " + this.clients.get(i).account.user + " " + this.clients.get(i).battleStatus + " " + this.clients.get(i).teamColor);
		if (founder != client) client.sendLine("CLIENTBATTLESTATUS " + founder.account.user + " " + founder.battleStatus + " " + founder.teamColor);
	}
	
	/* notifies all clients in the battle (including the client) about new battle status
	 * of the client. */
	public void notifyClientsOfBattleStatus(Client client) {
		sendToAllClients("CLIENTBATTLESTATUS " + client.account.user + " " + client.battleStatus + " " + client.teamColor);
	}
	
	/* sends String s to all clients participating in this battle */
	public void sendToAllClients(String s) {
		for (int i = 0; i < this.clients.size(); i++) {
			clients.get(i).sendLine(s);
		}
		founder.sendLine(s);
	}
	
	/* sends String s to all clients participating in this battle except for the founder */
	public void sendToAllExceptFounder(String s) {
		for (int i = 0; i < this.clients.size(); i++) {
			clients.get(i).sendLine(s);
		}
	}
	
	public String clientsToString() {
		if (this.clients.size() == 0) return "";
		String s = new String(clients.get(0).account.user);
		for (int i = 1; i < this.clients.size(); i++)
			s.concat(" " + clients.get(i).account.user);
		return s;
	}
	
	/* returns number of clients participating in this battle */
	public int getClientsSize() {
		return clients.size();
	}
	
	public Client getClient(int index) {
		try {
			return clients.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
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
		return founder.getInGameFromStatus();
	}
	
	/* returns number of spectators in this battle. Note that this operation is 
	 * not very fast - we have to go through the entire list of clients in this 
	 * battle to figure out spectator count. */
	public int spectatorCount() {
		int count = 0;
		for (int i = 0; i < clients.size(); i++)
			if (Misc.getModeFromBattleStatus(clients.get(i).battleStatus) == 0) count++;
		if (Misc.getModeFromBattleStatus(founder.battleStatus) == 0) count++;
		return count;
	}
	
	public int nonSpectatorCount() {
		return clients.size()+1-spectatorCount();
	}
	
	public boolean isClientInBattle(Client client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i) == client) return true;
		if (founder == client) return true;	
		
		return false;
	}
	
	public boolean isClientInBattle(String username) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).account.user.equals(username)) return true;
		if (founder.account.user.equals(username)) return true;	
		
		return false;
	}
	
	public Client getClient(String username) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).account.user.equals(username)) return clients.get(i);
		if (founder.account.user.equals(username)) return founder;	
		
		return null;
	}
	
	public void sendDisabledUnitsListToClient(Client client) {
		if (disabledUnits.size() == 0) return ; // nothing to send
		
		String line = (String)disabledUnits.get(0);
		for (int i = 1; i < disabledUnits.size(); i++)
			line = line.concat(" " + (String)disabledUnits.get(i));
			
		client.sendLine("DISABLEUNITS " + line);
	}
	
	/* returns number of bots in this battle (size of the bot list) */
	public int getBotsSize() {
		return bots.size();
	}
	
	/* returns Bot object of the specified bot, or null if the bot does not exist */
	public Bot getBot(String name) {
		for (int i = 0; i < bots.size(); i++)
			if (bots.get(i).name.equals(name)) return bots.get(i);
		return null;	
	}
	
	/* returns null if index is out of bounds */
	public Bot getBot(int index) {
		try {
			return bots.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	/* adds bot to the bot list */
	public void addBot(Bot bot) {
		bots.add(bot);
	}
	
	/* removes first bot in the bots list which is owned by the client */
	public boolean removeFirstBotOfClient(Client client) {
		for (int i = 0; i < bots.size(); i++)
			if (bots.get(i).ownerName.equals(client.account.user)) {
				sendToAllClients("REMOVEBOT " + ID + " " + bots.get(i).name);
				bots.remove(i);
				return true;
			}
		return false;	
	}
	
	/* removes specified bot from the bot list */
	public boolean removeBot(Bot bot) {
		return bots.remove(bot);
	}
	
	/* removes all bots owned by client */
	public void removeClientBots(Client client) {
		while (removeFirstBotOfClient(client)) ;
	}
	
	public void sendBotListToClient(Client client) {
		for (int i = 0; i < bots.size(); i++)
			client.sendLine("ADDBOT " + ID + " " + bots.get(i).name + " " + bots.get(i).ownerName + " " + bots.get(i).battleStatus + " " + bots.get(i).teamColor + " " + bots.get(i).AIDll);
	}
	
	public void sendStartRectsListToClient(Client client) {
		for (int i = 0; i < startRects.length; i++) {
			if (!startRects[i].enabled) continue; 
			client.sendLine("ADDSTARTRECT " + i + " " + startRects[i].left + " " + startRects[i].top + " " + startRects[i].right + " " + startRects[i].bottom);
		} 
	}
	
	public void sendScriptToClient(Client client) {
		client.sendLine("SCRIPTSTART");
		for (int i = 0; i < replayScript.size(); i++) {
			client.sendLine("SCRIPT " + replayScript.get(i));
		}
		client.sendLine("SCRIPTEND");
	}
	
	public void sendScriptToAllExceptFounder() {
		for (int i = 0; i < clients.size(); i++) {
			sendScriptToClient(clients.get(i));
		} 
	}
	
	public void sendScriptTagsToClient(Client client) {
		Iterator it = scriptTags.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry e = (Map.Entry)it.next();
			client.sendLine("SETSCRIPTTAG " + e.getKey() + " " + e.getValue());
		}
	}
	
	public void sendScriptTagsToAll() {
		for (int i = 0; i < clients.size(); i++) {
			sendScriptTagsToClient(clients.get(i));
		} 
	}
	
	/* will copy tempReplayScript to replayScript. This method is called
	 * when SCRIPTEND command is received. */
	public void ratifyTempScript() {
		replayScript = new ArrayList<String>(tempReplayScript); 
	}
}
