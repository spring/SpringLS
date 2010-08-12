
package com.springrts.chanserv;


import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Everything in here, and only this, is being persisted.
 * For example to a config file.
 * @author hoijui
 */
@XmlRootElement
public class Configuration implements Serializable {

	public String serverAddress = "";
	public int serverPort;
	public String username = "";
	public String password = "";
	public int remoteAccessPort;
	@XmlElementWrapper()
	@XmlElement(name="channel")
	public final List<Channel> channels;
	@XmlElementWrapper()
	@XmlElement(name="name")
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
