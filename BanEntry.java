/*
 * Created on 2007.11.10
 * 
 */

/**
 * @author Betalord
 *
 */

public class BanEntry {
//	public String owner; // person who issued the ban
//	public long banDate; // timestamp of when ban was issued
	public long expireDate; // timestamp of when ban will expire. Equals 0 when ban was issued for indefinite time
	public String username; // username of banned account (may be null as well)
	public long IP_start; // start of IP range which was banned (may be null as well)
	public long IP_end; // end of IP range which was banned (may be null as well)
	public int userID; // user ID of banned account. If 0, then ignore it.
//	public String privatReason; // reason for ban which only moderators (and admins) can see
	public String publicReason; // reason for ban which the person who got banned will see when he'll try to login
}
