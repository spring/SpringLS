/*
	Copyright (c) 2007 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.tasserver;


import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Betalord
 * @author hoijui
 */
@Entity
@Table(name = "BanEntries")
@NamedQueries({
	@NamedQuery(name = "ban_size",        query = "SELECT count(b.id) FROM BanEntry b"),
	@NamedQuery(name = "ban_size_active", query = "SELECT count(b.id) FROM BanEntry b WHERE ((b.enabled = TRUE) AND (b.expireDate IS NULL OR b.expireDate > CURRENT_TIMESTAMP))"),
	@NamedQuery(name = "ban_list",        query = "SELECT b FROM BanEntry b"),
	@NamedQuery(name = "ban_list_active", query = "SELECT b FROM BanEntry b WHERE ((b.enabled = TRUE) AND (b.expireDate IS NULL OR b.expireDate > CURRENT_TIMESTAMP))"),
	@NamedQuery(name = "ban_fetch",       query = "SELECT b FROM BanEntry b WHERE ((b.username = :username) OR ((b.ipStart <= :ip) AND (b.ipStart >= :ip)) OR (b.userId >= :userId))")
})
public class BanEntry implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	/** Username of the account that issued the ban. */
//	private String owner;
	/** Timestamp of when ban was issued. */
//	private long banDate;
	/**
	 * Timestamp of when ban will expire.
	 * Equals 0 when ban was issued for indefinite time
	 */
	@Column(
		name       = "ExpirationDate",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private long expireDate;
	/** Username of the banned account, which may be <code>null</code>. */
	@Column(
		name       = "Username",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true,
		length     = 40
		)
	private String username;
	/** The start of IP range which was banned, which may be <code>0</code>. */
	@Column(
		name       = "IP_start",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private long ipStart;
	/** The end of IP range which was banned, which may be <code>0</code>. */
	@Column(
		name       = "IP_end",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private long ipEnd;
	/**
	 * Account ID of the banned account.
	 * If <code>0</code>, then we ignore it.
	 */
	@Column(
		name       = "userID",
		unique     = true,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private int userId;
	/**
	 * Private reason for the ban.
	 * Only moderators (and admins) can see this.
	 */
//	private String privatReason;
	/**
	 * Public reason for the ban.
	 * The person who was banned will see this, when trying to login.
	 */
	@Column(
		name       = "PublicReason",
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private String publicReason;
	/**
	 * Whether this ban is active or disabled.
	 */
	@Column(
		name       = "Enabled",
		unique     = false,
		nullable   = false,
		insertable = true,
		updatable  = true
		)
	private boolean enabled;

	/** Used by JPA */
	public BanEntry() {}
	public BanEntry(long expireDate, String userName, long ipStart, long ipEnd,
			int userId, String publicReason)
	{
		this.expireDate   = expireDate;
		this.username     = userName;
		this.ipStart      = ipStart;
		this.ipEnd        = ipEnd;
		this.userId       = userId;
		this.publicReason = publicReason;
	}

	/**
	 * Timestamp of when ban will expire.
	 * Equals 0 when ban was issued for indefinite time
	 * @return the expireDate
	 */
	public long getExpireDate() {
		return expireDate;
	}

	/**
	 * Username of the banned account (may be 'null' as well)
	 * @return the username
	 */
	public String getUserName() {
		return username;
	}

	/**
	 * Start of IP range which was banned (may be 0)
	 * @return the ipStart
	 */
	public long getIpStart() {
		return ipStart;
	}

	/**
	 * End of IP range which was banned (may be 0)
	 * @return the ipEnd
	 */
	public long getIpEnd() {
		return ipEnd;
	}

	/**
	 * Account ID of the banned account. If 0, then ignore it
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Public reason for the ban.
	 * The person who was banned will see this, when trying to login.
	 * @return the publicReason
	 */
	public String getPublicReason() {
		return publicReason;
	}

	/**
	 * Returns whether this ban is enabled or disabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns whether this ban is active or disabled.
	 * A ban is active when it is enabled and not expired.
	 */
	public boolean isActive() {
		return (isEnabled() && ((getExpireDate() == 0)
				|| (getExpireDate() > System.currentTimeMillis())));
	}
}
