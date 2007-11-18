/*
 * Created on 2007.11.10
 * 
 */

/**
 * @author Betalord
 *
 */

import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BanSystem {
	
	private static ArrayList<BanEntry> banEntries = new ArrayList<BanEntry>();
	
	/**
	 * Will retrieve latest ban entries from the database
	 * */
	public static void fetchLatestBanList() {
		System.out.println("Trying to update ban list ...");
		
		banEntries.clear();
		
		ResultSet rs = TASServer.database.execQuery("SELECT ExpirationDate, Username, IP_start, IP_end, userID, PublicReason FROM BanEntries WHERE (Enabled=1 AND (ExpirationDate IS NULL OR ExpirationDate > CURRENT_TIMESTAMP))");
		
		try {
			while (rs.next()) {
				BanEntry ban = new BanEntry();
				ban.expireDate = (rs.getTimestamp("ExpirationDate") != null ? rs.getTimestamp("ExpirationDate").getTime() : 0);
				ban.username = rs.getString("Username");
				ban.IP_start = rs.getLong("IP_start");
				ban.IP_end = rs.getLong("IP_end");
				ban.userID = rs.getInt("userID");
				ban.publicReason = rs.getString("PublicReason");
				
				if ((ban.expireDate != 0) && (ban.expireDate < System.currentTimeMillis()))
					continue; // already expired
				banEntries.add(ban);
			}
		} catch (SQLException e) {
			System.out.println("Error reading ResultSet when reading ban entries!");
			TASServer.database.printSQLException(e);
			System.out.println("Error while trying to update ban list!");
			return ;
		}
		System.out.println("Ban list successfully updated.");
	}
	
	/**
	 * returns null if IP/username/userID is not banned, or corresponding BanEntry object otherwise 
	 * */
	public static BanEntry checkIfBanned(String username, long IP, int userID) {
		for (int i = 0; i < banEntries.size(); i++) {
			BanEntry ban = banEntries.get(i);
			// check if already expired:
			if ((ban.expireDate != 0) && (ban.expireDate < System.currentTimeMillis())) {
				// already expired, so lets remove it and skip it:
				banEntries.remove(i);
				i--;
				continue;
			}

			// check if banned by user ID:
			if ((ban.userID != 0) && (userID != 0)) {
				if (userID == ban.userID)
					return ban;
			}
			
			// check if banned by username:
			if (ban.username != null) {
				if (username.equals(ban.username))
					return ban;
			}

			// check if banned by IP address:
			if (ban.IP_start != 0) {
				if (checkIPAgainstRange(IP, ban.IP_start, ban.IP_end))
					return ban;
			}
		}
		return null;
	}
	
	/**
	 * returns true if this IP is within the given range
	 * */
	private static boolean checkIPAgainstRange(String IP, String IP_start, String IP_end) {
		String[] ip = IP.split("\\.");
		String[] start = IP_start.split("\\.");
		String[] end = IP_end.split("\\.");
		
		if (Integer.parseInt(ip[0]) < Integer.parseInt(start[0])) return false;
		else if (Integer.parseInt(ip[0]) == Integer.parseInt(start[0]))
			if (Integer.parseInt(ip[1]) < Integer.parseInt(start[1])) return false;
			else if (Integer.parseInt(ip[1]) == Integer.parseInt(start[1]))
				if (Integer.parseInt(ip[2]) < Integer.parseInt(start[2])) return false;
				else if (Integer.parseInt(ip[2]) == Integer.parseInt(start[2]))
					if (Integer.parseInt(ip[3]) < Integer.parseInt(start[3])) return false;
					else if (Integer.parseInt(ip[3]) == Integer.parseInt(start[3]))
						if (Integer.parseInt(ip[4]) < Integer.parseInt(start[4])) return false;
		
		if (Integer.parseInt(ip[0]) > Integer.parseInt(end[0])) return false;
		else if (Integer.parseInt(ip[0]) == Integer.parseInt(end[0]))
			if (Integer.parseInt(ip[1]) > Integer.parseInt(end[1])) return false;
			else if (Integer.parseInt(ip[1]) == Integer.parseInt(end[1]))
				if (Integer.parseInt(ip[2]) > Integer.parseInt(end[2])) return false;
				else if (Integer.parseInt(ip[2]) == Integer.parseInt(end[2]))
					if (Integer.parseInt(ip[3]) > Integer.parseInt(end[3])) return false;
					else if (Integer.parseInt(ip[3]) == Integer.parseInt(end[3]))
						if (Integer.parseInt(ip[4]) > Integer.parseInt(end[4])) return false;
		
		return true; // IP is obviously within the range
	}
	
	/**
	 * returns true if this IP is within the given range
	 * */
	private static boolean checkIPAgainstRange(long IP, long IP_start, long IP_end) {
		return ((IP >= IP_start) && (IP <= IP_end));
	}
	
	
}
