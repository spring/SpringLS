/*
 * Created on 2007.11.10
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Betalord
 */
public class BanSystem {

	private static final Log s_log  = LogFactory.getLog(BanSystem.class);

	private static ArrayList<BanEntry> banEntries = new ArrayList<BanEntry>();

	/**
	 * Will retrieve latest ban entries from the database
	 */
	public static void fetchLatestBanList() {
		s_log.info("Trying to update ban list ...");

		banEntries.clear();

		String query = "SELECT ExpirationDate, Username, IP_start, IP_end, userID, PublicReason FROM BanEntries WHERE (Enabled=1 AND (ExpirationDate IS NULL OR ExpirationDate > CURRENT_TIMESTAMP))";

		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;

		try {
			conn = DriverManager.getConnection(TASServer.database.getConnectionURL());
			stmt = conn.createStatement();
			rset = stmt.executeQuery(query);

			while (rset.next()) {
				BanEntry ban = new BanEntry(
						(rset.getTimestamp("ExpirationDate") != null ? rset.getTimestamp("ExpirationDate").getTime() : 0),
						rset.getString("Username"),
						rset.getLong("IP_start"),
						rset.getLong("IP_end"),
						rset.getInt("userID"),
						rset.getString("PublicReason"));

				if ((ban.getExpireDate() != 0) && (ban.getExpireDate() < System.currentTimeMillis())) {
					// already expired
					continue;
				}
				banEntries.add(ban);
			}

		} catch (SQLException e) {
			s_log.error("Failed reading ResultSet while reading ban entries!", e);
			DBInterface.logSQLException(s_log, e);
			s_log.error("Error while trying to update ban list!");
			return;
		} catch (Exception e) {
			Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem detected: error while trying to access the database.");
			s_log.error("Failed reading ResultSet while reading ban entries!", e);
			return;
		} finally {
			// always return connection back to pool, no matter what!
			try { rset.close(); } catch(Exception e) { }
			try { stmt.close(); } catch(Exception e) { }
			try { conn.close(); } catch(Exception e) { }
		}

		s_log.info("Ban list successfully updated.");
	}

	/**
	 * Returns null if IP/username/userID is not banned, or corresponding BanEntry object otherwise
	 */
	public static BanEntry checkIfBanned(String username, long IP, int userID) {
		for (int i = 0; i < banEntries.size(); i++) {
			BanEntry ban = banEntries.get(i);
			// check if already expired:
			if ((ban.getExpireDate() != 0) && (ban.getExpireDate() < System.currentTimeMillis())) {
				// already expired, so lets remove it and skip it:
				banEntries.remove(i);
				i--;
				continue;
			}

			// check if banned by user ID:
			if ((ban.getUserId() != 0) && (userID != 0)) {
				if (userID == ban.getUserId())
					return ban;
			}

			// check if banned by username:
			if (ban.getUsername() != null) {
				if (username.equals(ban.getUsername())) {
					return ban;
				}
			}

			// check if banned by IP address:
			if (ban.getIpStart() != 0) {
				if (checkIPAgainstRange(IP, ban.getIpStart(), ban.getIpEnd())) {
					return ban;
				}
			}
		}
		return null;
	}

	/**
	 * returns true if this IP is within the given range
	 */
	private static boolean checkIPAgainstRange(long IP, long IP_start, long IP_end) {
		return ((IP >= IP_start) && (IP <= IP_end));
	}
}
