/*
 * Created on 28.12.2005
 */

package com.springrts.chanserv;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * Use methods of this static class rather than System.out.println()!
 *
 * @author Betalord
 */
public class Log {

	/** folder where log files are put */
	static final String LOG_FOLDER = "./logs";

	static Semaphore logToDiskLock = new Semaphore(1, true);

	/** if true, we're in the middle of "part" output */
	private static boolean part = false;
	/** Whether all logs will be saved to the "externalLogFileName" file */
	public static boolean useExternalLogging = false;
	/**
	 * If enableExternalLogging is true,
	 * then this is the file to which log will be saved.
	 */
	public static String externalLogFileName = "";

	private static boolean logToDisk(String text, boolean newLine) {
		return toFile(externalLogFileName, text, newLine);
	}

	/**
	 * Timestamp is automatically added in front of the line.
	 * @param fname file name without path ("./logs" path is automatically added).
	 */
	private static boolean toFile(String fname, String line, boolean newLine) {

		try {
			logToDiskLock.acquire();
			return Misc.appendTextToFile(LOG_FOLDER + "/" + fname, Misc.getUnixTimestamp() + " " + line, newLine);
		} catch (InterruptedException e) {
			return false;
		} finally {
			logToDiskLock.release();
		}
	}

	public static boolean toFile(String fname, String line) {
		return toFile(fname, line, true);
	}


	public static void log(String s) {
		if (part) {
			part = false;
			System.out.println();
			if (useExternalLogging) {
				logToDisk("", true);
			}
		}
		Date date = new Date();
		String out = new SimpleDateFormat("<HH:mm:ss> ").format(date) + s;
		System.out.println(out);
		if (useExternalLogging) {
			logToDisk(out, true);
		}
	}

	@Deprecated
	public static void logPartBegin(String s) {
		if (part) {
			System.out.println();
			if (useExternalLogging) {
				logToDisk("", true);
			}
		}
		part = true;
		Date date = new Date();
		String out = new SimpleDateFormat("<HH:mm:ss> ").format(date) + s;
		System.out.print(out);
		if (useExternalLogging) {
			logToDisk(out, true);
		}
	}

	@Deprecated
	public static void logPartContinue(String s) {
		if (!part) {
			//*** this should not happen. Ignore it?
		}

		System.out.print(s);
		if (useExternalLogging) {
			logToDisk(s, false);
		}
	}

	@Deprecated
	public static void logPartEnd(String s) {
		part = false;
		System.out.println(s);
		if (useExternalLogging) {
			logToDisk(s, true);
		}
	}

	public static void error(String s) {
		//*** perhaps rather use System.err.println ?
		if (part) {
			part = false;
			System.out.println();
			if (useExternalLogging) {
				logToDisk("", true);
			}
		}
		String out = "<$ERROR " + Misc.easyDateFormat("HH:mm:ss") + "> " + s;
		System.out.println(out);
		if (useExternalLogging) {
			logToDisk(out, true);
		}
	}

	public static void debug(String s) {
		if (part) {
			part = false;
			System.out.println();
			if (useExternalLogging) {
				logToDisk("", true);
			}
		}
		String out = "<$DEBUG " + Misc.easyDateFormat("HH:mm:ss") + "> " + s;
		System.out.println(out);
		if (useExternalLogging) {
			logToDisk(out, true);
		}
	}
}
