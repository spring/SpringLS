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
import java.util.Vector;

public class Battle {
	static int IDCounter; // this is the ID that the next battle will have
	
	public int ID; // each battle has it's own unique ID
	public int type; // 0 = normal battle, 1 = battle replay
	public int natType; // NAT traversal technique used by the host. Use 0 for none.
	public String title; // description of the battle set by founder of the battle
	public Client founder; // founder (host) of the battle
	public Vector clients; // clients without the founder
	public Vector bots;
	public String mapName;
	public int maxPlayers;
	public String password; // use restricted() method to find out if battle is password-protected
	public int port;
	public int metal; // starting metal
	public int energy; // starting energy
	public int units; // max. units
	public int startPos; // 0 = fixed, 1 = random, 2 = choose in game
	public int gameEndCondition; // 0 = game continues if commander dies, 1 = game ends if commander dies
	public int hashCode; // see notes for description!
	public int rank; // if 0, no rank limit is set. If 1 or higher, only players with this rank (or higher) can join the battle (Note: rank index 1 means seconds rank, not the first one, since you can't limit game to players of the first rank because that means game is open to all players and you don't have to limit it in that case)
	public String modName;
	public Vector disabledUnits;
	public StartRect[] startRects;
	public boolean limitDGun;
	public boolean diminishingMMs;
	public boolean ghostedBuildings; 
	public boolean locked; // if true, battle is locked and noone can join it (until lock is released by founder)
	// following elements are used only with type=1:
	public Vector replayScript = new Vector(); // contains lines of the script file
	public Vector tempReplayScript = new Vector(); // here we save script lines until we receive SCRIPTEND command. Then we copy it to "replayScript" object and notify all clients about it. 
	
	
	
	/* Creates new Battle object from a command that client sent to server. This method
	 * parses the command (String s) and tries to read battle attributes from it. If
	 * unsuccessful, it returns null as a result. */
	public static Battle createBattleFromString(String s, Client founder) {
		String[] parsed = s.split(" ");
		if (parsed.length < 17) return null;
		String[] parsed2 = Misc.makeSentence(parsed, 16).split("\t");
		if (parsed2.length != 3) return null;
		
		String pass = parsed[3];
		if (!pass.equals("*")) if (!Misc.isValidName(pass)) return null;
		
		int type;
		int natType;
		int port;
		int maxPlayers;
		int startMetal;
		int startEnergy;
		int maxUnits;
		int startPos;
		int gameEndCondition;
		boolean limitDGun;
		boolean diminishingMMs;
		boolean ghostedBuildings;
		int hash;
		int rank;
		
		try {
			type = Integer.parseInt(parsed[1]);
			natType = Integer.parseInt(parsed[2]);
			// parsed[3] is password
			port = Integer.parseInt(parsed[4]);
			maxPlayers = Integer.parseInt(parsed[5]);
			startMetal = Integer.parseInt(parsed[6]);
			startEnergy = Integer.parseInt(parsed[7]);
			maxUnits = Integer.parseInt(parsed[8]);
			startPos = Integer.parseInt(parsed[9]);
			gameEndCondition = Integer.parseInt(parsed[10]);
			limitDGun = Misc.strToBool(parsed[11]);
			diminishingMMs = Misc.strToBool(parsed[12]);
			ghostedBuildings = Misc.strToBool(parsed[13]);			
			hash = Integer.parseInt(parsed[14]);
			rank = Integer.parseInt(parsed[15]);
		} catch (NumberFormatException e) {
			return null; 
		}
		
		if ((startPos < 0) || (startPos > 2)) return null;
		if ((gameEndCondition < 0) || (gameEndCondition > 1)) return null;
		if ((type < 0) || (type > 1)) return null;
		if ((natType < 0) || (natType > 2)) return null;

		return new Battle(type, natType, founder, pass, port, maxPlayers, startMetal, startEnergy, maxUnits, startPos, gameEndCondition, limitDGun, diminishingMMs, ghostedBuildings, hash, rank, parsed2[0], parsed2[1], parsed2[2]);
	}
	
	public Battle(int type, int natType, Client founder, String password, int port, int maxPlayers, int startMetal, int startEnergy, int maxUnits, int startPos, int gameEndCondition, boolean limitDGun, boolean diminishingMMs, boolean ghostedBuildings, int hashCode, int rank, String mapName, String title, String modName) {
		this.ID = IDCounter++;
		this.type = type;
		this.natType = natType;
		this.title = new String(title);
		this.founder = founder;
		this.clients = new Vector();
		this.bots = new Vector();
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
		this.modName = new String(modName);
		this.disabledUnits = new Vector();
		this.startRects = new StartRect[10];
		this.locked = false; // we assume this by default. Client must make sure it is unlocked.
		for (int i = 0; i < startRects.length; i++) startRects[i] = new StartRect();
	}
	
	/* creates BATTLEOPENED command from this battle and returns it as a result */
	public String createBattleOpenedCommand() {
		return "BATTLEOPENED " + ID + " " + type + " " + natType + " " + founder.account.user + " " + founder.IP + " "+ port + " " + maxPlayers + " " + Misc.boolToStr(restricted()) + " " + rank + " " + mapName + "\t" + title + "\t" + modName;
	}
	
