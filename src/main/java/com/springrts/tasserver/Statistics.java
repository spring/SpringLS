/*
 * Created on 24.12.2005
 */

package com.springrts.tasserver;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics file format:
 * <time> <# of active clients> <# of active battles> <# of accounts> <# of active accounts> <list of mods>
 * where <time> is of form: "hhmmss"
 * and "active battles" are battles that are in-game and have 2 or more players
 * in it and <list of mods> is a list of first k mods (where k is 0 or greater)
 * with frequencies of active battles using these mods. Example: XTA 0.66 15.
 * Note that delimiter in <list of mods> is TAB and not SPACE! See code for more
 * info.
 *
 * Aggregated statistics file format:
 * <date> <time> <# of active clients> <# of active battles> <# of accounts> <# of active accounts> <list of mods>
 * where <date> is of form: "ddMMyy"
 * and all other fields are of same format as those from normal statistics file.
 *
 * @author Betalord
 */
public class Statistics implements ContextReceiver {

	private static final Logger LOG  = LoggerFactory.getLogger(Statistics.class);

	/** in milliseconds */
	private final long saveStatisticsInterval = 1000 * 60 * 20;
	/**
	 * See the <a href="http://ploticus.sourceforge.net/">Ploticus page</a>
	 * for more info.
	 */
	private static final String PLOTICUS_FULLPATH = "./ploticus/bin/pl";
	private static final String STATISTICS_FOLDER = "./stats/";

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

