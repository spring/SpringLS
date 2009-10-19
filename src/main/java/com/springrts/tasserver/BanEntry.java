/*
 * Created on 2007.11.10
 */

package com.springrts.tasserver;


import java.io.Serializable;
import javax.persistence.*;

/**
 * @author Betalord
 */
@Entity
@Table(name="bans")
public class BanEntry implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	/** person who issued the ban */
//	private String owner;
	/** timestamp of when ban was issued */
//	private long banDate;
	/**
	 * Timestamp of when ban will expire.
	 * Equals 0 when ban was issued for indefinite time
	 */
	@Column(
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private long expireDate;
	/** Username of the banned account (may be 'null' as well) */
	@Column(
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true,
		length     = 40
		)
	private String username;
	/** Start of IP range which was banned (may be 0) */
	@Column(
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private long ipStart;
	/** End of IP range which was banned (may be 0) */
	@Column(
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private long ipEnd;
	/** Account ID of the banned account. If 0, then ignore it */
	@Column(
		unique     = true,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private int userId;
	/**
	 * Private reason for the ban.
	 * Only moderators (and admins) can see see this.
	 */
//	private String privatReason;
	/**
	 * Public reason for the ban.
	 * The person who was banned will see this, when trying to login.
	 */
	@Column(
		unique     = false,
		nullable   = true,
		insertable = true,
		updatable  = true
		)
	private String publicReason;

	/** Used by JPA */
	public BanEntry() {}
	public BanEntry(long expireDate, String username, long ipStart, long ipEnd,
			int userId, String publicReason) {

		this.expireDate   = expireDate;
		this.username     = username;
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
	public String getUsername() {
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
