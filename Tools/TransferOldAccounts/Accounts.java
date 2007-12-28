/*
 * Created on 2006.10.15
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 */

/**
 * @author Betalord
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Accounts {
	private static ArrayList<Account> accounts = new ArrayList<Account>(); // note: ArrayList is not synchronized! Use Vector class instead if multiple threads are going to access it.
	
	// 'map' is used to speed up searching for accounts by username (TreeMap class implements efficient Red-Black trees)
	private static TreeMap<String, Account> map = new TreeMap<String, Account>(
            new java.util.Comparator<String> () {
                public int compare(String s1, String s2)
                {
                  return s1.compareTo(s2);
                }
              }
      );

	// same as 'map', only difference is that it ignores case
	private static TreeMap<String, Account> mapNoCase = new TreeMap<String, Account>(
            new java.util.Comparator<String> () {
                public int compare(String s1, String s2)
                {
                  return s1.compareToIgnoreCase(s2);
                }
              }
      );

	private static long saveAccountInfoInterval = 1000 * 60 * 60; // in milliseconds
	private static long lastSaveAccountsTime = System.currentTimeMillis(); // time when we last saved accounts info to disk
	
	public static int getAccountsSize() {
		return accounts.size();
	}
	
	/* (re)loads accounts from disk */
	public static boolean loadAccounts()
	{
		long time = System.currentTimeMillis();
		try {
			BufferedReader in = new BufferedReader(new FileReader("accounts.txt"));

			accounts.clear();
			
			String line;
			String tokens[];
			
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	tokens = line.split(" ");
            	addAccount(new Account(tokens[0], tokens[1], Integer.parseInt(tokens[2], 2), Integer.parseInt(tokens[3]), Long.parseLong(tokens[4]), tokens[5], Long.parseLong(tokens[6]), tokens[7]));
	        }
            
            in.close();
			
		} catch (IOException e) {
			// catch possible io errors from readLine()
			System.out.println("IOException error while trying to update accounts info from accounts.txt!");
			return false;
		}
		
		System.out.println(accounts.size() + " accounts information read from accounts.txt (" + (System.currentTimeMillis() - time) + " ms)");
		
		return true;
	}
	
	// returns 'null' if username is valid, or error description otherwise
	public static String isUsernameValid(String username) {
		if (username.length() > 20) return "Username too long";
		if (username.length() < 2) return "Username too short";
		if (!username.matches("^[A-Za-z0-9_]+$")) return "Username contains invalid characters";
		// everything is OK:
		return null; 
	}
	
	// returns 'null' if password is valid, or error description otherwise
	public static String isPasswordValid(String password) {
		if (password.length() < 2) return "Password too short";
		if (password.length() > 30) return "Password too long"; // md5-base64 encoded passwords require 24 chars
		// we have to allow a bit wider range of possible chars as base64 can produce chars such as +, = and /
		if (!password.matches("^[\\x2B-\\x7A]+$")) return "Password contains invalid characters";
		// everything is OK:
		return null; 
	}
	
	// returns 'null' if username is valid, or error description otherwise.
	// This is used with "old" format of usernames which could also contain "[" and "]" characters.
	public static String isOldUsernameValid(String username) {
		if (username.length() > 20) return "Username too long";
		if (username.length() < 2) return "Username too short";
		if (!username.matches("^[A-Za-z0-9_\\[\\]]+$")) return "Username contains invalid characters";
		// everything is OK:
		return null; 
	}
	
	// returns 'null' if password is valid, or error description otherwise. 'baseUsername' is used to test nickname against
	// (nickname must contain part of username - it may only prefix and postfix the username)
	public static String isNicknameValid(String nickname, String baseUsername) {
		if (nickname.length() > 25) return "Nickname too long";
		if (nickname.length() < 2) return "Nickname too short";

		if (!nickname.matches("^[A-Za-z0-9_\\[\\]\\|]+$")) return "Nickname contains invalid characters";
		
		// check if prefix is valid:
		if (!nickname.matches("^([A-Za-z0-9\\[\\]\\|]+[\\|\\]])?" + baseUsername)) return "Invalid prefix found in nickname: embed your prefix in [] brackets or separate it by a | character";

		// check if postfix is valid:
		if (!nickname.matches(baseUsername + "([\\|\\[][A-Za-z0-9\\[\\]\\|]+)?$")) return "Invalid postfix found in nickname: embed your postfix in [] brackets or separate it by a | character";
		
		// check if prefix and postfix are both valid in one shot:
		if (!nickname.matches("^([A-Za-z0-9\\[\\]\\|]+[\\|\\]])?" + baseUsername + "([\\|\\[][A-Za-z0-9\\[\\]\\|]+)?$")) return "Nickname contains invalid prefix/postfix. Your username should be contained in your nickname!";

		// everything is OK:
		return null; 
	}	
	
	/* WARNING: caller must check if username/password is valid etc. himself! */
	public static void addAccount(Account acc) {
		accounts.add(acc);
		map.put(acc.user, acc);
		mapNoCase.put(acc.user, acc);
	}	
	
	public static boolean addAccountWithCheck(Account acc) {
		if (isUsernameValid(acc.user) != null) return false;
		if (isPasswordValid(acc.pass) != null) return false;

		// check for duplicate entries:
		if (doesAccountExist(acc.user)) return false;
		
		addAccount(acc);
		return true;
	}

	// returns null if account is not found:
	public static Account getAccount(String username) {
		return map.get(username);
	}

	/* returns null if index is out of bounds */
	public static Account getAccount(int index) {
		try {
			return accounts.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public static Account findAccountNoCase(String username) {
		return mapNoCase.get(username);
	}
	
	public static boolean doesAccountExist(String username) {
		return getAccount(username) != null;
	}
	
	public static Account removeFirstAccount() {
		Account acc = accounts.remove(0);
		map.remove(acc.user);
		mapNoCase.remove(acc.user);
		return acc;
	}
	
	public static Account removeAccount(int index) {
		Account acc = accounts.remove(index);
		map.remove(acc.user);
		mapNoCase.remove(acc.user);
		return acc;
	}	
	

}
