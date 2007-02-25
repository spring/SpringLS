/*
 * Created on 25.2.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Here are included all the routines needed with the anti-spam protection.
 * This system is separated from the rest of the bot code, however it does call
 * back the bot when it detects spamming.
 * 
 * Its current functionality is very basic - it is based on assigning penalty
 * points to users based on message length and message repetition. It is possible
 * to set penalty points for events per channel basis.
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.*;

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

class SpamSettings {
	public static final SpamSettings DEFAULT_SETTINGS = new SpamSettings(200, 1.0, 0.5, 0.5); 
	
	int longMsgLength; // length (in characters) of a message that is considered "long". For long messages, extra penalty points will be added
	double normalMsgPenalty; // this is penalty that is added each time user says something in the channel. Other penalties are added upon this one.
	double longMsgPenalty; // penalty value added if the message is longer than 'longMsgLength'
	double doubleMsgPenalty; // penalty value added if message is same as previous message
	
	public SpamSettings(int longMsgLength, double normalMsgPenalty, double longMsgPenalty, double doubleMsgPenalty) {
		this.longMsgLength = longMsgLength;
		this.normalMsgPenalty = normalMsgPenalty;
		this.longMsgPenalty = longMsgPenalty;
		this.doubleMsgPenalty = doubleMsgPenalty;
	}
	
	public SpamSettings() {
		// use the default settings:
		this(DEFAULT_SETTINGS.longMsgLength, DEFAULT_SETTINGS.normalMsgPenalty, DEFAULT_SETTINGS.longMsgPenalty, DEFAULT_SETTINGS.doubleMsgPenalty);
	}
	
	public String toString() {
		return longMsgLength + " " + normalMsgPenalty + " " + longMsgPenalty + " " + doubleMsgPenalty;
	}
	
	// this method is redundant. Use settings.toString() instead! 
	public static String spamSettingsToString(SpamSettings settings) {
		return settings.toString();
	}
	
	// returns null if settings string is malformed
	public static SpamSettings stringToSpamSettings(String settings) {
		SpamSettings ss = new SpamSettings();
		String[] parsed = settings.split(" ");
		if (parsed.length != 4) return null;
		try {
			ss.longMsgLength = Integer.parseInt(parsed[0]);
			ss.normalMsgPenalty = Double.parseDouble(parsed[1]);
			ss.longMsgPenalty = Double.parseDouble(parsed[2]);
			ss.doubleMsgPenalty = Double.parseDouble(parsed[3]);
		} catch (NumberFormatException e) {
			return null;
		}
		return ss;
	}
	
	/* returns true if settings string is valid */
	public static boolean validateSpamSettingsString(String settings) {
		return settings.matches("^\\d+ [0-9]*\\.?[0-9]+ [0-9]*\\.?[0-9]+ [0-9]*\\.?[0-9]+$");
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
                1000,        //initial delay
                1000);       //subsequent rate
	}
	
	// stops the anti-spam system
	public static void uninitialize() {
		try {
			antiSpamTimer.cancel();
		} catch (Exception e) {
			//
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
			if (rec.penaltyPoints >= 5.0) {
				rec.penaltyPoints = 0; // reset counter
				muteUser(chan, user);
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
