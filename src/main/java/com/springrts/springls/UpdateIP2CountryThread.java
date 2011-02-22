/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls;


import com.springrts.springls.util.Misc;
import com.springrts.springls.util.ZipUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread will download IP2Country database (CSV) files from two different
 * URL locations, unzip them, and then read and merge them together and create
 * local IP2Country file from which it will update current IP2Country info
 * (from this file server will read IP2Country info each time it is restarted).
 *
 * Two different IP2Country databases are used because none of the two is
 * perfect. For example, one does not cover ISPs located in Africa too well,
 * while the other does, but is smaller in general. Combined though, they mostly
 * cover all known IP addresses.
 *
 * Note that when building IP2Country info, it will, for a short time, allocate
 * quite large memory blocks (several tens of MB), so when figuring out how much
 * memory the server needs to run smoothly (using -Xms -Xmx java switches),
 * count that in as well.
 *
 * @author Betalord
 */
public class UpdateIP2CountryThread implements Runnable, ContextReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateIP2CountryThread.class);

	/** true if updating is already in progress */
	private AtomicBoolean inProgress;
	/**
	 * This is the maximum speed at which this thread will attempt
	 * to download files from the Internet in bytes per second.
	 */
	private static final int DOWNLOAD_LIMIT = 1024 * 128;

	private Context context = null;

	private IP2Country ip2Country = null;


	public UpdateIP2CountryThread(IP2Country ip2Country) {

		this.inProgress = new AtomicBoolean(false);
		this.ip2Country = ip2Country;
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	public synchronized boolean inProgress() {
		return inProgress.get();
	}

	/**
	 * Removes quotes without check.
	 * @param input the string to remove quotes from
	 * @return the input string with the first and the last char removed
	 */
	private static String stripQuotes(String input) {
		return input.substring(1, input.length() - 1);
	}

	@Override
	public void run() {

		File combinedData = null;
		FileWriter combinedDataFileOut = null;
		PrintWriter combinedDataOut = null;

		try {
			if (inProgress.compareAndSet(false, true)) {
				throw new IOException("Update already in progress");
			}

			List<URL> sourceUrls = new ArrayList<URL>(2);
			sourceUrls.add(new URL("http://ip-to-country.webhosting.info/downloads/ip-to-country.csv.zip"));
			sourceUrls.add(new URL("http://software77.net/cgi-bin/ip-country/geo-ip.pl?action=downloadZ"));

			ServerNotification sn = new ServerNotification("IP2Country database updated");
			sn.addLine("IP2Country database has just been successfully updated from:");
			sn.addLine("");

			combinedData = File.createTempFile("ip2country_combined", ".dat");
			combinedDataFileOut = new FileWriter(combinedData);
			combinedDataOut = new PrintWriter(new BufferedWriter(combinedDataFileOut));

			downloadAndCombineRawSources(sourceUrls, combinedDataOut, sn);

			long timeBuildDB = System.currentTimeMillis();
			TreeMap<IPRange, IPRange> ipTable = new TreeMap<IPRange, IPRange>();
			TreeSet<String> countries = new TreeSet<String>();
			ip2Country.buildDatabaseSafe(combinedData.getAbsolutePath(), ipTable, countries);
			ip2Country.assignDatabase(ipTable, countries);
			ip2Country.saveDatabase(ipTable, ip2Country.getDataFile().getAbsolutePath());
			timeBuildDB = System.currentTimeMillis() - timeBuildDB;

			// add notification
			sn.addLine("");
			sn.addLine(timeBuildDB + " ms - time taken to build IP2Country database from the merged file, clean it up and save it back to disk");
			sn.addLine(countries.size() + " countries mentioned in merged IP2Country database");
			context.getServerNotifications().addNotification(sn);

			LOG.info("IP2Country has just been successfully updated.");
		} catch (IOException ex) {
			ServerNotification sn = new ServerNotification("Unable to update IP2Country database");
			sn.addLine("Attempt to update from online IP2Country database failed. Stack trace:");
			sn.addException(ex);
			context.getServerNotifications().addNotification(sn);
		} finally {
			inProgress.set(false);

			// clean up
			if (combinedDataOut != null) {
				combinedDataOut.close();
			} else if (combinedDataFileOut != null) {
				try {
					combinedDataFileOut.close();
				} catch (IOException ex) {
					LOG.warn("Failed to close stream to the temporary IP2Country data file " + combinedData.getAbsolutePath(), ex);
				}
			}
			if ((combinedData != null) && combinedData.exists()) {
				boolean deleted = combinedData.delete();
				if (!deleted) {
					LOG.warn("Failed to delete the temporary IP2Country data file " + combinedData.getAbsolutePath());
				}
			}
		}
	}

	private static void downloadAndCombineRawSources(List<URL> sourceUrls,
			PrintWriter combinedDataOut, ServerNotification sn)
			throws IOException
	{
		for (URL url : sourceUrls) {
			File sourceArchive = null;
			File sourceRaw = null;
			FileReader sourceRawFileIn = null;
			BufferedReader sourceRawIn = null;

			try {
				// download
				long timeDownload = System.currentTimeMillis();
				sourceArchive = File.createTempFile("ip2country_source", ".dat.zip");
				long bytesDownloaded = Misc.download(url.toString(), sourceArchive.getAbsolutePath(), DOWNLOAD_LIMIT);
				timeDownload = System.currentTimeMillis() - timeDownload;

				// extract
				long timeExtract = System.currentTimeMillis();
				sourceRaw = File.createTempFile("ip2country_source", ".dat");
				ZipUtil.unzipSingleFile(sourceArchive, sourceRaw);
				timeExtract = System.currentTimeMillis() - timeExtract;

				// write to combined data file
				// duplicate entries and any inconsistencies will be removed
				// later
				long timeCombine = System.currentTimeMillis();
				sourceRawFileIn = new FileReader(sourceRaw.getAbsolutePath());
				sourceRawIn = new BufferedReader(sourceRawFileIn);

				String inLine;
				while ((inLine = sourceRawIn.readLine()) != null) {
					inLine = inLine.trim();
					if (inLine.equals("") || inLine.charAt(0) == '#') {
						continue;
					}

					String[] tokens = inLine.split(",");
					String outLine = String.format("%s,%s,%s",
							stripQuotes(tokens[0]), // IP FROM field
							stripQuotes(tokens[1]), // IP TO field
							stripQuotes(tokens[2])  // COUNTRY_CHAR2 field
							);

					combinedDataOut.println(outLine);
				}
				timeCombine = System.currentTimeMillis() - timeCombine;

				sn.addLine("- " + url.toString());
				sn.addLine("  Statistics:");
				sn.addLine("  * downloaded: " + (bytesDownloaded / 1024) + " KB");
				sn.addLine("  * time taken to download: " + timeDownload + " ms");
				sn.addLine("  * time taken to decompress: " + timeExtract + " ms");
				sn.addLine("  * time taken to re-write: " + timeCombine + " ms");
				sn.addLine("");
			} catch (IOException ex) {
				throw new IOException("Failed to download & extract IP2Country data from " + url.toString(), ex);
			} finally {
				// clean up
				if (sourceRawIn != null) {
					sourceRawIn.close();
				} else if (sourceRawFileIn != null) {
					sourceRawFileIn.close();
				}
				if (combinedDataOut != null) {
					combinedDataOut.close();
				}
				if ((sourceArchive != null) && sourceArchive.exists()) {
					boolean deleted = sourceArchive.delete();
					if (!deleted) {
						throw new IOException("Failed to delete the local archive after an incomplete download: " + sourceArchive.getAbsolutePath());
					}
				}
				if ((sourceRaw != null) && sourceRaw.exists()) {
					boolean deleted = sourceRaw.delete();
					if (!deleted) {
						throw new IOException("Failed to delete the local file after an incomplete extraction: " + sourceRaw.getAbsolutePath());
					}
				}
			}
		}
	}
}
