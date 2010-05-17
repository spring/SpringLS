/*
 * Created on 2006.10.29
 */

package com.springrts.tasserver;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This thread will download IP2Country database (csv) files from two different
 * URL locations, unzip them, and then read and merge them together and create
 * local IP2Country file from which it will update current IP2Country info
 * (from this file server will read IP2Country info each time it is restarted).
 *
 * Two different IP2Country databases are used because none of the two is perfect
 * (for example one doesn't cover ISPs located in Africa too well, the other does
 * but is smaller in general) but combined together they mostly cover all known
 * IP addresses.
 *
 * Note that when building IP2Country info, it will, for a short time, allocate
 * quite large memory blocks (several tens of MB), so when figuring out how much
 * memory the server needs to run smoothly (using -Xms -Xmx java switches),
 * count that in as well.
 *
 * @author Betalord
 */
public class UpdateIP2CountryThread implements Runnable, ContextReceiver {

	private final Log s_log  = LogFactory.getLog(UpdateIP2CountryThread.class);

	private boolean inProgress; // true if updating is already in progress
	private final int DOWNLOAD_LIMIT = 1024 * 128; // in bytes per second. This is the maximum speed at which this thread will attempt to download files from the internet

	private Context context = null;

	private IP2Country ip2Country = null;


	public UpdateIP2CountryThread(IP2Country ip2Country) {
		this.ip2Country = ip2Country;
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	public synchronized boolean inProgress() {
		return inProgress;
	}

	@Override
	public void run() {

		String tempZippedFile1 = "./temp_ip2country_1z.dat";
		String tempZippedFile2 = "./temp_ip2country_2z.dat";
		String tempUnzippedFile1 = "./temp_ip2country_1u.dat";
		String tempUnzippedFile2 = "./temp_ip2country_2u.dat";
		String tempIP2CountryFile = "./temp_ip2country.dat"; // here we store converted IP2Country file (we still have to clean it up though)

		PrintWriter out = null;
		BufferedReader in = null;
		String line = null;

		try {
			if (inProgress) {
				throw new Exception("Update already in progress");
			}
			inProgress = true;

			long time1; // time taken to download 1st ip2country archive
			long time2; // time taken to download 2nd ip2country archive
			long time3; // time taken to decompress 1st ip2country acrhive
			long time4; // time taken to decompress 2nd ip2country acrhive
			long time5; // time taken to merge both files and write result to new file
			long time6; // time taken to build IP2Country database from the merged file, clean it up and save it back to disk
			long bytes1; // byted downloaded from 1st URL
			long bytes2; // bytes downloaded from 2nd URL

			time1 = System.currentTimeMillis();
			bytes1 = Misc.download("http://ip-to-country.webhosting.info/downloads/ip-to-country.csv.zip", tempZippedFile1, DOWNLOAD_LIMIT);
			time1 = System.currentTimeMillis() - time1;

			time2 = System.currentTimeMillis();
			bytes2 = Misc.download("http://software77.net/cgi-bin/ip-country/geo-ip.pl?action=downloadZ", tempZippedFile2, DOWNLOAD_LIMIT);
			time2 = System.currentTimeMillis() - time2;

			time3 = System.currentTimeMillis();
			Misc.unzipSingleArchive(tempZippedFile1, tempUnzippedFile1);
			time3 = System.currentTimeMillis() - time3;

			time4 = System.currentTimeMillis();
			Misc.unzipSingleArchive(tempZippedFile2, tempUnzippedFile2);
			time4 = System.currentTimeMillis() - time4;

			time5 = System.currentTimeMillis();

			// we will now merge both databases into a single file. Later on we will remove all duplicate entries and any inconsistencies
			out = new PrintWriter(new BufferedWriter(new FileWriter(tempIP2CountryFile)));

			// read first file:
			in = new BufferedReader(new FileReader(tempUnzippedFile1));

			while ((line = in.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				if (line.charAt(0) == '#') {
					continue;
				}
				String[] tokens = line.split(",");

				out.println(
						tokens[0].substring(1, tokens[0].length() - 1) + "," + // IP FROM field
						tokens[1].substring(1, tokens[1].length() - 1) + "," + // IP TO field
						tokens[2].substring(1, tokens[2].length() - 1) // COUNTRY_CHAR2 field
						);
			}

			in.close();

			// read second file:
			in = new BufferedReader(new FileReader(tempUnzippedFile2));

			while ((line = in.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				if (line.charAt(0) == '#') {
					continue;
				}
				String[] tokens = line.split(",");

				out.println(
						tokens[0].substring(1, tokens[0].length() - 1) + "," + // IP FROM field
						tokens[1].substring(1, tokens[1].length() - 1) + "," + // IP TO field
						tokens[4].substring(1, tokens[4].length() - 1) // COUNTRY_CHAR2 field
						);
			}

			in.close();
			out.close();

			time5 = System.currentTimeMillis() - time5;

			time6 = System.currentTimeMillis();
			TreeMap<IPRange, IPRange> iptable = new TreeMap<IPRange, IPRange>();
			TreeSet<String> countries = new TreeSet<String>();
			ip2Country.buildDatabaseSafe(tempIP2CountryFile, iptable, countries);
			ip2Country.assignDatabase(iptable, countries);
			ip2Country.saveDatabase(iptable, TASServer.IP2COUNTRY_FILENAME);
			time6 = System.currentTimeMillis() - time6;

			// add notification:
			ServerNotification sn = new ServerNotification("IP2Country database updated");
			sn.addLine("IP2Country database has just been successfully updated from these addresses:");
			sn.addLine("");
			sn.addLine("- http://ip-to-country.webhosting.info/downloads/ip-to-country.csv.zip");
			sn.addLine("- http://software77.net/cgi-bin/ip-country/geo-ip.pl?action=downloadZ");
			sn.addLine("");
			sn.addLine("Statistics:");
			sn.addLine(bytes1 / 1024 + " KB - size of 1st IP2Country archive file");
			sn.addLine(bytes2 / 1024 + " KB - size of 2nd IP2Country archive file");
			sn.addLine(time1 + " ms - time taken to download 1st IP2Country archive file");
			sn.addLine(time2 + " ms - time taken to download 2nd IP2Country archive file");
			sn.addLine(time3 + " ms - time taken to decompress 1st IP2Country archive file");
			sn.addLine(time4 + " ms - time taken to decompress 2nd IP2Country archive file");
			sn.addLine(time5 + " ms - time taken to merge both files and write result to new file");
			sn.addLine(time6 + " ms - time taken to build IP2Country database from the merged file, clean it up and save it back to disk");
			sn.addLine(countries.size() + " countries mentioned in merged IP2Country database");
			context.getServerNotifications().addNotification(sn);

			s_log.info("IP2Country has just been successfully updated.");
		} catch (Exception e) {
			ServerNotification sn = new ServerNotification("Unable to update IP2Country database");
			sn.addLine("Attempt to update from online IP2Country database failed. Stack trace: ");
			sn.addLine(Misc.exceptionToFullString(e));
			context.getServerNotifications().addNotification(sn);
		} finally {
			inProgress = false;

			// clean up:
			Misc.deleteFile(tempZippedFile1);
			Misc.deleteFile(tempUnzippedFile1);
			Misc.deleteFile(tempZippedFile2);
			Misc.deleteFile(tempUnzippedFile2);
			Misc.deleteFile(tempIP2CountryFile);
		}

	}
}
