/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls;


import com.springrts.springls.util.Misc;
import com.springrts.springls.util.ProtocolUtil;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An account is the server-side representation of a human that may login.
 *
 * Each account is uniquely identified by its username.
 * Previously, the ID was used for that purpose,
 * but since all usernames must be unique,
 * we do not need another unique identifier anymore.
 *
 * @author Betalord
 */
@Entity
@Table(name = "users")
@NamedQueries({
	@NamedQuery(name = "acc_size",             query = "SELECT count(a.id) FROM Account a"),
//	@NamedQuery(name = "size_active",          query = "SELECT count(a.id) FROM Account a WHERE ((a.inGameTime >= :minInGameTime) AND (a.lastLogin > :oneWeekAgo))"),
//	q_size_active.setParameter("minInGameTime", Account.Rank.Beginner.getRequiredTime());
	@NamedQuery(name = "acc_size_active",      query = "SELECT count(a.id) FROM Account a WHERE ((a.inGameTime >= " + /*Account.Rank.Beginner.getRequiredTime()*/(5 * 60 * 60) + ") AND (a.lastLogin > :oneWeekAgo))"),
	@NamedQuery(name = "acc_list",             query = "SELECT a FROM Account a"),
	@NamedQuery(name = "acc_fetchByName",      query = "SELECT a FROM Account a WHERE a.name = :name"),
	@NamedQuery(name = "acc_fetchByLowerName", query = "SELECT a FROM Account a WHERE (LOWER(a.name) = :lowerName)"),
	@NamedQuery(name = "acc_fetchByLastIP",    query = "SELECT a FROM Account a WHERE a.lastIp = :ip")
})
public class Account implements Serializable, Cloneable {

	private static final Logger LOG = LoggerFactory.getLogger(Account.class);

	/**
	 * Accounts with these names can not be registered,
	 * as they may be used internally by the server.
	 */
	public static final Collection<String> RESERVED_NAMES
			= Arrays.asList(new String[] {
				"TASServer",
				"Server",
				"server",
				"SpringLS",
				"springls",
				"Spring",
				"spring",
			});

	/**
	 * Player ranks, based on in-game time.
	 * Current rank categories:
	 * < 5h = newbie
	 * 5h - 15h = beginner
	 * 15h - 30h = avarage
	 * 30h - 100h = above avarage
	 * 100h - 300h = experienced player
	 * 300h - 1000h = highly experienced player
	 * > 1000h = veteran
	 */
	public static enum Rank {
		Newbie            (0),
		Beginner          (5),
		Average           (15),
		AboveAverage      (30),
		Experienced       (100),
		HighlyExperienced (300),
		Veteran           (1000);

		private final long seconds;

		private Rank(long hours) {
			seconds = hours * 60 * 60;
		}

		/**
		 * Returns the minimum required amount of in-game time to obtain
		 * this rank, in seconds.
		 * @return min. in-game time in seconds
		 */
		public long getRequiredTime() {
			return seconds;
		}
	}

	/**
	 * The ordinal of the enum members have to match the values in the
	 * access bit field used in the server protocol.
	 */
	public static enum Access {
		/** not yet logged in */
		NONE,
		/** normal user access level */
		NORMAL,
		/** can kick and ban users + more */
		PRIVILEGED,
		/** can change users access level */
		ADMIN;

		public boolean isAtLeast(Access otherAccess) {
			return this.compareTo(otherAccess) >= 0;
		}

		public boolean isLessThen(Access otherAccess) {
			return this.compareTo(otherAccess) < 0;
		}
	}

	public static final int NO_USER_ID = 0;
	public static final int NO_ACCOUNT_ID = 0;
	public static final int NEW_ACCOUNT_ID = -1;
	public static final String NO_ACCOUNT_LAST_IP_STR = "?";


	// BEGIN: User specific data (stored in the DB)

	/**
	 * Unique account identification number.
	 * This is different to the <code>lastUserId</code>, because it
	 * @see #lastUserId
	 */
	private int id;

	/**
	 * Accounts login name.
	 * This is what you see, for example, in the lobby or in-game.
	 */
	private String name;

	/**
	 * Encrypted form of the password.
	 * TODO: add description of method used to encrypt the password; lobby side
	 */
	private String password;

	/**
	 * Date of when the name registered this account.
	 * In milliseconds, 0 means registration date is unknown.
	 * This applies to users that registered in some early version,
	 * when this field was not yet present. Note that this field was first
	 * introduced with Spring 0.67b3, Dec 18 2005.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long registrationDate;

	/**
	 * Time of the last login.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long lastLogin;

	/**
	 * Most recent IP used to log into this account.
	 * Note for devs: As this requires custom toString() and fromString()
	 * methods for persistence, we annotate it on the property methods.
	 */
	private InetAddress lastIp;

