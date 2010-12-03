/*
 * Created on 2007.11.9
 *
 */

package com.springrts.tasserver.transferoldaccounts;


import java.sql.*;

/**
 * @author Betalord
 *
 */
public class DBInterface {
	private Connection conn;
	private Statement stmt;

	public DBInterface() {
		// TODO
	}

	public Connection getConnection() {
		return conn;
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
		System.out.println("SQLException: " + e.getMessage());
		System.out.println("SQLState: " + e.getSQLState());
		System.out.println("VendorError: " + e.getErrorCode());
	}

}
