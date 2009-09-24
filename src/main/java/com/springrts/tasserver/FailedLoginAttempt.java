/*
 * Created on 2007.11.9
 */

package com.springrts.tasserver;

/**
 * @author Betalord
 */
public class FailedLoginAttempt {

	public String username;
	public int numOfFailedAttempts;
	public long timeOfLastFailedAttempt; // timestamp, relates to System.currentTimeMillis()
	public boolean logged; // did we log the repeated failed login attempt already?

	public FailedLoginAttempt(String username, int numOfFailedAttempts, long timeOfLastFailedAttempt) {
		this.username = username;
		this.numOfFailedAttempts = numOfFailedAttempts;
		this.timeOfLastFailedAttempt = timeOfLastFailedAttempt;
		logged = false;
	}
}
