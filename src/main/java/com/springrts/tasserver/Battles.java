/*
 * Created on 2006.11.2
 */

package com.springrts.tasserver;


import java.util.ArrayList;

/**
 * @author Betalord
 */
public class Battles {

	private static ArrayList<Battle> battles = new ArrayList<Battle>();

	public static int getBattlesSize() {
		return battles.size();
	}

	/**
	 * If battle with ID 'battleID' exist, it is returned,
	 * or else null is returned.
	 */
	public static Battle getBattleByID(int battleID) {
		for (int i = 0; i < battles.size(); i++)
			if (battles.get(i).ID == battleID) return battles.get(i);
		return null;
	}

	/** Returns null if index is out of bounds */
	public static Battle getBattleByIndex(int index) {
		try {
			return battles.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** Will close given battle and notify all clients about it */
	public static void closeBattleAndNotifyAll(Battle battle) {

		for (int i = 0; i < battle.getClientsSize(); i++) {
			battle.getClient(i).battleID = -1;
		}
		battle.founder.battleID = -1;
		Clients.sendToAllRegisteredUsers("BATTLECLOSED " + battle.ID);
		battles.remove(battle);
	}

	/**
	 * Removes client from a battle and notifies everyone. Also automatically checks if
	 * client is founder and closes the battle in that case. All client's bots in this
	 * battle are removed as well.
	 */
	public static boolean leaveBattle(Client client, Battle battle) {

		if (battle.founder == client) {
			closeBattleAndNotifyAll(battle);
		} else {
			if (client.battleID != battle.ID || !battle.removeClient(client)) {
				return false;
			}
			client.battleID = -1;
			battle.removeClientBots(client);
			Clients.sendToAllRegisteredUsers(new StringBuilder("LEFTBATTLE ")
					.append(battle.ID).append(" ")
					.append(client.account.user).toString());
		}

		return true;
	}

	/** Will send a list of all active battles and users participating in it to the given client */
	public static void sendInfoOnBattlesToClient(Client client) {

		client.beginFastWrite();
		for (int i = 0; i < battles.size(); i++) {
			Battle bat = battles.get(i);
			// make sure that clients behind NAT get local IPs and not external ones:
			boolean local = bat.founder.IP.equals(client.IP);
			client.sendLine(bat.createBattleOpenedCommandEx(local));
			// we have to send UPDATEBATTLEINFO command too in order to tell the user how many spectators are in the battle, for example.
			client.sendLine(new StringBuilder("UPDATEBATTLEINFO ")
					.append(bat.ID).append(" ")
					.append(bat.spectatorCount()).append(" ")
					.append(Misc.boolToStr(bat.locked)).append(" ")
					.append(bat.mapHash).append(" ")
					.append(bat.mapName).toString());
			for (int j = 0; j < bat.getClientsSize(); j++) {
				client.sendLine(new StringBuilder("JOINEDBATTLE ")
						.append(bat.ID).append(" ")
						.append(bat.getClient(j).account.user).toString());
			}
		}
		client.endFastWrite();
	}

	/**
	 * Creates new Battle object from a command that client sent to server.
	 * This method parses the command 's', and tries to read
	 * battle attributes from it.
	 * @return the created battle or 'null' on failure
	 */
	public static Battle createBattleFromString(String s, Client founder) {

		String[] parsed = s.split(" ");
		if (parsed.length < 10) {
			return null;
		}

		String[] parsed2 = Misc.makeSentence(parsed, 9).split("\t");
		if (parsed2.length != 3) {
			return null;
		}

		String pass = parsed[3];
		if (!pass.equals("*") && !pass.matches("^[A-Za-z0-9_]+$")) {
			// invalid characters in the password
			return null;
		}

		int type;
		int natType;
		int port;
		int maxPlayers;
		int hash;
		int rank;
		int maphash;

		try {
			type = Integer.parseInt(parsed[1]);
			natType = Integer.parseInt(parsed[2]);
			// parsed[3] is password
			port = Integer.parseInt(parsed[4]);
			maxPlayers = Integer.parseInt(parsed[5]);
			hash = Integer.parseInt(parsed[6]);
			rank = Integer.decode(parsed[7]);
			maphash = Integer.decode(parsed[8]);
		} catch (NumberFormatException e) {
			return null;
		}

		if ((type < 0) || (type > 1)) {
			return null;
		}
		if ((natType < 0) || (natType > 2)) {
			return null;
		}

		return new Battle(type, natType, founder, pass, port, maxPlayers, hash, rank, maphash, parsed2[0], parsed2[1], parsed2[2]);
	}

	/** Will add this battle object to battle list */
	public static void addBattle(Battle battle) {
		battles.add(battle);
	}
}
