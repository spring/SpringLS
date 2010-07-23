/*
 * Created on 5.1.2006
 */

package com.springrts.chanserv;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * @author Betalord
 */
public class Misc {

 	public static final String EOL = "\n";
 	private static String hex = "0123456789ABCDEF";


	private Misc() {
	}


	public static String easyDateFormat(String format) {

		Date today = new Date();
		DateFormat formatter = new SimpleDateFormat(format);
		String datenewformat = formatter.format(today);
		return datenewformat;
	}

	/** Returns a Unix-like timestamp */
	public static long getUnixTimestamp() {
		return (System.currentTimeMillis() / 1000);
	}

	/** Puts together strings from sl, starting at sl[startIndex] */
	public static String makeSentence(String[] sl, int startIndex) {

		if (startIndex > (sl.length - 1)) {
			return "";
		}

		StringBuilder res = new StringBuilder(sl[startIndex]);
		for (int i = startIndex+1; i < sl.length; i++) {
			res.append(" ").append(sl[i]);
		}

		return res.toString();
	}
	public static String makeSentence(List<String> sl, int startIndex) {

		if (startIndex > (sl.size() - 1)) {
			return "";
		}

		StringBuilder res = new StringBuilder(sl.get(startIndex));
		for (int i = startIndex+1; i < sl.size(); i++) {
			res.append(" ").append(sl.get(i));
		}

		return res.toString();
	}

	public static String boolToStr(boolean b) {
		return (b ? "1" : "0");
	}

	public static boolean strToBool(String s) {
		return s.equals("1");
	}

	public static char[] byteToHex(byte b) {

		char[] res = new char[2];

		res[0] = hex.charAt((b & 0xF0) >> 4);
		res[1] = hex.charAt(b & 0x0F);

		return res;
	}

	/**
	 * Converts time (in milliseconds) to
	 * "<x> days, <y> hours and <z> minutes" string
	 */
	public static String timeToDHM(long duration) {

		long days = duration / (1000 * 60 * 60 * 24);

		duration -= days * (1000 * 60 * 60 * 24);
		long hours = duration / (1000 * 60 * 60);

		duration -= hours * (1000 * 60 * 60);
		long minutes = duration / (1000 * 60);

		String res = String.format(
				"%i days, %i hours and %i minutes",
				days, hours, minutes);

		return res;
	}

	public static boolean appendTextToFile(String fname, String text, boolean newLine) {

		try {
			PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fname, true)));
			if (newLine) {
				out.println(text);
			} else {
				out.print(text);
			}
			out.close();
		} catch (Exception e) {
			// TODO: log
			return false;
		}
		return true;
	}

	/** Creates a string consisting of the specified amount of spaces */
	public static String enumSpaces(int len) {

		String result = "";
		for (int i = 0; i < len; i++) {
			result += " ";
		}
		return result;
	}

	/**
	 * Returns <code>false</code> if char is not an allowed character
	 * in the name of a channel, username, ...
	 */
	@Deprecated
	private static boolean isValidChar(char c) {

		return (
				((c >= '0') && (c <= '9'))  ||
				((c >= 'A') && (c <= 'Z'))  ||
				((c >= 'a') && (c <= 'z')) ||
				(c == '_') ||
				(c == '[') ||
				(c == ']')
				);
	}

	/**
	 * Returns <code>false</code> if name (of a channel, username, ...)
	 * is invalid
	 */
	public static boolean isValidName(String name) {

		return (name.length() > 0) && name.matches("^[0-9a-zA-Z_\\[\\]]*$");
		/*for (int i = 0; i < name.length(); i++) {
			if (!isValidChar(name.charAt(i))) {
				return false;
			}
		}

		return true;*/
	}
}
