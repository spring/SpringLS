/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Betalord
 */
public class FSAccountsService implements AccountsService {

	private static final Log s_log  = LogFactory.getLog(FSAccountsService.class);

	// note: ArrayList is not synchronized!
	// Use Vector class instead if multiple threads are going to access it
	private static List<Account> accounts = new ArrayList<Account>();
	private static SaveAccountsThread saveAccountsThread = null;
	private static int biggestAccountId = 1000;

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
	/** in milliseconds */
	private static long saveAccountInfoInterval = 1000 * 60 * 60;
	/**
	 * Time we last saved accounts info to disk.
	 * @see System.currentTimeMillis()
	 */
	private static long lastSaveAccountsTime = System.currentTimeMillis();

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
			if ((act.getRank().compareTo(Account.Rank.Newbie) > 0) &&
					(act.getLastLogin() > oneWeekAgo)) {
				activeAccounts++;
			}
		}

		return activeAccounts;
	}

	/**
	 * (Re-)Loads accounts from disk.
	 * @return false if loading failed, true otherwise
	 */
	@Override
	public boolean loadAccounts() {
		long time = System.currentTimeMillis();
		try {
			BufferedReader in = new BufferedReader(new FileReader(TASServer.ACCOUNTS_INFO_FILEPATH));

			accounts.clear();

			String line;
			String tokens[];

			while ((line = in.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				tokens = line.split(" ");
				int accountId = Account.NEW_ACCOUNT_ID;
				if (tokens.length >= 9) {
					accountId = Integer.parseInt(tokens[8]);
				}
				final int accessBitField = Integer.parseInt(tokens[2]);
				addAccount(new Account(
						tokens[0],
						tokens[1],
						Account.extractAccess(accessBitField),
						Integer.parseInt(tokens[3]),
						Long.parseLong(tokens[4]),
						tokens[5],
						Long.parseLong(tokens[6]),
						tokens[7],
						accountId,
						Account.extractBot(accessBitField),
						Account.extractInGameTime(accessBitField),
						Account.extractAgreementAccepted(accessBitField)));
			}

			in.close();

		} catch (IOException e) {
			// catch possible io errors from readLine()
			s_log.error("Failed updating accounts info from " + TASServer.ACCOUNTS_INFO_FILEPATH + "! Skipping ...", e);
			return false;
		}

		s_log.info(accounts.size() + " accounts information read from " + TASServer.ACCOUNTS_INFO_FILEPATH + " (" + (System.currentTimeMillis() - time) + " ms)");

		return true;
	}

	/**
	 * Saves accounts to permanent storage.
	 * @param block if false, this method will spawn a new thread,
	 *              so this method can return immediately (non-blocking mode).
	 *              If true, it will not return until the accounts have been saved.
	 */
	@Override
	public void saveAccounts(boolean block) {
		if ((saveAccountsThread != null) && (saveAccountsThread.isAlive())) {
			return; // already in progress. Let's just skip it ...
		}
		lastSaveAccountsTime = System.currentTimeMillis();
		List<Account> accounts_cpy = new ArrayList<Account>(accounts.size());
		Collections.copy(accounts_cpy, accounts);
		saveAccountsThread = new SaveAccountsThread(accounts_cpy);
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
		if ((!TASServer.LAN_MODE) && (System.currentTimeMillis() - lastSaveAccountsTime > saveAccountInfoInterval)) {
			saveAccounts(false);
			// note: lastSaveAccountsTime will get updated in saveAccounts() method!
		}
	}

	/**
	 * Returns 'null' if username is valid; an error description otherwise.
	 */
	@Override
	public String isUsernameValid(String username) {
		if (username.length() > 20) {
			return "Username too long";
		}
		if (username.length() < 2) {
			return "Username too short";
		}
		if (!username.matches("^[A-Za-z0-9_]+$")) {
			return "Username contains invalid characters";
		}
		// everything is OK:
		return null;
	}

	/**
	 * Returns 'null' if password is valid; an error description otherwise.
	 */
	@Override
	public String isPasswordValid(String password) {
		if (password.length() < 2) {
			return "Password too short";
		}
		if (password.length() > 30) {
			return "Password too long"; // md5-base64 encoded passwords require 24 chars
		}		// we have to allow a bit wider range of possible chars as base64 can produce chars such as +, = and /
		if (!password.matches("^[\\x2B-\\x7A]+$")) {
			return "Password contains invalid characters";
		}
		// everything is OK:
		return null;
	}

	/**
	 * Returns 'null' if username is valid; an error description otherwise.
	 * This is used with "old" format of usernames which could also contain "[" and "]" characters.
	 */
	@Override
	public String isOldUsernameValid(String username) {
		if (username.length() > 20) {
			return "Username too long";
		}
		if (username.length() < 2) {
			return "Username too short";
		}
		if (!username.matches("^[A-Za-z0-9_\\[\\]]+$")) {
			return "Username contains invalid characters";
		}
		// everything is OK:
		return null;
	}

	/**
	 * Returns 'null' if password is valid; an error description otherwise.
	 * The nickname must contain part of username - it may only prefix and postfix the username.
	 * @param baseUsername used to test nickname against
	 */
	@Override
	public String isNicknameValid(String nickname, String baseUsername) {
		if (nickname.length() > 20) {
			return "Nickname too long";
		}
		if (nickname.length() < 2) {
			return "Nickname too short";
		}

		if (!nickname.matches("^[A-Za-z0-9_\\[\\]\\|]+$")) {
			return "Nickname contains invalid characters";
		}

		// check if prefix is valid:
		if (!nickname.matches("^([A-Za-z0-9\\[\\]\\|]+[\\|\\]])?" + baseUsername)) {
			return "Invalid prefix found in nickname: embed your prefix in [] brackets or separate it by a | character";
		}

		// check if postfix is valid:
		if (!nickname.matches(baseUsername + "([\\|\\[][A-Za-z0-9\\[\\]\\|]+)?$")) {
			return "Invalid postfix found in nickname: embed your postfix in [] brackets or separate it by a | character";
		}

		// check if prefix and postfix are both valid in one shot:
		if (!nickname.matches("^([A-Za-z0-9\\[\\]\\|]+[\\|\\]])?" + baseUsername + "([\\|\\[][A-Za-z0-9\\[\\]\\|]+)?$")) {
			return "Nickname contains invalid prefix/postfix. Your username should be contained in your nickname!";
		}

		// everything is OK:
		return null;
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
	public boolean addAccountWithCheck(Account acc) {
		if (isUsernameValid(acc.getName()) != null) {
			return false;
		}
		if (isPasswordValid(acc.getPassword()) != null) {
			return false;
		}

		// check for duplicate entries:
		if (doesAccountExist(acc.getName())) {
			return false;
		}

		addAccount(acc);
		return true;
	}

	@Override
	public boolean removeAccount(Account acc) {
		boolean result = accounts.remove(acc);
		map.remove(acc.getName());
		mapNoCase.remove(acc.getName());
		return result;
	}

	@Override
	public boolean removeAccount(String username) {
		Account acc = getAccount(username);
		if (acc == null) {
			return false;
		}
		return removeAccount(acc);
	}

	/** Returns null if account is not found */
	@Override
	public Account getAccount(String username) {
		return map.get(username);
	}

	/** Returns null if index is out of bounds */
	@Override
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
	public boolean doesAccountExist(String username) {
		return getAccount(username) != null;
	}

	/** Will delete account 'oldAcc' and insert 'newAcc' into his position */
	@Override
	public boolean replaceAccount(Account oldAcc, Account newAcc) {
		int index = accounts.indexOf(oldAcc);
		if (index == -1) {
			return false; // 'oldAcc' does not exist!
		}
		accounts.set(index, newAcc);

		map.remove(oldAcc.getName());
		mapNoCase.remove(oldAcc.getName());
		map.put(newAcc.getName(), newAcc);
		mapNoCase.put(newAcc.getName(), newAcc);

		return true;
	}
}