	/**
	 * Unique name identification number.
	 * Equals NO_USER_ID (currently 0) if not used/set. By default it is not
	 * set. Multiple accounts may share same name ID. This value actually
	 * indicates last ID as sent with the LOGIN or USERID command by the client.
	 * We use it to detect spawned accounts (accounts registered by the same
	 * name), ban evasion etc.
	 * @see #id
	 */
	private int lastUserId;

	/**
	 * Resolved country code for this name's IP when he last logged on.
	 * If country could not be resolved, "XX" is used for country code,
	 * otherwise a 2-char country code is used.
	 * @see com.springrts.springls.ip2country.IP2CountryService#getCountryCode(InetAddress)
	 */
	private String lastCountry;

	/**
	 * How many seconds did the client spend in-game.
	 * Unix time-stamp compatible.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long inGameTime;

	/**
	 * Access type (eg.: admin, mod, user).
	 * Bit 31 must be 0 (due to int being a signed number, and we do not want to
	 * use any binary complement conversions).
	 */
	private Access access;

	/**
	 * Bot mode specifies whether this is an automated bot,
	 * for example an instance of ChanServ, or a normal account.
	 */
	private boolean bot;

	/**
	 * Whether the user accepted the agreement or not.
	 */
	private boolean agreementAccepted;

	// END: User specific data (stored in the DB)


	/**
	 * Creates a NULL Account.
	 * Used by JPA and the Client class.
	 */
	public Account() {

		this.id                = NO_ACCOUNT_ID;
		this.name              = "";
		this.password          = "";
		this.registrationDate  = 0;
		this.lastLogin         = 0;
		this.lastUserId        = NO_ACCOUNT_ID;
		this.lastIp            = null;
		this.lastCountry       = ProtocolUtil.COUNTRY_UNKNOWN;
		this.inGameTime        = 0;
		this.access            = Access.NONE;
		this.bot               = false;
		this.agreementAccepted = false;
	}
	/**
	 * Only used by 'FSAccountsService'.
	 */
	public Account(String name, String password, Access access, int lastUserId,
			long lastLogin, InetAddress lastIp, long registrationDate,
			String lastCountry, int id, boolean bot, long inGameTime,
			boolean agreementAccepted)
	{
		this.id                = id;
		this.name              = name;
		this.password          = password;
		this.registrationDate  = registrationDate;
		this.lastLogin         = lastLogin;
		this.lastUserId        = lastUserId;
		this.lastIp            = lastIp;
		this.lastCountry       = lastCountry;
		this.inGameTime        = inGameTime;
		this.access            = access;
		this.bot               = bot;
		this.agreementAccepted = agreementAccepted;
	}
	/**
	 * Used when a user registers a new account.
	 */
	public Account(String name, String password, InetAddress lastIp,
			String lastCountry)
	{
		//this.id                = NEW_ACCOUNT_ID; // will be generated by JPA
		this.name              = name;
		this.password          = password;
		this.registrationDate  = System.currentTimeMillis();
		this.lastLogin         = System.currentTimeMillis();
		this.lastUserId        = NO_USER_ID;
		this.lastIp            = lastIp;
		this.lastCountry       = lastCountry;
		this.inGameTime        = 0;
		this.access            = Access.NORMAL;
		this.bot               = false;
		this.agreementAccepted = false;
	}
	/**
	 * Used when logging in on a server in LAN mode.
	 */
	public Account(String name, String password) {
		this(name, password, null, ProtocolUtil.COUNTRY_UNKNOWN);
	}

	private Account(Account acc) {

		this.name              = acc.getName();
		this.password          = acc.getPassword();
		this.access            = acc.getAccess();
		this.lastUserId        = NO_USER_ID;
		this.lastLogin         = acc.getLastLogin();
		this.lastIp            = acc.getLastIp();
		this.registrationDate  = acc.getRegistrationDate();
		this.lastCountry       = acc.getLastCountry();
		this.id                = acc.getId();
		this.bot               = acc.isBot();
		this.inGameTime        = acc.getInGameTime();
		this.agreementAccepted = acc.isAgreementAccepted();
	}

	@Override
	public String toString() {
		return String.format("%s %s %s %d %d %s %d %s %d %s %d %s",
				getName(),
				getPassword(),
				getAccess().toString(),
				getLastUserId(),
				getLastLogin(),
				getLastIp().getHostAddress(),
				getRegistrationDate(),
				getLastCountry(),
				getId(),
				isBot(),
				getInGameTime(),
				isAgreementAccepted());
	}


