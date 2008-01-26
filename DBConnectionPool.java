/*
 * Created on 2008.1.26
 * 
 */

/**
 * @author Betalord
 * 
 * This class has been copied from the following example/tutorial:
 * http://www.javareference.com/jrexamples/viewexample.jsp?id=41
 * Which has been copied from this tutorial:
 * http://www.javareference.com/articles/viewarticle.jsp?id=30
 * 
 * I did minor modifications to it (for example eliminating closed connections).
 * 
 * WARNING: Make sure you return the Connection object back to the pool or else
 * there will be more and more ghosted connections over time. In order to do this,
 * enclose any code that retrieves Connection object in try..finally clause.
 * 
 */

import java.util.*;
import java.sql.*;

public class DBConnectionPool implements Runnable {

	// Number of initial connections to make.
	private int m_InitialConnectionCount = 5;    
	// A list of available connections for use.
	private Vector<Connection> m_AvailableConnections = new Vector<Connection>();
	// A list of connections being used currently.
	private Vector<Connection> m_UsedConnections = new Vector<Connection>();
	// The URL string used to connect to the database
	private String m_URLString = null;
	// The username used to connect to the database
	private String m_UserName = null;    
	// The password used to connect to the database
	private String m_Password = null;    
	// The cleanup thread
	private Thread m_CleanupThread = null;


	//Constructor
	public DBConnectionPool(String urlString, String user, String passwd) throws SQLException {
		// Initialize the required parameters
		m_URLString = urlString;
		m_UserName = user;
		m_Password = passwd;

		for (int cnt = 0; cnt < m_InitialConnectionCount; cnt++)
		{
			// Add a new connection to the available list.
			m_AvailableConnections.addElement(getConnection());
		}

		// Create the cleanup thread
		m_CleanupThread = new Thread(this);
		m_CleanupThread.start();
	}    

	private Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection(m_URLString, m_UserName, m_Password);
	}

	public synchronized Connection checkout() throws SQLException
	{
		Connection newConnxn = null;

		if(m_AvailableConnections.size() == 0)
		{
			// I'm out of connections. Create one more.
			newConnxn = getConnection();
			// Add this connection to the "Used" list.
			m_UsedConnections.addElement(newConnxn);
			// We don't have to do anything else since this is
			// a new connection.
		}
		else
		{
			// Connections exist !
			// Get a connection object
			newConnxn = (Connection)m_AvailableConnections.lastElement();
			// Remove it from the available list.
			m_AvailableConnections.removeElement(newConnxn);
			// Add it to the used list.
			m_UsedConnections.addElement(newConnxn);            
		}        

		// Either way, we should have a connection object now.
		return newConnxn;
	}


	public synchronized void checkin(Connection c)
	{
		checkin(c, true);
	}

	/** Set 'success' to false to tell the pool that the connection object is invalid. Pool will automatically remove such
	 * connection (and create a new one when needed). This is needed because the connection pool can't figure out itself if
	 * the connection is valid (it can check isClosed() method, but that won't tell if some fatal error occurred in the connection
	 * and is now invalid). */
	public synchronized void checkin(Connection c, boolean success) {
		if (c == null) return ; // shouldn't really happen!
		m_UsedConnections.removeElement(c);

		// Remove from used list.
		m_UsedConnections.removeElement(c);
		// Add to the available list
		if (success) {
			m_AvailableConnections.addElement(c);        
		}
	}

	public int availableCount()
	{
		return m_AvailableConnections.size();
	}

	public void run()
	{
		try
		{
			while(true)
			{
				synchronized(this)
				{
					while(m_AvailableConnections.size() > m_InitialConnectionCount)
					{
						// Clean up extra available connections.
						Connection c = (Connection)m_AvailableConnections.lastElement();
						m_AvailableConnections.removeElement(c);

						// Close the connection to the database.
						c.close();
					}

					// Clean up is done
				}

				// Now sleep for 1 minute
				Thread.sleep(60000 * 1);
			}    
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	} 
}
