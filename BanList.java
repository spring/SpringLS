import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

/*
 * Created on 9.11.2005
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

public class BanList {
	private Vector ips;
	private Vector reason;
	
	public BanList() {
		ips = new Vector();
		reason = new Vector();
	}
	
	public void clear() {
		ips.clear();
		reason.clear();
	}
	
	public void add(String IP, String reason) {
		this.ips.add(new String(IP));
		this.reason.add(new String(reason));
	}
	
	public boolean remove(String IP) {
		for (int i = 0; i < ips.size(); i++)
			if ((ips.get(i)).equals(IP)) {
				ips.remove(i);
				reason.remove(i);
				return true;
			}
		return false;	
	}
	
	public int size() {
		return ips.size();
	}
	
	public String getIP(int index) {
		if (index > ips.size()-1) return ""; else return (String)ips.get(index);
	}
	
	public String getReason(int index) {
		if (index > reason.size()-1) return ""; else return (String)reason.get(index);
	}

	public String getItem(int index) {
		if (index > ips.size()-1) return ""; else return ips.get(index) + " " + (String)reason.get(index);
	}
	
	public void loadFromFile(String filename)
	{
		clear();
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
            while ((line = in.readLine()) != null) {
            	if (line.equals("")) continue;
            	String[] ss = line.split(" ");
            	if (ss.length < 2) add(ss[0], ""); else add(ss[0], Misc.makeSentence(ss, 1));            	
	        }
            in.close();
		} catch (IOException e) {
			System.out.println("Couldn't find " + filename + ". Ban list is empty.");
			return ;
		}
		System.out.println("Ban list loaded from: " + filename + ".");
	}
	
	public boolean saveToFile(String filename)
	{
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));			

			for (int i = 0; i < this.size(); i++)
			{
				out.println(this.getIP(i) + " " + this.getReason(i));
			}
			
			out.close();
			
		} catch (IOException e) {
			System.out.println("IOException error while trying to write ban list to " + filename + "!");
			return false;
		}
		
		return true;
	}	
	
	public boolean banned(String IP) {
		return getIndex(IP) != -1;
	}

	public int getIndex(String IP) {
		String[] sp1 = IP.split("\\.");
		if (sp1.length != 4) return -1;
		
		for (int i = 0; i < this.size(); i++)
		{
			String[] sp2 = ((String)ips.get(i)).split("\\.");
			if (sp2.length != 4) continue; // invalid entry
			
			if (!sp2[0].equals("*")) if (!sp2[0].equals(sp1[0])) continue;
			if (!sp2[1].equals("*")) if (!sp2[1].equals(sp1[1])) continue;
			if (!sp2[2].equals("*")) if (!sp2[2].equals(sp1[2])) continue;
			if (!sp2[3].equals("*")) if (!sp2[3].equals(sp1[3])) continue;
			return i;
		}
		return -1;
	}

	
}
