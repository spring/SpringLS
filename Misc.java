/*
 * Created on 2005.6.16
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.*;
import java.text.*;
import java.net.*;
import java.security.*;

public class Misc {
 	static public final byte EOL = 13;
 	static private String hex = "0123456789ABCDEF"; 
	
	public static String easyDateFormat (String format) {
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(today);
    }
	
	public static String easyDateFormat(long date, String format) {
		Date d = new Date(date);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(d);
	}
	
	/* puts together strings from sl, starting at sl[startIndex] */
	public static String makeSentence(String[] sl, int startIndex) {
		if (startIndex > sl.length-1) return "";
		
		String res = new String(sl[startIndex]);
		for (int i = startIndex+1; i < sl.length; i++) res = res.concat(" " + sl[i]);
		
		return res;
	}
	
	/* returns false if char is not an allowed character in the name of a channel, nickname, username, ... */
	public static boolean isValidChar(char c) {
		if (
		   ((c >= 48) && (c <= 57))  || // numbers
		   ((c >= 65) && (c <= 90))  || // capital letters
		   ((c >= 97) && (c <= 122)) || // letters
		   (c == 95) || // underscore
		   (c == 91) || // left bracket "["
		   (c == 93)    // right bracket "]"
		   ) return true; else return false;
	}
	
	/* returns false if name (of a channel, nickname, username, ...) is invalid */
	public static boolean isValidName(String name) {
		for (int i = 0; i < name.length(); i++) if (!isValidChar(name.charAt(i))) return false;
		return true;
	}

	public static boolean isValidPass(String pass) {
		if (pass.length() < 2) return false;
		if (pass.length() > 30) return false; // md5-base64 encoded passwords require 24 chars
		// we have to allow a bit wider range of possible chars as base64 can produce chars such as +, = and /
		for (int i = 0; i < pass.length(); i++) if ((pass.charAt(i) < 43) || (pass.charAt(i) > 122)) return false;
		return true;
	}
	
	public static String boolToStr(boolean b) {
		if (b) return "1"; else return "0";
	}
	
	public static boolean strToBool(String s) {
		if (s.equals("1")) return true; else return false;
	}

	public static char[] byteToHex(byte b) {
		char[] res = new char[2];
		res[0] = hex.charAt((b & 0xF0) >> 4);
		res[1] = hex.charAt(b & 0x0F);
		return res;
	}
	
	/* this method will return local IP address such as "192.168.1.100" instead of "127.0.0.1".
	 * Found it here: http://forum.java.sun.com/thread.jspa?threadID=619056&messageID=3477258
	 *  */
	public static String getLocalIPAddress() {
		try 
	    {
			Enumeration e = NetworkInterface.getNetworkInterfaces();
	 
	        while(e.hasMoreElements()) {
	        	NetworkInterface netface = (NetworkInterface) e.nextElement();
	            Enumeration e2 = netface.getInetAddresses();
	 
	            while (e2.hasMoreElements()) {
	            	InetAddress ip = (InetAddress) e2.nextElement();
			        if(!ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":")==-1) {
			        	return ip.getHostAddress();
		    	    }
                }
	        }
	    } 
	    catch (Exception e) {
	    	return null;
	    }
	    return null;
	}
	
	public static long IP2Long(String IP) {
		long f1, f2, f3, f4;
		String tokens[] = IP.split("\\.");
		if (tokens.length != 4) return -1;
		try {
			f1 = Long.parseLong(tokens[0]) << 24;
			f2 = Long.parseLong(tokens[1]) << 16;
			f3 = Long.parseLong(tokens[2]) << 8;
			f4 = Long.parseLong(tokens[3]);
			return f1+f2+f3+f4;
		} catch (Exception e) {
			return -1;
		}
		
	}
	
	/* converts time (in milliseconds) to "<x> days, <y> hours and <z> minutes" string */
	public static String timeToDHM(long duration) {
		long temp = duration / (1000 * 60 * 60 * 24);
		String res = temp + " days, ";
		duration -= temp * (1000 * 60 * 60 * 24);
		temp = duration / (1000 * 60 * 60);
		res += temp + " hours and ";
		duration -= temp * (1000 * 60 * 60);
		temp = duration / (1000 * 60);
		res += temp + " minutes";
		return res;
	}
	
	/*
	* copied from: http://www.source-code.biz/snippets/java/3.htm
	*
	* Reallocates an array with a new size, and copies the contents
	* of the old array to the new array.
	* @param oldArray  the old array, to be reallocated.
	* @param newSize   the new array size.
	* @return          A new array with the same contents.
	*/
	public static Object resizeArray (Object oldArray, int newSize) {
	   int oldSize = java.lang.reflect.Array.getLength(oldArray);
	   Class elementType = oldArray.getClass().getComponentType();
	   Object newArray = java.lang.reflect.Array.newInstance(
	         elementType,newSize);
	   int preserveLength = Math.min(oldSize,newSize);
	   if (preserveLength > 0)
	      System.arraycopy (oldArray,0,newArray,0,preserveLength);
	   return newArray; }
	
	/* sorts an array of integers using simple bubble sort algorithm.
	 * Copied from http://en.wikisource.org/wiki/Bubble_sort
	 * */
	public static void bubbleSort(int data[])
	{
	   boolean isSorted;
	   int tempVariable;
	   int numberOfTimesLooped = 0;
	   
	   do
	   {
	      isSorted = true;
	      
	      for (int i = 1; i < data.length - numberOfTimesLooped; i++)
	      {
	         if (data[i] > data[i - 1])
	         {
	            tempVariable = data[i];
	            data[i] = data[i - 1];
	            data[i - 1] = tempVariable;

	            isSorted = false;
	         }
	      }

	      numberOfTimesLooped++;
	   }
	   while (!isSorted);
	}
	
	/* sorts an array of integers plus a parallel Vector of objects
	 * using simple bubble sort. */
	public static void bubbleSort(int data[], Vector list)
	{
	   boolean isSorted;
	   int tempInt;
	   Object tempObj;
	   int numberOfTimesLooped = 0;
	   
	   do
	   {
	      isSorted = true;
	      
	      for (int i = 1; i < data.length - numberOfTimesLooped; i++)
	      {
	         if (data[i] > data[i - 1])
	         {
	            tempInt = data[i];
	            tempObj = list.get(i);
	            data[i] = data[i - 1];
	            list.set(i, list.get(i - 1));
	            data[i - 1] = tempInt;
	            list.set(i - 1, tempObj);

	            isSorted = false;
	         }
	      }

	      numberOfTimesLooped++;
	   }
	   while (!isSorted);
	}	
	
	public static String getHashText(String plainText, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest mdAlgorithm = MessageDigest.getInstance(algorithm);

	    mdAlgorithm.update(plainText.getBytes());

	    byte[] digest = mdAlgorithm.digest();
	    StringBuffer hexString = new StringBuffer();

	    for (int i = 0; i < digest.length; i++) {
	    	plainText = Integer.toHexString(0xFF & digest[i]);

	        if (plainText.length() < 2) {
	            plainText = "0" + plainText;
	        }

	        hexString.append(plainText);
	    }

	    return hexString.toString();
	}
	
	public static byte[] getMD5(String plainText) throws NoSuchAlgorithmException
	{
	    MessageDigest mdAlgorithm = MessageDigest.getInstance("md5");

	    mdAlgorithm.update(plainText.getBytes());

	    byte[] digest = mdAlgorithm.digest();
	    return digest;
	}
	
	// this method encodes plain-text password to md5 hashed one in base-64 form:
	public static String encodePassword(String plainPassword) {
		try {
			return new sun.misc.BASE64Encoder().encode(getMD5(plainPassword));	
		} catch (Exception e) {
			// this should not happen!
			System.out.println("WARNING: Serious error occured: " + e.getMessage());
			TASServer.closeServerAndExit();
			return "";
		}
	}
	
	/* various methods dealing with battleStatus: */
	
	public static int getReadyStatusFromBattleStatus(int battleStatus) {
		return (battleStatus & 0x2) >> 1;
	}
	
	public static int getTeamNoFromBattleStatus(int battleStatus) {
		return (battleStatus & 0x3C) >> 2;
	}	
	
	public static int getAllyNoFromBattleStatus(int battleStatus) {
		return (battleStatus & 0x3C0) >> 6;
	}	
	
	public static int getModeFromBattleStatus(int battleStatus) {
		  return (battleStatus & 0x400) >> 10;
	}
	
	public static int getHandicapFromBattleStatus(int battleStatus) {
		return (battleStatus & 0x3F800) >> 11;
	}	
	
	public static int getTeamColorFromBattleStatus(int battleStatus) {
		return (battleStatus & 0x3C0000) >> 18;
	}
	
	public static int getSyncFromBattleStatus(int battleStatus) {
		return (battleStatus & 0xC00000) >> 22;
	}
	
	public static int getSideFromBattleStatus(int battleStatus) {
		return battleStatus & 0xF000000 >> 24;
	}
	
	public static int setReadyStatusOfBattleStatus(int battleStatus, int ready) {
		return (battleStatus & 0xFFFFFFFD) | (ready << 1);
	}

	public static int setTeamNoOfBattleStatus(int battleStatus, int team) {
		return (battleStatus & 0xFFFFFFC3) | (team << 2);
	}
	  
	public static int setAllyNoOfBattleStatus(int battleStatus, int ally) {
		return (battleStatus & 0xFFFFFC3F) | (ally << 6);
	}

	public static int setModeOfBattleStatus(int battleStatus, int mode) {
		return (battleStatus & 0xFFFFFBFF) | (mode << 10);
	}

	public static int setHandicapOfBattleStatus(int battleStatus, int handicap) {
		return (battleStatus & 0xFFFC07FF) | (handicap << 11);
	}
	
	public static int setTeamColorOfBattleStatus(int battleStatus, int color) {
		return (battleStatus & 0xFFC3FFFF) | (color << 18);
	}
	
	public static int setSyncOfBattleStatus(int battleStatus, int sync) {
		return (battleStatus & 0xFF3FFFFF) | (sync << 22);
	}
	
	public static int setSideOfBattleStatus(int battleStatus, int side) {
		return (battleStatus & 0xF0FFFFFF) | (side << 24);
	}
	
	/* various methods dealing with status: */
	
	public static int getInGameFromStatus(int status) {
		return status & 0x1;
	}
	
	public static int getAwayBitFromStatus(int status) {
		return (status & 0x2) >> 1;
	}
	
	public static int getRankFromStatus(int status) {
		return (status & 0x1C) >> 2;
	}
	
	public static int getAccessFromStatus(int status) {
		return (status & 0x20) >> 5;
	}

	public static int setInGameToStatus(int status, int inGame) {
		return (status & 0xFFFFFFFE) | inGame;
	}
	
	public static int setAwayBitToStatus(int status, int away) {
		return (status & 0xFFFFFFFD) | (away << 1);
	}
	
	public static int setRankToStatus(int status, int rank) {
		return (status & 0xFFFFFFE3) | (rank << 2);
	}

	public static int setAccessToStatus(int status, int access) {
		if ((access < 0) || (access > 1)) {
			System.out.println("Critical error: Invalid use of setAccessToStatus()! Shuting down the server ...");
			TASServer.closeServerAndExit();
		}
		return (status & 0xFFFFFFDF) | (access << 5);
	}
	
}
