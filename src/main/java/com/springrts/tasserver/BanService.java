/*
 * Created on 4. February 2010
 */

package com.springrts.tasserver;


import java.net.InetAddress;
import java.util.List;

/**
 * Data Access Object (DAO) interface for BanEntry's.
 * Used for retrieving and storing bans to permanent storage,
 * eg. in files or to a DB.
 *
 * @author hoijui
 */
public interface BanService {

	/**
	 * Returns the number of all enlisted bans.
	 */
	public int getBansSize();

	/**
	 * Returns the number of all bans currently active.
	 */
	public int getActiveBansSize();

	/** WARNING: caller must check if username/password is valid etc. himself! */
	public void addBanEntry(BanEntry ban);

	//public boolean addBanEntryWithCheck(BanEntry ban);

	public boolean removeBanEntry(BanEntry ban);

	/** Returns null if no matching ban is found */
	public BanEntry getBanEntry(String username, InetAddress ip, int userId);

	/**
	 * Save changes to a ban entry to permanent storage.
	 * @param ban the ban entry which got changed
	 * @return 'true' if changes were saved successfully
	 */
	public boolean mergeBanEntryChanges(BanEntry ban);

	/**
	 * Loads all ban entries from the persistent storage into memory.
	 * This should only be used for maintenance task, and not during general
	 * server up-time.
	 */
	public List<BanEntry> fetchAllBanEntries();

	/**
	 * Loads all active ban entries from the persistent storage into memory.
	 * This should only be used for maintenance task, and not during general
	 * server up-time.
	 */
	public List<BanEntry> fetchActiveBanEntries();
}