	/* same as createBattleOpenedCommand() but requires sender to tell what IP to use (local or external) */
	public String createBattleOpenedCommandEx(boolean local) {
		String ip;
		if (local) ip = founder.localIP;
		else ip = founder.IP;
		return "BATTLEOPENED " + ID + " " + type + " " + natType + " " + founder.account.user + " " + ip + " "+ port + " " + maxPlayers + " " + Misc.boolToStr(restricted()) + " " + rank + " " + mapName + "\t" + title + "\t" + modName;
	}
	
	/* sends series of CLIENTBATTLESTATUS command to client telling him about battle statuses
	 * of all clients in this battle EXCEPT for himself! */
	public void notifyOfBattleStatuses(Client client) {
		for (int i = 0; i < this.clients.size(); i++) 
			if ((Client)this.clients.get(i) == client) continue;
			else client.sendLine("CLIENTBATTLESTATUS " + ((Client)this.clients.get(i)).account.user + " " + ((Client)this.clients.get(i)).battleStatus);
		if (founder != client) client.sendLine("CLIENTBATTLESTATUS " + founder.account.user + " " + founder.battleStatus);
	}
	
	/* notifies all clients in the battle (including the client) about new battle status
	 * of the client. */
	public void notifyClientsOfBattleStatus(Client client) {
		sendToAllClients("CLIENTBATTLESTATUS " + client.account.user + " " + client.battleStatus);
	}
	
	/* sends String s to all clients participating in this battle */
	public void sendToAllClients(String s) {
		for (int i = 0; i < this.clients.size(); i++) {
			((Client)clients.get(i)).sendLine(s);
		}
		founder.sendLine(s);
	}
	
	/* sends String s to all clients participating in this battle except for the founder */
	public void sendToAllExceptFounder(String s) {
		for (int i = 0; i < this.clients.size(); i++) {
			((Client)clients.get(i)).sendLine(s);
		}
	}
	
	private String clientsToString() {
		if (this.clients.size() == 0) return "";
		String s = new String(((Client)clients.get(0)).account.user);
		for (int i = 1; i < this.clients.size(); i++)
			s.concat(" " + ((Client)clients.get(i)).account.user);
		return s;
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
	
	// returns number of spectators in the battle
	public int spectatorCount() {
		int count = 0;
		for (int i = 0; i < clients.size(); i++)
			if (Misc.getModeFromBattleStatus(((Client)clients.get(i)).battleStatus) == 0) count++;
		if (Misc.getModeFromBattleStatus(founder.battleStatus) == 0) count++;
		return count;
	}
	
	public int nonSpectatorCount() {
		return clients.size()+1-spectatorCount();
	}
	
	public boolean isClientInBattle(Client client) {
		for (int i = 0; i < clients.size(); i++)
			if (((Client)clients.get(i)) == client) return true;
		if (founder == client) return true;	
		
		return false;
	}
	
	public boolean isClientInBattle(String username) {
		for (int i = 0; i < clients.size(); i++)
			if (((Client)clients.get(i)).account.user.equals(username)) return true;
		if (founder.account.user.equals(username)) return true;	
		
		return false;
	}
	
	public Client getClient(String username) {
		for (int i = 0; i < clients.size(); i++)
			if (((Client)clients.get(i)).account.user.equals(username)) return (Client)clients.get(i);
		if (founder.account.user.equals(username)) return founder;	
		
		return null;
	}
	
	/* returns -1 if unit is not in disabled list, index otherwise */
	public int getUnitIndexInDisabledList(String unitname) {
		for (int i = 0; i < disabledUnits.size(); i++)
			if (((String)disabledUnits.get(i)).equals((unitname))) return i;
		return -1;	
	}
	
	public void sendDisabledUnitsListToClient(Client client) {
		if (disabledUnits.size() == 0) return ; // nothing to send
		
		String line = (String)disabledUnits.get(0);
		for (int i = 1; i < disabledUnits.size(); i++)
			line = line.concat(" " + (String)disabledUnits.get(i));
			
		client.sendLine("DISABLEUNITS " + line);
	}
	
	public int getBot(String name) {
		for (int i = 0; i < bots.size(); i++)
			if (((Bot)bots.get(i)).name.equals(name)) return i;
		return -1;	
	}

	/* removes first bot in bots list which is owned by client */
	public boolean removeFirstBotOfClient(Client client) {
		for (int i = 0; i < bots.size(); i++)
			if (((Bot)bots.get(i)).ownerName.equals(client.account.user)) {
				sendToAllClients("REMOVEBOT " + ID + " " + ((Bot)bots.get(i)).name);
				bots.remove(i);
				return true;
			}
		return false;	
	}
	
	/* removes all bots owned by client */
	public void removeClientBots(Client client) {
		while (removeFirstBotOfClient(client)) ;
	}
	
	public void sendBotListToClient(Client client) {
		for (int i = 0; i < bots.size(); i++)
			client.sendLine("ADDBOT " + ID + " " + ((Bot)bots.get(i)).name + " " + ((Bot)bots.get(i)).ownerName + " " + ((Bot)bots.get(i)).battleStatus + " " + ((Bot)bots.get(i)).AIDll);
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
			client.sendLine("SCRIPT " + (String)replayScript.get(i));
		}
		client.sendLine("SCRIPTEND");
	}
	
	public void sendScriptToAllExceptFounder() {
		for (int i = 0; i < clients.size(); i++) {
			sendScriptToClient((Client)clients.get(i));
		} 
	}
	
}
