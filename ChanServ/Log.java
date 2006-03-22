/*
 * Created on 28.12.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Use methods of this static class rather than System.out.println()! 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	
	private static boolean part = false; // if true, we're in the middle of "part" output
	public static boolean useExternalLogging = false; // if set to true, then all logs will be saved to "externalLogFileName" file
	public static String externalLogFileName = ""; // if enableExternalLogging is true, then this is the file to which log will be saved
	
	private static boolean logToDisk(String text, boolean newLine) {
		return Misc.outputLog(externalLogFileName, text, newLine);		
	}
	
	public static void log(String s) {
		if (part) {
			part = false;
			System.out.println();
			if (useExternalLogging) logToDisk("", true);
		}
		Date date = new Date();
		String out = new SimpleDateFormat("<HH:mm:ss> ").format(date) + s; 
		System.out.println(out);
		if (useExternalLogging) logToDisk(out, true);
	}
	
	public static void logPartBegin(String s) {
		if (part) {
			System.out.println();
			if (useExternalLogging) logToDisk("", true);
		}
		part = true;
		Date date = new Date();
		String out = new SimpleDateFormat("<HH:mm:ss> ").format(date) + s;
		System.out.print(out);
		if (useExternalLogging) logToDisk(out, true);
	}
	
	public static void logPartContinue(String s) {
		if (!part) {
			//*** this should not happen. Ignore it?
		}
		
		System.out.print(s);
		if (useExternalLogging) logToDisk(s, false);
	}

	public static void logPartEnd(String s) {
		part = false;
		System.out.println(s);
		if (useExternalLogging) logToDisk(s, true);
	}

	public static void error(String s) {
		//*** perhaps rather use System.err.println ?
		if (part) {
			part = false;
			System.out.println();
			if (useExternalLogging) logToDisk("", true);
		}
		String out = "<$ERROR " + Misc.easyDateFormat("HH:mm:ss") + "> " + s;
		System.out.println(out);
		if (useExternalLogging) logToDisk(out, true);
	}

	public static void debug(String s) {
		if (part) {
			part = false;
			System.out.println();
			if (useExternalLogging) logToDisk("", true);
		}
		String out = "<$DEBUG " + Misc.easyDateFormat("HH:mm:ss") + "> " + s; 
		System.out.println(out);
		if (useExternalLogging) logToDisk(out, true);
	}
}
