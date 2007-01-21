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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.text.*;
import java.io.*;
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
	
	/* puts together strings from 'a', starting at a[startIndex] 
	 * see http://leepoint.net/notes-java/data/strings/96string_examples/example_arrayToString.html
	 * on why StringBuffer is faster.
	 * */
	public static String makeSentence(String[] a, int startIndex) {
		if (startIndex > a.length-1) return "";
		
	    StringBuffer result = new StringBuffer();
        result.append(a[startIndex]);
        for (int i = startIndex+1; i < a.length; i++) {
            result.append(" ");
            result.append(a[i]);
        }
        
	    return result.toString();		
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
	
	/* int to hex, with leading zeroes and uppercase (http://www.rgagnon.com/javadetails/java-0004.html) */
	public static String intToHex(int i) {
		return Integer.toHexString(0x10000 | i).substring(1).toUpperCase();
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
	   return newArray; 
	}
	
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
	
	/* sorts an array of integers plus a parallel List of objects
	 * using simple bubble sort. This is a generic method: 
	 * http://java.sun.com/docs/books/tutorial/extra/generics/methods.html */
	public static <T> void bubbleSort(int data[], List<T> list)
	{
	   boolean isSorted;
	   int tempInt;
	   T tempObj;
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
	
	/* Will decompress ZIP archive to current folder.
	 * Code copied from here: http://www.rgagnon.com/javadetails/java-0067.html
	 * and slightly modified. */
	public static void unzipArchive(String fileName) throws IOException {
		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		ZipEntry e;

		while((e = zin.getNextEntry()) != null) {
			// unzip specific file from the archive:
			unzipSingleEntry(zin, e.getName());
		}
		
		zin.close();
	}

	/* will unzip only first entry from the archive file and save it as localFileName.
	 * If no file is found inside the archive, it will simply ignore it. */
	public static void unzipSingleArchive(String fileName, String localFileName) throws IOException {
		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		if (zin.getNextEntry() != null) unzipSingleEntry(zin, localFileName);
		zin.close();
	}
	
	/* will unzip next entry from given ZipInputStream */
	public static void unzipSingleEntry(ZipInputStream zin, String toFile) throws IOException {
		FileOutputStream out = new FileOutputStream(toFile);
		byte [] b = new byte[512];
		int len = 0;
		while ((len = zin.read(b)) != -1) {
			out.write(b, 0, len);
		}
		out.close();
	}	
	
	/* downloads a file from the given url and saves it to disk to specified file.
	 * 'downloadLimit' is specified in bytes per second (use 0 for unlimited) - this is 
	 * the maximum rate at which this method will attempt to download the file.
	 * Returns number of bytes written if it succeeds.
	 * Original code copied from: http://schmidt.devlib.org/java/file-download.html#source */
	public static long download(String address, String localFileName, int downloadLimit) throws MalformedURLException, IOException {
		long numWritten = 0;
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		
		// we will regulate download speed within 1 second time frames and always 
		// wait after the 1st read of the receive buffer in a new time frame 
		// for exactly 1 millisecond. That is neeeded because read() method
		// may return very quickly (in 0 ms, and even multiple times in 0 ms),
		// if we would calculate download speed for such very short time periods
		// we could get enormous data rate caused by dividing something very large 
		// with something very small, which we don't want (that's an error) - we
		// also want to avoid division by zero at the same time.

		long lastTimeStamp = System.nanoTime() / 1000000;
		int bytesSinceLastTimeStamp = 0;
		
		try {
			URL url = new URL(address);
			out = new BufferedOutputStream(
				new FileOutputStream(localFileName));
			conn = url.openConnection();
			in = conn.getInputStream();
			byte[] buffer = new byte[1024];
			int numRead = 0;
			
			while ((numRead = in.read(buffer)) != -1) {
				// limit download speed:
				if (downloadLimit > 0) {
					bytesSinceLastTimeStamp += numRead;
					long timeDiff = System.nanoTime() / 1000000 - lastTimeStamp;
					if (timeDiff > 0) { // to avoid division by zero
						double rate = (double)bytesSinceLastTimeStamp / (double)timeDiff * 1000.0;
						if (rate > downloadLimit)
							try {
						    	// sleep a bit:
								Thread.sleep(Math.round((double)bytesSinceLastTimeStamp / (double)downloadLimit * 1000.0 - timeDiff));
							} catch (InterruptedException ie) { }
	 					
						// check if we must start a new time frame:
						if (timeDiff > 1000) { 
							// start new time frame
							lastTimeStamp = System.nanoTime() / 1000000;
							bytesSinceLastTimeStamp = 0;
						}
					} else try { Thread.sleep(1); } catch (InterruptedException ie) { } // we need this because we don't check time between 0 and 1st millisecond in a time frame, but in the first millisecond a lot of data may be read from the socket buffer which we don't want because we can't regulate download speed accurately in that case
					
				}

				// write received data to file:
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
		return numWritten;
	}
	
	// see extended version of this method for more info!
	public static long download(String address, String localFileName) throws MalformedURLException, IOException {
		return download(address, localFileName, 0);
	}
	
	public static boolean deleteFile(String fileName) {
		return (new File(fileName)).delete();
	}
	
	/* this method is thread-safe (or at least it is if not called from multiple threads with same Exception object)
	 * and must remain such since multiple threads may call it. */
	public static String exceptionToFullString(Exception e) {
		String res = e.toString();
			             
		StackTraceElement[] trace = e.getStackTrace();
		for (int i = 0; i < trace.length; i++)
			res += "\r\n\tat " + trace[i];
		
		return res;
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
	
	public static int setSyncOfBattleStatus(int battleStatus, int sync) {
		return (battleStatus & 0xFF3FFFFF) | (sync << 22);
	}
	
	public static int setSideOfBattleStatus(int battleStatus, int side) {
		return (battleStatus & 0xF0FFFFFF) | (side << 24);
	}
	
}
