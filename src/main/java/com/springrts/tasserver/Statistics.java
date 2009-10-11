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
public class Statistics {

	private static final Log s_log  = LogFactory.getLog(Statistics.class);

	public static long lastStatisticsUpdate = System.currentTimeMillis(); // time (System.currentTimeMillis()) when we last updated statistics

	/**
	 * Saves Statistics to permanent storage.
	 * @return -1 on error; otherwise: time (in milliseconds) it took
	 *         to save the statistics file
	 */
	public static int saveStatisticsToDisk() {
		long taken;
		try {
			lastStatisticsUpdate = System.currentTimeMillis();
			taken = Statistics.autoUpdateStatisticsFile();
			if (taken == -1) {
				return -1;
			}
			Statistics.createAggregateFile(); // to simplify parsing
			Statistics.generatePloticusImages();
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
	private static long autoUpdateStatisticsFile() {

		String fname = TASServer.STATISTICS_FOLDER + now("ddMMyy") + ".dat";
		long startTime = System.currentTimeMillis();

		int activeBattlesCount = 0;
		for (int i = 0; i < Battles.getBattlesSize(); i++) {
			if ((Battles.getBattleByIndex(i).getClientsSize() >= 1 /* at least 1 client + founder == 2 players */) &&
					(Battles.getBattleByIndex(i).inGame())) {
				activeBattlesCount++;
			}
		}

		int activeAccounts = 0;
		for (int i = 0; i < Accounts.getAccountsSize(); i++) {
			if ((Accounts.getAccount(i).getInGameTime() > 300) &&
					(Accounts.getAccount(i).getLastLogin() > System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7)) {
				activeAccounts++;
			}
		}

		String topMods = currentlyPopularModsList();

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fname, true));
			out.write(new StringBuilder(now("HHmmss")).append(" ")
					.append(Clients.getClientsSize()).append(" ")
					.append(activeBattlesCount).append(" ")
					.append(Accounts.getAccountsSize()).append(" ")
					.append(activeAccounts).append(" ")
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
	private static boolean createAggregateFile() {
		String fname = TASServer.STATISTICS_FOLDER + "statistics.dat";

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
					BufferedReader in = new BufferedReader(new FileReader(TASServer.STATISTICS_FOLDER + formatter.format(temp) + ".dat"));
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

	private static boolean generatePloticusImages() {
		try {
			String cmd;
			String cmds[];
			SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
			SimpleDateFormat lastUpdateFormatter = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss (z)");
			Date endDate = formatter.parse(now("ddMMyy"));
			Date startDate = new Date();
			startDate.setTime(endDate.getTime() - 6 * 1000 * 60 * 60 * 24);

			// generate "server stats diagram":
			cmds = new String[8];
			cmds[0] = TASServer.PLOTICUS_FULLPATH;
			cmds[1] = "-png";
			cmds[2] = TASServer.STATISTICS_FOLDER + "info.pl";
			cmds[3] = "-o";
			cmds[4] = TASServer.STATISTICS_FOLDER + "info.png";
			cmds[5] = "lastupdate=" + lastUpdateFormatter.format(new Date());
			cmds[6] = "uptime=" + Misc.timeToDHM(System.currentTimeMillis() - TASServer.upTime);
			cmds[7] = "clients=" + Clients.getClientsSize();
			Runtime.getRuntime().exec(cmds).waitFor();

			// generate "online clients diagram":
			cmd = new StringBuilder(TASServer.PLOTICUS_FULLPATH)
					.append(" -png ")
					.append(TASServer.STATISTICS_FOLDER)
					.append("clients.pl -o ").append(TASServer.STATISTICS_FOLDER)
					.append("clients.png startdate=").append(formatter.format(startDate))
					.append(" enddate=").append(formatter.format(endDate))
					.append(" datafile=").append(TASServer.STATISTICS_FOLDER)
					.append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "active battles diagram":
			cmd = new StringBuilder(TASServer.PLOTICUS_FULLPATH)
					.append(" -png ").append(TASServer.STATISTICS_FOLDER)
					.append("battles.pl -o ").append(TASServer.STATISTICS_FOLDER)
					.append("battles.png startdate=").append(formatter.format(startDate))
					.append(" enddate=").append(formatter.format(endDate))
					.append(" datafile=").append(TASServer.STATISTICS_FOLDER).append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "accounts diagram":
			cmd = new StringBuilder(TASServer.PLOTICUS_FULLPATH)
					.append(" -png ").append(TASServer.STATISTICS_FOLDER)
					.append("accounts.pl -o ").append(TASServer.STATISTICS_FOLDER)
					.append("accounts.png startdate=").append(formatter.format(startDate))
					.append(" enddate=").append(formatter.format(endDate))
					.append(" datafile=").append(TASServer.STATISTICS_FOLDER).append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "popular mods chart":
			String[] params = getPopularModsList(now("ddMMyy")).split(("\t"));
			cmds = new String[params.length + 5];
			cmds[0] = TASServer.PLOTICUS_FULLPATH;
			cmds[1] = "-png";
			cmds[2] = TASServer.STATISTICS_FOLDER + "mods.pl";
			cmds[3] = "-o";
			cmds[4] = TASServer.STATISTICS_FOLDER + "mods.png";
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
	 * Will return list of mods being played right now (top 5 mods only) with frequencies.
	 * format: listlen "modname1" freq1 "modname2" freq" ...
	 * where delimiter is TAB (not SPACE). An empty list is denoted by 0 value for listlen.
	 */
	private static String currentlyPopularModsList() {

		List<String> mods = new ArrayList<String>();
		int[] freq = new int[0];
		boolean found = false;

		for (int i = 0; i < Battles.getBattlesSize(); i++) {
			if ((Battles.getBattleByIndex(i).inGame()) && (Battles.getBattleByIndex(i).getClientsSize() >= 1)) {
				// add to list or update in list:

				found = false;
				for (int j = 0; j < mods.size(); j++) {
					if (mods.get(j).equals(Battles.getBattleByIndex(i).modName)) {
						// mod already in the list. Just increase it's frequency:
						freq[j]++;
						found = true;
						break;
					}
				}

				if (!found) {
					mods.add(new String(Battles.getBattleByIndex(i).modName));
					freq = (int[]) Misc.resizeArray(freq, freq.length + 1);
					freq[freq.length - 1] = 1;
				}
			}
		}

		// now generate a list of top 5 mods with frequencies:
		if (mods.size() == 0) {
			return "0";
		}
		String result = "" + Math.min(5, mods.size()); // return 5 or less mods
		Misc.bubbleSort(freq, mods);
		for (int i = 0; i < Math.min(5, mods.size()); i++) {
			result = result + "\t" + mods.get(i) + "\t" + freq[i];
		}
		return result;
	}

	/**
	 * This will return popular mod list for a certain date (date must be on "ddMMyy"
	 * format). It will take first entry for every new hour and add it to the list
	 * (other entries for the same hour will be ignored).
	 * See comments for currentlyPopularModList() method for more info.
	 */
	private static String getPopularModsList(String date) {

		List<String> mods = new ArrayList<String>();
		int[] freq = new int[0];
		boolean found = false;

		try {
			int lastHour = -1;
			String line;
			String sHour;
			BufferedReader in = new BufferedReader(new FileReader(TASServer.STATISTICS_FOLDER + date + ".dat"));
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
						mods.add(new String(temp2[1 + i * 2]));
						freq = (int[]) Misc.resizeArray(freq, freq.length + 1);
						freq[freq.length - 1] = Integer.parseInt(temp2[2 + i * 2]);
					}
				}
			}
		} catch (Exception e) {
			s_log.error("*** Error in getPopularModsList(). Skipping ...", e);
			return "0";
		}

		// now generate a list of top 5 mods with frequencies:
		if (mods.size() == 0) {
			return "0";
		}
		final int k = Math.min(5, mods.size()); // return 5 or less mods
		StringBuilder result = new StringBuilder().append(k);
		Misc.bubbleSort(freq, mods); // Note: don't cut the array by k, or sorting won't have any effect!
		for (int i = 0; i < k; i++) {
			result.append("\t").append(mods.get(i)).append("\t").append(freq[i]);
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
}
