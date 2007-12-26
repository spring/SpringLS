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
	private Connection conn;
	private Statement stmt;
	
	public DBInterface() {
		// TODO
	}
	
	public boolean loadJDBCDriver() {
		try {
			// The newInstance() call is a work around for some 
			// broken Java implementations
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Log.log("JDBC driver loaded.");
		} catch (Exception e) {
			Log.error("JDBC driver not found!");
			return false;
		}
		return true;
	}

	public boolean connectToDatabase(String url, String username, String password) {
		Log.log("Trying to connect to database ...");
		try {
			conn = DriverManager.getConnection(url, username, password);
			stmt = conn.createStatement();
		} catch (SQLException e) {
			printSQLException(e);
			Log.error("Unable to connect to database!");
			return false;
		}
		
		Log.log("Connection to database established.");
		return true;
	}
	
	/** returns null if error occurs */
	public ResultSet execQuery(String query) {
		try {
			ResultSet rs = stmt.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			printSQLException(e);
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
		Log.log("SQLException: " + e.getMessage());
		Log.log("SQLState: " + e.getSQLState());
		Log.log("VendorError: " + e.getErrorCode());
	}
	
	/** returns true if table with specified name exists in the database */
	public boolean doesTableExist(String table) throws SQLException {
		stmt.execute("SHOW TABLES LIKE '" + table + "'");
		ResultSet rs = stmt.getResultSet();
		return rs.next();
	}

}
