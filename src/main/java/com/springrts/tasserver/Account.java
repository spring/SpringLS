/*
 * Created on 2005.6.17
 */

package com.springrts.tasserver;


import java.io.Serializable;
import javax.persistence.*;

/**
 * ---- NOTES ----
 * - Each account is uniquely identified by its username (I also used int ID in previous versions,
 *   but since all usernames must be unique, we don't need another unique identifier).
 *
 * @author Betalord
 */
@Entity(name="users")
public class Account implements Serializable {

	/*
	 * accessType bits (31 effective bits, last one is a sign bit and we don't use it):
	 * * bits 0 - 2 (3 bits): accessType level
	 *     0 - none (should not be used for logged-in clients)
	 *     1 - normal (limited)
	 *     2 - privileged
	 *     3 - admin
	 *     values 4 - 7 are reserved for future use
	 * * bits 3 - 22 (20 bits): in-game time (how many minutes did client spend in-game).
	 * * bit 23: agreement bit. It tells us whether name has already
	 *     read the "terms of use" and agreed to it. If not, we should
	 *     first send him the agreement and wait until he confirms it (before
	 *     allowing him to log on the server).
	 * * bit 24 - bot mode (0 - normal name, 1 - automated bot).
	 * * bits 25 - 30 (6 bits): reserved for future use.
	 * * bit 31: unused (integer sign)
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
	 */
	private static int rank1Limit = 60*5;    // in minutes
	private static int rank2Limit = 60*15;   // in minutes
	private static int rank3Limit = 60*30;   // in minutes
	private static int rank4Limit = 60*100;  // in minutes
	private static int rank5Limit = 60*300;  // in minutes
	private static int rank6Limit = 60*1000; // in minutes

	public static int NIL_ACCESS = 0; // for clients that haven't logged in yet
	public static int NORMAL_ACCESS = 1;
	public static int PRIVILEGED_ACCESS = 2;
	public static int ADMIN_ACCESS = 3;

	public static int NO_USER_ID = 0;
	public static int NO_ACCOUNT_ID = 0;
	public static int NEW_ACCOUNT_ID = -1;

	// BEGIN: User speccific data (stored in the DB)

	/**
	 * Unique account identification number.
	 * This is different to the <code>lastUserId</code>, because it
	 * @see lastUserId
	 */
	@Id
	@Column(
		name       = "id",
		unique     = true,
		nullable   = false,
		insertable = true,
		updatable  = false
		)
	private int id;

	/**
	 * Accounts name name.
	 * This is what you see, for example, in the lobby or in-game.
	 */
	@Column(
		name       = "name",
		unique     = true,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 40
		)
	private String name;

	/**
	 * Encrypted form of the password.
	 * TODO: add method of description
	 */
	@Column(
		name       = "password",
		unique     = true,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 32
		)
	private String password;

	/**
	 * Date of when the name registered this account.
	 * In miliseconds, 0 means registration date is unknown.
	 * This applies to users that registered in some early version,
	 * when this field was not yet present. Note that this field was first
	 * introduced with Spring 0.67b3, Dec 18 2005.
	 * @see System.currentTimeMillis()
	 */
	@Column(
		name       = "register_date",
		unique     = true,
		nullable   = true,
		insertable = true,
		updatable  = false
		)
	private long registrationDate;

	/**
	 * Time of the last login.
	 * @see System.currentTimeMillis()
	 */
	@Column(
		name       = "last_login",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true
		)
	private long lastLogin;

	/**
	 * Most recent IP used to log into this account.
	 */
	@Column(
		name       = "last_ip",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 15
		)
	private String lastIP;