		if (recording && ((System.currentTimeMillis() - lastStatisticsUpdate) > saveStatisticsInterval)) {
			saveStatisticsToDisk();
		}
	}

	private void ensureStatsDirExists() {

		// create statistics folder if it does not exist yet
		File file = new File(STATISTICS_FOLDER);
		if (!file.exists()) {
			boolean success = (file.mkdir());
			if (!success) {
				LOG.error("Unable to create folder: {}", STATISTICS_FOLDER);
			} else {
				LOG.info("Created missing folder: {}", STATISTICS_FOLDER);
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
			if (taken != -1) {
				createAggregateFile(); // to simplify parsing
				generatePloticusImages();
				LOG.info("*** Statistics saved to disk. Time taken: {} ms.",
						taken);
			}
		} catch (Exception ex) {
			LOG.error("*** Failed saving statistics... Stack trace:", ex);
			taken = -1;
		}

		return (int) taken;
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
			if ((context.getBattles().getBattleByIndex(i).getClientsSize() >= 1)
					// at least 1 client + founder == 2 players
					&& (context.getBattles().getBattleByIndex(i).inGame()))
			{
				activeBattlesCount++;
			}
		}

		String topMods = currentlyPopularModsList();

		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fname, true));
			out.write(new StringBuilder(now("HHmmss")).append(" ")
					.append(context.getClients().getClientsSize()).append(" ")
					.append(activeBattlesCount).append(" ")
					.append(context.getAccountsService().getAccountsSize()).append(" ")
					.append(context.getAccountsService().getActiveAccountsSize()).append(" ")
					.append(topMods).append("\r\n").toString());
		} catch (IOException ex) {
			LOG.error("Unable to access file <" + fname + ">. Skipping ...", ex);
			return -1;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ex) {
					LOG.trace("Failed closing file-writer for file: " + fname, ex);
				}
			}
		}

		LOG.info("Statistics has been updated to disk ...");

		return System.currentTimeMillis() - startTime;
	}

	/**
	 * This will create "statistics.dat" file which will contain all records
	 * from the last 7 days.
	 */
	private boolean createAggregateFile() {

		String fileName = STATISTICS_FOLDER + "statistics.dat";

		try {
			// overwrite if it exists, or create new one
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName, false));
			String line;

			SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
			Date today = today();
			long msPerDay = 1000 * 60 * 60 * 24;
			// get file names for last 7 days (that is today + previous 6 days)
			for (int i = 7; i > 0; i--) {
				Date day = new Date();
				day.setTime(today.getTime() - (((long) i - 1) * msPerDay));
				String dayStr = formatter.format(day);
				String fileNameDay = STATISTICS_FOLDER + formatter.format(dayStr) + ".dat";
				BufferedReader in = null;
				try {
					in = new BufferedReader(new FileReader(fileNameDay));
					//***LOG.info("--- Found: <{}>", fileNameDay);
					while ((line = in.readLine()) != null) {
						out.write(String.format("%s %s\r\n", dayStr, line));
					}
				} catch (IOException ex) {
					// just skip the file ...
					//***LOG.error("--- Skipped: <" + fileNameDay + ">", ex);
				} finally {
					if (in != null) {
						in.close();
					}
				}
			}

			out.close();
		} catch (Exception ex) {
			LOG.error("Unable to access file <" + fileName + ">. Skipping ...", ex);
			return false;
		}

		return true;
	}

	private boolean generatePloticusImages() {

		boolean ret = false;

		try {
			String cmd;
			String[] cmds;

			SimpleDateFormat dayFormatter = new SimpleDateFormat("ddMMyy");
			Date endDate = today(); // today_00:00
			Date startDate = new Date(endDate.getTime() - ((long) 6 * (1000 * 60 * 60 * 24))); // today_00:00 - 6 days
			String startDateString = dayFormatter.format(startDate);
			String endDateString = dayFormatter.format(endDate);

			SimpleDateFormat lastUpdateFormatter = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss (z)");

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
					.append("clients.png startdate=").append(startDateString)
					.append(" enddate=").append(endDateString)
					.append(" datafile=").append(STATISTICS_FOLDER)
					.append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "active battles diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("battles.pl -o ").append(STATISTICS_FOLDER)
					.append("battles.png startdate=").append(startDateString)
					.append(" enddate=").append(endDateString)
					.append(" datafile=").append(STATISTICS_FOLDER).append("statistics.dat").toString();
			Runtime.getRuntime().exec(cmd).waitFor();

			// generate "accounts diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("accounts.pl -o ").append(STATISTICS_FOLDER)
					.append("accounts.png startdate=").append(startDateString)
					.append(" enddate=").append(endDateString)
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
				if ((i % 2) != 0) {
					// odd index
					cmds[i + 5] = "mod" + ((i + 1) / 2) + "=" + params[i];
				} else {
					// even index
					cmds[i + 5] = "modfreq" + (i / 2) + "=" + params[i];
				}
			}
			Runtime.getRuntime().exec(cmds).waitFor();

			ret = true;
		} catch (InterruptedException ex) {
			LOG.error("*** Failed generating ploticus charts!", ex);
			ret = false;
		} catch (IOException ex) {
			LOG.error("*** Failed generating ploticus charts!", ex);
			ret = false;
		}

		return ret;
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

		BufferedReader in = null;
		try {
			int lastHour = -1;
			String line;
			String sHour;
			in = new BufferedReader(new FileReader(STATISTICS_FOLDER + date + ".dat"));
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
		} catch (Exception ex) {
			LOG.error("*** Error in getPopularModsList(). Skipping ...", ex);
			return "0";
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					// ignore
				}
			}
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
	 * @param format examples:
	 *   "dd MMMMM yyyy"
	 *   "yyyyMMdd"
	 *   "dd.MM.yy"
	 *   "MM/dd/yy"
	 *   "yyyy.MM.dd G 'at' hh:mm:ss z"
	 *   "EEE, MMM d, ''yy"
	 *   "h:mm a"
	 *   "H:mm:ss:SSS"
	 *   "K:mm a,z"
	 *   "yyyy.MMMMM.dd GGG hh:mm aaa"
	 *
	 * Taken from <a href="http://www.rgagnon.com/javadetails/java-0106.html">
	 * here</a>.
	 *
	 * Also see
	 * <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html">
	 * SimpleDateFormat JavaDoc</a> for more info.
	 */
	private static String now(String format) {

		Date now = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		String current = formatter.format(now);

		return current;
	}

	private static final String TODAY_FILTER_FORMAT = "ddMMyy";
	private static Date today() {

		Date today = null;

		try {
			today = new SimpleDateFormat(TODAY_FILTER_FORMAT).parse(now(TODAY_FILTER_FORMAT));
		} catch (ParseException ex) {
			// should not ever happen!
		}

		return today;
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
