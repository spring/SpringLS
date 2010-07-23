
package com.springrts.chanserv;


import java.io.Serializable;

/**
 *
 * @author hoijui
 */
public class Configuration implements Serializable {

	public String serverAddress = "";
	public int serverPort;
	public String username = "";
	public String password = "";
	public int remoteAccessPort;

	// database related:
	public String DB_URL = "jdbc:mysql://127.0.0.1/ChanServLogs";
	public String DB_username = "";
	public String DB_password = "";
}
