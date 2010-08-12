/*
 * Created on 2007.11.9
 */

package com.springrts.chanserv;


import java.sql.*;

/**
 * @author Betalord
 */
public class DBInterface {

	private Connection conn;
	private Statement stmt;

	public DBInterface() {

		conn = null;
		stmt = null;
	}

	public boolean isConnected() {
		return (conn != null);
	}

	public Connection getConnection() {
		return conn;
	}

	public boolean init(String url, String username, String password) {

		if (!loadJDBCDriver()) {
			return false;
		}
		if (!connectToDatabase(url, username, password)) {
			return false;
		}

		return true;
	}

	private boolean loadJDBCDriver() {

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

	private boolean connectToDatabase(String url, String username, String password) {

		Log.log("Trying to connect to database ...");
		try {
			conn = DriverManager.getConnection(url, username, password);
			stmt = conn.createStatement();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ex) {
					Log.error("Failed to disconnect from DB.");
				}
			}
			conn = null;
			stmt = null;
			printSQLException(e);
			Log.error("Unable to connect to database!");
			return false;
		}

		Log.log("Connection to database established.");
		return true;
	}

	/**
	 * Use for only those SQL statements that do not return any result.
	 * Returns false in case of any error.
	 */
	public boolean execUpdate(String query) {

		try {
			stmt.executeUpdate(query);
			return true;
		} catch (SQLException e) {
			printSQLException(e);
			return false;
		}
	}

	private void printSQLException(SQLException e) {

		Log.log("SQLException: " + e.getMessage());
		Log.log("SQLState: " + e.getSQLState());
		Log.log("VendorError: " + e.getErrorCode());
	}

	/** Returns true if table with specified name exists in the database */
	public boolean doesTableExist(String table) throws SQLException {

		stmt.execute("SHOW TABLES LIKE '" + table + "'");
		ResultSet rs = stmt.getResultSet();
		return rs.next();
	}
}
