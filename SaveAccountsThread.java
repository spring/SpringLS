/*
 * Created on 2006.10.21
 *
 * Note that it is vital that everything here is synchronized with main server thread.
 * Currently dupAccounts list is cloned from original accounts list so we don't have
 * to worry about thread-safety here (this is the easiest way since otherwise we would
 * have to make sure no account is added or removed while we are saving accounts to disk).
 * The second thing for which we must ensure thread-safety is Account.toString() method.
 * The only problem with Account.toString() method is calling MapGradeList.toString()
 * method, which could potentially cause problems (other fields used in Account.toString()
 * are mostly atomic or if they are not it doesn't hurt us really - example is 'long' type,
 * which consists of two 32 bit ints and is thus not atomic, but it won't cause corruption
 * in accounts file as it is used right now). So it is essential to ensure that MapGrading
 * class is thread-safe (or at least its toString() method is).
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
			sn.addLine("Serious error: accounts info could not be saved to disk. Exception trace:" + Misc.exceptionToFullString(e));
			ServerNotifications.addNotification(sn);

			return;
		}

		System.out.println(dupAccounts.size() + " accounts information written to " + TASServer.ACCOUNTS_INFO_FILEPATH + " successfully (" + (System.currentTimeMillis() - time) + " ms).");

        // let garbage collector free the duplicate accounts list:
        dupAccounts = null;
    }

}
