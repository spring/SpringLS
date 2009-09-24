/*
 * Created on 8.3.2006
 */

package com.springrts.chanserv;


/**
 * @author Betalord
 */
public class Client {

	public String name;
	private int status = 0;

	// some fields used with anti-spam system:
	public int numClientStatusChanges; // number of received CLIENTSTATUS commands since last checkpoint
	public long clientStatusChangeCheckpoint; // time of last "checkpoint" (that is time, when we last reset the counter)

	public Client(String name) {
		this.name = name;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isModerator() {
		return (status & 0x20) >> 5 == 1;
	}
}
