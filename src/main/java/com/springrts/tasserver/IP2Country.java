/*
 * Created on 2005.9.1
 */

package com.springrts.tasserver;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 */
public class IP2Country {

	private static final Logger LOG = LoggerFactory.getLogger(IP2Country.class);
	private static final String DEFAULT_DATA_FILE = "ip2country.dat";

	public static final String COUNTRY_UNKNOWN = "XX";

	private TreeSet<String> countries;
	private TreeMap<IPRange, IPRange> resolveTable;

	private UpdateIP2CountryThread updater;
	private Thread updateThread;

	private File dataFile;


	private static IP2Country singleton = new IP2Country();

	public static IP2Country getInstance() {
		return singleton;
	}

	public IP2Country() {

		countries = new TreeSet<String>();
		resolveTable = new TreeMap<IPRange, IPRange>();
		updater = null;
		updateThread = null;
		dataFile = new File(DEFAULT_DATA_FILE);
	}


	public File getDataFile() {
		return dataFile;
	}

	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}

	public boolean updateInProgress() {
		return ((updater != null) && updater.inProgress());
	}

	public boolean initializeAll() {

		try {
			buildDatabase(dataFile, resolveTable, countries);
			LOG.info("Using IP2Country info from file '{}'.",
					dataFile.getAbsolutePath());
		} catch (IOException ex) {
			LOG.warn("Could not find or read from file '{}'. Not using any"
					+ " IP2Country info.", dataFile.getAbsolutePath());
			LOG.debug("... reason:", ex);
			return false;
		}

		return true;
	}

	/**
	 * Will download appropriate IP2COUNTRY database from the Internet
	 * and update local database. Returns immediately (updating is done
	 * in a background thread).
	 */
	public void updateDatabase() {

		if (updateInProgress()) {
			// update already in progress. Let's just skip it ...
			return;
		}

		updater = new UpdateIP2CountryThread(this);
		updateThread = new Thread(updater);
		updateThread.setPriority(Thread.NORM_PRIORITY - 2);
		updateThread.start();
	}

	/**
	 * Will build IP2Country database from a custom format IP2Country file which
	 * is produced by the UpdateIP2CountryThread class.
	 * Results are saved into 'resolveTable' and 'countries' objects.
	 */
	public void buildDatabase(File from, TreeMap<IPRange, IPRange> resolveTable,
			TreeSet<String> countries) throws IOException
	{
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(from));

			countries.clear();
			resolveTable.clear();

			String line;
			String[] tokens;

			while ((line = in.readLine()) != null) {
				tokens = line.split(",");
				IPRange ip = new IPRange(Long.parseLong(tokens[0]),
						Long.parseLong(tokens[1]), tokens[2]);
				resolveTable.put(ip, ip);
				countries.add(tokens[2]);
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Same as buildDatabase() except that it uses several check for consistency
	 * and automatic merging / filtering of duplicate entries,
	 * which makes it much slower.
	 * Should be called only from a separate thread when updating the database.
	 */
	public void buildDatabaseSafe(String fromFile,
			TreeMap<IPRange, IPRange> resolveTable, TreeSet<String> countries)
			throws IOException
	{
		Reader inF = null;
		BufferedReader in = null;
		try {
			inF = new FileReader(fromFile);
			in = new BufferedReader(inF);

			countries.clear();
			resolveTable.clear();

			String line;
			String[] tokens;

			while ((line = in.readLine()) != null) {
				tokens = line.split(",");
				IPRange ip = new IPRange(Long.parseLong(tokens[0]),
						Long.parseLong(tokens[1]), tokens[2]);

				// check if this entry overlaps with any existing entry
				if (hasDuplicate(ip)) {
					continue;
				}

				// OK, add it to the table:
				resolveTable.put(ip, ip);
				countries.add(tokens[2]);
			}
		} finally {
			if (in != null) {
				in.close();
			} else if (inF != null) {
				inF.close();
			}
		}
	}

	private boolean hasDuplicate(IPRange ip) {

		boolean hasDuplicate = false;

		IPRange prev = null;
		IPRange next = null;
		try {
			// +1 because headMap() returns keys that are strictly less
			// than given key, but we want equals as well
			prev = resolveTable.headMap(new IPRange(ip.getFromIP() + 1,
					ip.getToIP() + 1, COUNTRY_UNKNOWN)).lastKey();

			// +1 because tailMap() returns keys that are bigger or
			// equal to given key, but we want strictly bigger ones
			next = resolveTable.tailMap(new IPRange(ip.getFromIP() + 1,
					ip.getToIP() + 1, COUNTRY_UNKNOWN)).firstKey();
		} catch (NoSuchElementException ex) {
			LOG.debug("Failed to check if IP range overlaps with any other: "
					+ ip, ex);
		}

		if (next == null) {
			// if either previous or next could not be fetched, assume there is
			// no dupicate
			hasDuplicate = false;
		} else if((prev.getFromIP() == ip.getFromIP())
				&& (prev.getToIP() == ip.getToIP()))
		{
			// duplicate!
			if (!prev.getCountryCode2().equals(ip.getCountryCode2())) {
				// FIXME this poses a problem - what to do about it?
				// We have two identical ranges, each pointing at
				// a different country. Which one is correct?
				// Current way: keep 1st entry and discharge 2nd,
				// but only if the 1st country is not EU or US.
				// The reason for this is that 1st database
				// generally uses US/EU for various countries within
				// these regions.
				if (prev.getCountryCode2().equals("EU")
						|| prev.getCountryCode2().equals("US"))
				{
					prev.setCountryCode2(ip.getCountryCode2());
				}
				hasDuplicate = true;
			} else {
				// discharge duplicate entry
				hasDuplicate = true;
			}
		} else if ((prev.getFromIP() == ip.getFromIP())
				&& (prev.getToIP() > ip.getToIP()))
		{
			if (!prev.getCountryCode2().equals(ip.getCountryCode2())) {
				// FIXME this poses a problem - what to do about it?
				// Currently, we simply discharge the 2nd entry,
				// hoping that the 1st one is correct and the 2nd
				// was not.
				hasDuplicate = true;
			} else {
				// discharge duplicate subrange
				hasDuplicate = true;
			}
		} else if ((prev.getFromIP() == ip.getFromIP())
				&& (prev.getToIP() < ip.getToIP()))
		{
			if (!prev.getCountryCode2().equals(ip.getCountryCode2())) {
				// FIXME this poses a problem - what to do about it?
				// Currently, we also add the second entry.
				// Since the 1st is narrower, it will stay on top
				// of 2nd one.
				// If some IP does not fit the narrower range,
				// it may still fit the wider one.
			} else {
				// We widen the original range, hopeing the 2nd
				// database is right about this specific range.
				prev.setToIP(ip.getToIP());
				hasDuplicate = true;
			}
		} else if ((prev.getFromIP() < ip.getFromIP())
				&& (prev.getToIP() >= ip.getToIP()))
		{
			if (!prev.getCountryCode2().equals(ip.getCountryCode2())) {
				// FIXME this poses a problem - what to do about it?
				// Currently, we simply discharge the 2nd entry,
				// hoping that the 1st one is correct and the 2nd
				// was not.
				return true;
			} else {
				// discharge duplicate subrange
				hasDuplicate = true;
			}
		} else if ((prev.getFromIP() < ip.getFromIP())
				&& (prev.getToIP() > ip.getFromIP())
				&& (prev.getToIP() < ip.getToIP()))
		{
			// FIXME this poses a problem - what to do about it?
			// Currently we simply discharge the 2nd entry, hoping
			// that the 1st one is correct (and 2nd wasn't)
			hasDuplicate = true;
		} else if ((next.getFromIP() < ip.getToIP())
				&& (next.getToIP() <= ip.getToIP()))
		{
			if (!next.getCountryCode2().equals(ip.getCountryCode2())) {
				// FIXME this poses a problem - what to do about it?
				// Currently, we also add the second entry.
				// Since the 1st is narrower, it will stay on top
				// of 2nd one.
				// If some IP does not fit the narrower range,
				// it may still fit the wider one.
			} else {
				// We widen the original range, hopeing the 2nd
				// database is right about this specific range.
				next.setToIP(ip.getToIP());
				hasDuplicate = true;
			}
		} else if ((next.getFromIP() < ip.getToIP())
				&& (next.getToIP() > ip.getToIP()))
		{
			// FIXME this poses a problem - what to do about it?
			// Currently, we simply discharge the 2nd entry,
			// hoping that the 1st one is correct and the 2nd
			// was not.
			hasDuplicate = true;
		}

		return hasDuplicate;
	}

	/** Will save given IP2County table to disk */
	public void saveDatabase(TreeMap<IPRange, IPRange> resolveTable,
			String fileName) throws IOException
	{
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			for (IPRange ipRange : resolveTable.values()) {
				out.println(ipRange.toString());
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/** Will replace current "resolve" and "countries" tables with new ones */
	public void assignDatabase(TreeMap<IPRange, IPRange> newResolveTable,
			TreeSet<String> newCountries)
	{
		resolveTable = newResolveTable;
		countries = newCountries;
	}

	public static Locale countryToLocale(String country) {

		String isoCountry = country.equals(COUNTRY_UNKNOWN) ? "" : country;
		return new Locale("", isoCountry);
	}

	/**
	 * Converts an IP address into the corresponding country code in the
	 * lobby protocol standard.
	 * @return 2-chars wide country code, as defined in ISO 3166-1 alpha-2,
	 *   or "XX" if the country is unknown.
	 */
	public String getCountryCode(InetAddress ip) {

		String result = COUNTRY_UNKNOWN;

		try {
			long longIp = Misc.ip2Long(ip);
			// +1 because headMap() returns keys that are strictly less than the
			// given key
			long longIpPlus = longIp + 1;
			IPRange x = resolveTable.headMap(new IPRange(longIpPlus, longIpPlus,
					COUNTRY_UNKNOWN)).lastKey();
			if ((x.getFromIP() <= longIp) && (x.getToIP() >= longIp)) {
				result = x.getCountryCode2();
			}
		} catch (NoSuchElementException ex) {
			LOG.debug("Failed to evaluate country code for IP: "
					+ ip.getHostAddress(), ex);
		}

		// quick fix for some non-standard country codes:
		result = result.toUpperCase();
		if (result.equals("UK")) {
			result = "GB";
		} else if (result.equals("FX")) {
			result = "FR";
		}

		return result;
	}

	/**
	 * Converts an IP address into a Locale, with the language unspecified.
	 * @see #getCountryCode(InetAddress)
	 */
	public Locale getLocale(InetAddress ip) {
		return countryToLocale(getCountryCode(ip));
	}
}
