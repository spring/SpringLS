/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * This will be used in LAN mode only, which means that you can login with any
 * account credentials.
 * @author hoijui
 */
public class LanAccountsService extends AbstractAccountsService {

	private List<Account> accounts;
	private int biggestAccountId;

	/**
	 * Used to speed up searching for accounts by username.
	 * (TreeMap class implements efficient Red-Black trees)
	 * @see mapNoCase
	 */
	private static TreeMap<String, Account> map = new TreeMap<String, Account>(
			new java.util.Comparator<String>() {

				public int compare(String s1, String s2) {
					return s1.compareTo(s2);
				}
			});

	/**
	 * Same as 'map', only that this ignores case.
	 * @see map
	 */
	private static TreeMap<String, Account> mapNoCase
			= new TreeMap<String, Account>(
			new java.util.Comparator<String>() {

				public int compare(String s1, String s2) {
					return s1.compareToIgnoreCase(s2);
				}
			});


	public LanAccountsService() {

		// NOTE ArrayList is not synchronized!
		// Use Collections.synchronizedList(...) instead,
		// if multiple threads are going to access it.
		accounts = new ArrayList<Account>();
		biggestAccountId = 1000;
	}


	@Override
	public boolean isReadyToOperate() {
		return true;
	}

	@Override
	public int getAccountsSize() {
		return getActiveAccountsSize();
	}

	@Override
	public int getActiveAccountsSize() {
		return accounts.size();
	}

	@Override
	public boolean loadAccounts() {
		return true;
	}

	@Override
	public void saveAccounts(boolean block) {}

	@Override
	public void saveAccountsIfNeeded() {}

	@Override
	public void addAccount(Account acc) {

		if (acc.getId() == Account.NEW_ACCOUNT_ID) {
			acc.setId(++biggestAccountId);
		} else if (acc.getId() > biggestAccountId) {
			biggestAccountId = acc.getId();
		}
		accounts.add(acc);
		map.put(acc.getName(), acc);
		mapNoCase.put(acc.getName(), acc);
	}

	@Override
	public void addAccounts(Iterable<Account> accs) {

		for (Account acc : accs) {
			addAccount(acc);
		}
	}

	@Override
	public boolean removeAccount(Account acc) {

		boolean result = accounts.remove(acc);

		map.remove(acc.getName());
		mapNoCase.remove(acc.getName());

		return result;
	}

	/** Returns null if account is not found */
	@Override
	public Account getAccount(String username) {
		return map.get(username);
	}

	/** Returns 'null' if index is out of bounds */
	public Account getAccount(int index) {

		try {
			return accounts.get(index);
		} catch (IndexOutOfBoundsException ex) {
			return null;
		}
	}

	@Override
	public Account findAccountNoCase(String username) {
		return mapNoCase.get(username);
	}

	@Override
	public Account findAccountByLastIP(InetAddress ip) {
		return null;
	}

	@Override
	public boolean mergeAccountChanges(Account account, String oldName) {

		// persistent here only means, that someone is currently logged in with
		// this name
		final boolean isPersistentAccount = map.containsKey(oldName);
		if (!isPersistentAccount) {
			return false;
		}

		final String newName = account.getName();
		if (!newName.equals(oldName)) {
			// the account was renamed
			map.remove(oldName);
			mapNoCase.remove(oldName);
			map.put(newName, account);
			mapNoCase.put(newName, account);
		}

		return true;
	}

	@Override
	public List<Account> fetchAllAccounts() {
		return accounts;
	}
}
