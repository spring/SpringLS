
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

	private String serverAddress;
	private int serverPort;
	private String username;
	private String password;
	private int remoteAccessPort;
	@XmlElementWrapper()
	@XmlElement(name="channel")
	private final List<Channel> channels;
	@XmlElementWrapper()
	@XmlElement(name="name")
	private final List<String> remoteAccessAccounts;

	Configuration() {

		serverAddress = "";
		serverPort = -1;
		username = "";
		password = "";

		channels = Collections.synchronizedList(new LinkedList<Channel>());
		remoteAccessAccounts = Collections.synchronizedList(new LinkedList<String>());
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getRemoteAccessPort() {
		return remoteAccessPort;
	}

	public void setRemoteAccessPort(int remoteAccessPort) {
		this.remoteAccessPort = remoteAccessPort;
	}

	public List<Channel> getChannels() {
		return channels;
	}

	public List<String> getRemoteAccessAccounts() {
		return remoteAccessAccounts;
	}
}
