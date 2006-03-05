/*
 * Created on 5.1.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.*;
import java.text.*;

public class Misc {
 	static public final byte EOL = 13;
 	static private String hex = "0123456789ABCDEF"; 
	
	public static String easyDateFormat (String format) {
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		String datenewformat = formatter.format(today);
		return datenewformat;
    }
	
	/* puts together strings from sl, starting at sl[startIndex] */
	public static String makeSentence(String[] sl, int startIndex) {
		if (startIndex > sl.length-1) return "";
		
		String res = new String(sl[startIndex]);
		for (int i = startIndex+1; i < sl.length; i++) res = res.concat(" " + sl[i]);
		
		return res;
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
	
}