	@Override
	public Account clone() {
		try {
			return (Account) super.clone();
		} catch (CloneNotSupportedException ex) {
			LOG.error("Failed cloning an Account", ex);
			return null;
		}
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.getName() != null ? this.getName().hashCode()
				: 0);
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
		if ((this.getName() == null) ? (other.getName() != null)
				: !this.name.equals(other.getName()))
		{
			return false;
		}
		return true;
	}

	/**
	 * AccessType bits.
	 * 31 are effective, while the last one is a sign bit which we do not use.
	 * * bits 0 - 2 (3 bits): accessType level
	 *     0 - none (should not be used for logged-in clients)
	 *     1 - normal (limited)
	 *     2 - privileged
	 *     3 - admin
	 *     values 4 - 7 are reserved for future use
	 * * bits 3 - 22 (20 bits): in-game time (how many minutes did client spend
	 *   in-game.)
	 * * bit 23: agreement bit. It tells us whether name has already
	 *     read the "terms of use" and agreed to it. If not, we should
	 *     first send him the agreement and wait until he confirms it (before
	 *     allowing him to log on the server).
	 * * bit 24 - bot mode (0 - normal name, 1 - automated bot).
	 * * bits 25 - 30 (6 bits): reserved for future use.
	 * * bit 31: unused (integer sign)
	 */
	@Transient
	public int getAccessBitField() {

		int bf = 0;

		bf += getAccess().ordinal();
		bf += ((getInGameTime() & 0xFFFFF) / 60) << 3;
		bf += isAgreementAccepted() ? 0x800000 : 0;
		bf += isBot() ? 0x1000000 : 0;

		return bf;
	}

	public static Access extractAccess(int accessBitField) {

		final int accessOrdinal = accessBitField & 0x3;
		if (accessOrdinal >= Access.values().length) {
			return null;
		} else {
			return Access.values()[accessOrdinal];
		}
	}
	public static long extractInGameTime(int accessBitField) {
		return ((accessBitField & 0x7FFFF8) >> 3) * 60L;
	}
	public static boolean extractAgreementAccepted(int accessBitField) {
		return ((accessBitField & 0x800000) >> 23) == 1;
	}
	public static boolean extractBot(int accessBitField) {
		return ((accessBitField & 0x1000000) >> 24) == 1;
	}

	/**
	 * Returns 'null' if username is valid; an error description otherwise.
	 */
	public static String isUsernameValid(String userName) {

		if (userName.length() > 20) {
			return "Username too long";
		} else if (userName.length() < 2) {
			return "Username too short";
		} else if (!userName.matches("^[A-Za-z0-9_]+$")) {
			return "Username contains invalid characters";
		} else {
			// everything is OK:
			return null;
		}
	}

	/**
	 * Returns 'null' if password is valid; an error description otherwise.
	 */
	public static String isPasswordValid(String toCheckPassword) {

		if (toCheckPassword.length() < 2) {
			return "Password too short";
		} else if (toCheckPassword.length() > 30) {
			// md5-base64 encoded passwords require 24 chars
			return "Password too long";
		}
		// we have to allow a bit wider range of possible chars,
		// as base64 can produce chars such as '+', '=' and '/'
		else if (!toCheckPassword.matches("^[\\x2B-\\x7A]+$")) {
			return "Password contains invalid characters";
		} else {
			// everything is OK:
			return null;
		}
	}

	/**
	 * Returns 'null' if username is valid; an error description otherwise.
	 * This is used with "old" format of user names which could also contain
	 * "[" and "]" characters.
	 * @param userName to be checked for validity
	 * @return an error description or 'null' if nickName is ok
	 */
	public static String isOldUsernameValid(String userName) {

		if (userName.length() > 20) {
			return "Username too long";
		} else if (userName.length() < 2) {
			return "Username too short";
		} else if (!userName.matches("^[A-Za-z0-9_\\[\\]]+$")) {
			return "Username contains invalid characters";
		} else {
			// everything is OK:
			return null;
		}
	}

	/**
	 * Checks if the given nick-name is a valid username.
	 * The nickname must contain part of the baseUserName.
	 * It may only prefix or postfix the baseUserName.
	 * @param nickName to be checked for validity
	 * @param baseUserName used to test nickName against
	 * @return an error description or <code>null</code> if nickName is valid
	 */
	public static String isNicknameValid(String nickName, String baseUserName) {

		if (nickName.length() > 20) {
			return "Nickname too long";
		} else if (nickName.length() < 2) {
			return "Nickname too short";
		} else if (!nickName.matches("^[A-Za-z0-9_\\[\\]\\|]+$")) {
			return "Nickname contains invalid characters";
		}
		// check if prefix is valid:
		else if (!nickName.matches("^([A-Za-z0-9\\[\\]\\|]+[\\|\\]])?"
				+ baseUserName))
		{
			return "Invalid prefix found in nickname: embed your prefix "
					+ "in [] brackets or separate it by a | character";
		}
		// check if postfix is valid:
		else if (!nickName.matches(baseUserName
				+ "([\\|\\[][A-Za-z0-9\\[\\]\\|]+)?$"))
		{
			return "Invalid postfix found in nickname: embed your postfix "
					+ "in [] brackets or separate it by a | character";
		}
		// check if prefix and postfix are both valid in one shot:
		else if (!nickName.matches("^([A-Za-z0-9\\[\\]\\|]+[\\|\\]])?"
				+ baseUserName + "([\\|\\[][A-Za-z0-9\\[\\]\\|]+)?$"))
		{
			return "Nickname contains invalid prefix/postfix. "
					+ "Your username should be contained in your nickname!";
		} else {
			// everything is OK:
			return null;
		}
	}

	@Transient
	public long getInGameTimeInMins() {
		return (getInGameTime() / 60L);
	}

	@Transient
	public Rank getRank() {

		final long igtSeconds = getInGameTime();

		Rank[] allRanks = Rank.values();
		for (int r = allRanks.length-1; r >= 0; r--) {
			final Rank curRank = allRanks[r];
			if (igtSeconds >= curRank.getRequiredTime()) {
				return curRank;
			}
		}

		return Rank.Newbie;
	}

	/**
	 * Adds <code>mins</code> minutes to the client's in-game time.
	 * This time is used to calculate the player's rank.
	 * @param mins number of minutes to add to the client's in-game time
	 * @return true if player's rank was changed, false otherwise.
	 */
	public boolean addMinsToInGameTime(long mins) {

		final Rank tmp = getRank();
		setInGameTime(getInGameTime() + (mins * 60));
		return tmp != getRank();
	}

	/**
	 * Unique account identification number.
	 * This is different to the <code>lastUserId</code>, because it
	 * @see #getLastUserId()
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(
		name       = "id",
		unique     = true,
		nullable   = false,
		insertable = true,
		updatable  = false
		)
	public int getId() {
		return id;
	}

	/**
	 * Unique account identification number.
	 * This is different to the <code>lastUserId</code>, because it
	 * @see #getLastUserId()
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
	@Column(
		name       = "name",
		unique     = true,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 40
		)
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
	@Column(
		name       = "password",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 32
		)
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

	// TODO convert to Date
	/**
	 * Date of when the name registered this account.
	 * In milliseconds (refers to {@link java.lang.System#currentTimeMillis()}).
	 * 0 means the registration date is unknown. This applies to users that
	 * registered in some early version, when this field was not yet present.
	 * Note that this field was first introduced with Spring 0.67b3,
	 * Dec 18 2005.
	 * @return the date of registration as a Unix time-stamp
	 */
	@Column(
		name       = "register_date",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = false
		)
	public long getRegistrationDate() {
		return registrationDate;
	}

	/**
	 * Date of when the name registered this account.
	 * In milliseconds (refers to {@link java.lang.System#currentTimeMillis()}).
	 * 0 means the registration date is unknown. This applies to users that
	 * registered in some early version, when this field was not yet present.
	 * Note that this field was first introduced with Spring 0.67b3,
	 * Dec 18 2005.
	 * Note: required for JPA only
	 * @param registrationDate the date of registration as a Unix time-stamp
	 */
	protected void setRegistrationDate(long registrationDate) {
		this.registrationDate = registrationDate;
	}

	// TODO convert to Date
	/**
	 * Time ({@link java.lang.System#currentTimeMillis()}) of the last login.
	 * @return the lastLogin
	 */
	@Column(
		name       = "last_login",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true
		)
	public long getLastLogin() {
		return lastLogin;
	}

	/**
	 * Time ({@link java.lang.System#currentTimeMillis()}) of the last login.
	 * @param lastLogin the lastLogin to set
	 */
	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	/**
	 * Most recent IP used to log into this account.
	 * @return the lastIP
	 */
	public InetAddress getLastIp() {
		return lastIp;
	}

	/**
	 * Most recent IP used to log into this account.
	 * @param lastIp the lastIP to set
	 */
	public void setLastIp(InetAddress lastIp) {
		this.lastIp = lastIp;
	}

	/**
	 * Returns the most recent IP used to log into this account as string.
	 * This should be used in persistence and lobby protocol specific parts
	 * only.
	 * @see #getLastIp()
	 */
	@Column(
		name       = "last_ip",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 15
		)
	public String getLastIpAsString() {

		if (lastIp == null) {
			return NO_ACCOUNT_LAST_IP_STR;
		} else {
			return lastIp.getHostAddress();
		}
	}

	/**
	 * Sets the most recent IP used to log into this account as string.
	 * This should be used in persistence and lobby protocol specific parts
	 * only.
	 * @see #setLastIp(InetAddress)
	 */
	public void setLastIpAsString(String lastIp) {

		if (lastIp.equals(NO_ACCOUNT_LAST_IP_STR)) {
			this.lastIp = null;
		} else {
			this.lastIp = Misc.parseIp(lastIp);
		}
	}

	/**
	 * Unique name identification number.
	 * Equals NO_USER_ID (currently 0) if not used/set. By default it is not
	 * set. Multiple accounts may share same name ID. This value actually
	 * indicates last ID as sent with the LOGIN or USERID command by the client.
	 * We use it to detect spawned accounts (accounts registered by the same
	 * name), ban evasion etc.
	 * @see #getId()
	 * @return the lastUserId
	 */
	@Column(
		name       = "last_id",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
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
	 * @see #getId()
	 * @param lastUserId the lastUserId to set
	 */
	public void setLastUserId(int lastUserId) {
		this.lastUserId = lastUserId;
	}

	/**
	 * Resolved country code for this name's IP when he last logged on.
	 * If country could not be resolved, "XX" is used for country code,
	 * otherwise a 2-char country code is used.
	 * @see com.springrts.springls.ip2country.IP2CountryService#getCountryCode(InetAddress)
	 * @return the lastCountry
	 */
	@Column(
		name       = "last_country",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true,
		length     = 2
		)
	public String getLastCountry() {
		return lastCountry;
	}

	/**
	 * Resolved country code for this name's IP when he last logged on.
	 * If country could not be resolved, "XX" is used for country code,
	 * otherwise a 2-char country code is used.
	 * @see com.springrts.springls.ip2country.IP2CountryService#getCountryCode(InetAddress)
	 * @param lastCountry the lastCountry to set
	 */
	public void setLastCountry(String lastCountry) {
		this.lastCountry = lastCountry;
	}

	/**
	 * How many seconds did the client spend in-game.
	 * Unix time-stamp compatible.
	 * @see java.lang.System#currentTimeMillis()
	 * @return the inGameTime
	 */
	@Column(
		name       = "ingame_time",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	public long getInGameTime() {
		return inGameTime;
	}

	/**
	 * How many seconds did the client spend in-game.
	 * Unix time-stamp compatible.
	 * @see java.lang.System#currentTimeMillis()
	 * @param inGameTime the inGameTime to set
	 */
	public void setInGameTime(long inGameTime) {
		this.inGameTime = inGameTime;
	}

	/**
	 * Access type.
	 * Bit 31 must be 0 (due to int being a signed number, and we don't want to
	 * use any binary complement conversions).
	 * @return the accessType
	 */
	@Column(
		name       = "access",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true
		)
	public Access getAccess() {
		return access;
	}

	/**
	 * Access type.
	 * Bit 31 must be 0 (due to int being a signed number, and we don't want to
	 * use any binary complement conversions).
	 * @param access the access type to set
	 */
	public void setAccess(Access access) {
		this.access = access;
	}

	/**
	 * Bot mode specifies whether this is an automated bot,
	 * for example an instance of ChanServ, or a normal account.
	 * @return whether this is an automated bot or not
	 */
	@Column(
		name       = "bot",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true
		)
	public boolean isBot() {
		return bot;
	}

	/**
	 * Bot mode specifies whether this is an automated bot,
	 * for example an instance of ChanServ, or a normal account.
	 * @param bot whether this is an automated bot or not
	 */
	public void setBot(boolean bot) {
		this.bot = bot;
	}

	/**
	 * Whether the user accepted the agreement or not.
	 * @return the agreementAccepted
	 */
	@Column(
		name       = "agreement",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true
		)
	public boolean isAgreementAccepted() {
		return agreementAccepted;
	}

	/**
	 * Whether the user accepted the agreement or not.
	 * @param agreementAccepted the agreementAccepted to set
	 */
	public void setAgreementAccepted(boolean agreementAccepted) {
		this.agreementAccepted = agreementAccepted;
	}
}
