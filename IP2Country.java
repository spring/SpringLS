import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

/*
 * Created on 2005.9.1
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
public class IP2Country {
	
	public static boolean initialized = false;
 	static public Vector countries = new Vector();
 	static private IPRange[] resolveTable;
	
	public static boolean initializeAll(String filename) {
		int count = 0;
		int allCount = 0;
		resolveTable = new IPRange[1];
		resolveTable[0] = new IPRange(0, 0, "??", "???", "Unknown");
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			
			String line;
			String tokens[];
			int found;
		
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	if (line.charAt(0) == '#') continue; 
            	tokens = line.split(",");
            	if (tokens.length != 7) {
            		String fixed = tokens[6];
            		for (int i = 7; i < tokens.length; i++)
            			fixed = fixed + "," + tokens[i];
            		tokens[6] = fixed;
            	}
            	
            	found = -1;
            	for (int i = 0; i < countries.size(); i++)
            		if (((String)countries.get(i)).equals(tokens[4].substring(1, tokens[4].length()-1))) {
            			found = i;
            			break;
            		}
            	if (found == -1) {
            		countries.add(tokens[4].substring(1, tokens[4].length()-1));
            		count++;
            	}
            	
            	allCount++;
	        }
            
            in.close();
			
		} catch (IOException e) {
			// catch possible io errors from readLine()
			initialized = false;
			return initialized;
		}

		System.out.println("There are " + count + " different countries mentioned in " + filename);
		
		resolveTable = new IPRange[allCount+1];
		resolveTable[0] = new IPRange(0, 0, "??", "???", "Unknown");
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			
			String line;
			String tokens[];
			int c = 1;
			
		
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	if (line.charAt(0) == '#') continue;
            	tokens = line.split(",");
            	if (tokens.length != 7) {
            		String fixed = tokens[6];
            		for (int i = 7; i < tokens.length; i++)
            			fixed = fixed + "," + tokens[i];
            		tokens[6] = fixed;
            	}
            	
            	resolveTable[c] = new IPRange(
            			Long.parseLong(tokens[0].substring(1, tokens[0].length()-1)),
            			Long.parseLong(tokens[1].substring(1, tokens[1].length()-1)),
            			tokens[4].substring(1, tokens[4].length()-1),
						tokens[5].substring(1, tokens[5].length()-1),
						tokens[6].substring(1, tokens[6].length()-1)
            	);
            	c++;
	        }
            
            in.close();
			
		} catch (IOException e) {
		}

		initialized = true;
		return initialized;
	}

	public static String getCountryCode(long IP) {
		for (int i = 0; i < resolveTable.length; i++)
			if ((IP >= resolveTable[i].IP_FROM) && (IP <= resolveTable[i].IP_TO)) {
				
				// quick fix for some non-standard country codes:
				if (resolveTable[i].COUNTRY_CODE2.toUpperCase().equals("UK")) return "gb";
				if (resolveTable[i].COUNTRY_CODE2.toUpperCase().equals("FX")) return "fr";
				
				for (int j = 0; j < countries.size(); j++)
					if (((String)countries.get(j)).equals(resolveTable[i].COUNTRY_CODE2)) return (String)countries.get(j);
			}
		return "xx";
	}
	
}
