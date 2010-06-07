/*
 * Created on 2009.10.17
 */

package com.springrts.tasserver;

import java.util.List;


/**
 * Data Access Object (DAO) interface for Account's.
 * Used for retrieving, and storing accounts to permanent storage,
 * eg. in files or to a DB.
 *
 * @author Betalord
 * @author hoijui
 */
public interface AccountsService extends ContextReceiver, LiveStateListener {

	/**
	 * Checks resources required for the service to operate.
	 */
	public boolean isReadyToOperate();

	/**
	 * Returns the number of all accounts.
	 */
	public int getAccountsSize();

	/**
	 * Returns the number of all active accounts.
	 * An account has to full fill two criteria to be considered active:
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
	 * Saves accounts only if they have not been saved for some time.
	 * This method should be called periodically!
	 * @see saveAccounts()
	 */
	public void saveAccountsIfNeeded();

	/** WARNING: caller must check if username/password is valid etc. himself! */
	public void addAccount(Account acc);

	/** WARNING: caller must check if usernames/passwords are valid etc. himself! */
	public void addAccounts(Iterable<Account> accs);

	public boolean addAccountWithCheck(Account acc);

	public boolean removeAccount(Account acc);

	public boolean removeAccount(String userName);

	/** Returns null if account is not found */
	public Account getAccount(String userName);

	public Account findAccountNoCase(String userName);

	/** Returns 'null' if no account ever connected from this IP */
	public Account findAccountByLastIP(String ip);

	public boolean doesAccountExist(String userName);

	/**
	 * Save changes to an account to permanent storage.
	 * @param account the account which got changed
	 * @param oldName the old value of the name attribute of the account
	 *                is only used by the 'FSAccountsService'
	 * @return 'true' if changes were saved successfully
	 */
	public boolean mergeAccountChanges(Account account, String oldName);

	/**
	 * Loads all accounts from the persistent storage into memory.
	 * This should only be used for maintenance task, and not during general
	 * server up-time.
	 */
	public List<Account> fetchAllAccounts();
}
