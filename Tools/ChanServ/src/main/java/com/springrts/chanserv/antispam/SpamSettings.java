
package com.springrts.chanserv.antispam;

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

	public static final SpamSettings DEFAULT_SETTINGS = new SpamSettings(5, 200, 1.0, 0.5, 0.5);

	/** When penalty points reach this limit, user gets muted */
	private int penaltyLimit;
	/**
	 * Length (in characters) of a message that is considered "long".
	 * For long messages, extra penalty points will be added
	 */
	private int longMsgLength;
	/**
	 * A penalty that is added each time user says something in the channel.
	 * Other penalties are added upon this one.
	 */
	private double normalMsgPenalty;
	/** penalty value added if the message is longer than 'longMsgLength' */
	private double longMsgPenalty;
	/** penalty value added if message is same as previous message */
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
		this(DEFAULT_SETTINGS.penaltyLimit, DEFAULT_SETTINGS.longMsgLength, DEFAULT_SETTINGS.normalMsgPenalty, DEFAULT_SETTINGS.longMsgPenalty, DEFAULT_SETTINGS.doubleMsgPenalty);
	}

	@Override
	public String toString() {
		return getPenaltyLimit() + " " + getLongMsgLength() + " " + getNormalMsgPenalty() + " " + getLongMsgPenalty() + " " + getDoubleMsgPenalty();
	}

	// this method is redundant. Use settings.toString() instead!
	public static String spamSettingsToString(SpamSettings settings) {
		return settings.toString();
	}

	// returns null if settings string is malformed
	public static SpamSettings stringToSpamSettings(String settings) {
		SpamSettings ss = new SpamSettings();
		String[] parsed = settings.split(" ");
		if (parsed.length != 5) {
			return null;
		}
		try {
			ss.penaltyLimit = Integer.parseInt(parsed[0]);
			ss.longMsgLength = Integer.parseInt(parsed[1]);
			ss.normalMsgPenalty = Double.parseDouble(parsed[2]);
			ss.longMsgPenalty = Double.parseDouble(parsed[3]);
			ss.doubleMsgPenalty = Double.parseDouble(parsed[4]);
		} catch (NumberFormatException e) {
			return null;
		}
		return ss;
	}

	/* returns true if settings string is valid */
	public static boolean validateSpamSettingsString(String settings) {
		return settings.matches("^\\d+ \\d+ [0-9]*\\.?[0-9]+ [0-9]*\\.?[0-9]+ [0-9]*\\.?[0-9]+$");
		// perhaps rather use "return stringToSpamSettings(settings) != null" ?
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
