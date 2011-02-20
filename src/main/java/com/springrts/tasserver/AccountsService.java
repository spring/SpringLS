/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.tasserver;


import java.net.InetAddress;
import java.util.List;

/**
 * Data Access Object (DAO) interface for <tt>Account</tt>s.
 * Used for retrieving and storing accounts to permanent storage,
 * for example in files or a DB.
 *
 * @author hoijui
 */
public interface AccountsService extends ContextReceiver, LiveStateListener,
		Updateable
{

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
	 * - last login is not more then 1 week ago
	 * - rank is higher then Newbie/Rank 1
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
	 *              If true, it will not return until the accounts have been
	 *              saved.
	 */
	public void saveAccounts(boolean block);

	/**
	 * Saves accounts only if they have not been saved for some time.
	 * This method should be called periodically!
	 * @see #saveAccounts(boolean block)
	 */
	public void saveAccountsIfNeeded();

	/**
	 * Add an account to be persisted.
	 * Note: The caller has to check if username/password is valid etc.!
	 */
	public void addAccount(Account acc);

	/**
	 * Add accounts to be persisted.
	 * Note: The caller has to check if usernames/passwords are valid etc.!
	 */
	public void addAccounts(Iterable<Account> accs);

	public boolean addAccountWithCheck(Account acc);

	public boolean removeAccount(Account acc);

	public boolean removeAccount(String userName);

	/** Returns null if account is not found */
	public Account getAccount(String userName);

	public Account findAccountNoCase(String userName);

	/** Returns 'null' if no account ever connected from this IP */
	public Account findAccountByLastIP(InetAddress ip);

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

	/**
	 * Indicates whether or not it is possible to register new accounts.
	 */
	public boolean isRegistrationEnabled();

	/**
	 * Sets whether or not it is possible to register new accounts.
	 * @return true if new value was successfully set
	 */
	public boolean setRegistrationEnabled(boolean registrationEnabled);

	/**
	 * Checks if a pair of login credentials are valid.
	 * This also obfuscates (by not logging) the reason why the login is
	 * invalid.
	 * @param username
	 * @param password
	 * @return the account that fits to the login credentials,
	 *         or <code>null</code>, in case they are not valid
	 */
	public Account verifyLogin(String username, String password);
}
