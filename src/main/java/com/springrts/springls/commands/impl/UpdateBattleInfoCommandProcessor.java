/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Battle;
import com.springrts.springls.Client;
import com.springrts.springls.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import com.springrts.springls.util.ProtocolUtil;
import java.util.List;

/**
 * Sent by server to all registered clients telling them some of the parameters
 * of the battle changed. Battle internal changes, like starting metal, energy,
 * starting position etc., are sent only to clients.
 * @author hoijui
 */
@SupportedCommand("UPDATEBATTLEINFO")
public class UpdateBattleInfoCommandProcessor extends AbstractCommandProcessor {

	public UpdateBattleInfoCommandProcessor() {
		// only the founder may change battle parameters!
		super(4, ARGS_MAX_NOCHECK, Account.Access.NORMAL, true, true);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		Battle battle = getBattle(client);

		String spectatorCountStr = args.get(0);
		String lockedStr = args.get(1);
		String mapHashStr = args.get(2);
		String mapName = Misc.makeSentence(args, 3);

		int spectatorCount = 0;
		boolean locked;
		int maphash;
		try {
			spectatorCount = Integer.parseInt(spectatorCountStr);
			locked = ProtocolUtil.numberToBool(Byte.parseByte(lockedStr));
			maphash = Integer.decode(mapHashStr);
		} catch (NumberFormatException ex) {
			return false;
		}

		battle.setMapName(mapName);
		battle.setLocked(locked);
		battle.setMapHash(maphash);
		getContext().getClients().sendToAllRegisteredUsers(
				String.format("UPDATEBATTLEINFO %d %d %d %s %s",
				battle.getId(),
				spectatorCount,
				ProtocolUtil.boolToNumber(battle.isLocked()),
				maphash,
				battle.getMapName()));

		return true;
	}
}
