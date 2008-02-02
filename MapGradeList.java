/*
 * Created on 2006.7.27
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Note: MapGradeList class is THREAD-SAFE - make sure you keep it so 
 * since thread-safety is needed as saveAccounts() method saves
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
	private Vector<MapGrade> list; // must be thread-safe list in order to ensure class'es thread-safety

	public MapGradeList() {
		list = new Vector<MapGrade>();
	}
	
	public synchronized boolean add(MapGrade mg) {
		return list.add(mg);
	}
	
	public synchronized boolean remove(MapGrade mg) {
		return list.remove(mg);
	}
	
	public MapGrade elementAt(int index) {
		try {
			return list.elementAt(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public int indexOf(Object elem) {
		return list.indexOf(elem);
	}
	
	public int size() {
		return list.size();
	}
	
	public synchronized MapGrade findMapGrade(String mapHash) {
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).hash.equals(mapHash)) return list.get(i);
		return null;
	}

	public synchronized String toString() {
		if (list.size() == 0) return "";
		else {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < list.size(); i++) {
				result.append(list.get(i).toString());
				result.append(i == list.size()-1 ? "" : " ");
			}
			return result.toString();
		}
	}
	
	public static MapGradeList createFromString(String s) {
		if (s.equals("")) return null;
		String[] tokens = s.split(" ");
		if (tokens.length % 3 != 0) return null;
		MapGradeList result = new MapGradeList();
		try {
			for (int i = 0; i < tokens.length / 3; i++) {
				result.add(new MapGrade(tokens[i*3], Integer.parseInt(tokens[i*3+1]), Integer.parseInt(tokens[i*3+2])));
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return result;
	}
	
}
