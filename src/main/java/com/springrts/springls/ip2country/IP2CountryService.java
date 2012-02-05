/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.ip2country;


import com.springrts.springls.util.ProtocolUtil;
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
 * @author hoijui
 */
public interface IP2CountryService {

//	public static final String COUNTRY_UNKNOWN = "XX";

//	public File getDataFile() {
//		return dataFile;
//	}
//
//	public void setDataFile(File dataFile) {
//		this.dataFile = dataFile;
//	}

//	public boolean updateInProgress() {
//		return ((updater != null) && updater.inProgress());
//	}
//
//	public boolean initializeAll() {
//
//		try {
//			buildDatabase(dataFile, resolveTable, countries);
//			LOG.info("Using IP2Country info from file '{}'.",
//					dataFile.getAbsolutePath());
//		} catch (IOException ex) {
//			LOG.warn("Could not find or read from file '{}'; reason: {}."
//					+ " -> Not using any IP2Country info.",
//					dataFile.getAbsolutePath(), ex.getMessage());
//			return false;
//		}
//
//		return true;
//	}
//
//	/**
//	 * Will download appropriate IP2COUNTRY database from the Internet
//	 * and update local database. Returns immediately (updating is done
//	 * in a background thread).
//	 */
//	public void updateDatabase() {
//
//		if (updateInProgress()) {
//			// update already in progress. Let's just skip it ...
//			return;
//		}
//
//		updater = new UpdateIP2CountryThread(this);
//		updateThread = new Thread(updater);
//		updateThread.setPriority(Thread.NORM_PRIORITY - 2);
//		updateThread.start();
//	}

//	/**
//	 * Will build IP2Country database from a custom format IP2Country file which
//	 * is produced by the UpdateIP2CountryThread class.
//	 * Results are saved into 'resolveTable' and 'countries' objects.
//	 */
//	public void buildDatabase(File from, TreeMap<IPRange, IPRange> resolveTable,
//			TreeSet<String> countries) throws IOException;
//
//	/**
//	 * Same as buildDatabase() except that it uses several check for consistency
//	 * and automatic merging / filtering of duplicate entries,
//	 * which makes it much slower.
//	 * Should be called only from a separate thread when updating the database.
//	 */
//	public void buildDatabaseSafe(String fromFile,
//			TreeMap<IPRange, IPRange> resolveTable, TreeSet<String> countries)
//			throws IOException;

//	/** Will save given IP2County table to disk */
//	public void saveDatabase(TreeMap<IPRange, IPRange> resolveTable,
//			String fileName) throws IOException;

//	/** Will replace current "resolve" and "countries" tables with new ones */
//	public void assignDatabase(TreeMap<IPRange, IPRange> newResolveTable,
//			TreeSet<String> newCountries);

//	public Locale countryToLocale(String country);

	/**
	 * Converts an IP address into the corresponding country code in the
	 * lobby protocol standard.
	 * @return 2-chars wide country code, as defined in ISO 3166-1 alpha-2,
	 *   or "XX" if the country is unknown.
	 */
	public String getCountryCode(InetAddress ip);

	/**
	 * Converts an IP address into a Locale, with the language unspecified.
	 * @see #getCountryCode(InetAddress)
	 */
	public Locale getLocale(InetAddress ip);
}
