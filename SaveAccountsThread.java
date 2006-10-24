/*
 * Created on 2006.10.21
 *  
 */

/**
 * @author Betalord
 *
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SaveAccountsThread extends Thread {

	private List dupAccounts; // duplicated accounts. Needed to ensure thread safety as well as accounts state consistency 
	
	public SaveAccountsThread(List dupAccounts) {
    	this.dupAccounts = dupAccounts;
	}
	
	private String exceptionToFullString(Exception e) {
		String res = e.toString();
			             
		StackTraceElement[] trace = e.getStackTrace();
		for (int i = 0; i < trace.length; i++)
			res += "\r\n\tat" + trace[i];
		
		return res;
	}
	
    public void run() {
    	System.out.println("Dumping accounts to disk in a separate thread ...");
    	long time = System.currentTimeMillis();

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(TASServer.ACCOUNTS_INFO_FILEPATH)));			

			for (int i = 0; i < dupAccounts.size(); i++) {
				out.println(dupAccounts.get(i).toString());
			}
			
			out.close();
		} catch (IOException e) {
			System.out.println("IOException error while trying to write accounts info to " + TASServer.ACCOUNTS_INFO_FILEPATH + "!");
			
			// add server notification:
			ServerNotification sn = new ServerNotification("Error saving accounts");
			sn.addLine("Serious error: accounts info could not be saved to disk. Exception trace:" + exceptionToFullString(e));
			ServerNotifications.addNotification(sn);
			
			return;
		}
		
		System.out.println(dupAccounts.size() + " accounts information written to " + TASServer.ACCOUNTS_INFO_FILEPATH + " successfully (" + (System.currentTimeMillis() - time) + " ms).");
    	
        // let garbage collector free the duplicate accounts list:
        dupAccounts = null;
    }

}
