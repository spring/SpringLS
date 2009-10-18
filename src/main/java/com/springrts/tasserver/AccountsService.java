/*
 * Created on 2009.10.17
 */

package com.springrts.tasserver;

import java.util.Set;


/**
 * Data Access Object (DAO) interface for Account's.
 * Used for retrieving, and storing accounts to permanent storage,
 * eg. in files or to a DB.
 *
 * @author Betalord
 * @author hoijui
 */
public interface AccountsService {

	/**
	 * Returns the number of all accounts.
	 */
	public int getAccountsSize();

	/**
	 * Returns the number of all active accounts.
	 * An account has to fullfill two criterias to be considered active:
	 * * last login is not more then 1 week ago
	 * * rank is higher then Newbie/Rank 1
	 */
	public int getActiveAccountsSize();

	/**
	 * (Re-)Loads accounts from disk.
	 * @return false if loading failed, true otherwise
	 */
	public boolean loadAccounts();

	/**
	 * Saves accounts to permanent storage.
	 * @param block if false, this method will spawn a new thread,
	 *              so this method can return immediately (non-blocking mode).
	 *              If true, it will not return until the accounts have been saved.
	 */
	public void saveAccounts(boolean block);

	/**
	 * Will call saveAccounts() only if they haven't been saved for some time.
	 * This method should be called periodically!
	 */
	public void saveAccountsIfNeeded();

	/** WARNING: caller must check if username/password is valid etc. himself! */
	public void addAccount(Account acc);

	public boolean addAccountWithCheck(Account acc);

	public boolean removeAccount(Account acc);

	public boolean removeAccount(String userName);

	/** Returns null if account is not found */
	public Account getAccount(String userName);

	public Account findAccountNoCase(String userName);

	/** Returns 'null' if no account ever connected from this IP */
	public Account findAccountByLastIP(String ip);

	public boolean doesAccountExist(String userName);

	/** Will delete account 'oldAcc' and insert 'newAcc' into his position */
	public boolean replaceAccount(Account oldAcc, Account newAcc);
}
