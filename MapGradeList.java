/*
 * Created on 2006.7.27
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Note: MapGradeList class is THREAD-SAFE! Make sure you don't break
 * its thread-safety since it is needed as saveAccounts() method saves
 * accounts in a separate thread.
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.*;

public class MapGradeList {
	private Vector/*MapGrade*/ list; // must be thread-safe list in order to ensure class'es thread-safety

	public MapGradeList() {
		list = new Vector();
	}
	
	public synchronized boolean add(MapGrade mg) {
		return list.add(mg);
	}
	
	public synchronized boolean remove(MapGrade mg) {
		return list.remove(mg);
	}
	
	public MapGrade elementAt(int index) {
		return (MapGrade)list.elementAt(index);
	}
	
	public int indexOf(Object elem) {
		return list.indexOf(elem);
	}
	
	public int size() {
		return list.size();
	}
	
	public synchronized MapGrade findMapGrade(String mapHash) {
		for (int i = 0; i < list.size(); i++)
			if (((MapGrade)list.get(i)).hash.equals(mapHash)) return (MapGrade)list.get(i);
		return null;
	}

	public synchronized String toString() {
		if (list.size() == 0) return "";
		else {
			String result = "";
			for (int i = 0; i < list.size(); i++) {
				result = result + ((MapGrade)list.get(i)).hash + " " + ((MapGrade)list.get(i)).grade + (i == list.size()-1 ? "" : " ");
			}
			return result;
		}
	}
	
	public static MapGradeList createFromString(String s) {
		if (s.equals("")) return null;
		String[] tokens = s.split(" ");
		if (tokens.length % 2 != 0) return null;
		MapGradeList result = new MapGradeList();
		try {
			for (int i = 0; i < tokens.length / 2; i++) {
				result.add(new MapGrade(tokens[i*2], Integer.parseInt(tokens[i*2+1])));
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return result;
	}
	
}
