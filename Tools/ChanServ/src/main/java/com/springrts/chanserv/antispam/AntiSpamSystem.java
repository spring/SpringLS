
package com.springrts.chanserv.antispam;


import com.springrts.chanserv.Client;

public interface AntiSpamSystem {

	/** Initializes the anti-spam system */
	public void initialize();

	/** Stops the anti-spam system */
	public void uninitialize();

	/**
	 * Call this method when user says something in the channel using SAY
	 * or SAYEX command
	 */
	public void processUserMsg(String chan, String user, String msg);

	public void processClientStatusChange(Client client);

	public void setSpamSettingsForChannel(String chan, SpamSettings settings);
}
