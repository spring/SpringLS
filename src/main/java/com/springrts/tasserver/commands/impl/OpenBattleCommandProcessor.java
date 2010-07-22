package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Battle;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Clients;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.CommandProcessor;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("OPENBATTLE")
public class OpenBattleCommandProcessor extends AbstractCommandProcessor
		implements CommandProcessor {

	public OpenBattleCommandProcessor() {
		super(Account.Access.NORMAL);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (client.getBattleID() != Battle.NO_BATTLE_ID) {
			client.sendLine("OPENBATTLEFAILED You are already hosting a battle!");
			return false;
		}
		Battle bat = getContext().getBattles().createBattleFromString(args, client);
		if (bat == null) {
			client.sendLine("OPENBATTLEFAILED Invalid command format or bad arguments");
			return false;
		}
		getContext().getBattles().addBattle(bat);
		client.setBattleStatus(0); // reset client's battle status
		client.setBattleID(bat.getId());
		client.setRequestedBattleID(Battle.NO_BATTLE_ID);

		boolean local;
		Clients clients = getContext().getClients();
		for (int i = 0; i < clients.getClientsSize(); i++) {
			Client c = clients.getClient(i);
			if (c.getAccount().getAccess().compareTo(Account.Access.NORMAL) < 0) {
				continue;
			}
			// make sure that clients behind NAT get local IPs and not external ones:
			local = client.getIp().equals(c.getIp());
			c.sendLine(bat.createBattleOpenedCommandEx(local));
		}

		// notify client that he successfully opened a new battle
		client.sendLine("OPENBATTLE " + bat.getId());
		client.sendLine("REQUESTBATTLESTATUS");
		return true;
	}
}
