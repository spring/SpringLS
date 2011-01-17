/*
 * Created on 2006.11.2
 */

package com.springrts.tasserver;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 */
public class Battles implements ContextReceiver {

	private static final Logger s_log  = LoggerFactory.getLogger(Battle.class);

	private List<Battle> battles;
	private Context context = null;


	public Battles() {
		this.battles = new ArrayList<Battle>();
	}


	@Override
	public void receiveContext(Context context) {

		this.context = context;
		for (Battle battle : battles) {
			battle.receiveContext(context);
		}
	}

	public void verify(Battle battle) {

		if (battle == null) {
			s_log.error("Invalid battle ID. Server will now exit!");
			context.getServerThread().closeServerAndExit();
		}
	}

	public int getBattlesSize() {
		return battles.size();
	}

	/**
	 * If battle with id 'battleID' exist, it is returned,
	 * or else null is returned.
	 */
	public Battle getBattleByID(int battleID) {
		for (int i = 0; i < battles.size(); i++) {
			if (battles.get(i).getId() == battleID) {
				return battles.get(i);
			}
		}
		return null;
	}

	/** Returns null if index is out of bounds */
	public Battle getBattleByIndex(int index) {
		try {
			return battles.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** Will close given battle and notify all clients about it */
	public void closeBattleAndNotifyAll(Battle battle) {

		for (int i = 0; i < battle.getClientsSize(); i++) {
			battle.getClient(i).setBattleID(Battle.NO_BATTLE_ID);
		}
		battle.getFounder().setBattleID(Battle.NO_BATTLE_ID);
		context.getClients().sendToAllRegisteredUsers("BATTLECLOSED " + battle.getId());
		battles.remove(battle);
	}

	/**
	 * Removes client from a battle and notifies everyone. Also automatically checks if
	 * client is founder and closes the battle in that case. All client's bots in this
	 * battle are removed as well.
	 */
	public boolean leaveBattle(Client client, Battle battle) {

		if (battle.getFounder() == client) {
			closeBattleAndNotifyAll(battle);
		} else {
			if (client.getBattleID() != battle.getId() || !battle.removeClient(client)) {
				return false;
			}
			client.setBattleID(Battle.NO_BATTLE_ID);
			battle.removeClientBots(client);
			context.getClients().sendToAllRegisteredUsers(new StringBuilder("LEFTBATTLE ")
					.append(battle.getId()).append(" ")
					.append(client.getAccount().getName()).toString());
		}

		return true;
	}

	/**
	 * Will send a list of all active battles and users participating in it to
	 * the given client
	 */
	public void sendInfoOnBattlesToClient(Client client) {

		client.beginFastWrite();
		for (int i = 0; i < battles.size(); i++) {
			Battle bat = battles.get(i);
			// make sure that clients behind NAT get local IPs and not external ones
			boolean local = bat.getFounder().getIp().equals(client.getIp());
			client.sendLine(bat.createBattleOpenedCommandEx(local));
			// We have to send UPDATEBATTLEINFO command too,
			// in order to tell the user how many spectators are in the battle,
			// for example.
			client.sendLine(new StringBuilder("UPDATEBATTLEINFO ")
					.append(bat.getId()).append(" ")
					.append(bat.spectatorCount()).append(" ")
					.append(Misc.boolToStr(bat.isLocked())).append(" ")
					.append(bat.getMapHash()).append(" ")
					.append(bat.getMapName()).toString());
			for (int j = 0; j < bat.getClientsSize(); j++) {
				client.sendLine(new StringBuilder("JOINEDBATTLE ")
						.append(bat.getId()).append(" ")
						.append(bat.getClient(j).getAccount().getName()).toString());
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
	public Battle createBattleFromString(List<String> args, Client founder) {

		if (args.size() < 9) {
			return null;
		}

		String[] parsed2 = Misc.makeSentence(args, 8).split("\t");
		if (parsed2.length != 3) {
			return null;
		}

		String pass = args.get(2);
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
			type = Integer.parseInt(args.get(0));
			natType = Integer.parseInt(args.get(1));
			// args.get(2) is password
			port = Integer.parseInt(args.get(3));
			maxPlayers = Integer.parseInt(args.get(4));
			hash = Integer.parseInt(args.get(5));
			rank = Integer.decode(args.get(6));
			maphash = Integer.decode(args.get(7));
		} catch (NumberFormatException e) {
			return null;
		}

		if ((type < 0) || (type > 1)) {
			return null;
		}
		if ((natType < 0) || (natType > 2)) {
			return null;
		}

		Battle battle = new Battle(type, natType, founder, pass, port, maxPlayers, hash, rank, maphash, parsed2[0], parsed2[1], parsed2[2]);

		return battle;
	}

	/**
	 * Will add this battle object to battle list
	 */
	public void addBattle(Battle battle) {

		battles.add(battle);
		battle.receiveContext(context);
	}
}
