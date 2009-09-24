/*
 * Created on 2005.6.17
 */

package com.springrts.tasserver;


/**
 * ---- NOTES ----
 * - Each account is uniquely identified by its username (I also used int ID in previous versions,
 *   but since all usernames must be unique, we don't need another unique identifier).
 *
 * @author Betalord
 */
public class Account {

	/*
	 * access bits (31 effective bits, last one is a sign bit and we don't use it):
	 * * bits 0 - 2 (3 bits): access level
	 *     0 - none (should not be used for logged-in clients)
	 *     1 - normal (limited)
	 *     2 - privileged
	 *     3 - admin
	 *     values 4 - 7 are reserved for future use
	 * * bits 3 - 22 (20 bits): in-game time (how many minutes did client spent in-game).
	 * * bit 23: agreement bit. It tells us whether user has already
	 *     read the "terms of use" and agreed to it. If not, we should
	 *     first send him the agreement and wait until he confirms it (before
	 *     allowing him to log on the server).
	 * * bit 24 - bot mode (0 - normal user, 1 - automated bot).
	 * * bits 25 - 30 (6 bits): reserved for future use.
	 * * bit 31: unused (integer sign)
	 *
	 */


	/*
	 * current rank categories:
	 * < 5h = newbie
	 * 5h - 15h = beginner
	 * 15h - 30h = avarage
	 * 30h - 100h = above avarage
	 * 100h - 300h = experienced player
	 * 300h - 1000h = highly experienced player
	 * > 1000h = veteran
	 *
	 * */
	private static int rank1Limit = 60*5; // in minutes
	private static int rank2Limit = 60*15; // in minutes
	private static int rank3Limit = 60*30; // in minutes
	private static int rank4Limit = 60*100; // in minutes
	private static int rank5Limit = 60*300; // in minutes
	private static int rank6Limit = 60*1000; // in minutes

	public static int NIL_ACCESS = 0; // for clients that haven't logged in yet
	public static int NORMAL_ACCESS = 1;
	public static int PRIVILEGED_ACCESS = 2;
	public static int ADMIN_ACCESS = 3;

	public static int NO_USER_ID = 0;
	public static int NO_ACCOUNT_ID = 0;
	public static int NEW_ACCOUNT_ID = -1;

	public String user;
	public String pass;
	public int access; // access type. Bit 31 must be 0 (due to int being signed number - we don't want to use any binary complement conversions).
	public int lastUserID; // unique user identification number. Equals NO_USER_ID (currently 0) if not used/set. By default it is not set. Multiple accounts may share same user ID. This value actually indicates last ID as sent with the LOGIN or USERID command by the client. We use it to detect spawned accounts (accounts registered by the same user), ban evasion etc.
	public long lastLogin; // time (System.currentTimeMillis()) of the last login
	public String lastIP; // the most recent IP used to log into this account
	public long registrationDate; // date when user registered this account. In miliseconds (refers to System.currentTimeMillis()). 0 means registration date is unknown (clients who registered in some early version when this field was not yet implemented. Note that this field was first introduced with Spring 0.67b3, Dec 18 2005).
	public String lastCountry; // resolved country code for this user's IP when he last logged on. If country could not be resolved, "XX" is used for country code, otherwise a 2-char country code is used
	public int accountID; // unique account identifier number

	public Account(String user, String pass, int access, int lastUserID, long lastLogin, String lastIP, long registrationDate, String lastCountry, int accountID)
	{
		this.user = user;
		this.pass = pass;
		this.access = access;
		this.lastUserID = lastUserID;
		this.lastLogin = lastLogin;
		this.lastIP = lastIP;
		this.registrationDate = registrationDate;
		this.lastCountry = lastCountry;
		this.accountID = accountID;
	}

	public Account(Account acc) {
		this.user = new String(acc.user);
		this.pass = new String(acc.pass);
		this.access = acc.access;
		this.lastUserID = NO_USER_ID;
		this.lastLogin = acc.lastLogin;
		this.lastIP = acc.lastIP;
		this.registrationDate = acc.registrationDate;
		this.lastCountry = new String(acc.lastCountry);
	}

	@Override
	public String toString() {
		return user + " " + pass + " " + Integer.toString(access, 2) + " " + lastUserID + " " + lastLogin + " " + lastIP + " " + registrationDate + " " + lastCountry + " " + accountID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Account)) return false;
		return this.user.equals(((Account)o).user);
	}

	public int accessLevel() {
		return access & 0x7;
	}

	public boolean getBotMode() {
		return ((access & 0x1000000) >> 24) == 1;
	}

	public void setBotMode(boolean bot) {
		int b = bot ? 1 : 0;
		access = (access & 0xFEFFFFFF) | (b << 24);
	}

	/* returns in-game time in minutes */
	public int getInGameTime() {
		return (access & 0x7FFFF8) >> 3;
	}

	public void setInGameTime(int mins) {
		access = (access & 0xFF800007) | (mins << 3);
	}

	public boolean getAgreement() {
		return ((access & 0x800000) >> 23) == 1;
	}

	/* must be 1 (true) or 0 (false) */
	public void setAgreement(boolean agreed) {
		int agr = agreed ? 1 : 0;
		access = (access & 0xFF7FFFFF) | (agr << 23);
	}

	public int getRank() {
		if (getInGameTime() >= rank6Limit) return 6;
		else if (getInGameTime() > rank5Limit) return 5;
		else if (getInGameTime() > rank4Limit) return 4;
		else if (getInGameTime() > rank3Limit) return 3;
		else if (getInGameTime() > rank2Limit) return 2;
		else if (getInGameTime() > rank1Limit) return 1;
		else return 0;
	}

	/* adds mins minutes to client's in-game time (this time is used to calculate
	 * player's rank). Returns true if player's rank was changed, false otherwise. */
	public boolean addMinsToInGameTime(int mins) {
		int tmp = getRank();
		setInGameTime(getInGameTime() + mins);
		return tmp != getRank();
	}
}
