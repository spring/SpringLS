/*
 * Created on 8.3.2006
 */

package com.springrts.chanserv;


/**
 * @author Betalord
 */
public class Client {

	private final String name;
	private int status = 0;

	/**
	 * Number of received CLIENTSTATUS commands since last checkpoint.
	 * Used with anti-spam system.
	 */
	private int statusChanges;
	/**
	 * Time of last "checkpoint" (that is time, when we last reset the counter).
	 * Used with anti-spam system.
	 */
	private long clientStatusChangeCheckpoint;

	public Client(String name) {
		this.name = name;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isModerator() {
		return (status & 0x20) >> 5 == 1;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the number of received CLIENTSTATUS commands
	 * since last checkpoint.
	 * Used with anti-spam system.
	 */
	public int getStatusChanges() {
		return statusChanges;
	}
	public void resetStatusChanges() {
		statusChanges = 0;
	}
	public void addOneStatusChange() {
		statusChanges++;
	}

	/**
	 * Time of last "checkpoint" (that is time, when we last reset the counter).
	 * Used with anti-spam system.
	 * @return the clientStatusChangeCheckpoint
	 */
	public long getClientStatusChangeCheckpoint() {
		return clientStatusChangeCheckpoint;
	}

	/**
	 * Time of last "checkpoint" (that is time, when we last reset the counter).
	 * Used with anti-spam system.
	 * @param clientStatusChangeCheckpoint the clientStatusChangeCheckpoint to set
	 */
	public void setClientStatusChangeCheckpoint(long clientStatusChangeCheckpoint) {
		this.clientStatusChangeCheckpoint = clientStatusChangeCheckpoint;
	}
}
