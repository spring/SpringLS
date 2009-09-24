/*
 * Created on 25.2.2006
 */

package com.springrts.chanserv;


import java.util.*;

/**
 * @author Betalord
 */
class SpamRecord {

	public double penaltyPoints; // cumulative penalty points
	public long timeOfLastMsg; // time of last line sent to the channel by this user
	public String lastMsg; // last line sent to the channel by this user

	public SpamRecord() {
		penaltyPoints = 0;
		timeOfLastMsg = 0;
		lastMsg = "";
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

	int penaltyLimit; // when penalty points reach this limit, user gets muted
	int longMsgLength; // length (in characters) of a message that is considered "long". For long messages, extra penalty points will be added
	double normalMsgPenalty; // this is penalty that is added each time user says something in the channel. Other penalties are added upon this one.
	double longMsgPenalty; // penalty value added if the message is longer than 'longMsgLength'
	double doubleMsgPenalty; // penalty value added if message is same as previous message

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
		return penaltyLimit + " " + longMsgLength + " " + normalMsgPenalty + " " + longMsgPenalty + " " + doubleMsgPenalty;
	}

	// this method is redundant. Use settings.toString() instead!
	public static String spamSettingsToString(SpamSettings settings) {
		return settings.toString();
	}

	// returns null if settings string is malformed
	public static SpamSettings stringToSpamSettings(String settings) {
		SpamSettings ss = new SpamSettings();
		String[] parsed = settings.split(" ");
		if (parsed.length != 5) return null;
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
}

class AntiSpamTask extends TimerTask {
	// here we will reduce penalty points for all users
	public void run() {
		if (!ChanServ.isConnected()) return;
		synchronized (AntiSpamSystem.spamRecords) {
			for (Iterator it = AntiSpamSystem.spamRecords.values().iterator(); it.hasNext(); ) {
				SpamRecord rec = (SpamRecord)it.next();

				rec.penaltyPoints = Math.max(0, rec.penaltyPoints - 1.0); // reduce by 1.0 penalty point each second
				if (rec.penaltyPoints == 0)
					it.remove();
			}
		}
	}
}

public class AntiSpamSystem {

	final static int CHECK_CLIENTSTATUSCHANGE_INTERVAL = 3000; // in milliseconds. Once every so milliseconds, we will reset the counter in Client class that counts how many CLIENTSTATUS command have been received from a user
	final static float MAX_CLIENTSTATUSCHANGE_FREQUENCY = 5.0f; // max frequency of CLIENTSTATUS command (if we receive more than this times per second, we have a problem. We must still check MIN_CLIENTSTATUCCHANGE_COUNT_BEFORE_ALERT before taking actions, because a user might change status quickly (in few milliseconds) by accident (if spring crashes, for example)
	final static int MIN_CLIENTSTATUCCHANGE_COUNT_BEFORE_ALERT = 5;

	// as a key in this list we use a combination of "channame:username"
	static protected Map<String, SpamRecord> spamRecords;

	// spam settings for each individual channel:
	static protected Map<String, SpamSettings> spamSettings;

	static private Timer antiSpamTimer;

	// initializes the anti-spam system
	public static void initialize() {
		spamRecords = new HashMap<String, SpamRecord>();
		spamSettings = new HashMap<String, SpamSettings>();

		antiSpamTimer = new Timer();
		antiSpamTimer.schedule(new AntiSpamTask(),
				1000,  // initial delay
				1000); // subsequent rate
	}

	// stops the anti-spam system
	public static void uninitialize() {
		try {
			antiSpamTimer.cancel();
		} catch (Exception e) {
			// do nothing
		}
	}

	/* call this method when user says something in the channel using SAY or SAYEX command */
	public static void processUserMsg(String chan, String user, String msg) {
		synchronized (spamRecords) {
			String key = chan + ":" + user;
			SpamSettings settings = spamSettings.get(chan);
			if (settings == null) settings = SpamSettings.DEFAULT_SETTINGS;

			SpamRecord rec = spamRecords.get(key);
			if (rec == null) {
				rec = new SpamRecord();
				spamRecords.put(key, rec);
			}

			// determine severity:
			double severity = settings.normalMsgPenalty;
			if (msg.length() > settings.longMsgLength) severity += settings.longMsgPenalty;
			if (rec.lastMsg.equals(msg)) severity += settings.doubleMsgPenalty;

			rec.penaltyPoints += severity;
			rec.lastMsg = msg;

			// check if user has gathered too many penalty points:
			if (rec.penaltyPoints >= settings.penaltyLimit) {
				rec.penaltyPoints = 0; // reset counter
				muteUser(chan, user);
			}
		}
	}

	public static void processClientStatusChange(Client client) {
		if (System.currentTimeMillis() - client.clientStatusChangeCheckpoint > CHECK_CLIENTSTATUSCHANGE_INTERVAL) {
			// reset the counter:
			client.clientStatusChangeCheckpoint = System.currentTimeMillis();
			client.numClientStatusChanges = 0;
		}

		client.numClientStatusChanges++;

		if (((System.currentTimeMillis() - client.clientStatusChangeCheckpoint) * 1.0f / client.numClientStatusChanges) > MAX_CLIENTSTATUSCHANGE_FREQUENCY) {
			if (client.numClientStatusChanges > MIN_CLIENTSTATUCCHANGE_COUNT_BEFORE_ALERT) {
				// reset the counter:
				client.clientStatusChangeCheckpoint = System.currentTimeMillis();
				client.numClientStatusChanges = 0;

				// take action:
				ChanServ.sendLine("KICKUSER " + client.name + " CLIENTSTATUS command abuse - frequency too high");
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
