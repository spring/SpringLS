/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Betalord
 */
public class FSAccountsService extends AbstractAccountsService implements AccountsService {

	private static final Log s_log  = LogFactory.getLog(FSAccountsService.class);

	/** in milliseconds */
	private static final long SAVE_ACCOUNT_INFO_INTERVAL = 1000 * 60 * 60;

	public static final String ACCOUNTS_INFO_FILEPATH = "accounts.txt";

	// note: ArrayList is not synchronized!
	// Use Collections.synchronizedList(...) instead,
	// if multiple threads are going to access it
	private List<Account> accounts = new ArrayList<Account>();
	private FSSaveAccountsThread saveAccountsThread = null;
	private int biggestAccountId = 1000;

	/**
	 * Time we last saved accounts info to disk.
	 * @see System.currentTimeMillis()
	 */
	private long lastSaveAccountsTime = System.currentTimeMillis();

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
	private static TreeMap<String, Account> mapNoCase = new TreeMap<String, Account>(
			new java.util.Comparator<String>() {

				public int compare(String s1, String s2) {
					return s1.compareToIgnoreCase(s2);
				}
			});


	@Override
	public boolean isReadyToOperate() {

		if (!(new File(ACCOUNTS_INFO_FILEPATH)).exists()) {
			s_log.warn("Accounts info file not found");
			return false;
		}

		return true;
	}

	@Override
	public int getAccountsSize() {
		return accounts.size();
	}

	@Override
	public int getActiveAccountsSize() {

		int activeAccounts = 0;

		final long oneWeekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
		for (int i = 0; i < getAccountsSize(); i++) {
			Account act = getAccount(i);
			if ((act.getRank().compareTo(Account.Rank.Newbie) > 0)
					&& (act.getLastLogin() > oneWeekAgo))
			{
				activeAccounts++;
			}
		}

		return activeAccounts;
	}


	/**
	 * Used to save an Account to persistent file storage.
	 * Only change this if you know what you are doing!
	 */
	public static String toPersistentString(final Account account) {
		return new StringBuilder(account.getName()).append(" ")
				.append(account.getPassword()).append(" ")
				.append(Integer.toString(account.getAccessBitField(), 2)).append(" ")
				.append(account.getLastUserId()).append(" ")
				.append(account.getLastLogin()).append(" ")
				.append(account.getLastIP()).append(" ")
				.append(account.getRegistrationDate()).append(" ")
				.append(account.getLastCountry()).toString();
	}
	/**
	 * Used to load a persistent Account from file storage.
	 * Only change this if you know what you are doing!
	 */
	public static Account parsePersistenString(final String actStr) {

		final String[] actParts = actStr.split(" ");
		int accountId = Account.NEW_ACCOUNT_ID;
		if (actParts.length >= 9) {
			accountId = Integer.parseInt(actParts[8]);
		}
		final int accessBitField = Integer.parseInt(actParts[2], 2); // input is of the form "1100110"
		Account act = new Account(
				actParts[0],
				actParts[1],
				Account.extractAccess(accessBitField),
				Integer.parseInt(actParts[3]),
				Long.parseLong(actParts[4]),
				actParts[5],
				Long.parseLong(actParts[6]),
				actParts[7],
				accountId,
				Account.extractBot(accessBitField),
				Account.extractInGameTime(accessBitField),
				Account.extractAgreementAccepted(accessBitField));

		return act;
	}

	/**
	 * (Re-)Loads accounts from disk.
	 * @return false if loading failed, true otherwise
	 */
	@Override
	public boolean loadAccounts() {
		long time = System.currentTimeMillis();
		try {
			BufferedReader in = new BufferedReader(new FileReader(ACCOUNTS_INFO_FILEPATH));

			accounts.clear();

			String line;

			while ((line = in.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				Account act = FSAccountsService.parsePersistenString(line);
				addAccount(act);
			}

			in.close();

		} catch (IOException e) {
			// catch possible io errors from readLine()
			s_log.error("Failed updating accounts info from " + ACCOUNTS_INFO_FILEPATH + "! Skipping ...", e);
			return false;
		}

		s_log.info(accounts.size() + " accounts information read from " + ACCOUNTS_INFO_FILEPATH + " (" + (System.currentTimeMillis() - time) + " ms)");

		return true;
	}

	/**
	 * Saves accounts to permanent storage.
	 * @param block if false, this method will spawn a new thread,
	 *              so this method can return immediately (non-blocking mode).
	 *              If 'true', it will not return until the accounts have been saved.
	 */
	@Override
	public void saveAccounts(boolean block) {
		if ((saveAccountsThread != null) && (saveAccountsThread.isAlive())) {
			return; // already in progress. Let's just skip it ...
		}
		lastSaveAccountsTime = System.currentTimeMillis();
		List<Account> accountsCopy = new ArrayList<Account>(accounts.size());
		Collections.copy(accountsCopy, accounts);
		saveAccountsThread = new FSSaveAccountsThread(new File(ACCOUNTS_INFO_FILEPATH), accountsCopy);
		saveAccountsThread.receiveContext(getContext());
		saveAccountsThread.start();

		if (block) {
			try {
				saveAccountsThread.join(); // wait until thread returns
			} catch (InterruptedException e) {
				// do nothing
			}
		}

		lastSaveAccountsTime = System.currentTimeMillis();
	}

	/**
	 * Will call saveAccounts() only if they haven't been saved for some time.
	 * This method should be called periodically!
	 */
	@Override
	public void saveAccountsIfNeeded() {

		// note: lastSaveAccountsTime will get updated in saveAccounts() method!
		long timeSinceLastSave = System.currentTimeMillis() - lastSaveAccountsTime;
		if (!getContext().getServer().isLanMode() && (timeSinceLastSave > SAVE_ACCOUNT_INFO_INTERVAL)) {
			saveAccounts(false);
		}
	}

	/** WARNING: caller must check if username/password is valid etc. himself! */
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
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public Account findAccountNoCase(String username) {
		return mapNoCase.get(username);
	}

	@Override
	public Account findAccountByLastIP(String ip) {

		Account account = null;

		final String[] ipSplit = ip.split("\\.");
		for (int i = 0; i < getAccountsSize(); i++) {
			Account actTmp = getAccount(i);
			if (!Misc.isSameIP(ipSplit, actTmp.getLastIP())) {
				continue;
			}
			account = actTmp;
		}

		return account;
	}

	@Override
	public boolean mergeAccountChanges(Account account, String oldName) {

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
