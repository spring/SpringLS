/*
 * Created on 2006.7.27
 *
 * - Since version 0.31 we are also keeping the time user has spent playing the map
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class MapGrade {
	public String hash; // 32-bit map hash in hexadecimal form
	public int grade; // a map grade in range [0..10], where 0 means user hasn't graded the map yet
	public int mins; // number of minutes that user spent playing this map in total 

	public MapGrade(String mapHash, int mapGrade, int mins) {
		this.hash = mapHash;
		this.grade = mapGrade;
		this.mins = mins;
	}
	
	public String toString() {
		return hash + " " + grade + " " + mins; 
	}	
}

