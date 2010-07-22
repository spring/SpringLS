/*
 * Created on 24.12.2005
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.text.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Statistics file format:
 * <time> <# of active clients> <# of active battles> <# of accounts> <# of active accounts> <list of mods>
 * where <time> is of form: "hhmmss"
 * and "active battles" are battles that are in-game and have 2 or more players in it
 * and <list of mods> is a list of first k mods (where k is 0 or greater) with frequencies
 * of active battles using these mods. Example: XTA 0.66 15. Note that delimiter in <list of mods>
 * is TAB and not SPACE! See code for more info.
 *
 * Aggregated statistics file format:
 * <date> <time> <# of active clients> <# of active battles> <# of accounts> <# of active accounts> <list of mods>
 * where <date> is of form: "ddMMyy"
 * and all other fields are of same format as those from normal statistics file.
 *
 * @author Betalord
 */
public class Statistics implements ContextReceiver {

	private final Log s_log  = LogFactory.getLog(Statistics.class);

	/** in milliseconds */
	private final long saveStatisticsInterval = 1000 * 60 * 20;
	private final String PLOTICUS_FULLPATH = "./ploticus/bin/pl"; // see http://ploticus.sourceforge.net/ for more info on ploticus
	private final String STATISTICS_FOLDER = "./stats/";

	/**
	 * Time when we last updated statistics.
	 * @see System.currentTimeMillis()
	 */
	private long lastStatisticsUpdate;

	private Context context = null;

	/**
	 * Whether statistics are recorded to disc on regular intervals or not.
	 */
	private boolean recording;


	public Statistics() {

		lastStatisticsUpdate = System.currentTimeMillis();
		recording = false;
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}


	/**
	 * This is called from the main game loop.
	 */
	public void update() {

		if (recording && (System.currentTimeMillis() - lastStatisticsUpdate > saveStatisticsInterval)) {
			saveStatisticsToDisk();
		}
	}

	private void ensureStatsDirExists() {

		// create statistics folder if it does not exist yet
		File file = new File(STATISTICS_FOLDER);
		if (!file.exists()) {
			boolean success = (file.mkdir());
			if (!success) {
				s_log.error(new StringBuilder("Unable to create folder: ").append(STATISTICS_FOLDER).toString());
			} else {
				s_log.info(new StringBuilder("Created missing folder: ").append(STATISTICS_FOLDER).toString());
			}
		}
	}

	/**
	 * Saves Statistics to permanent storage.
	 * @return -1 on error; otherwise: time (in milliseconds) it took
	 *         to save the statistics file
	 */
	public int saveStatisticsToDisk() {

		long taken;
		try {
			ensureStatsDirExists();

			lastStatisticsUpdate = System.currentTimeMillis();
			taken = autoUpdateStatisticsFile();
			if (taken == -1) {
				return -1;
			}
			createAggregateFile(); // to simplify parsing
			generatePloticusImages();
			s_log.info("*** Statistics saved to disk. Time taken: " + taken + " ms.");
		} catch (Exception e) {
			s_log.error("*** Failed saving statistics... Stack trace:", e);
			return -1;
		}
		return new Long(taken).intValue();
	}

