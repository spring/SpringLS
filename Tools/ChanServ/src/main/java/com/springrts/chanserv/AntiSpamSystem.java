/*
 * Created on 25.2.2006
 */

package com.springrts.chanserv;


import java.util.*;
import java.util.Map.Entry;

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

	public SpamRecord() {

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
class SpamSettings {

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

class AntiSpamTask extends TimerTask {
	
	/** Here we will reduce penalty points for all users */
	@Override
	public void run() {
		if (!ChanServ.isConnected()) {
			return;
		}
		synchronized (AntiSpamSystem.spamRecords) {
			for (Entry<String, SpamRecord> record : AntiSpamSystem.spamRecords.entrySet()) {
				SpamRecord rec = record.getValue();
				// reduce by 1.0 penalty point each second
				rec.setPenaltyPoints(Math.max(0, rec.getPenaltyPoints() - 1.0));
				if (rec.getPenaltyPoints() == 0) {
					AntiSpamSystem.spamRecords.remove(record.getKey());
				}
			}
		}
	}
}

class AntiSpamSystem {

	/**
	 * Once every so milliseconds, we will reset the counter in the Client
	 * that counts how many CLIENTSTATUS command have been received from a user
	 * In milliseconds.
	 */
	final static int CHECK_CLIENTSTATUSCHANGE_INTERVAL = 3000;
	/** Max frequency of CLIENTSTATUS command.
	 * If we receive more than this times per second,
	 * we have a problem.
	 * We must still check MIN_CLIENTSTATUCCHANGE_COUNT_BEFORE_ALERT before
	 * taking actions, because a user might change status quickly
	 * (in few milliseconds) by accident (if spring crashes, for example).
	 */
	final static float MAX_CLIENTSTATUSCHANGE_FREQUENCY = 5.0f;
	final static int MIN_CLIENTSTATUCCHANGE_COUNT_BEFORE_ALERT = 5;

	/** as a key in this list we use a combination of "channame:username" */
	protected static final Map<String, SpamRecord> spamRecords = new HashMap<String, SpamRecord>();

	/** spam settings for each individual channel */
	protected static Map<String, SpamSettings> spamSettings;

	private static Timer antiSpamTimer;


	private AntiSpamSystem() {
	}


	/** Initializes the anti-spam system */
	public static void initialize() {

		spamSettings = new HashMap<String, SpamSettings>();

		antiSpamTimer = new Timer();
		antiSpamTimer.schedule(new AntiSpamTask(),
				1000,  // initial delay
				1000); // subsequent rate
	}

	/** Stops the anti-spam system */
	public static void uninitialize() {
		try {
			antiSpamTimer.cancel();
		} catch (Exception e) {
			// do nothing
		}
	}

	/**
	 * Call this method when user says something in the channel using SAY
	 * or SAYEX command
	 */
	public static void processUserMsg(String chan, String user, String msg) {

		synchronized (spamRecords) {
			String key = chan + ":" + user;
			SpamSettings settings = spamSettings.get(chan);
			if (settings == null) {
				settings = SpamSettings.DEFAULT_SETTINGS;
			}

			SpamRecord rec = spamRecords.get(key);
			if (rec == null) {
				rec = new SpamRecord();
				spamRecords.put(key, rec);
			}

			// determine severity:
			double severity = settings.getNormalMsgPenalty();
			if (msg.length() > settings.getLongMsgLength()) {
				severity += settings.getLongMsgPenalty();
			}
			if (rec.getLastMsg().equals(msg)) {
				severity += settings.getDoubleMsgPenalty();
			}

			rec.setPenaltyPoints(rec.getPenaltyPoints() + severity);
			rec.setLastMsg(msg);

			// check if user has gathered too many penalty points:
			if (rec.getPenaltyPoints() >= settings.getPenaltyLimit()) {
				rec.setPenaltyPoints(0); // reset counter
				muteUser(chan, user);
			}
		}
	}

	public static void processClientStatusChange(Client client) {

		if (System.currentTimeMillis() - client.getClientStatusChangeCheckpoint() > CHECK_CLIENTSTATUSCHANGE_INTERVAL) {
			// reset the counter:
			client.setClientStatusChangeCheckpoint(System.currentTimeMillis());
			client.resetStatusChanges();
		}

		client.addOneStatusChange();

		if (((System.currentTimeMillis() - client.getClientStatusChangeCheckpoint()) * 1.0f / client.getStatusChanges()) > MAX_CLIENTSTATUSCHANGE_FREQUENCY) {
			if (client.getStatusChanges() > MIN_CLIENTSTATUCCHANGE_COUNT_BEFORE_ALERT) {
				// reset the counter:
				client.setClientStatusChangeCheckpoint(System.currentTimeMillis());
				client.resetStatusChanges();

				// take action:
				ChanServ.sendLine("KICKUSER " + client.getName() + " CLIENTSTATUS command abuse - frequency too high");
			}
		}
	}

	public static void setSpamSettingsForChannel(String chan, String settings) {

		SpamSettings ss = SpamSettings.stringToSpamSettings(settings);
		if (ss == null) {
			Log.error("AntiSpamSystem: malformed settings string in setSpamSettingsForChannel(): " + settings);
			spamSettings.put(chan, SpamSettings.DEFAULT_SETTINGS);
		} else {
			spamSettings.put(chan, ss);
		}
	}

	private static void muteUser(String chan, String user) {

		ChanServ.sendLine("MUTE " + chan + " " + user + " 15");
		ChanServ.sendLine("SAYPRIVATE " + user + " You have been temporarily muted due to spamming in channel #" + chan + ". You may get temporarily banned if you will continue to spam this channel.");
	}
}
