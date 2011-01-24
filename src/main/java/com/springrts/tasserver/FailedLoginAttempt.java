/*
 * Created on 2007.11.9
 */

package com.springrts.tasserver;


/**
 * @author Betalord
 */
public class FailedLoginAttempt {

	private String userName;
	private int failedAttempts;
	private long timeOfLastFailedAttempt;
	private boolean logged;

	public FailedLoginAttempt(String username, int numOfFailedAttempts,
			long timeOfLastFailedAttempt)
	{
		this.userName = username;
		this.failedAttempts = numOfFailedAttempts;
		this.timeOfLastFailedAttempt = timeOfLastFailedAttempt;
		this.logged = false;
	}

	public String getUserName() {
		return userName;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public long getTimeOfLastFailedAttempt() {
		return timeOfLastFailedAttempt;
	}

	/**
	 * Did we log the repeatedly failed login attempt already?
	 */
	public boolean isLogged() {
		return logged;
	}

	public void setFailedAttempts(int failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	public void addFailedAttempt() {

		this.timeOfLastFailedAttempt = System.currentTimeMillis();
		this.failedAttempts++;
	}

	public void setLogged(boolean logged) {
		this.logged = logged;
	}
}
