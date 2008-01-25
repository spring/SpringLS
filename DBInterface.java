/*
 * Created on 2007.11.9
 *
 */

/**
 * @author Betalord
 *
 */

import java.sql.*;
import java.io.*;

public class DBInterface {
	private Connection conn;
	private Statement stmt;

	// database information:
	private String url;
	private String username;
	private String password;
	
	public DBInterface() {
		// TODO
	}
	
	public boolean loadJDBCDriver() {
		try {
			// The newInstance() call is a work around for some 
			// broken Java implementations
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			System.out.println("JDBC driver loaded.");
		} catch (Exception e) {
			System.out.println("JDBC driver not found!");
			return false;
		}
		return true;
	}

	public boolean connectToDatabase(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
		
		System.out.println("Trying to connect to database ...");
		try {
			conn = DriverManager.getConnection(url, username, password);
			stmt = conn.createStatement();
		} catch (SQLException e) {
			printSQLException(e);
			System.out.println("Unable to connect to database!");
			return false;
		}
		
		System.out.println("Connection to database established.");
		return true;
	}
	
	/** returns null if error occurs */
	public ResultSet execQuery(String query) {
		try {
			ResultSet rs = stmt.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			if (e.getMessage().equals("No operations allowed after statement closed."))
				try {
					stmt = conn.createStatement(); 
				} catch (SQLException e2) {
					System.out.println("Unable to recreate stmt object: " + e2.getMessage());
				}
			printSQLException(e);
			System.out.println("SQLException getMessage() string: " + e.getMessage());
			
			return null;
		} catch (Exception e) {
			Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem detected: connection to mysql has been broken. Will now attempt to reconnect ...");
			
			// add server notification:
			ServerNotification sn = new ServerNotification("Connection to mysql broken");
			sn.addLine("Serious problem detected: connection to mysql has been broken.");
			sn.addLine("Exception description:");
			sn.addLine(e.toString());
			ServerNotifications.addNotification(sn);

			e.printStackTrace(); //*** DEBUG
			
			try {
				long time = System.currentTimeMillis();
				conn = DriverManager.getConnection(url, username, password);
				stmt = conn.createStatement();
				time = System.currentTimeMillis() - time;
				
				// add server notification:
				ServerNotification sn2 = new ServerNotification("Connection to mysql reestablished");
				sn2.addLine("Connection to mysql database has been successfully reestablished.");
				sn2.addLine("Time taken: " + time + " milliseconds.");
				ServerNotifications.addNotification(sn2);

				// now try to execute given query again:
				try {
					ResultSet rs = stmt.executeQuery(query);
					return rs;
				} catch (Exception ex) {
					ex.printStackTrace();
					Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem: unable to execute query (1st retry failed). Query is: '" + query + "'. Exception: " + ex.toString());
				}
				
			} catch (Exception ex) {
				Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem detected: connection to mysql could not be reestablished. Check the notification log for details.");				

				// add server notification:
				ServerNotification sn2 = new ServerNotification("Connection to mysql cannot be reestablished");
				sn2.addLine("Serious problem detected: connection to mysql cannot be reestablished.");
				sn2.addLine("Exception description:");
				sn2.addLine(ex.toString());
				ServerNotifications.addNotification(sn2);

				ex.printStackTrace(); //*** DEBUG
			}
			
			return null;
		}
	}
	
	/** use for only those SQL statements that do not return any result.
	 * Returns false in case of any error. */
	public boolean execUpdate(String query) {
		try {
			stmt.executeUpdate(query);
			return true;
		} catch (SQLException e) {
			printSQLException(e);
			return false;
		}
	}
	
	public void printSQLException(SQLException e) {
		System.out.println("SQLException: " + e.getMessage());
		System.out.println("SQLState: " + e.getSQLState());
		System.out.println("VendorError: " + e.getErrorCode());
	}

}