	/**
	 * Unique name identification number.
	 * Equals NO_USER_ID (currently 0) if not used/set. By default it is not
	 * set. Multiple accounts may share same name ID. This value actually
	 * indicates last ID as sent with the LOGIN or USERID command by the client.
	 * We use it to detect spawned accounts (accounts registered by the same
	 * name), ban evasion etc.
	 * @see id
	 */
	@Column(
		name       = "last_id",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private int lastUserId;

	/**
	 * Resolved country code for this name's IP when he last logged on.
	 * If country could not be resolved, "XX" is used for country code,
	 * otherwise a 2-char country code is used.
	 */
	@Column(
		name       = "last_country",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 2
		)
	private String lastCountry;

	/**
	 * Access type (eg.: admin, mod, user).
	 * Bit 31 must be 0 (due to int being a signed number, and we don't want to
	 * use any binary complement conversions).
	 */
	private int accessType;

	// END: User speccific data (stored in the DB)


	/**
	 * Used by JPA.
	 */
	public Account() {}
	/**
	 * Used Internally.
	 */
	public Account(String name, String password, int accessType, int lastUserId,
			long lastLogin, String lastIP, long registrationDate,
			String lastCountry, int id) {

		this.name             = name;
		this.password         = password;
		this.accessType       = accessType;
		this.lastUserId       = lastUserId;
		this.lastLogin        = lastLogin;
		this.lastIP           = lastIP;
		this.registrationDate = registrationDate;
		this.lastCountry      = lastCountry;
		this.id               = id;
	}

	public Account(Account acc) {

		this.name             = new String(acc.getName());
		this.password         = new String(acc.getPassword());
		this.accessType       = acc.accessType;
		this.lastUserId       = NO_USER_ID;
		this.lastLogin        = acc.lastLogin;
		this.lastIP           = new String(acc.getLastIP());
		this.registrationDate = acc.registrationDate;
		this.lastCountry      = new String(acc.getLastCountry());
		// TODO: possible bug: is the next correct/needed?
		//this.id               = acc.id;
	}

	@Override
	public String toString() {
		return new StringBuilder(getName()).append(" ")
				.append(getPassword()).append(" ")
				.append(Integer.toString(getAccessType(), 2)).append(" ")
				.append(getLastUserId()).append(" ")
				.append(getLastLogin()).append(" ")
				.append(getLastIP()).append(" ")
				.append(getRegistrationDate()).append(" ")
				.append(getLastCountry()).append(" ")
				.append(getId()).toString();
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.getName() != null ? this.getName().hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Account other = (Account) obj;
		if ((this.getName() == null) ? (other.getName() != null) : !this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public int accessLevel() {
		return getAccessType() & 0x7;
	}

	public boolean getBotMode() {
		return ((getAccessType() & 0x1000000) >> 24) == 1;
	}

	public void setBotMode(boolean bot) {
		int b = bot ? 1 : 0;
		setAccessType((getAccessType() & 0xFEFFFFFF) | (b << 24));
	}

	/**
	 * Returns the client's in-game time in minutes.
	 * @return the client's in-game time in minutes
	 */
	public int getInGameTime() {
		return (getAccessType() & 0x7FFFF8) >> 3;
	}

	public void setInGameTime(int mins) {
		setAccessType((getAccessType() & 0xFF800007) | (mins << 3));
	}

	public boolean getAgreement() {
		return ((getAccessType() & 0x800000) >> 23) == 1;
	}

	public void setAgreement(boolean agreed) {

		// must be 1 (true) or 0 (false)
		int agr = agreed ? 1 : 0;
		setAccessType((getAccessType() & 0xFF7FFFFF) | (agr << 23));
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

	/**
	 * Adds <code>mins</code> minutes to the client's in-game time.
	 * This time is used to calculate the player's rank.
	 * @param mins number of minutes to add to the client's in-game time
	 * @return true if player's rank was changed, false otherwise.
	 */
	public boolean addMinsToInGameTime(int mins) {
		int tmp = getRank();
		setInGameTime(getInGameTime() + mins);
		return tmp != getRank();
	}

	/**
	 * Unique account identification number.
	 * This is different to the <code>lastUserId</code>, because it
	 * @see lastUserId
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Unique account identification number.
	 * This is different to the <code>lastUserId</code>, because it
	 * @see lastUserId
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Accounts name name.
	 * This is what you see, for example, in the lobby or in-game.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Accounts name name.
	 * This is what you see, for example, in the lobby or in-game.
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Encrypted form of the password.
	 * TODO: add method of description
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Encrypted form of the password.
	 * TODO: add method of description
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Access type.
	 * Bit 31 must be 0 (due to int being a signed number, and we don't want to
	 * use any binary complement conversions).
	 * @return the accessType
	 */
	public int getAccessType() {
		return accessType;
	}

	/**
	 * Access type.
	 * Bit 31 must be 0 (due to int being a signed number, and we don't want to
	 * use any binary complement conversions).
	 * @param accessType the accessType to set
	 */
	public void setAccessType(int accessType) {
		this.accessType = accessType;
	}

	/**
	 * Unique name identification number.
	 * Equals NO_USER_ID (currently 0) if not used/set. By default it is not
	 * set. Multiple accounts may share same name ID. This value actually
	 * indicates last ID as sent with the LOGIN or USERID command by the client.
	 * We use it to detect spawned accounts (accounts registered by the same
	 * name), ban evasion etc.
	 * @see accountID
	 * @return the lastUserId
	 */
	public int getLastUserId() {
		return lastUserId;
	}

	/**
	 * Unique name identification number.
	 * Equals NO_USER_ID (currently 0) if not used/set. By default it is not
	 * set. Multiple accounts may share same name ID. This value actually
	 * indicates last ID as sent with the LOGIN or USERID command by the client.
	 * We use it to detect spawned accounts (accounts registered by the same
	 * name), ban evasion etc.
	 * @see accountID
	 * @param lastUserId the lastUserId to set
	 */
	public void setLastUserId(int lastUserId) {
		this.lastUserId = lastUserId;
	}

	/**
	 * Time (System.currentTimeMillis()) of the last login.
	 * @return the lastLogin
	 */
	public long getLastLogin() {
		return lastLogin;
	}

	/**
	 * Time (System.currentTimeMillis()) of the last login.
	 * @param lastLogin the lastLogin to set
	 */
	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	/**
	 * Most recent IP used to log into this account.
	 * @return the lastIP
	 */
	public String getLastIP() {
		return lastIP;
	}

	/**
	 * Most recent IP used to log into this account.
	 * @param lastIP the lastIP to set
	 */
	public void setLastIP(String lastIP) {
		this.lastIP = lastIP;
	}

	/**
	 * Date of when the name registered this account.
	 * In miliseconds (refers to System.currentTimeMillis()). 0 means
	 * registration date is unknown. This applies to users that registered in
	 * some early version, when this field was not yet present. Note that this
	 * field was first introduced with Spring 0.67b3, Dec 18 2005.
	 * @return the registrationDate
	 */
	public long getRegistrationDate() {
		return registrationDate;
	}

	/**
	 * Date of when the name registered this account.
	 * In miliseconds (refers to System.currentTimeMillis()). 0 means
	 * registration date is unknown. This applies to users that registered in
	 * some early version, when this field was not yet present. Note that this
	 * field was first introduced with Spring 0.67b3, Dec 18 2005.
	 * @param registrationDate the registrationDate to set
	 */
	public void setRegistrationDate(long registrationDate) {
		this.registrationDate = registrationDate;
	}

	/**
	 * Resolved country code for this name's IP when he last logged on.
	 * If country could not be resolved, "XX" is used for country code,
	 * otherwise a 2-char country code is used.
	 * @return the lastCountry
	 */
	public String getLastCountry() {
		return lastCountry;
	}

	/**
	 * Resolved country code for this name's IP when he last logged on.
	 * If country could not be resolved, "XX" is used for country code,
	 * otherwise a 2-char country code is used.
	 * @param lastCountry the lastCountry to set
	 */
	public void setLastCountry(String lastCountry) {
		this.lastCountry = lastCountry;
	}
}
