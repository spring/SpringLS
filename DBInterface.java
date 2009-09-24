/*
 * Created on 2007.11.9
 *
 */

/**
 * @author Betalord
 *
 * See this example:
 * http://svn.apache.org/viewvc/commons/proper/dbcp/trunk/doc/ManualPoolingDriverExample.java?view=markup
 *
 * Useful links:
 * http://wiki.apache.org/jakarta-commons/DBCP
 * http://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.html
 * http://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.Config.html
 *
 */

import java.sql.*;

import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.*;

public class DBInterface {

	// database information:
	private String url;
	private String username;
	private String password;
	private String validationQuery = "show variables like 'version'"; // see http://bugs.sakaiproject.org/jira/browse/SAK-9302;jsessionid=3DFD583309611E0D2F76C1E9BC3FDCB0?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
	private String connectionPoolName = "TAS_DB_Pool"; // not much important (would only be important if we had more pools)

	public DBInterface() {
		// TODO
	}

	public boolean initialize(String url, String username, String password) {

		this.url = url;
		this.username = username;
		this.password = password;

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

		// now comes the setting up of the driver etc.:

		// Lets prepare config for our pool ...
		// Useful links:
		// http://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.html
		// http://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.Config.html
		GenericObjectPool.Config config = new GenericObjectPool.Config();
		config.maxWait = 3000; // never wait more than 3 seconds. After that time, return NoSuchElementException
		config.timeBetweenEvictionRunsMillis = 30 * 1000; // 30 seconds
		config.minEvictableIdleTimeMillis = 30 * 60 * 1000; // 30 minutes
		config.testOnBorrow = true; // check that connection is valid before taking it out from the pool
		config.testWhileIdle = true; // test connections periodically to ensure they are valid (or else throw them out of the pool)

		//
		// First, we'll need a ObjectPool that serves as the
		// actual pool of connections.
		//
		// We'll use a GenericObjectPool instance, although
		// any ObjectPool implementation will suffice.
		//
		ObjectPool connectionPool = new GenericObjectPool(null, config);

		//
		// Next, we'll create a ConnectionFactory that the
		// pool will use to create Connections.
		// We'll use the DriverManagerConnectionFactory.
		//
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, username, password);

		//
		// Now we'll create the PoolableConnectionFactory, which wraps
		// the "real" Connections created by the ConnectionFactory with
		// the classes that implement the pooling functionality.
		//
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,validationQuery,false,true);

		//
		// Finally, we create the PoolingDriver itself...
		//
		PoolingDriver driver;
		try {
			Class.forName("org.apache.commons.dbcp.PoolingDriver");
			driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
		} catch (Exception e) {
			System.out.println("Unable to connect to database!");
			e.printStackTrace();
			return false;
		}

		//
		// ...and register our pool with it.
		//
		driver.registerPool(connectionPoolName,connectionPool);

		//
		// Now we can just use the connect string "jdbc:apache:commons:dbcp:TAS_DB_Pool"
		// to access our pool of Connections.
		//

		System.out.println("Database interface has been initialized successfully.");
		return true;
	}

	/* returns true if it can connect to the database and issue a dummy query, or false otherwise */
	public boolean testConnection() {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;

		try {
			conn = DriverManager.getConnection(TASServer.database.getConnectionURL());
			stmt = conn.createStatement();
			rset = stmt.executeQuery(validationQuery);
			if (rset == null)
				return false;
		} catch (Exception e) {
			Throwable t = e;
			while (t != null) {
				System.out.println(t.getMessage());
				t = t.getCause();
			}
			return false;
		} finally {
			// always return connection back to pool, no matter what!
			try { rset.close(); } catch(Exception e) { }
			try { stmt.close(); } catch(Exception e) { }
			try { conn.close(); } catch(Exception e) { }
		}

		return true;
	}

	public void printSQLException(SQLException e) {
		System.out.println("SQLException: " + e.getMessage());
		System.out.println("SQLState: " + e.getSQLState());
		System.out.println("VendorError: " + e.getErrorCode());
	}

	public void printDriverStats() {
		PoolingDriver driver = null;
		ObjectPool connectionPool = null;
		System.out.println("--------- DB driver status ---------");
		try {
			driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
			connectionPool = driver.getConnectionPool(connectionPoolName);
		} catch (Exception e) {
			System.out.println("Error while trying to get driver status. Error message: " + e.getMessage());
		}

		System.out.println("NumActive: " + connectionPool.getNumActive());
		System.out.println("NumIdle: " + connectionPool.getNumIdle());
		System.out.println("------------------------------------");
	}

	public void shutdownDriver() throws Exception {
		PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
		driver.closePool(connectionPoolName);
	}

	public String getConnectionURL() {
		return "jdbc:apache:commons:dbcp:" + connectionPoolName;
	}
}
