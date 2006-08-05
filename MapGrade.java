/*
 * Created on 2006.7.27
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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
	public int grade; // a map grade in range [0..9]

	public MapGrade(String mapHash, int mapGrade) {
		this.hash = mapHash;
		this.grade = mapGrade;
	}
}
