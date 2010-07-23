
package com.springrts.chanserv.antispam;


/**
 * @author Betalord
 */
class SpamRecord {

	/** cumulative penalty points */
	private double penaltyPoints;
	/** time of last line sent to the channel by this user */
	@Deprecated
	private long timeOfLastMsg;
	/** last line sent to the channel by this user */
	private String lastMsg;

	SpamRecord() {

		penaltyPoints = 0;
		timeOfLastMsg = 0;
		lastMsg = "";
	}

	/**
	 * cumulative penalty points
	 * @return the penaltyPoints
	 */
	public double getPenaltyPoints() {
		return penaltyPoints;
	}

	/**
	 * cumulative penalty points
	 * @param penaltyPoints the penaltyPoints to set
	 */
	public void setPenaltyPoints(double penaltyPoints) {
		this.penaltyPoints = penaltyPoints;
	}

	/**
	 * last line sent to the channel by this user
	 * @return the lastMsg
	 */
	public String getLastMsg() {
		return lastMsg;
	}

	/**
	 * last line sent to the channel by this user
	 * @param lastMsg the lastMsg to set
	 */
	public void setLastMsg(String lastMsg) {
		this.lastMsg = lastMsg;
	}
}