/**
 * Class that transfers old accounts (that were stored in
 * accounts.txt on the hard disk) to a database.
 *
 * Created on 2007/12/28
 *
 */

/**
 * @author Betalord
 *
 */

import java.util.*;
import java.sql.*;
import java.text.*;

public class TransferOldAccounts {

	// database related:
	public static DBInterface database;
	private static String DB_URL = "jdbc:mysql://127.0.0.1/spring";
	private static String DB_username = "";
	private static String DB_password = "";

	public static void closeAndExit() {
		closeAndExit(null);
	}

	public static void closeAndExit(String reason) {
		if (reason != null) {
			System.out.println("Shuting down. Reason: " + reason);
		}
		System.exit(0);
	}

	public static String easyDateFormat(long date, String format) {
		java.util.Date d = new java.util.Date(date);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(d);
	}

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Error - none or too many arguments.");
		    return ;
		}

		DB_username = args[0];
		DB_password = args[1];

		// establish connection with database:
		System.out.println("Connecting to database ...");
		database = new DBInterface();
		if (!database.loadJDBCDriver()) {
			closeAndExit();
		}
		if (!database.connectToDatabase(DB_URL, DB_username, DB_password)) {
			closeAndExit();
		}

		System.out.println("Loading accounts from disk ...");
		System.out.flush();
		Accounts.loadAccounts();

		// first, filter out any invalid accounts:
		for (int i = 0; i < Accounts.getAccountsSize(); i++) {
			Account acc = Accounts.getAccount(i);
			if (Accounts.isOldUsernameValid(acc.user) != null) {
				System.out.println("Invalid username: " + acc.toString());
				System.out.println("This user (" + acc.user + ") last logged in on " + easyDateFormat(acc.lastLogin, "d MMM yyyy HH:mm:ss z"));
				Accounts.removeAccount(i);
				i--;
				continue;
			}
		}

		System.out.println("After cleaning accounts, " + Accounts.getAccountsSize() + " valid accounts remain.");
		System.out.flush();

		// we will do INSERT in chunks, each of max size of 1000 entries
		for (int k = 0; k < Accounts.getAccountsSize() / 1000 + 1; k++) {
			StringBuffer insert = new StringBuffer("INSERT INTO OldAccounts (Username, Password, AccessBits, RegistrationDate, LastCountry) values ");

			int count = 0;
			for (int i = k*1000; i < Math.min(Accounts.getAccountsSize(), (k+1)*1000); i++) {
				Account acc = Accounts.getAccount(i);
				if (i == k*1000)
					insert.append("(?, ?, ?, ?, ?)");
				else
					insert.append(", (?, ?, ?, ?, ?)");
				count++;
			}

			insert.append(";");

			// set up the INSERT command:
			PreparedStatement pstmt = null;
			try {
	            pstmt = database.getConnection().prepareStatement(insert.toString());
	            for (int i = 0; i < count; i++) {
	            	Account acc = Accounts.getAccount(k*1000+i);
	                pstmt.setString(i*5+1, acc.user);
	                pstmt.setString(i*5+2, acc.pass);
	                pstmt.setInt(i*5+3, acc.access);
	                pstmt.setLong(i*5+4, acc.registrationDate);
	                pstmt.setString(i*5+5, acc.lastCountry);
	            }
	            // insert into database:
	    		System.out.println("Inserting chunk #" + k + "(" + count + " rows) into database ...");
	    		System.out.flush();
	            pstmt.executeUpdate();
	            pstmt.close();
			} catch (SQLException e) {
				System.out.println("Inserting into database failed.");
				e.printStackTrace();
				closeAndExit("Error while accessing database.");
			}
		}

		System.out.println("Done.");
	}

}
