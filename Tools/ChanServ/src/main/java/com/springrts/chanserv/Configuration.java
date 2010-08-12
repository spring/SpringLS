
package com.springrts.chanserv;


import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
	public final List<Channel> channels;
	public final List<String> remoteAccessAccounts;

	// database related:
	public String DB_URL = "jdbc:mysql://127.0.0.1/ChanServLogs";
	public String DB_username = "";
	public String DB_password = "";

	Configuration() {

		channels = Collections.synchronizedList(new LinkedList<Channel>());
		remoteAccessAccounts = Collections.synchronizedList(new LinkedList<String>());
	}
}
