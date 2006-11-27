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
	private static SaveAccountsThread saveAccountsThread = null;
	
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
			BufferedReader in = new BufferedReader(new FileReader(TASServer.ACCOUNTS_INFO_FILEPATH));

			accounts.clear();
			
			String line;
			String tokens[];
			
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	tokens = line.split(" ");
            	MapGradeList mgl;
            	if (tokens.length > 7) mgl = MapGradeList.createFromString(Misc.makeSentence(tokens, 7));
            	else mgl = new MapGradeList();
            	if (mgl == null) {
            		System.out.println("Error: malformed line in accounts data file: \"" + line + "\"");
            		continue;
            	}
            	addAccount(new Account(tokens[0], tokens[1], Integer.parseInt(tokens[2], 2), Long.parseLong(tokens[3]), tokens[4], Long.parseLong(tokens[5]), tokens[6], mgl));
	        }
            
            in.close();
			
		} catch (IOException e) {
			// catch possible io errors from readLine()
			System.out.println("IOException error while trying to update accounts info from " + TASServer.ACCOUNTS_INFO_FILEPATH + "! Skipping ...");
			return false;
		}
		
		System.out.println(accounts.size() + " accounts information read from " + TASServer.ACCOUNTS_INFO_FILEPATH + " (" + (System.currentTimeMillis() - time) + " ms)");
		
		return true;
	}
	
	/* if block==false, this method will spawn a new thread which will save the accounts,
	 * so this method can return immediately (non-blocking mode). If block==true, it will
	 * not return until accounts have been saved to disk. */
	public static void saveAccounts(boolean block) {
		if ((saveAccountsThread != null) && (saveAccountsThread.isAlive())) return; // already in progress. Let's just skip it ...
		
		lastSaveAccountsTime = System.currentTimeMillis();
		saveAccountsThread = new SaveAccountsThread((List)accounts.clone());
		saveAccountsThread.start();

		if (block) {
			try {
				saveAccountsThread.join(); // wait until thread returns
		    } catch (InterruptedException e) {
		    }
		}		
		
		lastSaveAccountsTime = System.currentTimeMillis();
	}
	
	/* will call saveAccounts() only if they haven't been saved for some time.
	 * This method should be called periodically! */
	public static void saveAccountsIfNeeded() {
	    if ((!TASServer.LAN_MODE) && (System.currentTimeMillis() - lastSaveAccountsTime > saveAccountInfoInterval)) {
	    	saveAccounts(false);
	    	// note: lastSaveAccountsTime will get updated in saveAccounts() method!
	    }
	}
	
	/* WARNING: caller must check if username/password is valid etc. himself! */
	public static void addAccount(Account acc) {
		accounts.add(acc);
		map.put(acc.user, acc);
		mapNoCase.put(acc.user, acc);
	}	
	
	public static boolean addAccountWithCheck(Account acc) {
		if (!Misc.isValidName(acc.user)) return false;
		if (!Misc.isValidPass(acc.pass)) return false;

		// check for duplicate entries:
		if (doesAccountExist(acc.user)) return false;
		
		addAccount(acc);
		return true;
	}
	
	public static boolean removeAccount(Account acc) {
		boolean result = accounts.remove(acc);
		map.remove(acc.user);
		mapNoCase.remove(acc.user);
		return result;
	}
	
	public static boolean removeAccount(String username) {
		Account acc = getAccount(username);
		if (acc == null) return false;
		return removeAccount(acc);
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
	
	/* will delete account 'oldAcc' and insert 'newAcc' into his position */
	public static boolean replaceAccount(Account oldAcc, Account newAcc) {
		int index = accounts.indexOf(oldAcc);
		if (index == -1) return false; // 'oldAcc' does not exist!
		
		accounts.set(index, newAcc);
		
		map.remove(oldAcc.user);
		mapNoCase.remove(oldAcc.user);
		map.put(newAcc.user, newAcc);
		mapNoCase.put(newAcc.user, newAcc);
		
		return true;
	}

}
