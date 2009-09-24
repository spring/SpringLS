/*
 * Created on 2005.9.1
 */

package com.springrts.tasserver;


import java.util.*;
import java.io.*;

/**
 * @author Betalord
 */
public class IP2Country {

	static private TreeSet<String> countries = new TreeSet<String>();
	static private TreeMap<IPRange, IPRange> resolveTable = new TreeMap<IPRange, IPRange>();

	public static boolean updateInProgress() {
		return UpdateIP2CountryThread.inProgress();
	}

	public static boolean initializeAll(String filename) {
		try {
			buildDatabase(filename, resolveTable, countries);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * Will download appropriate IP2COUNTRY database from the internet
	 * and update local database. Returns immediately (updating is done
	 * in a background thread).
	 */
	public static void updateDatabase() {
		if (UpdateIP2CountryThread.inProgress()) return; // update already in progress. Let's just skip it ...

		Thread tmp = new UpdateIP2CountryThread();
		tmp.setPriority(Thread.NORM_PRIORITY - 2);
		tmp.start();
	}

	/**
	 * Will build IP2Country database from a custom format IP2Country file which is
	 * produced by the UpdateIP2CountryThread class. Results are saved into 'resolveTable'
	 * and 'countries' objects.
	 */
	public static void buildDatabase(String fromFile, TreeMap<IPRange, IPRange> resolveTable, TreeSet<String> countries) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fromFile));

		countries.clear();
		resolveTable.clear();

		String line;
		String tokens[];

		while ((line = in.readLine()) != null) {
			tokens = line.split(",");
			IPRange ip = new IPRange(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]), tokens[2]);
			resolveTable.put(ip, ip);
			countries.add(tokens[2]);
		}

		in.close();
	}

	/**
	 * Same as buildDatabase() except that it uses several check for consistency and
	 * automatic merging / filtering of duplicate entries, which makes it much slower.
	 * Should be called only from a separate thread when updating the database.
	 */
	public static void buildDatabaseSafe(String fromFile, TreeMap<IPRange, IPRange> resolveTable, TreeSet<String> countries) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fromFile));

		countries.clear();
		resolveTable.clear();

		String line;
		String tokens[];

		while ((line = in.readLine()) != null) {
			tokens = line.split(",");
			IPRange ip = new IPRange(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]), tokens[2]);

			// check if this entry overlaps with any existing entry:
			try {
				// try to find a duplicate:
				IPRange prev = resolveTable.headMap(new IPRange(ip.IP_FROM + 1, ip.IP_TO + 1, "XX")).lastKey(); // +1 because headMap() returns keys that are strictly less than given key, but we want equals as well
				IPRange next = resolveTable.tailMap(new IPRange(ip.IP_FROM + 1, ip.IP_TO + 1, "XX")).firstKey(); // +1 because tailMap() returns keys that are bigger or equal to given key, but we want strictly bigger ones

				if ((prev.IP_FROM == ip.IP_FROM) && (prev.IP_TO == ip.IP_TO)) {
					// duplicate!
					if (!prev.COUNTRY_CODE2.equals(ip.COUNTRY_CODE2)) {
						// this poses a problem - we have two identical ranges each pointing to a different country. Which one is correct?
						// Current way: keep 1st entry and discharge 2nd, but only if the 1st country is not EU or US (reason for this is that 1st database generally uses US/EU for various countries within these regions):
						if (prev.COUNTRY_CODE2.equals("EU") || prev.COUNTRY_CODE2.equals("US")) prev.COUNTRY_CODE2 = ip.COUNTRY_CODE2;
						continue;
					}
					else continue; // discharge duplicate entry
				}
				else if ((prev.IP_FROM == ip.IP_FROM) && (prev.IP_TO > ip.IP_TO)) {
					if (!prev.COUNTRY_CODE2.equals(ip.COUNTRY_CODE2)) {
						// this poses a problem - what to do about it?
						// Currently we simply discharge the 2nd entry, hoping that the 1st one is correct (and 2nd wasn't)
						continue;
					}
					else continue; // discharge duplicate subrange
				}
				else if ((prev.IP_FROM == ip.IP_FROM) && (prev.IP_TO < ip.IP_TO)) {
					if (!prev.COUNTRY_CODE2.equals(ip.COUNTRY_CODE2)) {
						// this poses a problem - what to do about it?
						// Currently we also add the second entry, and since the 1st is narrower it will stay on top of 2nd one (if some IP doesn't fit the narrower range, it may still fit the wider one).
					} else {
						// we widen the original range (hopefully the 2nd database is right about this specific range):
						prev.IP_TO = ip.IP_TO;
						continue;
					}
				}
				else if ((prev.IP_FROM < ip.IP_FROM) && (prev.IP_TO >= ip.IP_TO)) {
					if (!prev.COUNTRY_CODE2.equals(ip.COUNTRY_CODE2)) {
						// this poses a problem - what should we do about it?
						// Currently we simply discharge the 2nd entry, hoping that the 1st one is correct (and 2nd wasn't)
						continue;
					}
					else continue; // discharge duplicate subrange
				}
				else if ((prev.IP_FROM < ip.IP_FROM) && (prev.IP_TO > ip.IP_FROM) && (prev.IP_TO < ip.IP_TO)) {
					// this poses a problem - what should we do about it?
					// Currently we simply discharge the 2nd entry, hoping that the 1st one is correct (and 2nd wasn't)
					continue;
				}
				else if ((next.IP_FROM < ip.IP_TO) && (next.IP_TO <= ip.IP_TO)) {
					if (!next.COUNTRY_CODE2.equals(ip.COUNTRY_CODE2)) {
						// this poses a problem - what should we do about it?
						// Currently we also add the second entry, and since the 1st is narrower it will stay on top of 2nd one (if some IP doesn't fit the narrower range, it may still fit the wider one).
					}
					else {
						// we widen the original range (hopefully the 2nd database is right about this specific range):
						next.IP_TO = ip.IP_TO;
						continue;
					}
				}
				else if ((next.IP_FROM < ip.IP_TO) && (next.IP_TO > ip.IP_TO)) {
					// this poses a problem - what should we do about it?
					// Currently we simply discharge the 2nd entry, hoping that the 1st one is correct (and 2nd wasn't)
					continue;
				}

			} catch (NoSuchElementException e) { }

			// ok add it to the table:
			resolveTable.put(ip, ip);
			countries.add(tokens[2]);
		}

		in.close();
	}

	/** Will save given IP2County table to disk */
	public static void saveDatabase(TreeMap resolveTable, String fileName) throws IOException {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			for (Iterator it = resolveTable.values().iterator(); it.hasNext(); ) {
				out.println(it.next().toString());
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/** Will replace current "resolve" and "countries" tables with new ones */
	public static void assignDatabase(TreeMap<IPRange, IPRange> newResolveTable, TreeSet<String> newCountries) {
		resolveTable = newResolveTable;
		countries = newCountries;
	}

	/** For a given IP address it returns corresponding country code (2-chars wide) */
	public static String getCountryCode(long IP) {
		String result = "XX";
		try {
			IPRange x = resolveTable.headMap(new IPRange(IP+1, IP+1, "XX")).lastKey(); // +1 because headMap() returns keys that are strictly less than given key
			if ((x.IP_FROM <= IP) && (x.IP_TO >= IP)) result = x.COUNTRY_CODE2;
		} catch (NoSuchElementException e) {
			// do nothing
		}

		// quick fix for some non-standard country codes:
		result = result.toUpperCase();
		if (result.equals("UK")) result = "GB";
		if (result.equals("FX")) result = "FR";

		return result;
	}
}
