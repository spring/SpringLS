
package com.springrts.chanserv.antispam;

import javax.xml.bind.annotation.XmlElement;

/**
 * Here are included all the routines needed with the anti-spam protection.
 * This system is separated from the rest of the bot code, however it does call
 * back the bot when it detects spamming.
 *
 * Its current functionality is very basic - it is based on assigning penalty
 * points to users based on message length and message repetition. It is possible
 * to set penalty points for events per channel basis.
 *
 * 25x12x2007: Added detection of "CLIENTSTATUS spam" exploit, which could freeze
 * all players on the server who are using TASClient.
 *
 * @author Betalord
 */
public class SpamSettings {

	public static final SpamSettings DEFAULT_SETTINGS = new SpamSettings();

	/** When penalty points reach this limit, user gets muted */
	@XmlElement()
	private int penaltyLimit;
	/**
	 * Length (in characters) of a message that is considered "long".
	 * For long messages, extra penalty points will be added
	 */
	@XmlElement()
	private int longMsgLength;
	/**
	 * A penalty that is added each time user says something in the channel.
	 * Other penalties are added upon this one.
	 */
	@XmlElement()
	private double normalMsgPenalty;
	/** penalty value added if the message is longer than 'longMsgLength' */
	@XmlElement()
	private double longMsgPenalty;
	/** penalty value added if message is same as previous message */
	@XmlElement()
	private double doubleMsgPenalty;

	public SpamSettings(int penaltyLimit, int longMsgLength, double normalMsgPenalty, double longMsgPenalty, double doubleMsgPenalty) {

		this.penaltyLimit = penaltyLimit;
		this.longMsgLength = longMsgLength;
		this.normalMsgPenalty = normalMsgPenalty;
		this.longMsgPenalty = longMsgPenalty;
		this.doubleMsgPenalty = doubleMsgPenalty;
	}

	public SpamSettings() {
		// use the default settings:
		this(5, 200, 1.0, 0.5, 0.5);
	}

	@Override
	public String toString() {
		return toProtocolString();
	}

	/**
	 * @see fromProtocolString()
	 */
	public String toProtocolString() {

		StringBuilder res = new StringBuilder();

		res.append(getPenaltyLimit());
		res.append(" ").append(getLongMsgLength());
		res.append(" ").append(getNormalMsgPenalty());
		res.append(" ").append(getLongMsgPenalty());
		res.append(" ").append(getDoubleMsgPenalty());

		return res.toString();
	}

	/**
	 * Returns null if settings string is malformed.
	 * @see toProtocolString()
	 */
	public static SpamSettings fromProtocolString(String settings) {

		SpamSettings ss = null;

		String[] parsed = settings.split(" ");
		if (parsed.length != 5) {
			throw new IllegalArgumentException("Malformed spam settings; Needs exactly 5 arguments.");
		}
		try {
			int penaltyLimit = Integer.parseInt(parsed[0]);
			int longMsgLength = Integer.parseInt(parsed[1]);
			double normalMsgPenalty = Double.parseDouble(parsed[2]);
			double longMsgPenalty = Double.parseDouble(parsed[3]);
			double doubleMsgPenalty = Double.parseDouble(parsed[4]);
			ss = new SpamSettings(penaltyLimit, longMsgLength, normalMsgPenalty, longMsgPenalty, doubleMsgPenalty);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Malformed spam settings; Has to consist of 2*int, 3*double.", ex);
		}

		return ss;
	}

	/**
	 * When penalty points reach this limit, user gets muted
	 * @return the penaltyLimit
	 */
	public int getPenaltyLimit() {
		return penaltyLimit;
	}

	/**
	 * Length (in characters) of a message that is considered "long".
	 * For long messages, extra penalty points will be added
	 * @return the longMsgLength
	 */
	public int getLongMsgLength() {
		return longMsgLength;
	}

	/**
	 * A penalty that is added each time user says something in the channel.
	 * Other penalties are added upon this one.
	 * @return the normalMsgPenalty
	 */
	public double getNormalMsgPenalty() {
		return normalMsgPenalty;
	}

	/**
	 * penalty value added if the message is longer than 'longMsgLength'
	 * @return the longMsgPenalty
	 */
	public double getLongMsgPenalty() {
		return longMsgPenalty;
	}

	/**
	 * penalty value added if message is same as previous message
	 * @return the doubleMsgPenalty
	 */
	public double getDoubleMsgPenalty() {
		return doubleMsgPenalty;
	}
}
