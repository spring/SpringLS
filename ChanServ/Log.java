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
	
	public static void log(String s) {
		if (part) {
			part = false;
			System.out.println();
		}
		Date date = new Date();
		System.out.println(new SimpleDateFormat("<HH:mm:ss> ").format(date) + s);
	}
	
	public static void logPartBegin(String s) {
		if (part) {
			System.out.println();
		}
		part = true;
		Date date = new Date();
		System.out.print(new SimpleDateFormat("<HH:mm:ss> ").format(date) + s);
	}
	
	public static void logPartContinue(String s) {
		if (!part) {
			//*** this should not happen. Ignore it?
		}
		
		System.out.print(s);
	}

	public static void logPartEnd(String s) {
		part = false;
		System.out.println(s);
	}

	public static void error(String s) {
		//*** perhaps rather use System.err.println ?
		if (part) {
			part = false;
			System.out.println();
		}
		System.out.println("<$ERROR " + Misc.easyDateFormat("HH:mm:ss") + "> " + s);
	}

	public static void debug(String s) {
		if (part) {
			part = false;
			System.out.println();
		}
		System.out.println("<$DEBUG " + Misc.easyDateFormat("HH:mm:ss") + "> " + s);
	}
}
