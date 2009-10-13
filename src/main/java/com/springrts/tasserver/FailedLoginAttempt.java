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
	/**
	 * @see System.currentTimeMillis()
	 */
	public long timeOfLastFailedAttempt;
	/**
	 * Did we log the repeatedly failed login attempt already?
	 */
	public boolean logged;

	public FailedLoginAttempt(String username, int numOfFailedAttempts, long timeOfLastFailedAttempt) {

		this.username = username;
		this.numOfFailedAttempts = numOfFailedAttempts;
		this.timeOfLastFailedAttempt = timeOfLastFailedAttempt;
		this.logged = false;
	}
}
