/*
 * Created on 2006.8.4
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

public class GlobalMapGrade {

	public String mapHash;
	public float avGrade = 0;
	public int noVotes = 0;
	
	public GlobalMapGrade(String mapHash) {
		this.mapHash = mapHash;
	}

	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof GlobalMapGrade)) return false;
		return this.mapHash.equals(((GlobalMapGrade)obj).mapHash);
	}
}
