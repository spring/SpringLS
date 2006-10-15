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
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * TODO Move writeAccountsInfo() to a separate thread so it won't block when processing
 *      commands like LOGIN, REGISTER, etc. 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeMap;
import java.util.Vector;

public class Accounts {
	private static Vector accounts = new Vector();
	
	// 'map' is used to speed up searching for accounts by username (TreeMap class implements very fast Red-Black trees)
	private static TreeMap map = new TreeMap(
            new java.util.Comparator () {
                public int compare(Object obj1, Object obj2)
                {
                  return ((String)obj1).compareTo((String)obj2);
                }
              }
      );

	// same as 'map', only difference is that is ignores case
	private static TreeMap mapNoCase = new TreeMap(
            new java.util.Comparator () {
                public int compare(Object obj1, Object obj2)
                {
                  return ((String)obj1).compareToIgnoreCase((String)obj2);
                }
              }
      );

	private static long saveAccountInfoInterval = 1000 * 60 * 60; // in milliseconds
	private static long lastSaveAccountsTime = System.currentTimeMillis(); // time when we last saved accounts info to disk
	
	public static int getAccountsSize() {
		return accounts.size();
	}
	
	/* (re)loads accounts information from disk */
	public static boolean readAccountsInfo()
	{
		try {
			BufferedReader in = new BufferedReader(new FileReader(TASServer.ACCOUNTS_INFO_FILEPATH));

			accounts.clear();
			
			String line;
			String tokens[];
			
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	tokens = line.split(" ");
            	MapGradeList mgl;
            	if (tokens.length > 6) mgl = MapGradeList.createFromString(Misc.makeSentence(tokens, 6));
            	else mgl = new MapGradeList();
            	if (mgl == null) {
            		System.out.println("Error: malformed line in accounts data file: \"" + line + "\"");
            		continue;
            	}
            	addAccount(new Account(tokens[0], tokens[1], Integer.parseInt(tokens[2], 2), Long.parseLong(tokens[3]), tokens[4], Long.parseLong(tokens[5]), mgl));
	        }
            
            in.close();
			
		} catch (IOException e) {
			// catch possible io errors from readLine()
			System.out.println("IOException error while trying to update accounts info from " + TASServer.ACCOUNTS_INFO_FILEPATH + "! Skipping ...");
			return false;
		}
		
		System.out.println(accounts.size() + " accounts information read from " + TASServer.ACCOUNTS_INFO_FILEPATH);
		
		return true;
	}
	
	public static boolean writeAccountsInfo()
	{
		lastSaveAccountsTime = System.currentTimeMillis();
		
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(TASServer.ACCOUNTS_INFO_FILEPATH)));			

			for (int i = 0; i < accounts.size(); i++)
			{
				out.println(accounts.get(i).toString());
			}
			
			out.close();
			
		} catch (IOException e) {
			System.out.println("IOException error while trying to write accounts info to " + TASServer.ACCOUNTS_INFO_FILEPATH + "!");
			return false;
		}
		
		System.out.println(accounts.size() + " accounts information written to " + TASServer.ACCOUNTS_INFO_FILEPATH + ".");
		
		return true;
	}
	
	/* will call saveAccountsInfo() only if it hasn't been saved for some time.
	 * This method should be called periodically! */
	public static void writeAccountsInfoIfNeeded() {
	    if ((!TASServer.LAN_MODE) && (System.currentTimeMillis() - lastSaveAccountsTime > saveAccountInfoInterval)) {
	    	writeAccountsInfo();
	    	// note: lastSaveAccountsTime will get updated in writeAccountsInfo() method!
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
	
	public static boolean removeAccount(String username) {
		Account acc = getAccount(username);
		if (acc == null) return false;
		
		accounts.remove(acc);
		map.remove(acc.user);
		mapNoCase.remove(acc.user);
		
		return true;
	}	
	
	public static boolean removeAccount(Account acc) {
		boolean result = accounts.remove(acc);
		map.remove(acc.user);
		mapNoCase.remove(acc.user);
		return result;
	}

	// returns null if account is not found:
	public static Account getAccount(String username) {
		return (Account)map.get(username);
	}

	public static Account getAccount(int index) {
		return (Account)accounts.get(index);
	}
	
	public static Account findAccountNoCase(String username) {
		return (Account)mapNoCase.get(username);
	}
	
	public static boolean doesAccountExist(String username) {
		return getAccount(username) != null;
	}

}