	/**
	 * Will append statistics file (or create one if it doesn't exist)
	 * and add latest statistics to it.
	 * @return milliseconds taken to calculate statistics, or -1 if it fails
	 */
	private long autoUpdateStatisticsFile() {

		String fname = STATISTICS_FOLDER + now("ddMMyy") + ".dat";
		long startTime = System.currentTimeMillis();

		int activeBattlesCount = 0;
		for (int i = 0; i < context.getBattles().getBattlesSize(); i++) {
			if ((context.getBattles().getBattleByIndex(i).getClientsSize() >= 1 /* at least 1 client + founder == 2 players */) &&
					(context.getBattles().getBattleByIndex(i).inGame())) {
				activeBattlesCount++;
			}
		}

		String topMods = currentlyPopularModsList();

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fname, true));
			out.write(new StringBuilder(now("HHmmss")).append(" ")
					.append(context.getClients().getClientsSize()).append(" ")
					.append(activeBattlesCount).append(" ")
					.append(context.getAccountsService().getAccountsSize()).append(" ")
					.append(context.getAccountsService().getActiveAccountsSize()).append(" ")
					.append(topMods).append("\r\n").toString());
			out.close();
		} catch (IOException e) {
			s_log.error("Unable to access file <" + fname + ">. Skipping ...", e);
			return -1;
		}

		s_log.info("Statistics has been updated to disk ...");

		return System.currentTimeMillis() - startTime;
	}

	/**
	 * This will create "statistics.dat" file which will contain all records
	 * from the last 7 days
	 */
	private boolean createAggregateFile() {
		String fname = STATISTICS_FOLDER + "statistics.dat";

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fname, false)); // overwrite if it exists, or create new one
			String line;

			SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
			Date today = formatter.parse(now("ddMMyy"));
			// get file names for last 7 days (that is today + last 6 days):
			for (int i = 7; i > 0; i--) {
				Date temp = new Date();
				temp.setTime(today.getTime() - (i - 1) * 1000 * 60 * 60 * 24);
				try {
					BufferedReader in = new BufferedReader(new FileReader(STATISTICS_FOLDER + formatter.format(temp) + ".dat"));
					//***s_log.info("--- Found: <" + TASServer.STATISTICS_FOLDER + formatter.format(temp) + ".dat>");
					while ((line = in.readLine()) != null) {
						out.write(new StringBuilder(formatter.format(temp)).append(" ").append(line).append("\r\n").toString());
					}
				} catch (IOException e) {
					// just skip the file ...
					//***s_log.error("--- Skipped: <" + TASServer.STATISTICS_FOLDER + formatter.format(temp) + ".dat>", e);
				}
			}

			out.close();
		} catch (Exception e) {
			s_log.error("Unable to access file <" + fname + ">. Skipping ...", e);
			return false;
		}

		return true;
	}

	private boolean generatePloticusImages() {
		try {
			String cmd;
			String cmds[];
			SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
			SimpleDateFormat lastUpdateFormatter = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss (z)");
			Date endDate = formatter.parse(now("ddMMyy"));
			Date startDate = new Date();
			startDate.setTime(endDate.getTime() - 6 * 1000 * 60 * 60 * 24);
			long upTime = System.currentTimeMillis() - context.getServer().getStartTime();

			// generate "server stats diagram":
			cmds = new String[8];
			cmds[0] = PLOTICUS_FULLPATH;
			cmds[1] = "-png";
			cmds[2] = STATISTICS_FOLDER + "info.pl";
			cmds[3] = "-o";
			cmds[4] = STATISTICS_FOLDER + "info.png";
			cmds[5] = "lastupdate=" + lastUpdateFormatter.format(new Date());
			cmds[6] = "uptime=" + Misc.timeToDHM(upTime);
			cmds[7] = "clients=" + context.getClients().getClientsSize();
			Runtime.getRuntime().exec(cmds).waitFor();

			// generate "online clients diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ")
					.append(STATISTICS_FOLDER)
					.append("clients.pl -o ").append(STATISTICS_FOLDER)
					.append("clients.png startdate=").append(formatter.format(startDate))
					.append(" enddate=").append(formatter.format(endDate))
					.append(" datafile=").append(STATISTICS_FOLDER)
					.append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "active battles diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("battles.pl -o ").append(STATISTICS_FOLDER)
					.append("battles.png startdate=").append(formatter.format(startDate))
					.append(" enddate=").append(formatter.format(endDate))
					.append(" datafile=").append(STATISTICS_FOLDER).append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "accounts diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("accounts.pl -o ").append(STATISTICS_FOLDER)
					.append("accounts.png startdate=").append(formatter.format(startDate))
					.append(" enddate=").append(formatter.format(endDate))
					.append(" datafile=").append(STATISTICS_FOLDER).append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "popular mods chart":
			String[] params = getPopularModsList(now("ddMMyy")).split(("\t"));
			cmds = new String[params.length + 5];
			cmds[0] = PLOTICUS_FULLPATH;
			cmds[1] = "-png";
			cmds[2] = STATISTICS_FOLDER + "mods.pl";
			cmds[3] = "-o";
			cmds[4] = STATISTICS_FOLDER + "mods.png";
			cmds[5] = "count=" + Integer.parseInt(params[0]);
			for (int i = 1; i < params.length; i++) {
				if (i % 2 == 1) {
					cmds[i + 5] = "mod" + ((i + 1) / 2) + "=" + params[i];
				} else {
					cmds[i + 5] = "modfreq" + (i / 2) + "=" + params[i];
				}
			}
			Runtime.getRuntime().exec(cmds).waitFor();

		} catch (Exception e) {
			s_log.error("*** Failed generating ploticus charts!", e);
			return false;
		}

		return true;
	}

	/**
	 * Will return list of mods being played right now (top 5 mods only)
	 * with frequencies.
	 * format: [list-len] "modname1" [freq1] "modname2" [freq]" ...
	 * Where delimiter is TAB (not SPACE).
	 * An empty list is denoted by 0 value for list-len.
	 */
	private String currentlyPopularModsList() {

		List<String> mods = new ArrayList<String>();
		int[] freq = new int[0];

		for (int i = 0; i < context.getBattles().getBattlesSize(); i++) {
			if ((context.getBattles().getBattleByIndex(i).inGame()) && (context.getBattles().getBattleByIndex(i).getClientsSize() >= 1)) {
				// add to list or update in list:

				boolean found = false;
				for (int j = 0; j < mods.size(); j++) {
					if (mods.get(j).equals(context.getBattles().getBattleByIndex(i).getModName())) {
						// mod already in the list. Just increase it's frequency:
						freq[j]++;
						found = true;
						break;
					}
				}

				if (!found) {
					mods.add(context.getBattles().getBattleByIndex(i).getModName());
					freq = (int[]) Misc.resizeArray(freq, freq.length + 1);
					freq[freq.length - 1] = 1;
				}
			}
		}

		return createModPopularityString(mods, freq);
	}

	/**
	 * This will return popular mod list for a certain date (date must be on "ddMMyy"
	 * format). It will take first entry for every new hour and add it to the list
	 * (other entries for the same hour will be ignored).
	 * See comments for currentlyPopularModList() method for more info.
	 */
	private String getPopularModsList(String date) {

		List<String> mods = new ArrayList<String>();
		int[] freq = new int[0];
		boolean found = false;

		try {
			int lastHour = -1;
			String line;
			String sHour;
			BufferedReader in = new BufferedReader(new FileReader(STATISTICS_FOLDER + date + ".dat"));
			while ((line = in.readLine()) != null) {
				sHour = line.substring(0, 2); // 00 .. 23
				if (lastHour == Integer.parseInt(sHour)) {
					continue; // skip this line
				}
				lastHour = Integer.parseInt(sHour);
				String temp = Misc.makeSentence(line.split(" "), 5);
				String[] temp2 = temp.split("\t");
				if (temp2.length % 2 != 1) {
					throw new Exception("Bad mod list format"); // number of arguments must be odd
				}
				int noMods = Integer.parseInt(temp2[0]);
				if (temp2.length != noMods * 2 + 1) {
					throw new Exception("Bad mod list format");
				}
				for (int i = 0; i < noMods; i++) {
					found = false;
					for (int j = 0; j < mods.size(); j++) {
						if (mods.get(j).equals(temp2[1 + i * 2])) {
							// mod already in the list. Just increase it's frequency:
							freq[j] += Integer.parseInt(temp2[2 + i * 2]);
							found = true;
							break;
						}
					}
					if (!found) {
						mods.add(temp2[1 + i * 2]);
						freq = (int[]) Misc.resizeArray(freq, freq.length + 1);
						freq[freq.length - 1] = Integer.parseInt(temp2[2 + i * 2]);
					}
				}
			}
		} catch (Exception e) {
			s_log.error("*** Error in getPopularModsList(). Skipping ...", e);
			return "0";
		}

		return createModPopularityString(mods, freq);
	}
	private static String createModPopularityString(List<String> modNames, int[] numBattles) {

		// now generate a list of top 5 mods with frequencies:
		StringBuilder result = new StringBuilder(512);
		int numMods = Math.min(5, modNames.size()); // return 5 or less mods
		result.append(numMods);
		// Note: do not cut the array by numMods,
		//       or sorting will not have any effect!
		Misc.bubbleSort(numBattles, modNames);
		for (int m = 0; m < numMods; m++) {
			result.append("\t").append(modNames.get(m)).append("\t").append(numBattles[m]);
		}

		return result.toString();
	}

	/**
	 * Usage (some examples):
	 *
	 * System.out.println(now("dd MMMMM yyyy"));
	 * System.out.println(now("yyyyMMdd"));
	 * System.out.println(now("dd.MM.yy"));
	 * System.out.println(now("MM/dd/yy"));
	 * System.out.println(now("yyyy.MM.dd G 'at' hh:mm:ss z"));
	 * System.out.println(now("EEE, MMM d, ''yy"));
	 * System.out.println(now("h:mm a"));
	 * System.out.println(now("H:mm:ss:SSS"));
	 * System.out.println(now("K:mm a,z"));
	 * System.out.println(now("yyyy.MMMMM.dd GGG hh:mm aaa"));
	 *
	 * Taken from http://www.rgagnon.com/javadetails/java-0106.html.
	 *
	 * Also see http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html
	 * for more info on SimpleDateFormat.
	 */
	private static String now(String format) {

		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		String current = formatter.format(today);

		return current;
	}

	/**
	 * Whether statistics are recorded or not.
	 * @return the recording
	 */
	public boolean isRecording() {
		return recording;
	}

	/**
	 * Whether statistics are recorded or not.
	 * @param recording the recording to set
	 */
	public void setRecording(boolean recording) {
		this.recording = recording;
	}
}
