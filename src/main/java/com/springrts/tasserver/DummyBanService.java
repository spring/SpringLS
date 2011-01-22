/*
 * Created on 2. April 2010
 */

package com.springrts.tasserver;


import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Dummy implementation of a BanService.
 * This will always contain 0 bans.
 */
public class DummyBanService implements BanService{

	@Override
	public int getBansSize() {
		return 0;
	}

	@Override
	public int getActiveBansSize() {
		return 0;
	}

	@Override
	public void addBanEntry(BanEntry ban) {}

	@Override
	public boolean removeBanEntry(BanEntry ban) {
		return true;
	}

	@Override
	public BanEntry getBanEntry(String username, InetAddress ip, int userId) {
		return null;
	}

	@Override
	public boolean mergeBanEntryChanges(BanEntry ban) {
		return true;
	}

	@Override
	public List<BanEntry> fetchAllBanEntries() {
		return new ArrayList<BanEntry>(0);
	}

	@Override
	public List<BanEntry> fetchActiveBanEntries() {
		return new ArrayList<BanEntry>(0);
	}
}
