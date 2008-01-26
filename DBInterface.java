/*
 * Created on 2007.11.9
 *
 */

/**
 * @author Betalord
 *
 */

import java.sql.*;

public class DBInterface {
	private DBConnectionPool connPool;

	// database information:
	private String url;
	private String username;
	private String password;
	
	public DBInterface() {
		// TODO
	}
	
	public boolean initialize(String url, String username, String password) {
		// load JDBC driver:
		try {
			// The newInstance() call is a work around for some 
			// broken Java implementations
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			System.out.println("JDBC driver loaded.");
		} catch (Exception e) {
			System.out.println("JDBC driver not found!");
			return false;
		}

		// create connection(s) to database, etc.:
		this.url = url;
		this.username = username;
		this.password = password;
		
		System.out.println("Trying to connect to database ...");
		try {
			connPool = new DBConnectionPool(url, username, password); 
		} catch (SQLException e) {
			printSQLException(e);
			System.out.println("Unable to connect to database!");
			return false;
		}
		
		System.out.println("Connection to database established.");
		return true;
	}
	
	
	/** returns null if error occurred */
	public ResultSet execQuery(String query) {
		ResultSet result = execQueryInternal(query);
		// if it failed, lets retry it one more time (and then give up):
		if (result == null) {
			result = execQueryInternal(query);
		}
		
		return result;
	}
	
	/** returns null if error occurred */
	private ResultSet execQueryInternal(String query) {
		Connection conn = null;
		boolean success = true;
		try {
			conn = connPool.checkout();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			printSQLException(e);
			System.out.println("SQLException getMessage() string: " + e.getMessage());
			success = false;
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
			
			success = false;
			
			return null;
		} finally {
			// always return connection back to pool, no matter what!
			connPool.checkin(conn, success);
		}		
	}
	
	/** use only for those SQL statements that do not return any result.
	 * Returns false in case of any error. */
	public boolean execUpdate(String query) {
		Connection conn = null;
		boolean success = true;
		try {
			conn = connPool.checkout();
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(query);
			return true;
		} catch (SQLException e) {
			printSQLException(e);
			success = false;
			return false;
		} finally {
			// always return connection back to pool, no matter what!
			connPool.checkin(conn, success);
		}
	}
	
	public void printSQLException(SQLException e) {
		System.out.println("SQLException: " + e.getMessage());
		System.out.println("SQLState: " + e.getSQLState());
		System.out.println("VendorError: " + e.getErrorCode());
	}

}
